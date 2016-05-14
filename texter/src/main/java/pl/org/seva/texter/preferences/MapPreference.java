package pl.org.seva.texter.preferences;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
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

/**
 * Created by wiktor on 20.08.15.
 */
public class MapPreference extends DialogPreference implements
        GoogleMap.OnMapLongClickListener,
        View.OnClickListener,
        ILocationChangedListener,
        GoogleMap.OnCameraChangeListener,
        IPermissionGrantedListener{

    /** Geo URI for Warsaw. */
    public static final String DEFAULT_VALUE = "geo:52.233333,21.016667";  // Warsaw

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

    private Context context;
    private GoogleMap map;
    private Button useCurrentButton;

    private SupportMapFragment mapFragment;

    public MapPreference(Context context, AttributeSet attrs) {
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
                if (GPSManager.getInstance().isLocationAvailable()) {
                    useCurrentButton.setEnabled(false);
                }
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return DEFAULT_VALUE;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        GPSManager.getInstance().removeLocationChangedListener(this);
        toastShown = false;
        if (positiveResult) {
            persistString(toString());
            PreferenceManager.getDefaultSharedPreferences(context).edit().
                    putFloat(ZOOM_PROPERTY_NAME, zoom).commit();
        }
        else {
            zoom = PreferenceManager.getDefaultSharedPreferences(context).
                    getFloat(ZOOM_PROPERTY_NAME, ZOOM_DEFAULT_VALUE);
            String value = getPersistedString(DEFAULT_VALUE);
            lat = parseLatitude(value);
            lon = parseLongitude(value);
        }
        GPSManager.getInstance().removeLocationChangedListener(this);

        if (mapFragment != null) {
            // Without enclosing in the if, throws:
            // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
            ((android.support.v4.app.FragmentActivity) getContext()).
                    getSupportFragmentManager().beginTransaction().remove(mapFragment).commit();
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        // Create instance of custom BaseSavedState
        final SavedState myState = new SavedState(superState);
        // Set the state's value with the class member that holds current
        // setting value
        myState.lat = lat;
        myState.lon = lon;
        myState.toastShown = toastShown;
        myState.zoom = zoom;

        // If called after onSaveInstanceState, throws:
        // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
        ((android.support.v4.app.FragmentActivity) getContext()).
                getSupportFragmentManager().beginTransaction().remove(mapFragment).commit();
        mapFragment = null;

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
            value = getPersistedString(DEFAULT_VALUE);
            lat = parseLatitude(value);
            lon = parseLongitude(value);
        }
        else {
            value = DEFAULT_VALUE;
            persistString(value);
        }
        lat = parseLatitude(value);
        lon = parseLongitude(value);
    }

    @Override
    protected View onCreateDialogView() {
        final View result = super.onCreateDialogView();

        MapsInitializer.initialize(context);

        mapFragment = (SupportMapFragment)
                ((android.support.v4.app.FragmentActivity)getContext()).
                getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
                if (ContextCompat.checkSelfPermission(
                        getContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
                    map.setMyLocationEnabled(true);
                }
                else {
                    PermissionsManager.getInstance().addPermissionListener(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            MapPreference.this);
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

                map.setOnMapLongClickListener(MapPreference.this);
                map.setOnCameraChangeListener(MapPreference.this);

                useCurrentButton = (Button) result.findViewById(R.id.current_location_button);
                useCurrentButton.setOnClickListener(MapPreference.this);
                useCurrentButton.setEnabled(GPSManager.getInstance().isLocationAvailable());
                if (!useCurrentButton.isEnabled()) {
                    GPSManager.getInstance().addLocationChangedListener(MapPreference.this);
                }
                if (!toastShown) {
                    Toast.makeText(
                            context,
                            R.string.long_press,
                            Toast.LENGTH_SHORT).show();
                    toastShown = true;
                }
                GPSManager.getInstance().addLocationChangedListener(MapPreference.this);
            }
        });

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

    public static String toString(double lat, double lon) {
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
    public void onLocationChanged(Location location) {
        useCurrentButton.setEnabled(true);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(zoom).build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        zoom = cameraPosition.zoom;
    }

    @Override
    public void onPermissionGranted(String permission) {
        if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) && map != null &&
                ContextCompat.checkSelfPermission(
                        getContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            PermissionsManager.getInstance().
                    removePermissionListener(Manifest.permission.ACCESS_FINE_LOCATION, this);
        }
    }

    private static class SavedState extends BaseSavedState {
        // Member that holds the setting's value
        // Change this data type to match the type saved by your Preference
        private double lat;
        private double lon;
        private boolean toastShown;
        private float zoom;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            lat = source.readDouble();  // Change this to read the appropriate data type
            lon = source.readDouble();
            toastShown = source.readInt() != 0;
            zoom = source.readFloat();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeDouble(lat);  // Change this to write the appropriate data type
            dest.writeDouble(lon);
            dest.writeInt(toastShown ? 1 : 0);
            dest.writeFloat(zoom);
        }

        // Standard creator object using an instance of this class
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
