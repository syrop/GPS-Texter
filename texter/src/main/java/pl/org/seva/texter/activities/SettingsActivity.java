package pl.org.seva.texter.activities;

import pl.org.seva.texter.R;
import pl.org.seva.texter.listeners.IPermissionDeniedListener;
import pl.org.seva.texter.listeners.IPermissionGrantedListener;
import pl.org.seva.texter.managers.GPSManager;
import pl.org.seva.texter.managers.PermissionsManager;

import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.WindowManager;

public class SettingsActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        IPermissionGrantedListener, IPermissionDeniedListener {

    /** If device is not enabled to send SMS, this entire category will be hidden. */
    public static final String CATEGORY_SMS = "category_sms";

    /** Unless true, SMS will be disabled and SMS-related options grayed out. */
    public static final String SMS_ENABLED = "pref_enable_sms";
    /** All text messages will be sent to this number. */
    public static final String SMS_NUMBER = "pref_phone_number";
    /** Beyond this distance from home, no messages will be sent. */
    public static final String MAXIMUM_DISTANCE = "pref_max_distance";
    /** Request updates every so many seconds. */
    public static final String LOCATION_UPDATE_FREQUENCY = "pref_update_frequency";
    /** Location to measure distance from. */
    public static final String HOME_LOCATION = "pref_home_location";
    /** If true, time will be sent with every SMS. */
    public static final String INCLUDE_TIME = "pref_include_time";
    /** If true, speed will be sent with every SMS. */
    public static final String INCLUDE_SPEED = "pref_include_speed";
    /** If true, location will be sent with every SMS. */
    public static final String INCLUDE_LOCATION = "pref_include_location";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // Attaching the layout to the toolbar object
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);

		setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SMS_ENABLED, false)) {
            if (!checkPermission()) {
                PermissionsManager.getInstance().addPermissionGrantedListener(
                        Manifest.permission.READ_CONTACTS,
                        this);
                PermissionsManager.getInstance().addPermissionDeniedListener(
                        Manifest.permission.READ_CONTACTS,
                        this);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.READ_CONTACTS, },
                    PermissionsManager.PERMISSION_READ_CONTACTS_REQUEST);
            return false;
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case SMS_ENABLED:  // off by default
                if (sharedPreferences.getBoolean(SMS_ENABLED, false)) {
                    checkPermission();
                }
                break;
            case LOCATION_UPDATE_FREQUENCY:
                GPSManager.getInstance().updateFrequencyChanged(this);
                break;
            case HOME_LOCATION:
                GPSManager.getInstance().updateHome();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String permissions[],
            @NonNull int[] grantResults) {
        if (requestCode == PermissionsManager.PERMISSION_READ_CONTACTS_REQUEST) {
            PermissionsManager.getInstance().onRequestPermissionsResult(permissions, grantResults);
        }

    }

    @Override
    public void onPermissionDenied(String permission) {
        if (permission.equals(Manifest.permission.READ_CONTACTS)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_CONTACTS)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.perm_contacts_rationale).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkPermission();
                    }
                });
                builder.create().show();
            }
            else {
                PermissionsManager.getInstance().removePermissionGrantedListener(
                        Manifest.permission.READ_CONTACTS,
                        this);
            }
            PermissionsManager.getInstance().removePermissionDeniedListener(
                    Manifest.permission.READ_CONTACTS,
                    this);
        }
    }

    @Override
    public void onPermissionGranted(String permission) {
        if (permission.equals(Manifest.permission.READ_CONTACTS)) {
            PermissionsManager.getInstance().removePermissionDeniedListener(
                    Manifest.permission.READ_CONTACTS,
                    this);
            PermissionsManager.getInstance().removePermissionDeniedListener(
                    Manifest.permission.READ_CONTACTS,
                    this);
        }
    }
}
