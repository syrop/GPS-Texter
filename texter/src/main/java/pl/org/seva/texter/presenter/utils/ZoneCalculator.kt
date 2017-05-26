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

package pl.org.seva.texter.presenter.utils

import android.util.SparseArray

import javax.inject.Inject
import javax.inject.Singleton

import pl.org.seva.texter.model.DistanceZone

@Singleton
class ZoneCalculator @Inject
internal constructor() {

    private val zones = SparseArray<DistanceZone>()

    // Needs to be called from a synchronized block.
    fun clearCache() {
        zones.clear()
    }

    // Needs to be called from a synchronized block.
    fun calculateZone(distance: Double): DistanceZone {
        var check = 0
        var min = 0
        val max: Int
        while (check < distance) {  // Calculate min and max.
            min = check
            check += Constants.KM_INTERVAL
        }
        max = check
        var zone: DistanceZone? = zones.get(min)
        if (zone == null) {
            clearCache()
            zone = DistanceZone(min, max)
            zone.increaseCounter()
            zones.put(min, zone)
        } else {
            zone.increaseCounter()
        }

        return zone
    }
}
