/*
 * Copyright (C) 2026 Verlintas
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import AppKit
import AVFoundation
import SwiftUI
import UniformTypeIdentifiers

enum AspectMode: String, CaseIterable, Identifiable {
    case fit
    case crop

    var id: String { rawValue }

    var title: String {
        switch self {
        case .fit: "适应屏幕"
        case .crop: "裁剪填满"
        }
    }
}

enum PlayerSheetKind: String, Identifiable {
    case speed
    case audio
    case subtitle
    case aspect
    case equalizer
    case bookmarks

    var id: String { rawValue }

    var title: String {
        switch self {
        case .speed: "播放速度"
        case .audio: "音轨"
        case .subtitle: "字幕"
        case .aspect: "画面比例"
        case .equalizer: "均衡器"
        case .bookmarks: "章节与书签"
        }
    }
}

struct PlayerView: View {
    @EnvironmentObject private var player: PlayerCore
    @EnvironmentObject private var library: LibraryStore

    let onClose: () -> Void
    let onPlay: (URL) -> Void

    @AppStorage("accentTheme") private var accentRaw = AccentTheme.brand.rawValue

    @State private var controlsVisible = true
    @State private var hideTask: Task<Void, Never>?
    @State private var activeSheet: PlayerSheetKind?
    @State private var isScrubbing = false
    @State private var scrubValue: Double = 0
    @State private var previewImage: NSImage?
    @State private var previewTask: Task<Void, Never>?
    @State private var isSubtitleImporterPresented = false
    @State private var playlist: [URL] = []
    @State private var playlistIndex = 0
    @State private var aspectMode: AspectMode = .fit
    @FocusState private var isFocused: Bool

    private var accent: AccentTheme { AccentTheme(rawValue: accentRaw) ?? .brand }
    private var displayedTime: Int64 {
        isScrubbing ? Int64(scrubValue) : player.timeMs
    }
    private var canGoPrevious: Bool { playlistIndex > 0 }
    private var canGoNext: Bool { playlistIndex + 1 < playlist.count }

    private let rates: [Float] = [0.25, 0.5, 0.75, 1, 1.25, 1.5, 2, 3, 4]

    var body: some View {
        ZStack {
            Color.black

            GeometryReader { proxy in
                let size = surfaceSize(in: proxy.size)
                VideoSurface(player: player)
                    .frame(width: size.width, height: size.height)
                    .frame(width: proxy.size.width, height: proxy.size.height)
                    .clipped()
                    .onTapGesture { toggleControls() }
            }

            if player.isBuffering && !player.isEnded {
                ProgressView()
                    .controlSize(.large)
                    .tint(.white)
            }

            if player.isEnded {
                endedOverlay
            } else if controlsVisible {
                chrome
                    .transition(.opacity)
            }

            if let sheet = activeSheet {
                sheetOverlay(sheet)
                    .zIndex(10)
                    .ignoresSafeArea()
            }
        }
        .ignoresSafeArea()
        .focusable()
        .focused($isFocused)
        .onAppear {
            isFocused = true
            rebuildPlaylist()
            revealControls()
        }
        .onChange(of: player.currentMediaURL) { _, _ in
            rebuildPlaylist()
            previewImage = nil
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
            if activeSheet != nil {
                withAnimation(.easeOut(duration: 0.16)) { activeSheet = nil }
            } else {
                onClose()
            }
            return .handled
        }
        .onDisappear {
            hideTask?.cancel()
            previewTask?.cancel()
        }
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

    // MARK: - Video sizing

    private func surfaceSize(in container: CGSize) -> CGSize {
        guard player.videoSize.width > 0, player.videoSize.height > 0,
              container.width > 0, container.height > 0 else {
            return container
        }
        let videoAspect = player.videoSize.width / player.videoSize.height
        switch aspectMode {
        case .fit:
            let height = container.width / videoAspect
            if height <= container.height {
                return CGSize(width: container.width, height: height)
            }
            return CGSize(width: container.height * videoAspect, height: container.height)
        case .crop:
            let height = container.width / videoAspect
            if height >= container.height {
                return CGSize(width: container.width, height: height)
            }
            return CGSize(width: container.height * videoAspect, height: container.height)
        }
    }

    // MARK: - Chrome

    private var chrome: some View {
        ZStack {
            VStack(spacing: 0) {
                topBar
                Spacer()
                bottomBar
            }
            .background(
                VStack(spacing: 0) {
                    LinearGradient(
                        colors: [Color.black.opacity(0.62), .clear],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 140)
                    Spacer()
                    LinearGradient(
                        colors: [.clear, Color.black.opacity(0.82)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 220)
                }
                .allowsHitTesting(false)
            )

            transportRow
        }
    }

    private var transportRow: some View {
        HStack(spacing: 24) {
            circleButton(systemName: "backward.end.fill", enabled: canGoPrevious, help: "上一个") {
                guard canGoPrevious else { return }
                onPlay(playlist[playlistIndex - 1])
            }
            circleButton(systemName: "gobackward.10", help: "后退 10 秒") {
                player.seekBy(-10_000)
                revealControls()
            }
            Button {
                player.togglePlayPause()
                revealControls()
            } label: {
                Image(systemName: player.isPlaying ? "pause.fill" : "play.fill")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(Color(red: 0.06, green: 0.07, blue: 0.09))
                    .frame(width: 58, height: 58)
                    .background(Circle().fill(Color.white.opacity(0.96)))
                    .shadow(color: .black.opacity(0.35), radius: 16, y: 6)
            }
            .buttonStyle(.plain)
            circleButton(systemName: "goforward.10", help: "前进 10 秒") {
                player.seekBy(10_000)
                revealControls()
            }
            circleButton(systemName: "forward.end.fill", enabled: canGoNext, help: "下一个") {
                guard canGoNext else { return }
                onPlay(playlist[playlistIndex + 1])
            }
        }
    }

    private var topBar: some View {
        HStack(spacing: 12) {
            Button(action: onClose) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 14, weight: .semibold))
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
                Text(formatDuration(player.durationMs))
                    .font(.system(size: 11).monospacedDigit())
                    .foregroundStyle(.white.opacity(0.55))
            }

            if playlist.count > 1 {
                Text("\(playlistIndex + 1) / \(playlist.count)")
                    .font(.system(size: 10, weight: .medium).monospacedDigit())
                    .foregroundStyle(.white.opacity(0.45))
                    .padding(.horizontal, 7)
                    .padding(.vertical, 2)
                    .background(Capsule().fill(Color.white.opacity(0.1)))
            }

            Spacer()

            Button {
                NSApp.keyWindow?.toggleFullScreen(nil)
            } label: {
                Image(systemName: "arrow.up.left.and.arrow.down.right")
                    .font(.system(size: 12, weight: .semibold))
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
        VStack(spacing: 10) {
            GeometryReader { proxy in
                let width = proxy.size.width
                let fraction = player.durationMs > 0
                    ? min(1, max(0, Double(displayedTime) / Double(player.durationMs)))
                    : 0
                let previewWidth: CGFloat = 168
                let previewOffset = min(
                    max(0, width * fraction - previewWidth / 2),
                    max(0, width - previewWidth)
                )
                ZStack(alignment: .topLeading) {
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
                                schedulePreview(at: scrubValue)
                            } else {
                                player.seek(toMs: Int64(scrubValue))
                                isScrubbing = false
                                previewImage = nil
                            }
                            revealControls()
                        }
                    )
                    .tint(accent.color)
                    .onChange(of: scrubValue) { _, value in
                        if isScrubbing {
                            schedulePreview(at: value)
                        }
                    }

                    if isScrubbing, let previewImage {
                        VStack(alignment: .leading, spacing: 0) {
                            Image(nsImage: previewImage)
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                                .frame(width: previewWidth, height: previewWidth * 9 / 16)
                                .clipped()
                                .overlay(alignment: .bottomTrailing) {
                                    Text(formatDuration(displayedTime))
                                        .font(.system(size: 10, weight: .semibold).monospacedDigit())
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(Color.black.opacity(0.65))
                                        .clipShape(RoundedRectangle(cornerRadius: 4))
                                        .padding(5)
                                }
                                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                                        .strokeBorder(.white.opacity(0.2), lineWidth: 1)
                                )
                                .shadow(color: .black.opacity(0.5), radius: 12, y: 4)
                        }
                        .offset(x: previewOffset, y: -previewWidth * 9 / 16 - 34)
                        .allowsHitTesting(false)
                    }
                }
            }
            .frame(height: 22)

            HStack {
                Text(formatDuration(displayedTime))
                    .font(.system(size: 11, weight: .medium).monospacedDigit())
                    .foregroundStyle(.white.opacity(0.8))
                Spacer()
                if player.isBuffering {
                    Text("缓冲中…")
                        .font(.system(size: 10))
                        .foregroundStyle(.white.opacity(0.55))
                }
                Spacer()
                Text(formatDuration(player.durationMs))
                    .font(.system(size: 11, weight: .medium).monospacedDigit())
                    .foregroundStyle(.white.opacity(0.55))
            }
            .padding(.horizontal, 2)

            HStack(spacing: 0) {
                toolButton(label: String(format: "%.2fx", player.rate)) {
                    Image(systemName: "speedometer")
                } action: {
                    openSheet(.speed)
                }

                toolButton(label: "音频") {
                    Image(systemName: "waveform")
                } action: {
                    openSheet(.audio)
                }

                toolButton(label: "字幕") {
                    Image(systemName: "captions.bubble")
                } action: {
                    openSheet(.subtitle)
                }

                toolButton(label: "均衡器") {
                    Image(systemName: "slider.vertical.3")
                } action: {
                    openSheet(.equalizer)
                }

                toolButton(label: "画面") {
                    Image(systemName: "aspectratio")
                } action: {
                    openSheet(.aspect)
                }

                toolButton(label: "书签") {
                    Image(systemName: "bookmark")
                } action: {
                    openSheet(.bookmarks)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 2)
        }
        .padding(.horizontal, 22)
        .padding(.bottom, 20)
    }

    private func toolButton<Icon: View>(
        label: String,
        @ViewBuilder icon: () -> Icon,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(spacing: 4) {
                icon()
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(.white.opacity(0.92))
                    .frame(width: 42, height: 42)
                    .background(Circle().fill(Color.white.opacity(0.12)))
                Text(label)
                    .font(.system(size: 9.5, weight: .medium))
                    .foregroundStyle(.white.opacity(0.82))
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .help(label)
    }

    private func circleButton(
        systemName: String,
        enabled: Bool = true,
        help: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(.white.opacity(enabled ? 0.9 : 0.28))
                .frame(width: 36, height: 36)
                .background(Circle().fill(Color.white.opacity(enabled ? 0.08 : 0.03)))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .help(help)
    }

    // MARK: - Playlist

    private func rebuildPlaylist() {
        guard let url = player.currentMediaURL else { return }
        let siblings = LibraryStore.siblingVideos(of: url)
        playlist = siblings
        playlistIndex = siblings.firstIndex(of: url) ?? 0
    }

    // MARK: - Sheets

    private func openSheet(_ sheet: PlayerSheetKind) {
        withAnimation(.spring(response: 0.28, dampingFraction: 0.9)) {
            activeSheet = sheet
        }
        if sheet == .subtitle || sheet == .audio {
            player.refreshTracks()
        }
    }

    private func sheetOverlay(_ sheet: PlayerSheetKind) -> some View {
        ZStack(alignment: .bottom) {
            Color.black.opacity(0.5)
                .ignoresSafeArea()
                .onTapGesture {
                    withAnimation(.easeOut(duration: 0.18)) { activeSheet = nil }
                }

            sheetContent(sheet)
                .padding(.bottom, 18)
                .transition(.move(edge: .bottom).combined(with: .opacity))
        }
        .transition(.opacity)
    }

    @ViewBuilder
    private func sheetContent(_ sheet: PlayerSheetKind) -> some View {
        PlayerSheetShell(title: sheet.title) {
            withAnimation(.easeOut(duration: 0.16)) { activeSheet = nil }
        } content: {
            switch sheet {
            case .speed: speedSheet
            case .audio: audioSheet
            case .subtitle: subtitleSheet
            case .aspect: aspectSheet
            case .equalizer: equalizerSheet
            case .bookmarks: bookmarksSheet
            }
        }
    }

    private var speedSheet: some View {
        ScrollView {
            VStack(spacing: 2) {
                ForEach(rates, id: \.self) { value in
                    sheetRow(
                        title: String(format: "%.2fx", value),
                        subtitle: value == 1 ? "正常速度" : nil,
                        selected: abs(player.rate - value) < 0.01
                    ) {
                        player.setRate(value)
                        closeSheet()
                    }
                }
            }
            .padding(.horizontal, 6)
        }
        .frame(maxHeight: 300)
    }

    private var audioSheet: some View {
        VStack(spacing: 2) {
            ForEach(player.audioTracks) { track in
                sheetRow(
                    title: track.name,
                    selected: track.id == player.currentAudioTrack
                ) {
                    player.selectAudioTrack(track.id)
                    closeSheet()
                }
            }
        }
        .padding(.horizontal, 6)
    }

    private var subtitleSheet: some View {
        VStack(spacing: 2) {
            sheetRow(title: "关闭字幕", selected: player.currentSubtitleTrack < 0) {
                player.selectSubtitleTrack(-1)
                closeSheet()
            }
            ForEach(player.subtitleTracks.filter { $0.id >= 0 }) { track in
                sheetRow(
                    title: track.name,
                    selected: track.id == player.currentSubtitleTrack
                ) {
                    player.selectSubtitleTrack(track.id)
                    closeSheet()
                }
            }
            sheetRow(title: "添加字幕文件…", subtitle: "SRT / ASS / VTT", icon: "plus") {
                activeSheet = nil
                isSubtitleImporterPresented = true
            }
        }
        .padding(.horizontal, 6)
    }

    private var aspectSheet: some View {
        VStack(spacing: 2) {
            ForEach(AspectMode.allCases) { mode in
                sheetRow(
                    title: mode.title,
                    subtitle: mode == .crop ? "等比放大铺满屏幕，裁掉多余边缘" : "完整画面，保留黑边",
                    selected: mode == aspectMode
                ) {
                    aspectMode = mode
                    closeSheet()
                }
            }
        }
        .padding(.horizontal, 6)
    }

    private var equalizerSheet: some View {
        ScrollView {
            VStack(spacing: 2) {
                sheetRow(title: "关闭", subtitle: "不启用均衡器", selected: player.equalizerPreset == nil) {
                    player.setEqualizerPreset(nil)
                    closeSheet()
                }
                ForEach(Array(PlayerCore.equalizerPresetNames().enumerated()), id: \.offset) { index, name in
                    sheetRow(title: name, selected: player.equalizerPreset == index) {
                        player.setEqualizerPreset(index)
                        closeSheet()
                    }
                }
            }
            .padding(.horizontal, 6)
        }
        .frame(maxHeight: 320)
    }

    private var bookmarksSheet: some View {
        let url = player.currentMediaURL
        let items = url.map { library.bookmarks(for: $0) } ?? []
        return VStack(spacing: 2) {
            sheetRow(title: "添加当前书签", subtitle: formatDuration(player.timeMs), icon: "plus") {
                if let url {
                    library.addBookmark(url: url, positionMs: player.timeMs)
                }
            }
            if items.isEmpty {
                Text("还没有书签，播放时点「添加当前书签」记录位置")
                    .font(.system(size: 11))
                    .foregroundStyle(Palette.textTertiary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
            } else {
                ForEach(items) { bookmark in
                    HStack(spacing: 10) {
                        Button {
                            player.seek(toMs: bookmark.positionMs)
                            closeSheet()
                        } label: {
                            HStack {
                                Image(systemName: "bookmark.fill")
                                    .font(.system(size: 11))
                                    .foregroundStyle(accent.color)
                                Text(bookmark.label)
                                    .font(.system(size: 13, weight: .medium).monospacedDigit())
                                    .foregroundStyle(.white)
                                Spacer()
                            }
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)

                        Button {
                            if let url {
                                library.removeBookmark(url: url, id: bookmark.id)
                            }
                        } label: {
                            Image(systemName: "trash")
                                .font(.system(size: 11))
                                .foregroundStyle(Palette.textTertiary)
                                .padding(8)
                        }
                        .buttonStyle(.plain)
                    }
                    .background(
                        RoundedRectangle(cornerRadius: 9, style: .continuous)
                            .fill(Color.white.opacity(0.02))
                    )
                }
            }
        }
        .padding(.horizontal, 6)
    }

    private func sheetRow(
        title: String,
        subtitle: String? = nil,
        icon: String? = nil,
        selected: Bool = false,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 10) {
                if let icon {
                    Image(systemName: icon)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(accent.color)
                        .frame(width: 18)
                }
                VStack(alignment: .leading, spacing: 1) {
                    Text(title)
                        .font(.system(size: 13, weight: selected ? .semibold : .regular))
                        .foregroundStyle(.white)
                    if let subtitle {
                        Text(subtitle)
                            .font(.system(size: 10))
                            .foregroundStyle(Palette.textTertiary)
                    }
                }
                Spacer()
                if selected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(accent.color)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 9)
            .background(
                RoundedRectangle(cornerRadius: 9, style: .continuous)
                    .fill(selected ? accent.color.opacity(0.13) : Color.clear)
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func closeSheet() {
        withAnimation(.easeOut(duration: 0.16)) { activeSheet = nil }
    }

    // MARK: - Ended

    private var endedOverlay: some View {
        VStack(spacing: 14) {
            Image(systemName: "checkmark.circle")
                .font(.system(size: 40, weight: .light))
                .foregroundStyle(.white.opacity(0.8))
            Text("播放结束")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(.white)
            HStack(spacing: 10) {
                if canGoNext {
                    Button {
                        onPlay(playlist[playlistIndex + 1])
                    } label: {
                        Label("播放下一集", systemImage: "forward.end.fill")
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(accent.color)
                } else {
                    Button {
                        player.seek(toMs: 0)
                        player.play()
                    } label: {
                        Label("重新播放", systemImage: "arrow.counterclockwise")
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(accent.color)
                }
                Button("返回", action: onClose)
                    .buttonStyle(.bordered)
            }
            .controlSize(.large)
        }
        .padding(30)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Color.black.opacity(0.64))
        )
    }

    // MARK: - Scrub preview

    private func schedulePreview(at milliseconds: Double) {
        guard let url = player.currentMediaURL, url.isFileURL else {
            previewImage = nil
            return
        }
        previewTask?.cancel()
        previewTask = Task {
            try? await Task.sleep(nanoseconds: 90_000_000)
            if Task.isCancelled { return }
            let seconds = max(0, milliseconds / 1000)
            let image = await Task.detached(priority: .userInitiated) {
                PlayerView.frameImage(url: url, seconds: seconds)
            }.value
            await MainActor.run {
                if isScrubbing {
                    previewImage = image
                }
            }
        }
    }

    nonisolated private static func frameImage(url: URL, seconds: Double) -> NSImage? {
        let asset = AVURLAsset(url: url)
        let generator = AVAssetImageGenerator(asset: asset)
        generator.appliesPreferredTrackTransform = true
        generator.maximumSize = CGSize(width: 336, height: 189)
        let time = CMTime(seconds: seconds, preferredTimescale: 600)
        guard let cgImage = try? generator.copyCGImage(at: time, actualTime: nil) else {
            return nil
        }
        return NSImage(cgImage: cgImage, size: NSSize(width: cgImage.width, height: cgImage.height))
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
        guard player.isPlaying, activeSheet == nil else { return }
        hideTask = Task {
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            guard !Task.isCancelled else { return }
            await MainActor.run {
                if activeSheet == nil {
                    withAnimation(.easeIn(duration: 0.25)) { controlsVisible = false }
                }
            }
        }
    }
}

private struct PlayerSheetShell<Content: View>: View {
    let title: String
    let onClose: () -> Void
    @ViewBuilder let content: Content

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text(title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white)
                Spacer()
                Button(action: onClose) {
                    Image(systemName: "xmark")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Palette.textSecondary)
                        .frame(width: 26, height: 26)
                        .background(Circle().fill(Color.white.opacity(0.08)))
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 10)

            content
                .padding(.horizontal, 8)
                .padding(.bottom, 14)
        }
        .frame(width: 520)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Color(red: 0.075, green: 0.09, blue: 0.13))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .strokeBorder(Palette.border, lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.55), radius: 30, y: 10)
    }
}
