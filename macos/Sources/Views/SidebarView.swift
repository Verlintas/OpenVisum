/*
 * Copyright (C) 2026 Verlintas
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import SwiftUI

struct SidebarView: View {
    @Binding var section: SidebarSection
    @Binding var collapsed: Bool
    let accent: AccentTheme
    @ObservedObject var library: LibraryStore

    var body: some View {
        VStack(alignment: collapsed ? .center : .leading, spacing: 0) {
            header

            VStack(spacing: 3) {
                ForEach([SidebarSection.home, SidebarSection.library]) { item in
                    SidebarButton(
                        section: item,
                        selected: section == item,
                        accent: accent,
                        collapsed: collapsed
                    ) {
                        section = item
                    }
                }
            }
            .padding(.top, 14)

            Spacer(minLength: 8)

            storageBlock
                .padding(.horizontal, collapsed ? 8 : 12)
                .padding(.bottom, 12)

            if !collapsed {
                Text("v0.1.0 · libVLC 3.0.24")
                    .font(.system(size: 10))
                    .foregroundStyle(Palette.textTertiary)
                    .padding(.horizontal, 18)
                    .padding(.bottom, 8)
            }

            SidebarButton(
                section: .settings,
                selected: section == .settings,
                accent: accent,
                collapsed: collapsed
            ) {
                section = .settings
            }
            .padding(.bottom, 10)
        }
        .frame(maxHeight: .infinity, alignment: .top)
        .background(Palette.surface.opacity(0.55))
        .clipped()
    }

    private var header: some View {
        HStack(spacing: 9) {
            AppMark(size: 27, accent: accent)
                .contentShape(Rectangle())
                .onTapGesture {
                    if collapsed {
                        withAnimation(.spring(response: 0.32, dampingFraction: 0.88)) {
                            collapsed = false
                        }
                    }
                }
                .help(collapsed ? "展开侧栏" : "OpenVisum")
            if !collapsed {
                VStack(alignment: .leading, spacing: 1) {
                    Text("OpenVisum")
                        .font(.system(size: 13.5, weight: .semibold))
                        .lineLimit(1)
                    Text("macOS")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundStyle(Palette.textTertiary)
                }
            }
            Spacer(minLength: 0)
            if !collapsed {
                Button {
                    withAnimation(.spring(response: 0.32, dampingFraction: 0.88)) {
                        collapsed = true
                    }
                } label: {
                    Image(systemName: "sidebar.left")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(Palette.textTertiary)
                        .frame(width: 22, height: 22)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .help("收起侧栏")
            }
        }
        .padding(.horizontal, collapsed ? 10 : 16)
        .padding(.top, 40)
        .padding(.bottom, 8)
    }

    private var storageBlock: some View {
        VStack(alignment: collapsed ? .center : .leading, spacing: collapsed ? 4 : 8) {
            HStack(spacing: 10) {
                StorageRing(fraction: library.storageFraction, accent: accent, size: collapsed ? 44 : 38)
                if !collapsed {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("存储")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundStyle(Palette.textTertiary)
                        Text("已用 \(formatBytes(library.storageUsedBytes)) / \(formatBytes(library.storageTotalBytes))")
                            .font(.system(size: 10.5))
                            .foregroundStyle(Palette.textSecondary)
                            .lineLimit(1)
                            .truncationMode(.middle)
                    }
                }
            }
            if !collapsed {
                Text("媒体库 \(formatBytes(library.libraryBytes)) · \(library.items.count) 个视频")
                    .font(.system(size: 10))
                    .foregroundStyle(Palette.textTertiary)
                    .lineLimit(1)
                GeometryReader { proxy in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color.white.opacity(0.1))
                        Capsule()
                            .fill(accent.color.opacity(0.8))
                            .frame(width: max(3, proxy.size.width * library.storageFraction))
                    }
                }
                .frame(height: 3)
            }
        }
        .padding(collapsed ? 6 : 11)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color.white.opacity(0.04))
        )
    }
}

struct StorageRing: View {
    let fraction: Double
    let accent: AccentTheme
    var size: CGFloat = 38

    @State private var animatedFraction: Double = 0

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.white.opacity(0.12), lineWidth: 4)
            Circle()
                .trim(from: 0, to: min(1, max(0, animatedFraction)))
                .stroke(
                    accent.color,
                    style: StrokeStyle(lineWidth: 4, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
            Text("\(Int((animatedFraction * 100).rounded()))%")
                .font(.system(size: size * 0.22, weight: .semibold).monospacedDigit())
                .foregroundStyle(.white)
        }
        .frame(width: size, height: size)
        .onAppear {
            withAnimation(.easeOut(duration: 0.9)) { animatedFraction = fraction }
        }
        .onChange(of: fraction) { _, value in
            withAnimation(.easeOut(duration: 0.5)) { animatedFraction = value }
        }
    }
}
