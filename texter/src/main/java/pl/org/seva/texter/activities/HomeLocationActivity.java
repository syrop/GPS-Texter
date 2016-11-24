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

package pl.org.seva.texter.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import pl.org.seva.texter.R;
import pl.org.seva.texter.databinding.ActivityHomeLocationBinding;
import pl.org.seva.texter.listeners.ILocationChangedListener;
import pl.org.seva.texter.listeners.IPermissionGrantedListener;
import pl.org.seva.texter.managers.GPSManager;
import pl.org.seva.texter.managers.PermissionsManager;
import pl.org.seva.texter.utils.Constants;

/**
 * Created by wiktor on 04.09.16.
 */
public class HomeLocationActivity extends AppCompatActivity implements
        ILocationChangedListener,
        IPermissionGrantedListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraIdleListener {

    private static final String STATE = "STATE";
    private static final String ZOOM_PROPERTY_NAME = "map_preference_gui_zoom";
    private static final float ZOOM_DEFAULT_VALUE = 7.5f;
    private static final String HOME_LOCATION_PREFERENCE = "HOME_LOCATION";

    /** Latitude. */
    private double lat;
    /** Longitude. */
    private double lon;
    private boolean toastShown;
    private float zoom;
    private boolean currentLocationAvailable;

    private GoogleMap map;
    private Button useCurrentButton;
    /** True indicates restoring from a saved state. */
    private boolean restored;
    private MapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }

        String value = getPersistedString();
        lat = parseLatitude(value);
        lon = parseLongitude(value);

        ActivityHomeLocationBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_home_location);
        FragmentManager fm = getFragmentManager();
        mapFragment = (MapFragment) fm.findFragmentByTag("map");
        if (mapFragment == null) {
            mapFragment = new MapFragment();
            fm.beginTransaction().add(binding.mapContainer.getId(), mapFragment, "map").commit();
        }
        MapsInitializer.initialize(this);
        useCurrentButton = binding.currentLocationButton;

        boolean locationPermitted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
        boolean locationAvailable = locationPermitted &&
                GPSManager.getInstance().isLocationAvailable();
        useCurrentButton.setEnabled(locationAvailable);
        if (!toastShown) {
            Toast.makeText(
                    this,
                    R.string.long_press,
                    Toast.LENGTH_SHORT).show();
            toastShown = true;
        }
        GPSManager.getInstance().addLocationChangedListener(HomeLocationActivity.this);

        mapFragment.getMapAsync(googleMap -> {
            map = googleMap;
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
            }
            else {
                PermissionsManager.getInstance().addPermissionGrantedListener(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        this);
            }
            zoom = PreferenceManager.getDefaultSharedPreferences(this).
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

            map.setOnMapLongClickListener(this);
            map.setOnCameraIdleListener(this);
        });

        if (!locationPermitted) {
            useCurrentButton.setEnabled(false);
            PermissionsManager.getInstance().addPermissionGrantedListener(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    this);
        }
    }

    @Override
    public void onBackPressed() {
        GPSManager.getInstance().removeLocationChangedListener(this);
        toastShown = false;

        persistString(toString());
        PreferenceManager.getDefaultSharedPreferences(this).edit().
                putFloat(ZOOM_PROPERTY_NAME, zoom).apply();
        GPSManager.getInstance().updateHome();
        GPSManager.getInstance().removeLocationChangedListener(this);

        if (mapFragment != null) {
            // Without enclosing in the if, throws:
            // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
            getFragmentManager().beginTransaction().remove(mapFragment).commit();
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Create instance of custom BaseSavedState.
        final HomeLocationActivity.SavedState myState
                = new HomeLocationActivity.SavedState();
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
            getFragmentManager().beginTransaction().remove(mapFragment).commit();
            mapFragment = null;
        }
    }

    protected void onRestoreInstanceState(Bundle state) {
        // Cast state to custom BaseSavedState and pass to superclass
        HomeLocationActivity.SavedState myState = (HomeLocationActivity.SavedState) state.get(STATE);
        lat = myState.lat;
        lon = myState.lon;
        toastShown = myState.toastShown;
        zoom = myState.zoom;
        restored = true;
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

    private static double parseLatitude(String uri) {
        String str = uri.substring(uri.indexOf(":") + 1, uri.indexOf(","));
        return Double.valueOf(str);
    }

    private static double parseLongitude(String uri) {
        String str = uri.substring(uri.indexOf(",") + 1);
        return Double.valueOf(str);
    }

    private void persistString(String val) {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putString(HOME_LOCATION_PREFERENCE, val).apply();
    }

    private String getPersistedString() {
        return PreferenceManager.getDefaultSharedPreferences(this).
                getString(HOME_LOCATION_PREFERENCE, Constants.DEFAULT_HOME_LOCATION);
    }

    private void updateMarker() {
        map.clear();
        MarkerOptions marker = new MarkerOptions().position(
                new LatLng(lat, lon)).
                title(getString(R.string.home));
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        map.addMarker(marker);
    }

    @Override
    public void onLocationChanged() {
        if (map == null || useCurrentButton == null) {
            return;
        }
        currentLocationAvailable = true;
        useCurrentButton.setEnabled(true);
    }

    @Override
    public void onPermissionGranted(String permission) {
        if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) && map != null &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            useCurrentButton.setEnabled(false);
            PermissionsManager.getInstance().
                removePermissionGrantedListener(Manifest.permission.ACCESS_FINE_LOCATION, this);
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        lat = latLng.latitude;
        lon = latLng.longitude;
        updateMarker();
        if (currentLocationAvailable) {
            useCurrentButton.setEnabled(true);
        }
    }

    @Override
    public void onCameraIdle() {
        zoom = map.getCameraPosition().zoom;
    }

    public void onUseCurrentLocation(View view) {
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


    private static class SavedState implements Parcelable {
        private double lat;
        private double lon;
        private boolean toastShown;
        private float zoom;

        SavedState() {
        }

        SavedState(Parcel source) {
            lat = source.readDouble();
            lon = source.readDouble();
            toastShown = source.readInt() != 0;
            zoom = source.readFloat();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeDouble(lat);
            dest.writeDouble(lon);
            dest.writeInt(toastShown ? 1 : 0);
            dest.writeFloat(zoom);
        }

        public static final Parcelable.Creator<HomeLocationActivity.SavedState> CREATOR =
            new Parcelable.Creator<HomeLocationActivity.SavedState>() {

                public HomeLocationActivity.SavedState createFromParcel(Parcel in) {
                    return new HomeLocationActivity.SavedState(in);
                }

                public HomeLocationActivity.SavedState[] newArray(int size) {
                    return new HomeLocationActivity.SavedState[size];
                }
            };
    }
}
