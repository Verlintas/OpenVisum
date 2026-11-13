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
import UniformTypeIdentifiers

enum SidebarSection: String, CaseIterable, Identifiable {
    case home
    case library
    case settings

    var id: String { rawValue }

    var title: String {
        switch self {
        case .home: "首页"
        case .library: "媒体库"
        case .settings: "设置"
        }
    }

    var icon: String {
        switch self {
        case .home: "house"
        case .library: "square.stack"
        case .settings: "gearshape"
        }
    }
}

struct RootView: View {
    let player: PlayerCore
    @EnvironmentObject private var library: LibraryStore
    @StateObject private var appState = AppState.shared

    @AppStorage("accentTheme") private var accentRaw = AccentTheme.brand.rawValue
    @AppStorage("rememberPosition") private var rememberPosition = true

    @State private var section: SidebarSection = .home
    @State private var isPlayerPresented = false
    @State private var isFilePickerPresented = false
    @State private var isDropTargeted = false

    private var accent: AccentTheme { AccentTheme(rawValue: accentRaw) ?? .brand }

    var body: some View {
        Group {
            if isPlayerPresented {
                PlayerView(onClose: closePlayer)
                    .transition(.opacity.combined(with: .scale(scale: 1.015)))
            } else {
                NavigationSplitView {
                    sidebar
                        .navigationSplitViewColumnWidth(min: 200, ideal: 216, max: 240)
                } detail: {
                    switch section {
                    case .home:
                        HomeView(
                            accent: accent,
                            onPlay: play,
                            onOpenFile: { isFilePickerPresented = true },
                            onOpenLibrary: { section = .library }
                        )
                    case .library:
                        LibraryView(
                            accent: accent,
                            onPlay: play,
                            onOpenSettings: { section = .settings }
                        )
                    case .settings:
                        SettingsView(accentRaw: $accentRaw)
                    }
                }
                .tint(accent.color)
                .transition(.opacity)
            }
        }
        .background(Palette.background)
        .preferredColorScheme(.dark)
        .background(WindowConfigurator())
        .fileImporter(
            isPresented: $isFilePickerPresented,
            allowedContentTypes: [.movie, .audio, .video],
            allowsMultipleSelection: false
        ) { result in
            if case .success(let urls) = result, let url = urls.first {
                play(url)
            }
        }
        .onChange(of: appState.pendingURL) { _, url in
            if let url { play(url) }
        }
        .task {
            player.onProgress = { url, position, duration in
                library.markPlayed(url: url, positionMs: position, durationMs: duration)
            }
            if let url = appState.pendingURL {
                play(url)
            }
        }
        .onDrop(of: [.fileURL], isTargeted: $isDropTargeted) { providers in
            guard let provider = providers.first else { return false }
            _ = provider.loadObject(ofClass: URL.self) { url, _ in
                guard let url else { return }
                Task { @MainActor in
                    play(url)
                }
            }
            return true
        }
        .overlay {
            if isDropTargeted {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .strokeBorder(accent.color, style: StrokeStyle(lineWidth: 2, dash: [8, 6]))
                    .padding(10)
                    .allowsHitTesting(false)
            }
        }
    }

    private var sidebar: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 10) {
                AppMark(size: 28, accent: accent)
                VStack(alignment: .leading, spacing: 1) {
                    Text("OpenVisum")
                        .font(.system(size: 14, weight: .semibold))
                    Text("macOS")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundStyle(Palette.textTertiary)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 18)
            .padding(.bottom, 22)

            ForEach([SidebarSection.home, SidebarSection.library]) { item in
                SidebarButton(
                    section: item,
                    selected: section == item,
                    accent: accent
                ) {
                    section = item
                }
            }

            Spacer()

            VStack(alignment: .leading, spacing: 6) {
                Text("\(library.items.count) 个视频")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(Palette.textSecondary)
                Text("v0.1.0 · libVLC 3.0.24")
                    .font(.system(size: 10))
                    .foregroundStyle(Palette.textTertiary)
            }
            .padding(.horizontal, 18)
            .padding(.bottom, 14)

            SidebarButton(
                section: .settings,
                selected: section == .settings,
                accent: accent
            ) {
                section = .settings
            }
            .padding(.bottom, 8)
        }
        .frame(maxHeight: .infinity, alignment: .top)
        .background(Palette.surface.opacity(0.55))
    }

    private func play(_ url: URL) {
        if isPlayerPresented, player.currentMediaURL == url {
            return
        }
        player.open(url: url)
        withAnimation(.spring(response: 0.32, dampingFraction: 0.9)) {
            isPlayerPresented = true
        }
        if rememberPosition, let resume = library.progress[url.absoluteString]?.positionMs, resume > 5_000 {
            Task {
                try? await Task.sleep(nanoseconds: 1_200_000_000)
                if player.isPlaying {
                    player.seek(toMs: resume)
                }
            }
        }
    }

    private func closePlayer() {
        if let url = player.currentMediaURL, player.durationMs > 0 {
            library.markPlayed(
                url: url,
                positionMs: player.timeMs,
                durationMs: player.durationMs
            )
        }
        player.pause()
        withAnimation(.easeOut(duration: 0.2)) {
            isPlayerPresented = false
        }
        appState.pendingURL = nil
    }
}

struct SidebarButton: View {
    let section: SidebarSection
    let selected: Bool
    let accent: AccentTheme
    let action: () -> Void

    @State private var isHovering = false

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: section.icon)
                    .font(.system(size: 13, weight: .semibold))
                    .frame(width: 18)
                Text(section.title)
                    .font(.system(size: 13, weight: selected ? .semibold : .regular))
                Spacer()
            }
            .foregroundStyle(selected ? accent.color : Palette.textSecondary)
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background(
                RoundedRectangle(cornerRadius: 9, style: .continuous)
                    .fill(
                        selected
                            ? accent.color.opacity(0.16)
                            : (isHovering ? Color.white.opacity(0.05) : Color.clear)
                    )
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(.horizontal, 8)
        .onHover { isHovering = $0 }
        .animation(.easeOut(duration: 0.14), value: isHovering)
    }
}

struct AppMark: View {
    var size: CGFloat = 28
    var accent: AccentTheme = .brand

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.28, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [
                            Color(red: 0.11, green: 0.14, blue: 0.27),
                            Color(red: 0.04, green: 0.06, blue: 0.12),
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
            Circle()
                .strokeBorder(
                    LinearGradient(
                        colors: [accent.color, accent.color.opacity(0.55)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: size * 0.06
                )
                .padding(size * 0.2)
            Image(systemName: "play.fill")
                .font(.system(size: size * 0.26, weight: .bold))
                .foregroundStyle(.white)
                .offset(x: size * 0.02)
        }
        .frame(width: size, height: size)
    }
}
