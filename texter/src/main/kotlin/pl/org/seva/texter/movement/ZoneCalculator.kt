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
 * If you like this program, consider donating bitcoin: bc1qncxh5xs6erq6w4qz3a7xl7f50agrgn3w58dsfp
 */

package pl.org.seva.texter.movement

import android.util.SparseArray
import pl.org.seva.texter.main.Constants

import pl.org.seva.texter.data.DistanceZone
import pl.org.seva.texter.main.instance

val zoneCalculator get() = instance<ZoneCalculator>()

class ZoneCalculator {

    private val zones = SparseArray<DistanceZone>()

    fun clearCache() = zones.clear()

    fun calculateZone(distance: Double): DistanceZone {
        var check = 0
        var min = 0
        val max: Int
        while (check < distance) {
            min = check
            check += Constants.KM_THRESHOLD
        }
        max = check
        var zone: DistanceZone? = zones.get(min)
        if (zone?.increaseCounter() == null) {
            clearCache()
            zone = DistanceZone(min, max)
            zone.increaseCounter()
            zones.put(min, zone)
        }

        return zone
    }
}
