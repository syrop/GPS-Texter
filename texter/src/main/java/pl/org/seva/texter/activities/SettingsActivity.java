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

import pl.org.seva.texter.R;
import pl.org.seva.texter.controller.SMSController;
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

import java.util.ArrayList;
import java.util.List;

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

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SMS_ENABLED, false)) {
            if (!processPermissions()) {
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


    /**
     * All actions that require permissions must be placed here. The methods performs them or
     * asks for permissions if they haven't been granted already.
     *
     * @return false if particularly READ_CONTACTS was't granted previously
     */
    private boolean processPermissions() {
        List<String> permissions = new ArrayList<>();
        boolean result = true;

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS) !=
                PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_CONTACTS);
            result = false;
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS) !=
                PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS);
        }
        if (!permissions.isEmpty()) {
            String arr[] = new String[permissions.size()];
            permissions.toArray(arr);
            ActivityCompat.requestPermissions(
                    this,
                    arr,
                    PermissionsManager.PERMISSION_READ_CONTACTS_REQUEST);
        }
        return result;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case SMS_ENABLED:  // off by default
                if (sharedPreferences.getBoolean(SMS_ENABLED, false)) {
                    if (!processPermissions()) {
                        PermissionsManager.getInstance().addPermissionGrantedListener(
                                Manifest.permission.READ_CONTACTS,
                                this);
                        PermissionsManager.getInstance().addPermissionDeniedListener(
                                Manifest.permission.READ_CONTACTS,
                                this);
                    }
                }
                break;
            case LOCATION_UPDATE_FREQUENCY:
                GPSManager.getInstance().updateFrequencyChanged(this);
                break;
            case HOME_LOCATION:
                GPSManager.getInstance().updateHome();
                SMSController.getInstance().resetZones();
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
                    Manifest.permission.READ_CONTACTS) &&
                    !PermissionsManager.getInstance().
                            rationaleShown(Manifest.permission.READ_CONTACTS)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.perm_contacts_rationale).
                        setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PermissionsManager.getInstance().
                                onRationaleShown(Manifest.permission.READ_CONTACTS);
                        processPermissions();
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
