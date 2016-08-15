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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import pl.org.seva.texter.activities.SettingsActivity;
import pl.org.seva.texter.listeners.IDistanceChangedListener;
import pl.org.seva.texter.listeners.IHomeChangedListener;
import pl.org.seva.texter.listeners.ILocationChangedListener;
import pl.org.seva.texter.listeners.IProviderListener;
import pl.org.seva.texter.preferences.MapPreference;
import pl.org.seva.texter.utils.Constants;

public class GPSManager implements LocationListener {

    private static final double ACCURACY_THRESHOLD = 0.1;  // a hundred meters

    private static final double EARTH_RADIUS = 6371.0;  // in kilometers

    private static GPSManager instance;

    /** Minimal distance (in meters) that will be counted between two subsequent updates. */
    private static final int MIN_DISTANCE = 10;

    private static final int SIGNIFICANT_TIME_LAPSE = 1000 * 60 * 2;

    private SharedPreferences preferences;

    private LocationManager locationManager;
    private final List<IDistanceChangedListener> distanceListeners;
    private final List<IHomeChangedListener> homeChangedListeners;
    private final List<ILocationChangedListener> locationChangedListeners;
    private final List<IProviderListener> providerListeners;
    /** Location last received from the update. */
    private Location location;
    /** Last calculated distance. */
    private double distance;
    /** Last calculated speed. */
    private double speed;

    private boolean initialized;

    private double homeLat;
    private double homeLon;
    private long time;

    private Context context;

    private GPSManager() {
        distanceListeners = new ArrayList<>();
        homeChangedListeners = new ArrayList<>();
        locationChangedListeners = new ArrayList<>();
        providerListeners = new ArrayList<>();
    }

    public static GPSManager getInstance() {
        if (instance == null) {
            synchronized (GPSManager.class) {
                if (instance == null) {
                    instance = new GPSManager();
                }
            }
        }
        return instance;
    }

    public static void shutdown() {
        synchronized (GPSManager.class) {
            if (instance != null) {
                instance.removeUpdates();
                instance = null;
            }
        }
    }

    public String getLocationUrl() {
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
        int updateFrequency = getUpdateFrequency();
        this.context = context;
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(this);
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    updateFrequency,
                    MIN_DISTANCE,
                    this);
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    updateFrequency,
                    MIN_DISTANCE,
                    this);
        }
    }

    private void removeUpdates() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(this);
    }

    public void updateHome() {
        updateDistance();
        String homeLocation = preferences.
                getString(SettingsActivity.HOME_LOCATION, Constants.DEFAULT_HOME_LOCATION);
        homeLat = MapPreference.parseLatitude(homeLocation);
        homeLon = MapPreference.parseLongitude(homeLocation);
        if (location != null) {
            distance = calculateDistance(
                    homeLat,
                    homeLon,
                    location.getLatitude(),
                    location.getLongitude());
        }
        synchronized (homeChangedListeners) {
            for (IHomeChangedListener l : homeChangedListeners) {
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
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
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
	
	public void addDistanceChangedListener(IDistanceChangedListener listener) {
		if (listener == null) {
            return;
        }
        synchronized (distanceListeners) {
            distanceListeners.remove(listener);
            distanceListeners.add(listener);
        }
	}

    public void removeDistanceChangedListener(IDistanceChangedListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (distanceListeners) {
            distanceListeners.remove(listener);
        }
    }

	public void addHomeChangedListener(IHomeChangedListener listener) {
        synchronized (homeChangedListeners) {
            removeHomeChangedListener(listener);
            homeChangedListeners.add(listener);
        }
	}

    public void removeHomeChangedListener(IHomeChangedListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (homeChangedListeners) {
            homeChangedListeners.remove(listener);
        }
    }

    public void addLocationChangedListener(ILocationChangedListener listener) {
        synchronized (locationChangedListeners) {
            removeLocationChangedListener(listener);
            locationChangedListeners.add(listener);
        }
    }

    public void removeLocationChangedListener(ILocationChangedListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (locationChangedListeners) {
            locationChangedListeners.remove(listener);
        }
    }

    public void addProviderListener(IProviderListener listener) {
        synchronized (providerListeners) {
            removeProviderListener(listener);
            providerListeners.add(listener);
        }
    }

    private void removeProviderListener(IProviderListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (providerListeners) {
            providerListeners.remove(listener);
        }
    }

	/**
	 * Determines whether one Location reading is better than the current Location fix.
	 * @param location  The new Location that you want to evaluate
	 * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	 */
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
	    boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

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

	/**
	 * Checks whether two providers are the same.
	 */
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

    public boolean isLocationServiceAvailable() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location.getAccuracy() >= ACCURACY_THRESHOLD * 1000.0) { // removed dependence on previous accuracy
            return;
        }
        if (!GPSManager.isBetterLocation(location, this.location)) {
            return;
        }
        TimerManager.getInstance().reset();
        long time = System.currentTimeMillis();
        speed = calculateSpeed(this.location, location, time - this.time);
        this.location = location;
        this.distance = calculateDistance();  // distance in kilometres
        this.time = time;
        synchronized (distanceListeners) {
            for (IDistanceChangedListener listener : distanceListeners) {
                listener.onDistanceChanged();
            }
        }
        synchronized (locationChangedListeners) {
            for (ILocationChangedListener listener : locationChangedListeners) {
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

    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
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

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // do nothing
    }

    @Override
    public void onProviderEnabled(String provider) {
        synchronized (providerListeners) {
            for (IProviderListener listener : providerListeners) {
                listener.onProviderEnabled();
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        synchronized (providerListeners) {
            for (IProviderListener listener : providerListeners) {
                listener.onProviderDisabled();
            }
        }
    }
}
