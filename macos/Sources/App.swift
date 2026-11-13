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

@main
struct OpenVisumApp: App {
    @StateObject private var player = PlayerCore()
    @StateObject private var library = LibraryStore()

    init() {
        setbuf(stdout, nil)
        setbuf(stderr, nil)
        ovLog("app: init")
        if let argument = CommandLine.arguments.dropFirst().first {
            let url = URL(fileURLWithPath: argument)
            if FileManager.default.fileExists(atPath: url.path) {
                AppState.shared.pendingURL = url
                ovLog("app: pending launch url \(url.lastPathComponent)")
            }
        }
        DispatchQueue.main.async {
            NSApp.setActivationPolicy(.regular)
            NSApp.activate(ignoringOtherApps: true)
        }
    }

    var body: some Scene {
        WindowGroup {
            RootView(player: player)
                .environmentObject(player)
                .environmentObject(library)
                .frame(minWidth: 980, minHeight: 640)
                .preferredColorScheme(.dark)
        }
        .windowStyle(.hiddenTitleBar)
        .defaultSize(width: 1180, height: 760)
        .commands {
            CommandGroup(replacing: .newItem) {
                Button("打开文件…") {
                    AppState.shared.requestOpenFile()
                }
                .keyboardShortcut("o")
            }
        }
    }
}

final class AppState: ObservableObject {
    static let shared = AppState()

    @Published var pendingURL: URL?
    @Published var isPickerPresented = false

    func requestOpenFile() {
        isPickerPresented = true
    }
}
