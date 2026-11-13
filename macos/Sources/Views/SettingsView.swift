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

struct SettingsView: View {
    @EnvironmentObject private var library: LibraryStore

    @Binding var accentRaw: String
    @AppStorage("rememberPosition") private var rememberPosition = true

    private var accent: AccentTheme { AccentTheme(rawValue: accentRaw) ?? .brand }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                Text("设置")
                    .font(.system(size: 22, weight: .bold, design: .rounded))

                section(title: "外观") {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("强调色")
                            .font(.system(size: 12, weight: .medium))
                        LazyVGrid(
                            columns: [GridItem(.adaptive(minimum: 92, maximum: 120), spacing: 10)],
                            alignment: .leading,
                            spacing: 10
                        ) {
                            ForEach(AccentTheme.allCases) { theme in
                                Button {
                                    withAnimation(.easeOut(duration: 0.2)) {
                                        accentRaw = theme.rawValue
                                    }
                                } label: {
                                    HStack(spacing: 8) {
                                        Circle()
                                            .fill(theme.color)
                                            .frame(width: 14, height: 14)
                                        Text(theme.title)
                                            .font(.system(size: 12, weight: .medium))
                                            .foregroundStyle(.white)
                                        Spacer(minLength: 0)
                                        if accent == theme {
                                            Image(systemName: "checkmark")
                                                .font(.system(size: 10, weight: .bold))
                                                .foregroundStyle(theme.color)
                                        }
                                    }
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 9)
                                    .background(
                                        RoundedRectangle(cornerRadius: 9, style: .continuous)
                                            .fill(
                                                accent == theme
                                                    ? theme.color.opacity(0.16)
                                                    : Palette.surfaceElevated
                                            )
                                    )
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 9, style: .continuous)
                                            .strokeBorder(
                                                accent == theme
                                                    ? theme.color.opacity(0.55)
                                                    : Palette.border,
                                                lineWidth: 1
                                            )
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }

                section(title: "播放") {
                    Toggle(isOn: $rememberPosition) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("记住播放位置")
                                .font(.system(size: 12, weight: .medium))
                            Text("再次打开视频时从上次的位置继续")
                                .font(.system(size: 11))
                                .foregroundStyle(Palette.textSecondary)
                        }
                    }
                    .toggleStyle(.switch)
                    .tint(accent.color)
                }

                section(title: "媒体库") {
                    VStack(alignment: .leading, spacing: 10) {
                        if library.folders.isEmpty {
                            Text("尚未添加文件夹")
                                .font(.system(size: 11))
                                .foregroundStyle(Palette.textSecondary)
                        }
                        ForEach(library.folders, id: \.self) { folder in
                            HStack(spacing: 8) {
                                Image(systemName: "folder")
                                    .font(.system(size: 11))
                                    .foregroundStyle(accent.color)
                                Text(folder.path)
                                    .font(.system(size: 11))
                                    .lineLimit(1)
                                    .truncationMode(.middle)
                                Spacer()
                                Button {
                                    library.removeFolder(folder)
                                } label: {
                                    Image(systemName: "minus.circle")
                                        .font(.system(size: 11))
                                }
                                .buttonStyle(.plain)
                                .foregroundStyle(Palette.textTertiary)
                            }
                            .padding(8)
                            .background(
                                RoundedRectangle(cornerRadius: 8, style: .continuous)
                                    .fill(Palette.surfaceElevated)
                            )
                        }
                        Button {
                            library.addFolders()
                        } label: {
                            Label("添加文件夹", systemImage: "plus")
                                .font(.system(size: 12, weight: .medium))
                        }
                        .buttonStyle(.bordered)
                        .tint(accent.color)
                    }
                }

                section(title: "关于") {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(spacing: 8) {
                            Text("OpenVisum for macOS")
                                .font(.system(size: 12, weight: .semibold))
                            Text("v0.1.0")
                                .font(.system(size: 11).monospacedDigit())
                                .foregroundStyle(Palette.textTertiary)
                        }
                        Text("由 libVLC 3.0.24 驱动 · GPL-3.0-or-later")
                            .font(.system(size: 11))
                            .foregroundStyle(Palette.textSecondary)
                        HStack(spacing: 14) {
                            Link("GitHub", destination: URL(string: "https://github.com/Verlintas/OpenVisum")!)
                            Link("官网", destination: URL(string: "https://verlintas.dev/OpenVisum/")!)
                        }
                        .font(.system(size: 11))
                    }
                }
            }
            .padding(28)
            .frame(maxWidth: 720, alignment: .leading)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(Palette.background)
    }

    private func section<Content: View>(
        title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.system(size: 11, weight: .semibold))
                .textCase(.uppercase)
                .tracking(1.1)
                .foregroundStyle(Palette.textTertiary)
            content()
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .panelStyle()
        }
    }
}
