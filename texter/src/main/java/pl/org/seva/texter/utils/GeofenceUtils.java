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

package pl.org.seva.texter.utils;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wiktor on 15.08.16.
 */
@SuppressWarnings("unused")
class GeofenceUtils {

    private GeofenceUtils() {
        //
    }

    List<Geofence> createGeofences(LatLng home, int count) {
        List<Geofence> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int radius = (i + 1) * Constants.KM_INTERVAL * 1000;
            result.add(new Geofence.Builder()
                    .setRequestId("km_" + radius)
                    .setCircularRegion(
                            home.latitude,
                            home.longitude,
                            radius)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL |
                        Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setLoiteringDelay(Constants.TIME_IN_ZONE)
                    .build());
        }

        return result;
    }

    public GeofencingRequest getGeofencingRequest(List<Geofence> geofences) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(0)
                .addGeofences(geofences)
                .build();
    }
}
