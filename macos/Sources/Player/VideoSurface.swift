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
import SwiftUI

/// NSView that hands itself to libVLC once it is attached to a window.
final class VLCVideoNSView: NSView {
    weak var player: PlayerCore?

    override init(frame frameRect: NSRect) {
        super.init(frame: frameRect)
        wantsLayer = true
        layer?.backgroundColor = NSColor.black.cgColor
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        wantsLayer = true
    }

    override func viewDidMoveToWindow() {
        super.viewDidMoveToWindow()
        guard let player else { return }
        player.attachVideoView(self)
    }
}

struct VideoSurface: NSViewRepresentable {
    @ObservedObject var player: PlayerCore

    func makeNSView(context: Context) -> VLCVideoNSView {
        let view = VLCVideoNSView()
        view.player = player
        return view
    }

    func updateNSView(_ nsView: VLCVideoNSView, context: Context) {
        nsView.player = player
    }
}
