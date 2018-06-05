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
 * If you like this program, consider donating bitcoin: 37vHXbpPcDBwcCTpZfjGL63JRwn6FPiXTS
 */

package pl.org.seva.texter.movement

import android.location.Location

object DistanceCalculator {

    init {
        System.loadLibrary("native-lib")
    }

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double) =
            distance(lat1, lon1, lat2, lon2)


    fun speedKph(loc1: Location, loc2: Location, time: Long) = speed(
            loc1.latitude,
            loc1.longitude,
            loc2.latitude,
            loc2.longitude,
            time)

    private external fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double
    private external fun speed(lat1: Double, lon1: Double, lat2: Double, lon2: Double, time: Long): Double
}
