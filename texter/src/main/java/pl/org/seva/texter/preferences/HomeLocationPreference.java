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

package pl.org.seva.texter.preferences;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import pl.org.seva.texter.R;
import pl.org.seva.texter.listeners.ILocationChangedListener;
import pl.org.seva.texter.listeners.IPermissionGrantedListener;
import pl.org.seva.texter.managers.GPSManager;
import pl.org.seva.texter.managers.PermissionsManager;
import pl.org.seva.texter.utils.Constants;

/**
 * Created by wiktor on 20.08.15.
 */
public class HomeLocationPreference extends DialogPreference implements
        GoogleMap.OnMapLongClickListener,
        View.OnClickListener,
        ILocationChangedListener,
        GoogleMap.OnCameraIdleListener,
        IPermissionGrantedListener{

    private static final String HOME_LOCATION = "HOME_LOCATION";

    private static final String ZOOM_PROPERTY_NAME = "map_preference_gui_zoom";
    private static final float ZOOM_DEFAULT_VALUE = 7.5f;

    /** Latitude. */
    private double lat;
    /** * Longitude. */
    private double lon;
    private boolean toastShown;
    private float zoom;
    /** True indicates restoring from a saved state. */
    private boolean restored;

    private final Context context;
    private GoogleMap map;
    private Button useCurrentButton;

    private MapFragment mapFragment;

    public HomeLocationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        setDialogLayoutResource(R.layout.home_location_dialog);
        setDialogIcon(null);
    }

    @Override public void onClick(View v) {
        if (v == useCurrentButton) {
            LatLng loc = GPSManager.getInstance().getLatLng();
            if (loc != null) {
                lat = loc.latitude;
                lon = loc.longitude;
                updateMarker();
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(lat, lon)).
                                zoom(zoom).build();
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return Constants.DEFAULT_HOME_LOCATION;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        GPSManager.getInstance().removeLocationChangedListener(this);
        toastShown = false;
        if (positiveResult) {
            persistString(toString());
            PreferenceManager.getDefaultSharedPreferences(context).edit().
                    putFloat(ZOOM_PROPERTY_NAME, zoom).apply();
        }
        else {
            zoom = PreferenceManager.getDefaultSharedPreferences(context).
                    getFloat(ZOOM_PROPERTY_NAME, ZOOM_DEFAULT_VALUE);
            String value = getPersistedString(HOME_LOCATION);
            lat = parseLatitude(value);
            lon = parseLongitude(value);
        }
        GPSManager.getInstance().removeLocationChangedListener(this);

        if (mapFragment != null) {
            // Without enclosing in the if, throws:
            // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
            ((android.support.v4.app.FragmentActivity) getContext()).
                    getFragmentManager().beginTransaction().remove(mapFragment).commit();
        }

        super.onDialogClosed(positiveResult);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        // Create instance of custom BaseSavedState.
        final SavedState myState = new SavedState(superState);
        // Set the state's value with the class member that holds current
        // setting value.
        myState.lat = lat;
        myState.lon = lon;
        myState.toastShown = toastShown;
        myState.zoom = zoom;

        // Equals null inside the setting activity if map has not been invoked.
        if (mapFragment != null) {
            // If called after onSaveInstanceState, throws:
            // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
            ((android.support.v4.app.FragmentActivity) getContext()).
                    getFragmentManager().beginTransaction().remove(mapFragment).commit();
            mapFragment = null;
        }

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // Cast state to custom BaseSavedState and pass to superclass
        SavedState myState = (SavedState) state;
        lat = myState.lat;
        lon = myState.lon;
        toastShown = myState.toastShown;
        zoom = myState.zoom;
        restored = true;

        super.onRestoreInstanceState(myState.getSuperState());
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

    @Override
    protected View onCreateDialogView() {
        final View result = super.onCreateDialogView();

        MapsInitializer.initialize(context);

        mapFragment = (MapFragment)
                ((Activity) getContext()).getFragmentManager().findFragmentById(R.id.map);

        useCurrentButton = (Button) result.findViewById(R.id.current_location_button);
        useCurrentButton.setOnClickListener(HomeLocationPreference.this);
        boolean locationPermitted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
        boolean locationAvailable = locationPermitted &&
                GPSManager.getInstance().isLocationAvailable();
        useCurrentButton.setEnabled(locationAvailable);
        if (!locationAvailable) {
            GPSManager.getInstance().addLocationChangedListener(HomeLocationPreference.this);
        }
        if (!toastShown) {
            Toast.makeText(
                    context,
                    R.string.long_press,
                    Toast.LENGTH_SHORT).show();
            toastShown = true;
        }
        GPSManager.getInstance().addLocationChangedListener(HomeLocationPreference.this);

        mapFragment.getMapAsync(googleMap -> {
            map = googleMap;
            if (ContextCompat.checkSelfPermission(
                    getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
            }
            else {
                PermissionsManager.getInstance().addPermissionGrantedListener(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        HomeLocationPreference.this);
            }
            zoom = PreferenceManager.getDefaultSharedPreferences(context).
                    getFloat(ZOOM_PROPERTY_NAME, ZOOM_DEFAULT_VALUE);

            updateMarker();
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat, lon)).zoom(zoom).build();
            if (!restored) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            else {
                map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }

            map.setOnMapLongClickListener(HomeLocationPreference.this);
            map.setOnCameraIdleListener(HomeLocationPreference.this);
        });

        if (!locationPermitted) {
            useCurrentButton.setEnabled(false);
            PermissionsManager.getInstance().addPermissionGrantedListener(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    this);
        }

        return result;
    }

    private void updateMarker() {
        map.clear();
        MarkerOptions marker = new MarkerOptions().position(
                new LatLng(lat, lon)).
                title(context.getString(R.string.home));
        // Changing marker icon
        marker.icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        // adding marker
        map.addMarker(marker);
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

    @Override
    public void onMapLongClick(LatLng point) {
        lat = point.latitude;
        lon = point.longitude;
        updateMarker();
        useCurrentButton.setEnabled(true);
    }

    @Override
    public void onLocationChanged() {
        if (map == null || useCurrentButton == null) {
            return;
        }
        useCurrentButton.setEnabled(true);
    }

    @Override
    public void onCameraIdle() {
        zoom = map.getCameraPosition().zoom;
    }

    @Override
    public void onPermissionGranted(String permission) {
        if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) && map != null &&
                ContextCompat.checkSelfPermission(
                    getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) ==PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            useCurrentButton.setEnabled(false);
            PermissionsManager.getInstance().
                    removePermissionGrantedListener(Manifest.permission.ACCESS_FINE_LOCATION, this);
        }
    }

    private static class SavedState extends BaseSavedState {
        private double lat;
        private double lon;
        private boolean toastShown;
        private float zoom;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel source) {
            super(source);
            lat = source.readDouble();
            lon = source.readDouble();
            toastShown = source.readInt() != 0;
            zoom = source.readFloat();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeDouble(lat);
            dest.writeDouble(lon);
            dest.writeInt(toastShown ? 1 : 0);
            dest.writeFloat(zoom);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
            new Parcelable.Creator<SavedState>() {

                public SavedState createFromParcel(Parcel in) {
                    return new SavedState(in);
                }

                public SavedState[] newArray(int size) {
                    return new SavedState[size];
                }
            };
    }
}
