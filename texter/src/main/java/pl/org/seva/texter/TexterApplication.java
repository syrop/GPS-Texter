/*
 * Copyright (C) 2017 Wiktor Nizio
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

package pl.org.seva.texter;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import javax.inject.Inject;

import pl.org.seva.texter.presenter.dagger.DaggerTexterComponent;
import pl.org.seva.texter.presenter.dagger.TexterComponent;
import pl.org.seva.texter.presenter.source.ActivityRecognitionSource;
import pl.org.seva.texter.presenter.source.LocationSource;
import pl.org.seva.texter.presenter.service.TexterService;

public class TexterApplication extends Application {

    private static final String TAG = TexterApplication.class.getSimpleName();

    @SuppressWarnings({"CanBeFinal"})
    @Inject
    LocationSource locationSource;
    @SuppressWarnings({"CanBeFinal"})
    @Inject
    ActivityRecognitionSource activityRecognitionSource;

    private boolean isServiceRunning;
    private boolean isDeviceStationary;
    private boolean isProviderEnabled;

    private TexterComponent component;

    @Override
    public void onCreate() {
        super.onCreate();
        component = createComponent();
        component.inject(this);
        addGpsProviderListeners();
        addActivityRecognitionListeners();
    }

    public boolean hardwareCanSendSms() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("CheckResult")
    private void addGpsProviderListeners() {
        locationSource
                .providerEnabledListener()
                .subscribe(__ -> onProviderEnabled());
        locationSource
                .providerDisabledListener()
                .subscribe(__ -> onProviderDisabled());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("CheckResult")
    private void addActivityRecognitionListeners() {
        activityRecognitionSource
                .stationaryListener()
                .subscribe(__ -> onDeviceStationary());

        activityRecognitionSource
                .movingListener()
                .subscribe(__ -> onDeviceMoving());
    }

    protected TexterComponent createComponent() {
        return DaggerTexterComponent.create();
    }

    private void onDeviceStationary() {
        isDeviceStationary = true;
        stopService();
    }

    private void onDeviceMoving() {
        isDeviceStationary = false;
        if (isProviderEnabled) {
            startService();
        }
    }

    private void onProviderEnabled() {
        Log.d(TAG, "Provider is enabled");
        isProviderEnabled = true;
        if (!isDeviceStationary) {
            startService();
        }
    }

    private void onProviderDisabled() {
        Log.d(TAG, "Provider is disabled");
        isProviderEnabled = false;
        stopService();
    }

    private void startService() {
        if (isServiceRunning) {
            return;
        }
        startService(new Intent(getBaseContext(), TexterService.class));
        isServiceRunning = true;
    }

    public void stopService() {
        if (!isServiceRunning) {
            return;
        }
        stopService(new Intent(getBaseContext(), TexterService.class));
        isServiceRunning = false;
    }

    public TexterComponent getComponent() {
        return component;
    }
}
