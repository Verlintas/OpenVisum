/*
 * Copyright (C) 2026 Verlintas
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var library: LibraryStore

    let accent: AccentTheme
    let onPlay: (URL) -> Void
    let onOpenFile: () -> Void
    let onOpenLibrary: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 26) {
                header

                if let hero = library.heroEntry() {
                    heroCard(item: hero.0, progress: hero.1)
                }

                let continuing = library.continueItems()
                if !continuing.isEmpty {
                    MediaRow(
                        title: "继续观看",
                        items: continuing.map(\.0),
                        accent: accent,
                        onPlay: onPlay,
                        onShowAll: onOpenLibrary
                    )
                }

                let recent = library.recentlyAdded()
                if !recent.isEmpty {
                    MediaRow(
                        title: "最近添加",
                        items: recent,
                        accent: accent,
                        onPlay: onPlay,
                        onShowAll: onOpenLibrary
                    )
                }

                if !library.favoriteItems.isEmpty {
                    MediaRow(
                        title: "收藏",
                        items: library.favoriteItems,
                        accent: accent,
                        onPlay: onPlay,
                        onShowAll: onOpenLibrary
                    )
                }

                if library.items.isEmpty {
                    emptyState
                }
            }
            .padding(28)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Palette.background)
    }

    private var header: some View {
        HStack(alignment: .bottom) {
            VStack(alignment: .leading, spacing: 4) {
                Text(greeting)
                    .font(.system(size: 25, weight: .bold, design: .rounded))
                Text("今天想看点什么？")
                    .font(.system(size: 13))
                    .foregroundStyle(Palette.textSecondary)
            }
            Spacer()
            Button(action: onOpenFile) {
                Label("打开文件", systemImage: "folder")
                    .font(.system(size: 12, weight: .medium))
            }
            .buttonStyle(.borderedProminent)
            .tint(accent.color)
            .controlSize(.large)
        }
    }

    private var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5..<12: return "早上好"
        case 12..<18: return "下午好"
        default: return "晚上好"
        }
    }

    private func heroCard(item: MediaItem, progress: PlaybackProgress?) -> some View {
        ZStack(alignment: .bottomLeading) {
            HeroThumbnail(item: item, library: library)
                .frame(height: 340)
                .frame(maxWidth: .infinity)
                .clipped()

            LinearGradient(
                colors: [.clear, Color.black.opacity(0.35), Color.black.opacity(0.82)],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: 12) {
                Text(progress == nil ? "立即播放" : "继续观看")
                    .font(.system(size: 11, weight: .semibold))
                    .tracking(1.4)
                    .textCase(.uppercase)
                    .foregroundStyle(accent.color)

                Text(item.displayName)
                    .font(.system(size: 30, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .shadow(color: .black.opacity(0.3), radius: 10, y: 2)

                HStack(spacing: 8) {
                    metaChip(item.folderName)
                    if let progress, progress.durationMs > 0 {
                        metaChip("\(formatDuration(progress.positionMs)) / \(formatDuration(progress.durationMs))")
                    }
                    if let size = try? item.url.resourceValues(forKeys: [.fileSizeKey]).fileSize {
                        metaChip(formatBytes(Int64(size)))
                    }
                }

                if let progress, progress.durationMs > 0 {
                    progressBar(fraction: Double(progress.positionMs) / Double(progress.durationMs))
                        .frame(width: 340)
                }

                HStack(spacing: 10) {
                    Button {
                        onPlay(item.url)
                    } label: {
                        Label(progress == nil ? "播放" : "继续播放", systemImage: "play.fill")
                            .font(.system(size: 13, weight: .semibold))
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.white)
                    .foregroundStyle(.black)
                    .controlSize(.large)

                    Button {
                        onPlay(item.url)
                    } label: {
                        Label("从头播放", systemImage: "arrow.counterclockwise")
                            .font(.system(size: 13, weight: .medium))
                    }
                    .buttonStyle(.bordered)
                    .tint(.white)
                    .foregroundStyle(.white)
                    .controlSize(.large)
                }
                .padding(.top, 2)
            }
            .padding(28)

            VStack {
                HStack {
                    Spacer()
                    Button {
                        library.toggleFavorite(item)
                    } label: {
                        Image(systemName: library.isFavorite(item) ? "heart.fill" : "heart")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(library.isFavorite(item) ? accent.color : .white)
                            .frame(width: 38, height: 38)
                            .background(Circle().fill(Color.black.opacity(0.35)))
                            .overlay(Circle().strokeBorder(.white.opacity(0.18), lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                    .padding(22)
                }
                Spacer()
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .strokeBorder(Palette.border, lineWidth: 1)
        )
    }

    private func metaChip(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 11, weight: .medium))
            .foregroundStyle(.white.opacity(0.85))
            .padding(.horizontal, 9)
            .padding(.vertical, 4)
            .background(Capsule().fill(Color.white.opacity(0.12)))
    }

    private func progressBar(fraction: Double) -> some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(Color.white.opacity(0.22))
                Capsule()
                    .fill(accent.color)
                    .frame(width: max(3, proxy.size.width * min(1, max(0, fraction))))
            }
        }
        .frame(height: 3)
    }

    private var emptyState: some View {
        VStack(spacing: 14) {
            Image(systemName: "play.rectangle.on.rectangle")
                .font(.system(size: 34, weight: .light))
                .foregroundStyle(Palette.textTertiary)
            Text("把视频拖进窗口，或从文件夹开始")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(Palette.textSecondary)
            HStack(spacing: 10) {
                Button("打开文件", action: onOpenFile)
                    .buttonStyle(.borderedProminent)
                    .tint(accent.color)
                Button("添加文件夹") {
                    library.addFolders()
                }
                .buttonStyle(.bordered)
            }
            .controlSize(.large)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 56)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Palette.surface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(
                    Palette.border,
                    style: StrokeStyle(lineWidth: 1, dash: [7, 5])
                )
        )
    }
}

struct MediaRow: View {
    @EnvironmentObject private var library: LibraryStore

    let title: String
    let items: [MediaItem]
    let accent: AccentTheme
    let onPlay: (URL) -> Void
    let onShowAll: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 11) {
            HStack {
                Text(title)
                    .font(.system(size: 16, weight: .semibold))
                Spacer()
                Button("查看全部", action: onShowAll)
                    .buttonStyle(.link)
                    .font(.system(size: 12))
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 14) {
                    ForEach(items) { item in
                        MediaCard(item: item, accent: accent, onPlay: onPlay)
                    }
                }
                .padding(.vertical, 2)
            }
        }
    }
}

struct MediaCard: View {
    @EnvironmentObject private var library: LibraryStore

    let item: MediaItem
    let accent: AccentTheme
    let onPlay: (URL) -> Void

    @State private var isHovering = false

    var body: some View {
        Button {
            onPlay(item.url)
        } label: {
            VStack(alignment: .leading, spacing: 7) {
                ZStack {
                    MediaThumbnail(item: item, library: library)
                        .frame(width: 210, height: 118)
                        .clipped()
                    if isHovering {
                        Color.black.opacity(0.3)
                        Image(systemName: "play.circle.fill")
                            .font(.system(size: 34))
                            .foregroundStyle(.white)
                    }
                    if library.isFavorite(item) {
                        VStack {
                            HStack {
                                Spacer()
                                Image(systemName: "heart.fill")
                                    .font(.system(size: 10))
                                    .foregroundStyle(accent.color)
                                    .padding(6)
                                    .background(Circle().fill(Color.black.opacity(0.45)))
                                    .padding(6)
                            }
                            Spacer()
                        }
                    }
                }
                .frame(width: 210, height: 118)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(alignment: .bottom) {
                    let fraction = library.progressFraction(for: item)
                    if fraction > 0 {
                        GeometryReader { proxy in
                            VStack {
                                Spacer()
                                Capsule()
                                    .fill(accent.color)
                                    .frame(width: proxy.size.width * fraction, height: 3)
                            }
                        }
                    }
                }

                Text(item.displayName)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                    .frame(width: 210, alignment: .leading)
                Text(item.folderName)
                    .font(.system(size: 10))
                    .foregroundStyle(Palette.textTertiary)
                    .lineLimit(1)
                    .frame(width: 210, alignment: .leading)
            }
            .contentShape(Rectangle())
            .scaleEffect(isHovering ? 1.015 : 1)
        }
        .buttonStyle(.plain)
        .onHover { hovering in
            withAnimation(.easeOut(duration: 0.15)) { isHovering = hovering }
        }
        .contextMenu {
            Button("播放") { onPlay(item.url) }
            Button(library.isFavorite(item) ? "取消收藏" : "收藏") {
                library.toggleFavorite(item)
            }
            Button("在访达中显示") {
                NSWorkspace.shared.activateFileViewerSelecting([item.url])
            }
        }
    }
}

struct MediaThumbnail: View {
    let item: MediaItem
    @ObservedObject var library: LibraryStore

    var body: some View {
        if let image = library.thumbnail(for: item) {
            Image(nsImage: image)
                .resizable()
                .aspectRatio(contentMode: .fill)
        } else {
            ZStack {
                LinearGradient(
                    colors: [Palette.surfaceElevated, Palette.surface],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                Image(systemName: "film")
                    .font(.system(size: 24, weight: .light))
                    .foregroundStyle(Palette.textTertiary)
            }
        }
    }
}

struct HeroThumbnail: View {
    let item: MediaItem
    @ObservedObject var library: LibraryStore

    var body: some View {
        if let image = library.thumbnail(for: item) {
            Image(nsImage: image)
                .resizable()
                .aspectRatio(contentMode: .fill)
        } else {
            ZStack {
                LinearGradient(
                    colors: [Palette.surfaceElevated, Palette.surface],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                Image(systemName: "film")
                    .font(.system(size: 40, weight: .light))
                    .foregroundStyle(Palette.textTertiary)
            }
        }
    }
}
