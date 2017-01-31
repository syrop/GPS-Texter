package pl.org.seva.texter.utils;

import android.location.Location;

public class Calculator {

    private static final double EARTH_RADIUS = 6371.0;  // [km]

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    public static double calculateSpeed(Location loc1, Location loc2, long time) {
        double seconds = (double) time / 1000.0;
        double hours = seconds / 3600.0;
        if (hours == 0.0) {
            return 0.0;
        }
        double distance = Calculator.calculateDistance(
                loc1.getLatitude(),
                loc1.getLongitude(),
                loc2.getLatitude(),
                loc2.getLongitude());
        return distance / hours;
    }

    private Calculator() {
        //
    }
}
