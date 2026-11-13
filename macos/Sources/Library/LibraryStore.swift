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
import AVFoundation
import CryptoKit
import SwiftUI

struct MediaItem: Identifiable, Hashable {
    let url: URL

    var id: String { url.absoluteString }
    var displayName: String { url.deletingPathExtension().lastPathComponent }
    var folderName: String { url.deletingLastPathComponent().lastPathComponent }
    var fileExtension: String { url.pathExtension.uppercased() }
}

struct PlaybackProgress: Codable {
    var positionMs: Int64
    var durationMs: Int64
    var lastPlayedAt: Date
}

@MainActor
final class LibraryStore: ObservableObject {

    @Published private(set) var items: [MediaItem] = []
    @Published private(set) var folders: [URL] = []
    @Published private(set) var progress: [String: PlaybackProgress] = [:]
    @Published private(set) var thumbnails: [String: NSImage] = [:]
    @Published private(set) var isScanning = false

    private var thumbnailTasks: Set<String> = []
    private let storageURL: URL
    private let thumbnailDirectory: URL

    private static let videoExtensions: Set<String> = [
        "mp4", "mkv", "webm", "avi", "mov", "m4v", "ts", "m2ts", "flv", "wmv",
        "mpg", "mpeg", "3gp", "rmvb", "rm", "vob", "ogv", "divx",
    ]

    private struct Persisted: Codable {
        var folders: [String]
        var progress: [String: PlaybackProgress]
    }

    init() {
        let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("OpenVisum", isDirectory: true)
        try? FileManager.default.createDirectory(at: support, withIntermediateDirectories: true)
        storageURL = support.appendingPathComponent("library.json")
        thumbnailDirectory = support.appendingPathComponent("thumbnails", isDirectory: true)
        try? FileManager.default.createDirectory(at: thumbnailDirectory, withIntermediateDirectories: true)
        load()
        refresh()
    }

    // MARK: - Folders

    func addFolders() {
        let panel = NSOpenPanel()
        panel.canChooseDirectories = true
        panel.canChooseFiles = false
        panel.allowsMultipleSelection = true
        panel.prompt = "添加"
        panel.message = "选择包含视频的文件夹"
        guard panel.runModal() == .OK else { return }
        for url in panel.urls where !folders.contains(url) {
            folders.append(url)
        }
        save()
        refresh()
    }

    func removeFolder(_ url: URL) {
        folders.removeAll { $0 == url }
        save()
        refresh()
    }

    // MARK: - Scanning

    func refresh() {
        let folders = self.folders
        isScanning = true
        Task.detached(priority: .userInitiated) {
            var found: [MediaItem] = []
            let manager = FileManager.default
            for folder in folders {
                guard let enumerator = manager.enumerator(
                    at: folder,
                    includingPropertiesForKeys: [.isRegularFileKey],
                    options: [.skipsHiddenFiles, .skipsPackageDescendants]
                ) else { continue }
                while let fileURL = enumerator.nextObject() as? URL {
                    guard Self.videoExtensions.contains(fileURL.pathExtension.lowercased()) else { continue }
                    found.append(MediaItem(url: fileURL))
                }
            }
            let sorted = found.sorted {
                $0.url.lastPathComponent.localizedStandardCompare($1.url.lastPathComponent) == .orderedAscending
            }
            await MainActor.run {
                self.items = sorted
                self.isScanning = false
            }
        }
    }

    // MARK: - Progress

    func markPlayed(url: URL, positionMs: Int64, durationMs: Int64) {
        guard durationMs > 0 else { return }
        let finished = positionMs >= durationMs - 10_000
        progress[url.absoluteString] = PlaybackProgress(
            positionMs: finished ? 0 : max(0, positionMs),
            durationMs: durationMs,
            lastPlayedAt: Date()
        )
        save()
    }

    func progressFraction(for item: MediaItem) -> Double {
        guard let entry = progress[item.id], entry.durationMs > 0 else { return 0 }
        return min(1, max(0, Double(entry.positionMs) / Double(entry.durationMs)))
    }

    func resumePosition(for item: MediaItem) -> Int64? {
        guard let entry = progress[item.id], entry.positionMs > 5_000 else { return nil }
        return entry.positionMs
    }

    func recentItems(limit: Int = 12) -> [MediaItem] {
        items
            .compactMap { item -> (MediaItem, Date)? in
                guard let entry = progress[item.id] else { return nil }
                return (item, entry.lastPlayedAt)
            }
            .sorted { $0.1 > $1.1 }
            .prefix(limit)
            .map(\.0)
    }

    func continueWatching() -> (MediaItem, PlaybackProgress)? {
        for item in recentItems(limit: 24) {
            guard let entry = progress[item.id] else { continue }
            if entry.positionMs > 10_000, entry.durationMs - entry.positionMs > 15_000 {
                return (item, entry)
            }
        }
        return nil
    }

    // MARK: - Thumbnails

    func thumbnail(for item: MediaItem) -> NSImage? {
        if let cached = thumbnails[item.id] { return cached }
        guard !thumbnailTasks.contains(item.id) else { return nil }
        if let cached = NSImage(contentsOf: thumbnailCacheURL(for: item.id)) {
            thumbnails[item.id] = cached
            return cached
        }
        thumbnailTasks.insert(item.id)
        let url = item.url
        let id = item.id
        let cacheURL = thumbnailCacheURL(for: id)
        Task.detached(priority: .utility) {
            let image = Self.generateThumbnail(url: url)
            if let image {
                await MainActor.run {
                    self.thumbnails[id] = image
                }
                if let data = Self.jpegData(image) {
                    try? data.write(to: cacheURL)
                }
            }
            await MainActor.run {
                self.thumbnailTasks.remove(id)
            }
        }
        return nil
    }

    private func thumbnailCacheURL(for id: String) -> URL {
        let digest = SHA256.hash(data: Data(id.utf8)).map { String(format: "%02x", $0) }.joined()
        return thumbnailDirectory.appendingPathComponent(String(digest.prefix(24)) + ".jpg")
    }

    nonisolated private static func generateThumbnail(url: URL) -> NSImage? {
        let asset = AVURLAsset(url: url)
        let generator = AVAssetImageGenerator(asset: asset)
        generator.appliesPreferredTrackTransform = true
        generator.maximumSize = CGSize(width: 720, height: 405)
        let seconds = CMTimeGetSeconds(asset.duration)
        let targetSeconds: Double
        if seconds.isFinite, seconds > 0 {
            targetSeconds = min(max(seconds * 0.25, 5), 180)
        } else {
            targetSeconds = 1
        }
        let time = CMTime(seconds: targetSeconds, preferredTimescale: 600)
        guard let cgImage = try? generator.copyCGImage(at: time, actualTime: nil) else {
            return nil
        }
        return NSImage(cgImage: cgImage, size: NSSize(width: cgImage.width, height: cgImage.height))
    }

    nonisolated private static func jpegData(_ image: NSImage) -> Data? {
        guard let tiff = image.tiffRepresentation,
              let rep = NSBitmapImageRep(data: tiff) else { return nil }
        return rep.representation(using: .jpeg, properties: [.compressionFactor: 0.82])
    }

    // MARK: - Persistence

    private func load() {
        guard let data = try? Data(contentsOf: storageURL),
              let persisted = try? JSONDecoder().decode(Persisted.self, from: data) else { return }
        folders = persisted.folders.map { URL(fileURLWithPath: $0) }
        progress = persisted.progress
    }

    private func save() {
        let persisted = Persisted(
            folders: folders.map(\.path),
            progress: progress
        )
        guard let data = try? JSONEncoder().encode(persisted) else { return }
        try? data.write(to: storageURL, options: .atomic)
    }
}

func formatDuration(_ milliseconds: Int64) -> String {
    let total = max(0, milliseconds / 1000)
    let hours = total / 3600
    let minutes = (total % 3600) / 60
    let seconds = total % 60
    if hours > 0 {
        return String(format: "%d:%02d:%02d", hours, minutes, seconds)
    }
    return String(format: "%02d:%02d", minutes, seconds)
}
