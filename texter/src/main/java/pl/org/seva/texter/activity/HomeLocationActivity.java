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

package pl.org.seva.texter.activity;

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

import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import pl.org.seva.texter.R;
import pl.org.seva.texter.application.TexterApplication;
import pl.org.seva.texter.dagger.Graph;
import pl.org.seva.texter.databinding.ActivityHomeLocationBinding;
import pl.org.seva.texter.manager.GpsManager;
import pl.org.seva.texter.manager.LastLocationManager;
import pl.org.seva.texter.manager.PermissionsManager;
import pl.org.seva.texter.utils.Constants;

public class HomeLocationActivity extends AppCompatActivity implements
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraIdleListener {

    private GpsManager gpsManager;
    private LastLocationManager lastLocationManager;
    private PermissionsManager permissionsManager;

    private static final String STATE = "STATE";
    private static final String ZOOM_PROPERTY_NAME = "map_preference_gui_zoom";
    private static final float ZOOM_DEFAULT_VALUE = 7.5f;

    private Disposable locationChangedSubscription = Disposables.empty();

    /** Latitude. */
    private double lat;
    /** Longitude. */
    private double lon;
    private boolean toastShown;
    private float zoom;
    private boolean isCurrentLocationAvailable;

    private GoogleMap map;
    private Button useCurrentButton;
    /** True indicates restoring from a saved state. */
    private boolean animateCamera = true;
    private int mapContainerId;
    private MapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            HomeLocationActivity.SavedState myState =
                    (HomeLocationActivity.SavedState) savedInstanceState.get(STATE);
            lat = myState.lat;
            lon = myState.lon;
            toastShown = myState.toastShown;
            zoom = myState.zoom;
            animateCamera = false;
        }

        Graph graph = ((TexterApplication) getApplication()).getGraph();
        gpsManager = graph.gpsManager();
        lastLocationManager = graph.lastLocationManager();
        permissionsManager = graph.permissionsManager();

        String value = getPersistedString();
        lat = parseLatitude(value);
        lon = parseLongitude(value);

        ActivityHomeLocationBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_home_location);
        mapContainerId = binding.mapContainer.getId();
        MapsInitializer.initialize(this);
        useCurrentButton = binding.currentLocationButton;

        boolean locationPermitted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
        boolean locationAvailable = locationPermitted &&
                lastLocationManager.isLocationAvailable();
        useCurrentButton.setEnabled(locationAvailable);
        if (!toastShown) {
            Toast.makeText(
                    this,
                    R.string.long_press,
                    Toast.LENGTH_SHORT).show();
            toastShown = true;
        }
        locationChangedSubscription = gpsManager.locationChangedListener().subscribe(
                __ -> onLocationChanged());

        if (!locationPermitted) {
            useCurrentButton.setEnabled(false);
            setLocationPermissionListeners();
        }
    }

    private void setLocationPermissionListeners() {
        permissionsManager
                .permissionGrantedListener()
                .filter(permission -> permission.equals(Manifest.permission.ACCESS_FINE_LOCATION))
                .subscribe(__ -> onLocationPermissionGranted());
    }

    @Override
    protected void onResume() {
        super.onResume();
        FragmentManager fm = getFragmentManager();
        mapFragment = (MapFragment) fm.findFragmentByTag("map");
        if (mapFragment == null) {
            mapFragment = new MapFragment();
            fm.beginTransaction().add(mapContainerId, mapFragment, "map").commit();
        }
        mapFragment.getMapAsync(googleMap -> {
            map = googleMap;
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
            }
            else {
                setLocationPermissionListeners();
            }
            zoom = PreferenceManager.getDefaultSharedPreferences(this).
                    getFloat(ZOOM_PROPERTY_NAME, ZOOM_DEFAULT_VALUE);

            updateMarker();
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat, lon)).zoom(zoom).build();
            if (animateCamera) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            else {
                map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            animateCamera = false;

            map.setOnMapLongClickListener(this);
            map.setOnCameraIdleListener(this);
        });
    }

    @Override
    public void onBackPressed() {
        locationChangedSubscription.dispose();
        toastShown = false;

        persistString(toString());
        PreferenceManager.getDefaultSharedPreferences(this).edit().
                putFloat(ZOOM_PROPERTY_NAME, zoom).apply();
        gpsManager.onHomeLocationChanged();

        if (mapFragment != null) {
            // Without enclosing in the if, throws:
            // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
            getFragmentManager().beginTransaction().remove(mapFragment).commit();
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        final HomeLocationActivity.SavedState myState
                = new HomeLocationActivity.SavedState();
        myState.lat = lat;
        myState.lon = lon;
        myState.toastShown = toastShown;
        myState.zoom = zoom;

        if (mapFragment != null) {
            getFragmentManager().beginTransaction().remove(mapFragment).commit();
            mapFragment = null;
        }
        super.onSaveInstanceState(outState);
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
            .putString(SettingsActivity.HOME_LOCATION, val).apply();
    }

    private String getPersistedString() {
        return PreferenceManager.getDefaultSharedPreferences(this).
                getString(SettingsActivity.HOME_LOCATION, Constants.DEFAULT_HOME_LOCATION);
    }

    private void updateMarker() {
        map.clear();
        MarkerOptions marker = new MarkerOptions().position(
                new LatLng(lat, lon)).
                title(getString(R.string.home));
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        map.addMarker(marker);
    }

    private void onLocationChanged() {
        if (map == null || useCurrentButton == null) {
            return;
        }
        isCurrentLocationAvailable = true;
        useCurrentButton.setEnabled(true);
    }

    private void onLocationPermissionGranted() {
        if (map != null) {
            //noinspection MissingPermission
            map.setMyLocationEnabled(true);
            useCurrentButton.setEnabled(false);
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        lat = latLng.latitude;
        lon = latLng.longitude;
        updateMarker();
        if (isCurrentLocationAvailable) {
            useCurrentButton.setEnabled(true);
        }
    }

    @Override
    public void onCameraIdle() {
        zoom = map.getCameraPosition().zoom;
    }

    public void onUseCurrentLocationButtonClicked(@SuppressWarnings("UnusedParameters") View view) {
        useCurrentButton.setEnabled(false);
        LatLng loc = lastLocationManager.getLatLng();
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
