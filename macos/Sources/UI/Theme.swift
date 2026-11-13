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

enum AccentTheme: String, CaseIterable, Identifiable {
    case brand
    case teal
    case green
    case amber
    case rose
    case violet
    case crimson
    case slate

    var id: String { rawValue }

    var title: String {
        switch self {
        case .brand: "品牌蓝"
        case .teal: "青碧"
        case .green: "松绿"
        case .amber: "琥珀"
        case .rose: "玫瑰"
        case .violet: "紫罗兰"
        case .crimson: "绯红"
        case .slate: "石墨"
        }
    }

    var color: Color {
        switch self {
        case .brand: Color(red: 0.49, green: 0.55, blue: 1.0)
        case .teal: Color(red: 0.30, green: 0.82, blue: 0.88)
        case .green: Color(red: 0.40, green: 0.73, blue: 0.42)
        case .amber: Color(red: 1.0, green: 0.72, blue: 0.30)
        case .rose: Color(red: 0.94, green: 0.38, blue: 0.57)
        case .violet: Color(red: 0.70, green: 0.53, blue: 1.0)
        case .crimson: Color(red: 0.94, green: 0.33, blue: 0.31)
        case .slate: Color(red: 0.56, green: 0.64, blue: 0.68)
        }
    }
}

enum Palette {
    static let background = Color(red: 0.043, green: 0.055, blue: 0.086)
    static let surface = Color(red: 0.075, green: 0.090, blue: 0.133)
    static let surfaceElevated = Color(red: 0.098, green: 0.118, blue: 0.173)
    static let border = Color.white.opacity(0.08)
    static let textSecondary = Color.white.opacity(0.62)
    static let textTertiary = Color.white.opacity(0.40)
}

extension View {
    func panelStyle(cornerRadius: CGFloat = 16) -> some View {
        background(
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                .fill(Palette.surface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                .strokeBorder(Palette.border, lineWidth: 1)
        )
    }
}
