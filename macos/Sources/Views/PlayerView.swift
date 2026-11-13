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
import SwiftUI
import UniformTypeIdentifiers

struct PlayerView: View {
    @EnvironmentObject private var player: PlayerCore

    let onClose: () -> Void

    @AppStorage("accentTheme") private var accentRaw = AccentTheme.brand.rawValue
    @State private var controlsVisible = true
    @State private var hideTask: Task<Void, Never>?
    @State private var isScrubbing = false
    @State private var scrubValue: Double = 0
    @State private var isSubtitleImporterPresented = false
    @FocusState private var isFocused: Bool

    private var accent: AccentTheme { AccentTheme(rawValue: accentRaw) ?? .brand }
    private var displayedTime: Int64 {
        isScrubbing ? Int64(scrubValue) : player.timeMs
    }

    var body: some View {
        ZStack {
            Color.black
            VideoSurface(player: player)
                .onTapGesture { toggleControls() }

            if player.isBuffering && !player.isEnded {
                ProgressView()
                    .controlSize(.large)
                    .tint(.white)
            }

            if player.isEnded {
                endedOverlay
            }

            if controlsVisible && !player.isEnded {
                chrome
                    .transition(.opacity)
            }
        }
        .ignoresSafeArea()
        .focusable()
        .focused($isFocused)
        .onAppear {
            isFocused = true
            revealControls()
        }
        .onContinuousHover { phase in
            if case .active = phase {
                revealControls()
            }
        }
        .onChange(of: player.isPlaying) { _, playing in
            if playing {
                revealControls()
            } else {
                hideTask?.cancel()
                withAnimation(.easeOut(duration: 0.18)) { controlsVisible = true }
            }
        }
        .onKeyPress(.space) {
            player.togglePlayPause()
            revealControls()
            return .handled
        }
        .onKeyPress(.leftArrow) {
            player.seekBy(-10_000)
            revealControls()
            return .handled
        }
        .onKeyPress(.rightArrow) {
            player.seekBy(10_000)
            revealControls()
            return .handled
        }
        .onKeyPress(.escape) {
            onClose()
            return .handled
        }
        .onDisappear { hideTask?.cancel() }
        .fileImporter(
            isPresented: $isSubtitleImporterPresented,
            allowedContentTypes: [.data],
            allowsMultipleSelection: false
        ) { result in
            if case .success(let urls) = result, let url = urls.first {
                player.addSubtitleFile(url)
            }
        }
    }

    // MARK: - Chrome

    private var chrome: some View {
        VStack {
            topBar
            Spacer()
            bottomBar
        }
        .background(
            VStack(spacing: 0) {
                LinearGradient(
                    colors: [Color.black.opacity(0.66), .clear],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 140)
                Spacer()
                LinearGradient(
                    colors: [.clear, Color.black.opacity(0.78)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 200)
            }
            .allowsHitTesting(false)
        )
    }

    private var topBar: some View {
        HStack(spacing: 12) {
            Button(action: onClose) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(8)
                    .background(Circle().fill(Color.white.opacity(0.12)))
            }
            .buttonStyle(.plain)

            Text(player.title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.white)
                .lineLimit(1)
                .shadow(radius: 8)

            if player.durationMs > 0 {
                Text("\(player.durationMs / 1000 / 60) 分钟")
                    .font(.system(size: 11).monospacedDigit())
                    .foregroundStyle(.white.opacity(0.55))
            }

            Spacer()

            Button {
                NSApp.keyWindow?.toggleFullScreen(nil)
            } label: {
                Image(systemName: "arrow.up.left.and.arrow.down.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.9))
                    .padding(8)
                    .background(Circle().fill(Color.white.opacity(0.1)))
            }
            .buttonStyle(.plain)
            .help("全屏")
        }
        .padding(.horizontal, 18)
        .padding(.top, 46)
    }

    private var bottomBar: some View {
        VStack(spacing: 8) {
            HStack(spacing: 12) {
                Text(formatDuration(displayedTime))
                    .font(.system(size: 11, weight: .medium).monospacedDigit())
                    .foregroundStyle(.white.opacity(0.8))
                    .frame(width: 52, alignment: .trailing)

                Slider(
                    value: Binding(
                        get: { isScrubbing ? scrubValue : Double(player.timeMs) },
                        set: { scrubValue = $0 }
                    ),
                    in: 0...Double(max(player.durationMs, 1)),
                    onEditingChanged: { editing in
                        if editing {
                            scrubValue = Double(player.timeMs)
                            isScrubbing = true
                        } else {
                            player.seek(toMs: Int64(scrubValue))
                            isScrubbing = false
                        }
                        revealControls()
                    }
                )
                .tint(accent.color)

                Text(formatDuration(player.durationMs))
                    .font(.system(size: 11, weight: .medium).monospacedDigit())
                    .foregroundStyle(.white.opacity(0.6))
                    .frame(width: 52, alignment: .leading)
            }

            HStack(spacing: 14) {
                circleButton(systemName: "gobackward.10", help: "后退 10 秒") {
                    player.seekBy(-10_000)
                    revealControls()
                }

                Button {
                    player.togglePlayPause()
                    revealControls()
                } label: {
                    Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                        .font(.system(size: 19, weight: .bold))
                        .foregroundStyle(Color(red: 0.06, green: 0.07, blue: 0.09))
                        .frame(width: 46, height: 46)
                        .background(Circle().fill(Color.white.opacity(0.94)))
                }
                .buttonStyle(.plain)

                circleButton(systemName: "goforward.10", help: "前进 10 秒") {
                    player.seekBy(10_000)
                    revealControls()
                }

                Spacer()

                HStack(spacing: 6) {
                    Image(systemName: player.volume == 0 ? "speaker.slash.fill" : "speaker.wave.2.fill")
                        .font(.system(size: 11))
                        .foregroundStyle(.white.opacity(0.75))
                    Slider(
                        value: Binding(
                            get: { Double(player.volume) },
                            set: { player.setVolume(Int32($0)) }
                        ),
                        in: 0...100
                    )
                    .tint(accent.color)
                    .frame(width: 76)
                }

                rateMenu

                if player.audioTracks.count > 1 {
                    audioMenu
                }

                subtitleMenu
            }
            .padding(.top, 2)
        }
        .padding(.horizontal, 22)
        .padding(.bottom, 26)
    }

    private func circleButton(
        systemName: String,
        help: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(.white.opacity(0.92))
                .frame(width: 38, height: 38)
                .background(Circle().fill(Color.white.opacity(0.1)))
        }
        .buttonStyle(.plain)
        .help(help)
    }

    private var rateMenu: some View {
        Menu {
            ForEach([0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0, 4.0], id: \.self) { value in
                Button {
                    player.setRate(Float(value))
                } label: {
                    if abs(Double(player.rate) - value) < 0.01 {
                        Label(String(format: "%.2fx", value), systemImage: "checkmark")
                    } else {
                        Text(String(format: "%.2fx", value))
                    }
                }
            }
        } label: {
            Text(String(format: "%.2fx", player.rate))
                .font(.system(size: 11, weight: .semibold).monospacedDigit())
                .foregroundStyle(.white.opacity(0.9))
                .padding(.horizontal, 9)
                .padding(.vertical, 7)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.white.opacity(0.1))
                )
        }
        .menuStyle(.borderlessButton)
        .menuIndicator(.hidden)
        .fixedSize()
    }

    private var audioMenu: some View {
        Menu {
            ForEach(player.audioTracks) { track in
                Button {
                    player.selectAudioTrack(track.id)
                } label: {
                    if track.id == player.currentAudioTrack {
                        Label(track.name, systemImage: "checkmark")
                    } else {
                        Text(track.name)
                    }
                }
            }
        } label: {
            Image(systemName: "waveform")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(.white.opacity(0.9))
                .frame(width: 34, height: 32)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.white.opacity(0.1))
                )
        }
        .menuStyle(.borderlessButton)
        .menuIndicator(.hidden)
        .fixedSize()
        .help("音轨")
    }

    private var subtitleMenu: some View {
        Menu {
            Button("关闭字幕") { player.selectSubtitleTrack(-1) }
            Divider()
            ForEach(player.subtitleTracks) { track in
                if track.id >= 0 {
                    Button {
                        player.selectSubtitleTrack(track.id)
                    } label: {
                        if track.id == player.currentSubtitleTrack {
                            Label(track.name, systemImage: "checkmark")
                        } else {
                            Text(track.name)
                        }
                    }
                }
            }
            Divider()
            Button("添加字幕文件…") {
                isSubtitleImporterPresented = true
            }
        } label: {
            Image(systemName: "captions.bubble")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(.white.opacity(0.9))
                .frame(width: 34, height: 32)
                .background(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(Color.white.opacity(0.1))
                )
        }
        .menuStyle(.borderlessButton)
        .menuIndicator(.hidden)
        .fixedSize()
        .help("字幕")
    }

    private var endedOverlay: some View {
        VStack(spacing: 14) {
            Image(systemName: "checkmark.circle")
                .font(.system(size: 40, weight: .light))
                .foregroundStyle(.white.opacity(0.8))
            Text("播放结束")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(.white)
            HStack(spacing: 10) {
                Button {
                    player.seek(toMs: 0)
                    player.play()
                } label: {
                    Label("重新播放", systemImage: "arrow.counterclockwise")
                }
                .buttonStyle(.borderedProminent)
                .tint(accent.color)
                Button("返回", action: onClose)
                    .buttonStyle(.bordered)
            }
            .controlSize(.large)
        }
        .padding(30)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Color.black.opacity(0.62))
        )
    }

    // MARK: - Controls visibility

    private func toggleControls() {
        if controlsVisible {
            hideTask?.cancel()
            withAnimation(.easeIn(duration: 0.2)) { controlsVisible = false }
        } else {
            revealControls()
        }
        isFocused = true
    }

    private func revealControls() {
        if !controlsVisible {
            withAnimation(.easeOut(duration: 0.2)) { controlsVisible = true }
        }
        hideTask?.cancel()
        guard player.isPlaying else { return }
        hideTask = Task {
            try? await Task.sleep(nanoseconds: 2_600_000_000)
            guard !Task.isCancelled else { return }
            await MainActor.run {
                withAnimation(.easeIn(duration: 0.25)) { controlsVisible = false }
            }
        }
    }
}
