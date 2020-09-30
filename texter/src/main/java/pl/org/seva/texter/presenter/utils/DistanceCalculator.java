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

    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
      float[] distance = new float[2];
      Location.distanceBetween(lat1, lon1, lat2, lon2, distance);
      return distance[0] / 1000.0;
    }

    public static double speedKph(Location loc1, Location loc2, long time) {
      double seconds = time / 1000.0;
      double hours = seconds / 3600.0;
      double distance = distanceKm(
          loc1.getLatitude(),
          loc1.getLongitude(),
          loc2.getLatitude(),
          loc2.getLongitude());
      return distance / hours;
    }
}
