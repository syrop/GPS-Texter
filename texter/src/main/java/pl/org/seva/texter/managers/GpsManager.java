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

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import pl.org.seva.texter.activities.SettingsActivity;
import pl.org.seva.texter.listeners.DistanceListener;
import pl.org.seva.texter.listeners.HomeLocationListener;
import pl.org.seva.texter.listeners.LocationListener;
import pl.org.seva.texter.listeners.ProviderListener;
import pl.org.seva.texter.preferences.HomeLocationPreference;
import pl.org.seva.texter.utils.Constants;

public class GpsManager implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final double ACCURACY_THRESHOLD = 0.1;  // a hundred meters

    private static final double EARTH_RADIUS = 6371.0;  // in kilometers

    private static GpsManager instance;

    /** Minimal distance (in meters) that will be counted between two subsequent updates. */
    private static final float MIN_DISTANCE = 5.0f;

    private static final int SIGNIFICANT_TIME_LAPSE = 1000 * 60 * 2;

    private SharedPreferences preferences;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private final List<DistanceListener> distanceListeners;
    private final List<HomeLocationListener> homeChangedListeners;
    private final List<LocationListener> locationChangedListeners;
    private final List<ProviderListener> providerListeners;

    /** Location last received from the update. */
    private Location location;
    /** Last calculated distance. */
    private double distance;
    /** Last calculated speed. */
    private double speed;

    private boolean initialized;
    private boolean connected;
    private boolean paused;

    private double homeLat;
    private double homeLon;
    private long time;


    private GpsManager() {
        distanceListeners = new ArrayList<>();
        homeChangedListeners = new ArrayList<>();
        locationChangedListeners = new ArrayList<>();
        providerListeners = new ArrayList<>();
    }

    public static GpsManager getInstance() {
        if (instance == null) {
            synchronized (GpsManager.class) {
                if (instance == null) {
                    instance = new GpsManager();
                }
            }
        }
        return instance;
    }

    public static void shutdown(Context context) {
        synchronized (GpsManager.class) {
            if (instance != null) {
                instance.removeUpdates(context);
                instance = null;
            }
        }
    }

    String getLocationUrl() {
        if (location == null) {
            return "";
        }
        return "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
    }

    /**
     * @return minimum time between two subsequent updates from one provider (in millis)
     */
    private int getUpdateFrequency() {
        String timeStr = preferences.getString(SettingsActivity.LOCATION_UPDATE_FREQUENCY, "");
        int seconds = timeStr.length() > 0 ? Integer.valueOf(timeStr) : 0;
        return seconds * 1000;
    }

    public double getDistance() {
        return distance;
    }

    public double getSpeed() {
        return speed;
    }

    public void updateFrequencyChanged(Context context) {
        requestLocationUpdates(context);
    }

    private void requestLocationUpdates(Context context) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            googleApiClient.connect();
        }
    }

    private void removeUpdates(Context context) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        googleApiClient.disconnect();
    }

    public void updateHome() {
        updateDistance();
        String homeLocation = preferences.
                getString(SettingsActivity.HOME_LOCATION, Constants.DEFAULT_HOME_LOCATION);
        homeLat = HomeLocationPreference.parseLatitude(homeLocation);
        homeLon = HomeLocationPreference.parseLongitude(homeLocation);
        if (location != null) {
            distance = calculateDistance(
                    homeLat,
                    homeLon,
                    location.getLatitude(),
                    location.getLongitude());
        }
        synchronized (homeChangedListeners) {
            //noinspection Convert2streamapi
            for (HomeLocationListener l : homeChangedListeners) {
                l.onHomeChanged();
            }
        }
    }

    /**
     * Initializes the GPS.
     *
     * Actions taken depend on whether the app possesses the permission. If not, has to be
     * called again.
     *
     * @param activity the calling activity
     * @return true if actions requiring the permission have been performed
     */
    public boolean init(Activity activity) {
        boolean granted = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
        if (initialized) {
            return granted;
        }
        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(activity)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        activity.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                locationSettingsChanged();
            }
        }, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        if (location == null) {
            location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        }
        updateHome();

        if (granted) {
            initWithPermissions(activity);
            initialized = true;
        }

        return granted;
    }

    private void initWithPermissions(Context context) {
        requestLocationUpdates(context);
    }

    public void addDistanceChangedListener(DistanceListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (distanceListeners) {
            distanceListeners.remove(listener);
            distanceListeners.add(listener);
        }
    }

    public void removeDistanceListener(DistanceListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (distanceListeners) {
            distanceListeners.remove(listener);
        }
    }

    public void addHomeChangedListener(HomeLocationListener listener) {
        synchronized (homeChangedListeners) {
            removeHomeChangedListener(listener);
            homeChangedListeners.add(listener);
        }
    }

    public void removeHomeChangedListener(HomeLocationListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (homeChangedListeners) {
            homeChangedListeners.remove(listener);
        }
    }

    public void addLocationChangedListener(LocationListener listener) {
        synchronized (locationChangedListeners) {
            removeLocationChangedListener(listener);
            locationChangedListeners.add(listener);
        }
    }

    public void removeLocationChangedListener(LocationListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (locationChangedListeners) {
            locationChangedListeners.remove(listener);
        }
    }

    public void addProviderListener(ProviderListener listener) {
        synchronized (providerListeners) {
            removeProviderListener(listener);
            providerListeners.add(listener);
        }
    }

    private void removeProviderListener(ProviderListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (providerListeners) {
            providerListeners.remove(listener);
        }
    }

    private static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > SIGNIFICANT_TIME_LAPSE;
        boolean isSignificantlyOlder = timeDelta < -SIGNIFICANT_TIME_LAPSE;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        }
        else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(
                location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        }
        else if (isNewer && !isLessAccurate) {
            return true;
        }
        else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private double getHomeLat() {
        return homeLat;
    }

    private double getHomeLng() {
        return homeLon;
    }

    public LatLng getHomeLatLng() {
        return new LatLng(getHomeLat(), getHomeLng());
    }

    public LatLng getLatLng() {
        if (location == null) {
            return null;
        }
        return new LatLng(location.getLatitude(), location.getLongitude());
    }

    public boolean isLocationAvailable() {
        return location != null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location.getAccuracy() >= ACCURACY_THRESHOLD * 1000.0) {
            return;
        }
        if (!GpsManager.isBetterLocation(location, this.location)) {
            return;
        }
        TimerManager.getInstance().reset();
        long time = System.currentTimeMillis();
        speed = calculateSpeed(this.location, location, time - this.time);
        this.location = location;
        this.distance = calculateDistance();  // distance in kilometres
        this.time = time;
        synchronized (distanceListeners) {
            //noinspection Convert2streamapi
            for (DistanceListener listener : distanceListeners) {
                listener.onDistanceChanged();
            }
        }
        synchronized (locationChangedListeners) {
            //noinspection Convert2streamapi
            for (LocationListener listener : locationChangedListeners) {
                listener.onLocationChanged();
            }
        }
    }

    private void updateDistance() {
        if (location == null) {
            return;
        }
        distance = calculateDistance();
    }

    private double calculateDistance() {
        return calculateDistance(  // distance in kilometres
                location.getLatitude(),
                location.getLongitude(),
                getHomeLat(),
                getHomeLng());
    }

    private double calculateSpeed(Location loc1, Location loc2, long time) {
        if (loc1 == null || loc2 == null || this.time == 0 || time == 0 ||
                loc1.getLatitude() == loc2.getLatitude() &&
                        loc1.getLongitude() == loc2.getLongitude()) {
            return 0.0;
        }
        double seconds = (double) time / 1000.0;
        double hours = seconds / 3600.0;
        if (hours == 0.0) {
            return 0.0;
        }
        double distance = calculateDistance(
                loc1.getLatitude(),
                loc1.getLongitude(),
                loc2.getLatitude(),
                loc2.getLongitude());
        return distance / hours;
    }

    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        connected = true;
        int updateFrequency = getUpdateFrequency();
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(updateFrequency)
                .setSmallestDisplacement(MIN_DISTANCE);

        //noinspection MissingPermission
        LocationServices.FusedLocationApi.
                requestLocationUpdates(googleApiClient, locationRequest, this);
        callProviderListener();
    }

    public void callProviderListener() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        PendingResult<LocationSettingsResult> pendingResult =
                LocationServices.SettingsApi.checkLocationSettings(
                        googleApiClient,
                        builder.build());
        pendingResult.setResultCallback(locationSettingsResult -> {
            connected = locationSettingsResult.getLocationSettingsStates().isLocationUsable();
            if (connected) {
                synchronized (providerListeners) {
                    //noinspection Convert2streamapi
                    for (ProviderListener listener : providerListeners) {
                        listener.onProviderEnabled();
                    }
                }
            }
            else {
                synchronized (providerListeners) {
                    //noinspection Convert2streamapi
                    for (ProviderListener listener : providerListeners) {
                        listener.onProviderDisabled();
                    }
                }
            }
        });
    }

    public void pauseUpdates() {
        if (paused) {
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);

        paused = true;

    }

    public void resumeUpdates() {
        if (!paused) {
            return;
        }
        //noinspection MissingPermission
        LocationServices.FusedLocationApi.
                requestLocationUpdates(googleApiClient, locationRequest, this);

        paused = false;

    }

    private void locationSettingsChanged() {
        if (locationRequest == null) {
            return;
        }

        callProviderListener();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }
}
