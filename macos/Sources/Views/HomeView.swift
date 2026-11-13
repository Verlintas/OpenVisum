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

struct HomeView: View {
    @EnvironmentObject private var library: LibraryStore
    @EnvironmentObject private var player: PlayerCore

    let accent: AccentTheme
    let onPlay: (URL) -> Void
    let onOpenFile: () -> Void
    let onOpenLibrary: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                header

                if let (item, progress) = library.continueWatching() {
                    continueCard(item: item, progress: progress)
                }

                let recent = library.recentItems()
                if !recent.isEmpty {
                    recentSection(recent)
                }

                if library.items.isEmpty {
                    emptyState
                } else if recent.isEmpty {
                    recentlyAddedSection
                }
            }
            .padding(30)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Palette.background)
        .navigationTitle("")
    }

    private var header: some View {
        HStack(alignment: .bottom) {
            VStack(alignment: .leading, spacing: 4) {
                Text(greeting)
                    .font(.system(size: 26, weight: .bold, design: .rounded))
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

    private func continueCard(item: MediaItem, progress: PlaybackProgress) -> some View {
        Button {
            onPlay(item.url)
        } label: {
            ZStack(alignment: .bottomLeading) {
                thumbnail(item: item)
                    .frame(height: 300)
                    .frame(maxWidth: .infinity)
                    .clipped()

                LinearGradient(
                    colors: [.clear, Color.black.opacity(0.75)],
                    startPoint: .center,
                    endPoint: .bottom
                )

                VStack(alignment: .leading, spacing: 10) {
                    Text("继续观看")
                        .font(.system(size: 11, weight: .semibold))
                        .textCase(.uppercase)
                        .tracking(1.2)
                        .foregroundStyle(accent.color)
                    Text(item.displayName)
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                    HStack(spacing: 10) {
                        Text(item.folderName)
                            .font(.system(size: 12))
                            .foregroundStyle(.white.opacity(0.7))
                        Text("·")
                            .foregroundStyle(.white.opacity(0.4))
                        Text("\(formatDuration(progress.positionMs)) / \(formatDuration(progress.durationMs))")
                            .font(.system(size: 12).monospacedDigit())
                            .foregroundStyle(.white.opacity(0.7))
                    }
                    progressBar(fraction: progress.durationMs > 0
                        ? Double(progress.positionMs) / Double(progress.durationMs) : 0)
                        .frame(width: 320)
                }
                .padding(26)
            }
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(Palette.border, lineWidth: 1)
            )
            .overlay(alignment: .center) {
                Image(systemName: "play.circle.fill")
                    .font(.system(size: 54))
                    .foregroundStyle(.white.opacity(0.92))
                    .shadow(radius: 14)
                    .opacity(0.9)
            }
        }
        .buttonStyle(.plain)
    }

    private func recentSection(_ recent: [MediaItem]) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("最近播放")
                    .font(.system(size: 16, weight: .semibold))
                Spacer()
                Button("打开媒体库", action: onOpenLibrary)
                    .buttonStyle(.link)
                    .font(.system(size: 12))
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 14) {
                    ForEach(recent) { item in
                        Button {
                            onPlay(item.url)
                        } label: {
                            VStack(alignment: .leading, spacing: 8) {
                                thumbnail(item: item)
                                    .frame(width: 210, height: 118)
                                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    .overlay(alignment: .bottom) {
                                        let fraction = library.progressFraction(for: item)
                                        if fraction > 0 {
                                            progressBar(fraction: fraction)
                                                .padding(.horizontal, 8)
                                                .padding(.bottom, 7)
                                        }
                                    }
                                Text(item.displayName)
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundStyle(.white)
                                    .lineLimit(1)
                                    .frame(width: 210, alignment: .leading)
                                    .multilineTextAlignment(.leading)
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.vertical, 2)
            }
        }
    }

    private var recentlyAddedSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("最近加入")
                    .font(.system(size: 16, weight: .semibold))
                Spacer()
                Button("打开媒体库", action: onOpenLibrary)
                    .buttonStyle(.link)
                    .font(.system(size: 12))
            }
            LazyVGrid(
                columns: [GridItem(.adaptive(minimum: 210, maximum: 280), spacing: 16)],
                alignment: .leading,
                spacing: 18
            ) {
                ForEach(library.items.prefix(8)) { item in
                    Button {
                        onPlay(item.url)
                    } label: {
                        VStack(alignment: .leading, spacing: 8) {
                            thumbnail(item: item)
                                .frame(height: 118)
                                .frame(maxWidth: .infinity)
                                .clipped()
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                            Text(item.displayName)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(.white)
                                .lineLimit(1)
                            Text(item.folderName)
                                .font(.system(size: 10))
                                .foregroundStyle(Palette.textTertiary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
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

    @ViewBuilder
    private func thumbnail(item: MediaItem) -> some View {
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
                    .font(.system(size: 26, weight: .light))
                    .foregroundStyle(Palette.textTertiary)
            }
        }
    }
}
