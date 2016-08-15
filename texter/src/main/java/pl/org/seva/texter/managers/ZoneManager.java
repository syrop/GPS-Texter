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

package pl.org.seva.texter.managers;

import android.util.SparseArray;

import pl.org.seva.texter.model.ZoneModel;
import pl.org.seva.texter.utils.Constants;

/**
 * Created by wiktor on 5/26/16.
 */
public class ZoneManager {

    private static ZoneManager instance;

    private final SparseArray<ZoneModel> zones;

    public static ZoneManager getInstance() {
        if (instance == null ) {
            synchronized (ZoneManager.class) {
                if (instance == null) {
                    instance = new ZoneManager();
                }
            }
        }
        return instance;
    }

    public static void shutdown() {
        synchronized (ZoneManager.class) {
            instance = null;
        }
    }

    private ZoneManager() {
        zones = new SparseArray<>();
    }

    // Needs to be called from a synchronized block.
    public void clear() {
        zones.clear();
    }

    // Needs to be called from a synchronized block.
    public ZoneModel zone(double distance) {
        int check = 0;
        int min = 0;
        int max;
        while (check < distance) {  // Calculate min and max.
            min = check;
            check += Constants.KM_INTERVAL;
        }
        max = check;
        ZoneModel zone = zones.get(min);
        if (zone == null) {
            clear();
            zone = new ZoneModel(min, max);
            zone.increaseCounter();
            zones.put(min, zone);
        }
        else {
            zone.increaseCounter();
        }

        return zone;
    }
}
