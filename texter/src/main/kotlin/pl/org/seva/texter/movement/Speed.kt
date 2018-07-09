/*
 * Copyright (C) 2017 Wiktor Nizio
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * If you like this program, consider donating bitcoin: 36uxha7sy4mv6c9LdePKjGNmQe8eK16aX6
 */

package pl.org.seva.texter.movement

import android.annotation.SuppressLint

fun getSpeedString(speed: Double, speedUnit: String): String {
    @SuppressLint("DefaultLocale")
    var result = String.format("%.1f", speed) + " " + speedUnit
    if (result.contains(".0")) {
        result = result.replace(".0", "")
    } else if (result.contains(",0")) {
        result = result.replace(",0", "")
    }
    return result
}
