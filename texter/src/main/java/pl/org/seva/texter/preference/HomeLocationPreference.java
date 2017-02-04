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

package pl.org.seva.texter.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;

import pl.org.seva.texter.utils.Constants;

/**
 * Created by wiktor on 20.08.15.
 */
public class HomeLocationPreference extends Preference {

    private static final String HOME_LOCATION = "HOME_LOCATION";

    /** Latitude. */
    private double lat;
    /** * Longitude. */
    private double lon;

    public HomeLocationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return Constants.DEFAULT_HOME_LOCATION;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        String value;
        if (restorePersistedValue) {
            value = getPersistedString(HOME_LOCATION);
            lat = parseLatitude(value);
            lon = parseLongitude(value);
        }
        else {
            value = Constants.DEFAULT_HOME_LOCATION;
            persistString(value);
        }
        lat = parseLatitude(value);
        lon = parseLongitude(value);
    }

    public String toString() {
        return toString(lat, lon);
    }

    private static String toString(double lat, double lon) {
        return ("geo:") +
        (int) lat + "." +
        Double.toString(lat - (int) lat).substring(2, 8) + "," +
        (int) lon + "." +
        Double.toString(lon - (int) lon).substring(2, 8);
    }

    public static double parseLatitude(String uri) {
        String str = uri.substring(uri.indexOf(":") + 1, uri.indexOf(","));
        return Double.valueOf(str);
    }

    public static double parseLongitude(String uri) {
        String str = uri.substring(uri.indexOf(",") + 1);
        return Double.valueOf(str);
    }
}
