/*
 * Copyright (C) 2026 Verlintas
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import SwiftUI

enum LibraryFilter: String, CaseIterable, Identifiable {
    case all
    case favorites

    var id: String { rawValue }

    var title: String {
        switch self {
        case .all: "全部"
        case .favorites: "收藏"
        }
    }
}

struct LibraryView: View {
    @EnvironmentObject private var library: LibraryStore

    let accent: AccentTheme
    let onPlay: (URL) -> Void
    let onOpenSettings: () -> Void

    @State private var query = ""
    @State private var filter: LibraryFilter = .all

    private var filteredItems: [MediaItem] {
        var base = filter == .favorites ? library.favoriteItems : library.items
        if !query.isEmpty {
            base = base.filter { $0.displayName.localizedCaseInsensitiveContains(query) }
        }
        return base
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            Divider().overlay(Palette.border)
            chips
            content
        }
        .background(Palette.background)
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text("媒体库")
                    .font(.system(size: 22, weight: .bold, design: .rounded))
                Text("\(library.items.count) 个视频 · \(library.folders.count) 个文件夹")
                    .font(.system(size: 11))
                    .foregroundStyle(Palette.textSecondary)
            }
            Spacer()
            searchField
            Button {
                library.addFolders()
            } label: {
                Label("添加文件夹", systemImage: "folder.badge.plus")
                    .font(.system(size: 12, weight: .medium))
            }
            .buttonStyle(.bordered)
            Button {
                library.refresh()
            } label: {
                Image(systemName: "arrow.clockwise")
            }
            .buttonStyle(.bordered)
            .help("重新扫描")
        }
        .padding(.horizontal, 26)
        .padding(.vertical, 16)
        .background(Palette.surface.opacity(0.4))
    }

    private var searchField: some View {
        HStack(spacing: 6) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 11))
                .foregroundStyle(Palette.textTertiary)
            TextField("搜索视频", text: $query)
                .textFieldStyle(.plain)
                .font(.system(size: 12))
                .frame(width: 150)
            if !query.isEmpty {
                Button {
                    query = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 11))
                        .foregroundStyle(Palette.textTertiary)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Palette.surfaceElevated)
        )
    }

    private var chips: some View {
        HStack(spacing: 8) {
            ForEach(LibraryFilter.allCases) { option in
                let selected = filter == option
                let count = option == .all ? library.items.count : library.favoriteItems.count
                Button {
                    withAnimation(.easeOut(duration: 0.18)) {
                        filter = option
                    }
                } label: {
                    HStack(spacing: 5) {
                        Text(option.title)
                        Text("\(count)")
                            .font(.system(size: 10).monospacedDigit())
                            .foregroundStyle(selected ? accent.color.opacity(0.8) : Palette.textTertiary)
                    }
                    .font(.system(size: 12, weight: selected ? .semibold : .regular))
                    .foregroundStyle(selected ? accent.color : Palette.textSecondary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(
                        Capsule()
                            .fill(selected ? accent.color.opacity(0.16) : Color.white.opacity(0.05))
                    )
                    .overlay(
                        Capsule().strokeBorder(
                            selected ? accent.color.opacity(0.45) : Color.clear,
                            lineWidth: 1
                        )
                    )
                }
                .buttonStyle(.plain)
            }
            Spacer()
        }
        .padding(.horizontal, 26)
        .padding(.vertical, 12)
    }

    @ViewBuilder
    private var content: some View {
        if library.folders.isEmpty {
            emptyState(
                icon: "folder",
                title: "还没有添加文件夹",
                subtitle: "添加一个文件夹，里面的视频会自动出现在媒体库"
            ) {
                Button("添加文件夹") { library.addFolders() }
                    .buttonStyle(.borderedProminent)
                    .tint(accent.color)
                    .controlSize(.large)
            }
        } else if filteredItems.isEmpty {
            emptyState(
                icon: filter == .favorites ? "heart" : "magnifyingglass",
                title: emptyTitle,
                subtitle: emptySubtitle
            ) {
                if !query.isEmpty {
                    Button("清除搜索") { query = "" }
                        .buttonStyle(.bordered)
                } else if filter == .favorites {
                    Button("浏览全部视频") { filter = .all }
                        .buttonStyle(.bordered)
                }
            }
        } else {
            ScrollView {
                LazyVGrid(
                    columns: [GridItem(.adaptive(minimum: 210, maximum: 280), spacing: 16)],
                    spacing: 18
                ) {
                    ForEach(filteredItems) { item in
                        GridMediaCard(item: item, accent: accent, onPlay: onPlay)
                    }
                }
                .padding(24)
            }
        }
    }

    private var emptyTitle: String {
        if !query.isEmpty { return "没有匹配「\(query)」的视频" }
        return filter == .favorites ? "还没有收藏" : "没有找到视频"
    }

    private var emptySubtitle: String {
        if !query.isEmpty { return "换个关键词试试" }
        return filter == .favorites ? "在视频卡片上点爱心即可收藏" : "尝试重新扫描，或添加其他文件夹"
    }

    private func emptyState<Actions: View>(
        icon: String,
        title: String,
        subtitle: String,
        @ViewBuilder actions: () -> Actions
    ) -> some View {
        VStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 32, weight: .light))
                .foregroundStyle(Palette.textTertiary)
            Text(title)
                .font(.system(size: 15, weight: .semibold))
            Text(subtitle)
                .font(.system(size: 12))
                .foregroundStyle(Palette.textSecondary)
            actions()
                .padding(.top, 4)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct GridMediaCard: View {
    @EnvironmentObject private var library: LibraryStore

    let item: MediaItem
    let accent: AccentTheme
    let onPlay: (URL) -> Void

    @State private var isHovering = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack {
                MediaThumbnail(item: item, library: library)
                    .frame(height: 120)
                    .frame(maxWidth: .infinity)
                    .clipped()
                if isHovering {
                    Color.black.opacity(0.28)
                    Image(systemName: "play.circle.fill")
                        .font(.system(size: 38))
                        .foregroundStyle(.white)
                }
            }
            .frame(height: 120)
            .frame(maxWidth: .infinity)
            .clipped()
            .overlay(alignment: .bottomLeading) {
                Text(item.fileExtension)
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(.white.opacity(0.92))
                    .padding(.horizontal, 6)
                    .padding(.vertical, 3)
                    .background(Color.black.opacity(0.55))
                    .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
                    .padding(7)
            }
            .overlay(alignment: .topTrailing) {
                Button {
                    library.toggleFavorite(item)
                } label: {
                    Image(systemName: library.isFavorite(item) ? "heart.fill" : "heart")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(library.isFavorite(item) ? accent.color : .white.opacity(0.85))
                        .padding(6)
                        .background(Circle().fill(Color.black.opacity(0.45)))
                }
                .buttonStyle(.plain)
                .padding(6)
                .opacity(isHovering || library.isFavorite(item) ? 1 : 0)
            }
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
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .onTapGesture { onPlay(item.url) }
            .onHover { hovering in
                withAnimation(.easeOut(duration: 0.15)) { isHovering = hovering }
            }

            Text(item.displayName)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(.white)
                .lineLimit(1)
            Text(item.folderName)
                .font(.system(size: 10))
                .foregroundStyle(Palette.textTertiary)
                .lineLimit(1)
        }
        .contentShape(Rectangle())
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
