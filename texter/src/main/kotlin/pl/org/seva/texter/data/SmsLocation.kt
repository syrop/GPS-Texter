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
 * If you like this program, consider donating bitcoin: 3JVNWUeVH118S3pzU4hDgkUNwEeNarZySf
 */

package pl.org.seva.texter.data

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
class SmsLocation(
        var distance: Double = 0.0,  // in kilometers
        var minutes: Int = 0, // in minutes since midnight
        var direction: Int = 0,
        var speed: Double = 0.0
): Parcelable {

    val sign: String
        get() = when {
            direction == 0 -> ""
            direction < 0 -> "-"
            else -> "+"
        }

    override fun equals(other: Any?): Boolean {
        // ignore direction
        if (other !is SmsLocation) {
            return false
        }
        return other.distance == distance && other.minutes == minutes && other.speed == speed
    }

    override fun hashCode(): Int {
        // ignore direction
        val l = java.lang.Double.doubleToRawLongBits(distance) xor java.lang.Double.doubleToLongBits(speed)
        var result = (l shr 32 xor (l and 0x0000ffffL)).toInt()
        result = result xor minutes
        return result
    }

    fun setTime(minutes: Int): SmsLocation {
        this.minutes = minutes
        return this
    }
}
