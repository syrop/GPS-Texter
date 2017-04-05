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

import android.location.Location;

public class DistanceCalculator {

    static {
        System.loadLibrary("native-lib");
    }

    private DistanceCalculator() {
    }

    public static double distanceInKm(double lat1, double lon1, double lat2, double lon2) {
        return distance(lat1, lon1, lat2, lon2);
    }


    public static double speedInKph(Location loc1, Location loc2, long time) {
        return speed(
                loc1.getLatitude(),
                loc1.getLongitude(),
                loc2.getLatitude(),
                loc2.getLongitude(),
                time);
    }

    private static native double distance(double lat1, double lon1, double lat2, double lon2);
    private static native double speed(double lat1, double lon1, double lat2, double lon2, long time);
}
