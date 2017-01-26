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
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pl.org.seva.texter.R;
import pl.org.seva.texter.adapters.TitledPagerAdapter;
import pl.org.seva.texter.controller.SmsController;
import pl.org.seva.texter.databinding.ActivityMainBinding;
import pl.org.seva.texter.databinding.HelpDialogLayoutBinding;
import pl.org.seva.texter.databinding.StartupDialogLayoutBinding;
import pl.org.seva.texter.fragments.HistoryFragment;
import pl.org.seva.texter.fragments.StatsFragment;
import pl.org.seva.texter.fragments.NavigationFragment;
import pl.org.seva.texter.layouts.SlidingTabLayout;
import pl.org.seva.texter.listeners.PermissionGrantedListener;
import pl.org.seva.texter.listeners.ProviderListener;
import pl.org.seva.texter.managers.GpsManager;
import pl.org.seva.texter.managers.HistoryManager;
import pl.org.seva.texter.managers.PermissionsManager;
import pl.org.seva.texter.managers.SmsManager;
import pl.org.seva.texter.managers.ZoneManager;
import pl.org.seva.texter.services.TexterService;
import pl.org.seva.texter.managers.TimerManager;

public class MainActivity extends AppCompatActivity implements
        PermissionGrantedListener, ProviderListener {

    private static final String PREF_STARTUP_SHOWN = "pref_startup_shown";

    private static final int STATS_TAB_POSITION = 0;
    private static final int MAP_TAB_POSITION = 1;
    private static final int HISTORY_TAB_POSITION = 2;

    private static final int GOOGLE_REQUEST_CODE = 0;

    private static final int NUMBER_OF_TABS = 3;

    /** Number of milliseconds that will be taken for a double click. */
    private static final long DOUBLE_CLICK_MILLIS = 5000;
    /** Used when counting a double click. */
    private long clickTime;
    /** Obtained from intent, may be null. */
    private String action;
    private boolean serviceRunning;
    private boolean showSettings;
    private boolean shuttingDown;
    private Dialog dialog;
    private ActivityMainBinding binding;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        action = getIntent().getAction();
        if (action == null) {
            finish();
        }
        // Set up colors depending on SDK version.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            int color;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                color = getResources().getColor(R.color.ColorPrimaryDark, getTheme());
            }
            else {
                color = getResources().getColor(R.color.ColorPrimaryDark);
            }
            window.setStatusBarColor(color);
        }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        CharSequence titles[] = new CharSequence[NUMBER_OF_TABS];
        titles[STATS_TAB_POSITION] = getString(R.string.stats_tab_name);
        titles[MAP_TAB_POSITION] = getString(R.string.map_tab_name);
        titles[HISTORY_TAB_POSITION] = getString(R.string.history_tab_name);

        SmsController.getInstance().init(getPackageManager().
                hasSystemFeature(PackageManager.FEATURE_TELEPHONY));

        Toolbar toolbar = binding.toolBar.toolBar;
        setSupportActionBar(toolbar);
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(StatsFragment.newInstance());
        fragments.add(NavigationFragment.newInstance());
        fragments.add(HistoryFragment.newInstance());

        TitledPagerAdapter adapter =
                new TitledPagerAdapter(getFragmentManager(), titles).
                        setItems(fragments);

        ViewPager pager = binding.pager;
        if (pager != null) {
            pager.setAdapter(adapter);
        }

        SlidingTabLayout tabs = binding.tabs;

        if (tabs != null) {
            final int tabColor;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                tabColor = getResources().getColor(R.color.tabsScrollColor, getTheme());
            }
            else {
                tabColor = getResources().getColor(R.color.tabsScrollColor);
            }
            tabs.setDistributeEvenly();
            tabs.setCustomTabColorizer(position -> tabColor);
            tabs.setViewPager(pager);
        }
        if (TimerManager.getInstance().getState() == Thread.State.NEW) {
            TimerManager.getInstance().start();
        }
        else if (savedInstanceState == null && action != null && action.equals(Intent.ACTION_MAIN)) {
            TimerManager.getInstance().reset();
        }

        SmsManager.getInstance().init(this, getString(R.string.speed_unit));

        int googlePlay = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (googlePlay != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().
                    getErrorDialog(this, googlePlay, GOOGLE_REQUEST_CODE).show();
        }

        initGps();
        addListeners();
        if (!showStartupDialog()) {
            processPermissions();
        }
    }

    /**
     * All actions that require permissions must be placed here. The methods performs them or
     * asks for permissions if they haven't been granted already.
     *
     * @return true if all permissions had been granted before calling the method
     */
    private boolean processPermissions() {
        List<String> permissions = new ArrayList<>();
        addListeners();
        if (!initGps()) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (SmsManager.getInstance().isTextingEnabled() && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS) !=
                PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS);
        }
        if (permissions.isEmpty()) {
            return true;
        }
        String arr[] = new String[permissions.size()];
        permissions.toArray(arr);
        ActivityCompat.requestPermissions(
                this,
                arr,
                PermissionsManager.PERMISSION_ACCESS_FINE_LOCATION_REQUEST);

        return false;
    }

    private void addListeners() {
        GpsManager.getInstance().addDistanceChangedListener(SmsController.getInstance());
        GpsManager.getInstance().addProviderListener(this);
    }

    private boolean initGps() {
        boolean permissionGranted = GpsManager.getInstance().init(this);

        if (!permissionGranted) {
            PermissionsManager.getInstance().addPermissionGrantedListener(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    this);
        }
        else {
            GpsManager.getInstance().callProviderListener();
        }

        return permissionGranted;
    }

    private boolean showStartupDialog() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean(PREF_STARTUP_SHOWN, false)) {
            return false;
        }
        dialog = new Dialog(this);
        dialog.setCancelable(false);
        StartupDialogLayoutBinding dialogBinding = DataBindingUtil.inflate(
                LayoutInflater.from(this),
                R.layout.startup_dialog_layout,
                (ViewGroup) binding.getRoot(),
                false);
        dialog.setContentView(dialogBinding.getRoot());
        WebView web = dialogBinding.web;

        String language = Locale.getDefault().getLanguage();
        web.getSettings().setDefaultTextEncodingName("utf-8");

        web.loadUrl(language.equals("pl") ?
                "file:///android_asset/startup_pl.html" :
                "file:///android_asset/startup_en.html");

        dialogBinding.dismiss.setOnClickListener(v -> {
            processPermissions();
            dialog.dismiss();
            prefs.edit().putBoolean(PREF_STARTUP_SHOWN, true).apply();  // asynchronously
        });
        dialogBinding.settings.setOnClickListener(v -> {
            dialog.dismiss();
            prefs.edit().putBoolean(PREF_STARTUP_SHOWN, true).apply();
            showSettings = true;  // Only relevant if permission is not granted.
            if (processPermissions()) {
                // Called if permission has already been granted, e.g. when API < 23.
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        dialog.show();
        return true;
    }

    private void showHelpDialog() {
        dialog = new Dialog(this);
        dialog.setCancelable(false);
        HelpDialogLayoutBinding dialogBinding = DataBindingUtil.inflate(
                LayoutInflater.from(this),
                R.layout.help_dialog_layout,
                (ViewGroup) binding.getRoot(),
                false);
        dialog.setContentView(dialogBinding.getRoot());
        WebView web = dialogBinding.web;
        web.getSettings().setDefaultTextEncodingName("utf-8");

        String language = Locale.getDefault().getLanguage();

        web.loadUrl(language.equals("pl") ?
                "file:///android_asset/help_pl.html" :
                "file:///android_asset/help_en.html");

        dialogBinding.ok.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void startService() {
        if (serviceRunning) {
            return;
        }
        startService(new Intent(getBaseContext(), TexterService.class));
        serviceRunning = true;
    }

    private void stopService() {
        if (!serviceRunning) {
            return;
        }
        stopService(new Intent(getBaseContext(), TexterService.class));
        serviceRunning = false;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String permissions[],
            @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == PermissionsManager.PERMISSION_ACCESS_FINE_LOCATION_REQUEST) {
            PermissionsManager.getInstance().onRequestPermissionsResult(permissions, grantResults);
        }
    }

    @Override
    public void onDestroy() {
        // Also called when the screen is rotated.
        if (dialog != null) {
            dialog.dismiss();
        }
        if (action != null && action.equals(Intent.ACTION_MAIN)) {
            // Condition is false when activity has been launched from a notification.
            if (shuttingDown) {
                stopService();
                HistoryManager.shutdown();
                GpsManager.shutdown(this);
                PermissionsManager.shutdown();
                SmsManager.shutdown();
                TimerManager.shutdown();
                ZoneManager.shutdown();
            }
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - clickTime < DOUBLE_CLICK_MILLIS) {
            shuttingDown = true;
            super.onBackPressed();
        }
        else {
            Toast.makeText(this, R.string.tap_back_second_time, Toast.LENGTH_SHORT).
                    show();
            clickTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_help:
                showHelpDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPermissionGranted(String permission) {
        if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            initGps();  // listeners already added
            GpsManager.getInstance().callProviderListener();
            PermissionsManager.getInstance().
                    removePermissionGrantedListener(Manifest.permission.ACCESS_FINE_LOCATION, this);
            if (showSettings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        }
    }

    @Override
    public void onProviderEnabled() {
        startService();
    }

    @Override
    public void onProviderDisabled() {
        stopService();
    }
}
