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

import SwiftUI

struct LibraryView: View {
    @EnvironmentObject private var library: LibraryStore

    let accent: AccentTheme
    let onPlay: (URL) -> Void
    let onOpenSettings: () -> Void

    @State private var query = ""

    private var filteredItems: [MediaItem] {
        guard !query.isEmpty else { return library.items }
        return library.items.filter {
            $0.displayName.localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            Divider().overlay(Palette.border)
            content
        }
        .background(Palette.background)
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 14) {
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
                icon: "magnifyingglass",
                title: query.isEmpty ? "没有找到视频" : "没有匹配「\(query)」的视频",
                subtitle: query.isEmpty ? "尝试重新扫描，或添加其他文件夹" : "换个关键词试试"
            ) {
                if !query.isEmpty {
                    Button("清除搜索") { query = "" }
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
                        LibraryCard(item: item, accent: accent) {
                            onPlay(item.url)
                        }
                        .contextMenu {
                            Button("播放") { onPlay(item.url) }
                            Button("在访达中显示") {
                                NSWorkspace.shared.activateFileViewerSelecting([item.url])
                            }
                        }
                    }
                }
                .padding(24)
            }
        }
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

private struct LibraryCard: View {
    @EnvironmentObject private var library: LibraryStore

    let item: MediaItem
    let accent: AccentTheme
    let onPlay: () -> Void

    @State private var isHovering = false

    var body: some View {
        Button(action: onPlay) {
            VStack(alignment: .leading, spacing: 8) {
                ZStack {
                    if let image = library.thumbnail(for: item) {
                        Image(nsImage: image)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } else {
                        LinearGradient(
                            colors: [Palette.surfaceElevated, Palette.surface],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                        Image(systemName: "film")
                            .font(.system(size: 24, weight: .light))
                            .foregroundStyle(Palette.textTertiary)
                    }
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
                .overlay(alignment: .bottom) {
                    let fraction = library.progressFraction(for: item)
                    if fraction > 0 {
                        GeometryReader { proxy in
                            VStack {
                                Spacer()
                                Capsule()
                                    .fill(accent.color)
                                    .frame(
                                        width: proxy.size.width * fraction,
                                        height: 3
                                    )
                            }
                        }
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                VStack(alignment: .leading, spacing: 2) {
                    Text(item.displayName)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                    Text(item.folderName)
                        .font(.system(size: 10))
                        .foregroundStyle(Palette.textTertiary)
                        .lineLimit(1)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .onHover { hovering in
            withAnimation(.easeOut(duration: 0.15)) {
                isHovering = hovering
            }
        }
    }
}
