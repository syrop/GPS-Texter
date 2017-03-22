package pl.org.seva.texter.manager;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LastLocationManager {

    /** Location last received from the update. */
    private Location location;
    /** Last calculated distance. */
    private double distance;
    /** Last calculated speed. */
    private double speed;

    @Inject public LastLocationManager() {
    }

    public Location getLocation() {
        return  location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    String getLocationUrl() {
        if (location == null) {
            return "";
        }
        return "http://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
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
}
