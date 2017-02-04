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

package pl.org.seva.texter.manager;

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

import pl.org.seva.texter.activity.SettingsActivity;
import pl.org.seva.texter.preference.HomeLocationPreference;
import pl.org.seva.texter.utils.Calculator;
import pl.org.seva.texter.utils.Constants;
import rx.Observable;
import rx.subjects.PublishSubject;

public class GpsManager implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static final double ACCURACY_THRESHOLD = 0.1;  // equals to one hundred meters

    private static GpsManager instance;

    /** Minimal distance (in meters) that will be counted between two subsequent updates. */
    private static final float MIN_DISTANCE = 5.0f;

    private static final int SIGNIFICANT_TIME_LAPSE = 1000 * 60 * 2;

    private SharedPreferences preferences;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private final PublishSubject<Void> distanceSubject;
    private final PublishSubject<Void> homeChangedSubject;
    private final PublishSubject<Void> providerEnabledSubject;
    private final PublishSubject<Void> providerDisabledSubject;
    private final PublishSubject<Void> locationChangedSubject;

    /** Location last received from the update. */
    private Location location;
    /** Last calculated distance. */
    private double distance;
    /** Last calculated speed. */
    private double speed;

    private boolean initialized;
    private boolean connected;

    private double homeLat;
    private double homeLon;
    private long time;


    private GpsManager() {
        distanceSubject = PublishSubject.create();
        homeChangedSubject = PublishSubject.create();
        locationChangedSubject = PublishSubject.create();
        providerEnabledSubject = PublishSubject.create();
        providerDisabledSubject = PublishSubject.create();
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
                instance.instanceShutdown(context);
                instance = null;
            }
        }
        ActivityRecognitionManager.shutdown();
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

    private void instanceShutdown(Context context) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (googleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
    }

    public void onHomeLocationChanged() {
        updateDistance();
        String homeLocation = preferences.
                getString(SettingsActivity.HOME_LOCATION, Constants.DEFAULT_HOME_LOCATION);
        homeLat = HomeLocationPreference.parseLatitude(homeLocation);
        homeLon = HomeLocationPreference.parseLongitude(homeLocation);
        if (location != null) {
            distance = Calculator.calculateDistance(
                    homeLat,
                    homeLon,
                    location.getLatitude(),
                    location.getLongitude());
        }
        homeChangedSubject.onNext(null);
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

        onHomeLocationChanged();

        if (granted) {
            AfterPermissionGranted(activity);
            initialized = true;
        }

        return granted;
    }

    private void AfterPermissionGranted(Context context) {
        requestLocationUpdates(context);
        ActivityRecognitionManager.getInstance().init(context);
    }

    public Observable<Void> distanceChangedListener() {
        return distanceSubject.asObservable();
    }

    public Observable<Void> homeChangedListener() {
        return homeChangedSubject.asObservable();
    }

    public Observable<Void> locationChangedListener() {
        return locationChangedSubject.asObservable();
    }

    public Observable<Void> providerEnabledListener() {
        return providerEnabledSubject.asObservable();
    }

    public Observable<Void> providerDisabledListener() {
        return providerDisabledSubject.asObservable();
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
        speed = calculateSpeedOrReturnZero(this.location, location, time - this.time);
        this.location = location;
        this.distance = calculateCurrentDistance();  // distance in kilometres
        this.time = time;
        distanceSubject.onNext(null);
        locationChangedSubject.onNext(null);
    }

    private void updateDistance() {
        if (location == null) {
            return;
        }
        distance = calculateCurrentDistance();
    }

    private double calculateCurrentDistance() {
        return Calculator.calculateDistance(  // distance in kilometres
                location.getLatitude(),
                location.getLongitude(),
                getHomeLat(),
                getHomeLng());
    }

    private double calculateSpeedOrReturnZero(Location loc1, Location loc2, long time) {
        if (loc1 == null || loc2 == null || this.time == 0 || time == 0 ||
                loc1.getLatitude() == loc2.getLatitude() &&
                        loc1.getLongitude() == loc2.getLongitude()) {
            return 0.0;
        }
        if (time == 0.0) {
            return 0.0;
        }
        return Calculator.calculateSpeed(loc1, loc2, time);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        connected = true;

        if (location == null) {
            //noinspection MissingPermission
            location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        }

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
                providerEnabledSubject.onNext(null);
            }
            else {
                providerDisabledSubject.onNext(null);
            }
        });
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
