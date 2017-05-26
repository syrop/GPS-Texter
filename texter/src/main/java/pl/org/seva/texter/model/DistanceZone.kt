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
 */

package pl.org.seva.texter.model

import android.os.Parcel
import android.os.Parcelable

class DistanceZone(val min: Int, val max: Int) : Parcelable {
    var counter: Int = 0
        private set
    private val time: Long = System.currentTimeMillis()

    fun increaseCounter() {
        counter++
    }

    val delay: Long
        get() = System.currentTimeMillis() - time

    override fun toString(): String {
        return "[$min km - $max km]"
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeInt(min)
        out.writeInt(max)
        out.writeInt(counter)
        out.writeLong(time)
    }
}
