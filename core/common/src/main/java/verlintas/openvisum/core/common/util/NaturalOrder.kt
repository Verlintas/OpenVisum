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

package verlintas.openvisum.core.common.util

/**
 * Natural ordering for file names: digit runs compare numerically so that
 * "Ep 2" sorts before "Ep 10".
 */
object NaturalOrder : Comparator<String> {

    override fun compare(left: String, right: String): Int {
        var i = 0
        var j = 0
        while (i < left.length && j < right.length) {
            val leftChar = left[i]
            val rightChar = right[j]
            if (leftChar.isDigit() && rightChar.isDigit()) {
                val leftEnd = digitEnd(left, i)
                val rightEnd = digitEnd(right, j)
                val numeric = compareNumbers(left, i, leftEnd, right, j, rightEnd)
                if (numeric != 0) return numeric
                i = leftEnd
                j = rightEnd
            } else {
                val folded = leftChar.lowercaseChar().compareTo(rightChar.lowercaseChar())
                if (folded != 0) return folded
                i++
                j++
            }
        }
        return (left.length - i).compareTo(right.length - j)
    }

    private fun digitEnd(text: String, start: Int): Int {
        var index = start
        while (index < text.length && text[index].isDigit()) index++
        return index
    }

    private fun compareNumbers(
        left: String,
        leftStart: Int,
        leftEnd: Int,
        right: String,
        rightStart: Int,
        rightEnd: Int,
    ): Int {
        val leftSignificant = leftStart + left.substring(leftStart, leftEnd).indexOfFirst { it != '0' }
            .let { if (it == -1) leftEnd - 1 else it }
        val rightSignificant = rightStart + right.substring(rightStart, rightEnd).indexOfFirst { it != '0' }
            .let { if (it == -1) rightEnd - 1 else it }
        val leftLength = leftEnd - leftSignificant
        val rightLength = rightEnd - rightSignificant
        if (leftLength != rightLength) return leftLength - rightLength
        for (offset in 0 until leftLength) {
            val diff = left[leftSignificant + offset].compareTo(right[rightSignificant + offset])
            if (diff != 0) return diff
        }
        return 0
    }
}
