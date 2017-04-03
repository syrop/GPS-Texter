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

package pl.org.seva.texter.presenter.utils;

import android.util.SparseArray;

import javax.inject.Inject;
import javax.inject.Singleton;

import pl.org.seva.texter.model.DistanceZone;

@Singleton
public class ZoneCalculator {

    private final SparseArray<DistanceZone> zones = new SparseArray<>();

    @Inject
    ZoneCalculator() {
    }

    // Needs to be called from a synchronized block.
    public void clearCache() {
        zones.clear();
    }

    // Needs to be called from a synchronized block.
    public DistanceZone calculateZone(double distance) {
        int check = 0;
        int min = 0;
        int max;
        while (check < distance) {  // Calculate min and max.
            min = check;
            check += Constants.KM_INTERVAL;
        }
        max = check;
        DistanceZone zone = zones.get(min);
        if (zone == null) {
            clearCache();
            zone = new DistanceZone(min, max);
            zone.increaseCounter();
            zones.put(min, zone);
        }
        else {
            zone.increaseCounter();
        }

        return zone;
    }
}
