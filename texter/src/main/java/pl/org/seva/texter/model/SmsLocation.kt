/*
 * Copyright (C) 2016 Wiktor Nizio
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
 */

package pl.org.seva.texter.model

import android.os.Parcel
import android.os.Parcelable

class SmsLocation
    : Parcelable {
    var distance: Double = 0.toDouble()  // in kilometers
    var minutes: Int = 0
        private set // in minutes since midnight
    var direction: Int = 0
    var speed: Double = 0.toDouble()

    override fun equals(other: Any?): Boolean {
        // ignore direction
        if (other !is SmsLocation) {
            return false
        }
        val model = other
        return model.distance == distance && model.minutes == minutes && model.speed == speed
    }

    override fun hashCode(): Int {
        // ignore direction
        val l = java.lang.Double.doubleToRawLongBits(distance) xor java.lang.Double.doubleToLongBits(speed)
        var result = (l shr 32 xor (l and 0x0000ffffL)).toInt()
        result = result xor minutes
        return result
    }

    val sign: String
        get() {
            if (direction == 0) {
                return ""
            } else if (direction < 0) {
                return "-"
            } else {
                return "+"
            }
        }

    fun setTime(minutes: Int): SmsLocation {
        this.minutes = minutes
        return this
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeDouble(distance)
        out.writeInt(minutes)
        out.writeInt(direction)
        out.writeDouble(speed)
    }
}
