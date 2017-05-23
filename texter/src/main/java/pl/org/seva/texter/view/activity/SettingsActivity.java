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

package pl.org.seva.texter.view.activity;

import pl.org.seva.texter.R;
import pl.org.seva.texter.presenter.source.LocationSource;
import pl.org.seva.texter.presenter.utils.PermissionsUtils;
import pl.org.seva.texter.presenter.utils.SmsSender;
import pl.org.seva.texter.view.adapter.TitledPagerAdapter;
import pl.org.seva.texter.TexterApplication;
import pl.org.seva.texter.presenter.dagger.Graph;
import pl.org.seva.texter.view.fragment.SettingsFragment;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class SettingsActivity extends AppCompatActivity {

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    PermissionsUtils permissionsUtils;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    LocationSource locationSource;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    SmsSender smsSender;

    /** If device is not enabled to send SMS, this entire category will be hidden. */
    public static final String CATEGORY_SMS = "category_sms";

    /** Unless true, SMS will be disabled and SMS-related options grayed out. */
    public static final String SMS_ENABLED = "pref_enable_sms";
    /** All text messages will be sent to this number. */
    public static final String SMS_NUMBER = "pref_phone_number";
    /** Beyond this distance from home, no messages will be sent. */
    public static final String MAXIMUM_DISTANCE = "pref_max_distance";
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

        Graph graph = ((TexterApplication) getApplication()).getGraph();
        graph.inject(this);

        setContentView(R.layout.activity_settings);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // Attaching the layout to the toolbar object
        Toolbar toolbar = findViewById(R.id.toolbar);

		setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SMS_ENABLED, false)) {
            if (!processPermissions()) {
                setReadContactsPermissionListeners();
            }
        }

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(SettingsFragment.newInstance());

        TitledPagerAdapter adapter =
                new TitledPagerAdapter(getFragmentManager(), null).
                        setItems(fragments);
        ViewPager pager = findViewById(R.id.pager);
        if (pager != null) {
            pager.setAdapter(adapter);
        }
    }

    private void setReadContactsPermissionListeners() {
        permissionsUtils
                .permissionDeniedListener()
                .filter(permission -> permission.equals(Manifest.permission.READ_CONTACTS))
                .subscribe(__ -> onShowContactsPermissionDenied());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this::onSharedPreferenceChanged);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this::onSharedPreferenceChanged);
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
                    PermissionsUtils.PERMISSION_READ_CONTACTS_REQUEST);
        }
        return result;
    }

    private void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case SMS_ENABLED:  // off by default
                if (sharedPreferences.getBoolean(SMS_ENABLED, false)) {
                    if (!processPermissions()) {
                        setReadContactsPermissionListeners();
                    }
                }
                break;
            case HOME_LOCATION:
                locationSource.onHomeLocationChanged();
                smsSender.resetZones();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String permissions[],
            @NonNull int[] grantResults) {
        if (requestCode == PermissionsUtils.PERMISSION_READ_CONTACTS_REQUEST) {
            permissionsUtils.onRequestPermissionsResult(permissions, grantResults);
        }
    }

    private void onShowContactsPermissionDenied() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_CONTACTS) &&
                permissionsUtils
                        .isRationaleNeeded(Manifest.permission.READ_CONTACTS)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.perm_contacts_rationale).
                    setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        permissionsUtils
                                .onRationaleShown(Manifest.permission.READ_CONTACTS);
                        processPermissions();
                    });
            builder.create().show();
        }
    }
}
