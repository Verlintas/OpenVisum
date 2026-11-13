/*
 * Copyright (C) 2026 Verlintas
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This file is part of OpenVisum.
 *
 * OpenVisum is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenVisum is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * OpenVisum. If not, see <https://www.gnu.org/licenses/>.
 */

import AppKit
import Foundation
import LibVLC

/// Thin wrapper around a libVLC player instance.
@MainActor
final class PlayerCore: ObservableObject {

    struct Track: Identifiable, Hashable {
        let id: Int32
        let name: String
    }

    @Published private(set) var isPlaying = false
    @Published private(set) var isBuffering = false
    @Published private(set) var isEnded = false
    @Published private(set) var timeMs: Int64 = 0
    @Published private(set) var durationMs: Int64 = 0
    @Published private(set) var volume: Int32 = 80
    @Published private(set) var rate: Float = 1
    @Published private(set) var title = ""
    @Published private(set) var audioTracks: [Track] = []
    @Published private(set) var subtitleTracks: [Track] = []
    @Published private(set) var currentAudioTrack: Int32 = -1
    @Published private(set) var currentSubtitleTrack: Int32 = -1
    @Published private(set) var errorMessage: String?

    private var instance: OpaquePointer?
    private var player: OpaquePointer?
    private var currentURL: URL?
    private var timer: Timer?
    private var tickCount = 0
    private weak var videoView: NSView?

    /// Called every few seconds with (url, positionMs, durationMs).
    var onProgress: ((URL, Int64, Int64) -> Void)?

    init() {
        PlayerCore.configurePluginPathIfNeeded()
        ovLog("player: init, creating libVLC")
        instance = libvlc_new(0, nil)
        ovLog("player: libVLC ready (\(instance != nil))")
        if instance == nil {
            errorMessage = "Failed to create the libVLC instance"
        }
        startTimer()
    }

    deinit {
        timer?.invalidate()
        if let player {
            libvlc_media_player_release(player)
        }
        if let instance {
            libvlc_release(instance)
        }
    }

    static func configurePluginPathIfNeeded() {
        if let embedded = Bundle.main.resourceURL?.appendingPathComponent("vlc/plugins"),
           FileManager.default.fileExists(atPath: embedded.path) {
            setenv("VLC_PLUGIN_PATH", embedded.path, 1)
            return
        }
        if let plugins = Bundle.main.object(forInfoDictionaryKey: "VLCPluginsPath") as? String,
           FileManager.default.fileExists(atPath: plugins) {
            setenv("VLC_PLUGIN_PATH", plugins, 1)
        }
    }

    // MARK: - Media

    func attachVideoView(_ view: NSView) {
        videoView = view
        if let player {
            libvlc_media_player_set_nsobject(player, Unmanaged.passUnretained(view).toOpaque())
        }
    }

    func open(url: URL, title explicitTitle: String? = nil, autoplay: Bool = true) {
        guard let instance else { return }
        stopPlayback()
        errorMessage = nil
        isEnded = false
        timeMs = 0
        durationMs = 0

        let media: OpaquePointer?
        if url.isFileURL {
            media = url.path.withCString { libvlc_media_new_path(instance, $0) }
        } else {
            media = url.absoluteString.withCString { libvlc_media_new_location(instance, $0) }
        }
        guard let media else {
            errorMessage = "Cannot open \(url.lastPathComponent)"
            return
        }

        guard let newPlayer = libvlc_media_player_new_from_media(media) else {
            libvlc_media_release(media)
            errorMessage = "Cannot create a media player"
            ovLog("player: cannot create media player for \(url.lastPathComponent)")
            return
        }
        ovLog("player: opened \(url.lastPathComponent)")
        libvlc_media_release(media)

        player = newPlayer
        currentURL = url
        title = explicitTitle ?? url.deletingPathExtension().lastPathComponent
        libvlc_audio_set_volume(newPlayer, volume)
        if rate != 1 {
            libvlc_media_player_set_rate(newPlayer, rate)
        }
        if let videoView {
            libvlc_media_player_set_nsobject(newPlayer, Unmanaged.passUnretained(videoView).toOpaque())
        }
        if autoplay {
            play()
        }
        refreshTracks()
    }

    var currentMediaURL: URL? { currentURL }

    func close() {
        stopPlayback()
        currentURL = nil
        title = ""
        audioTracks = []
        subtitleTracks = []
        isEnded = false
        timeMs = 0
        durationMs = 0
    }

    private func stopPlayback() {
        guard let player else { return }
        libvlc_media_player_stop(player)
        libvlc_media_player_release(player)
        self.player = nil
        isPlaying = false
        isBuffering = false
    }

    // MARK: - Transport

    func play() {
        guard let player else { return }
        libvlc_media_player_play(player)
    }

    func pause() {
        guard let player else { return }
        libvlc_media_player_pause(player)
    }

    func togglePlayPause() {
        guard let player else { return }
        if libvlc_media_player_is_playing(player) == 1 {
            libvlc_media_player_pause(player)
        } else {
            libvlc_media_player_play(player)
        }
    }

    func seek(toMs position: Int64) {
        guard let player else { return }
        let clamped = max(0, min(position, durationMs > 0 ? durationMs : position))
        libvlc_media_player_set_time(player, clamped)
        timeMs = clamped
    }

    func seekBy(_ deltaMs: Int64) {
        seek(toMs: timeMs + deltaMs)
    }

    func setVolume(_ value: Int32) {
        volume = max(0, min(100, value))
        if let player {
            libvlc_audio_set_volume(player, volume)
        }
    }

    func setRate(_ value: Float) {
        rate = value
        if let player {
            libvlc_media_player_set_rate(player, value)
        }
    }

    // MARK: - Tracks

    func refreshTracks() {
        guard let player else { return }
        audioTracks = readTracks(libvlc_audio_get_track_description(player))
        subtitleTracks = readTracks(libvlc_video_get_spu_description(player))
        currentAudioTrack = libvlc_audio_get_track(player)
        currentSubtitleTrack = libvlc_video_get_spu(player)
    }

    func selectAudioTrack(_ id: Int32) {
        guard let player else { return }
        libvlc_audio_set_track(player, id)
        currentAudioTrack = id
    }

    func selectSubtitleTrack(_ id: Int32) {
        guard let player else { return }
        libvlc_video_set_spu(player, id)
        currentSubtitleTrack = id
    }

    func addSubtitleFile(_ url: URL) {
        guard let player else { return }
        url.path.withCString { path in
            libvlc_media_player_add_slave(
                player,
                libvlc_media_slave_type_subtitle,
                path,
                true
            )
        }
        refreshTracks()
    }

    private func readTracks(
        _ head: UnsafeMutablePointer<libvlc_track_description_t>?
    ) -> [Track] {
        guard var node = head else { return [] }
        var tracks: [Track] = []
        defer { libvlc_track_description_list_release(head) }
        while true {
            let value = node.pointee
            let name = value.psz_name.map { String(cString: $0) } ?? "Track \(value.i_id)"
            tracks.append(Track(id: value.i_id, name: name))
            guard let next = value.p_next else { break }
            node = next
        }
        return tracks
    }

    // MARK: - Polling

    private func startTimer() {
        timer?.invalidate()
        let timer = Timer(timeInterval: 0.2, repeats: true) { [weak self] _ in
            Task { @MainActor [weak self] in
                self?.tick()
            }
        }
        RunLoop.main.add(timer, forMode: .common)
        self.timer = timer
    }

    private func tick() {
        tickCount += 1
        guard let player else {
            isPlaying = false
            isBuffering = false
            return
        }
        let state = libvlc_media_player_get_state(player)
        let rawState = Int(state.rawValue)
        isPlaying = libvlc_media_player_is_playing(player) == 1
        isBuffering = rawState == 2 || rawState == 1
        isEnded = rawState == 6
        if rawState == 7 {
            errorMessage = "Playback failed"
        }
        let length = libvlc_media_player_get_length(player)
        if length > 0 {
            durationMs = Int64(length)
        }
        let current = libvlc_media_player_get_time(player)
        if current >= 0 {
            timeMs = Int64(current)
        }
        if tickCount % 5 == 0 {
            refreshTracks()
        }
        if tickCount % 25 == 0, isPlaying, durationMs > 0, let url = currentURL {
            onProgress?(url, timeMs, durationMs)
        }
    }
}
