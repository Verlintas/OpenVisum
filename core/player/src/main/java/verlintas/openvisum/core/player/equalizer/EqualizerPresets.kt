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

package verlintas.openvisum.core.player.equalizer

import org.videolan.libvlc.MediaPlayer

object EqualizerPresets {

    val presetNames: List<String>
        get() = List(MediaPlayer.Equalizer.getPresetCount()) { index ->
            MediaPlayer.Equalizer.getPresetName(index)
        }

    val bandCount: Int
        get() = MediaPlayer.Equalizer.getBandCount()

    fun bandFrequency(index: Int): Float = MediaPlayer.Equalizer.getBandFrequency(index)

    fun amplitudesForPreset(index: Int): List<Float> {
        val equalizer = MediaPlayer.Equalizer.createFromPreset(index)
        return List(bandCount) { band -> equalizer.getAmp(band) }
    }
}
