package pl.org.seva.texter.application;

import android.content.Intent;
import android.support.multidex.MultiDexApplication;

import pl.org.seva.texter.controller.SmsController;
import pl.org.seva.texter.manager.ActivityRecognitionManager;
import pl.org.seva.texter.manager.GpsManager;
import pl.org.seva.texter.service.TexterService;

public class TexterApplication extends MultiDexApplication {

    private boolean serviceRunning;

    private boolean isDeviceStationary;
    private boolean isProviderEnabled;

    @Override
    public void onCreate() {
        super.onCreate();
        addGpsListeners();
        addActivityRecognitionListeners();
    }

    private void addGpsListeners() {
        GpsManager
                .getInstance()
                .distanceChangedListener().subscribe(
                ignore -> SmsController.getInstance().onDistanceChanged());
        GpsManager
                .getInstance()
                .providerEnabledListener()
                .subscribe(ignore -> onProviderEnabled());
        GpsManager
                .getInstance()
                .providerDisabledListener()
                .subscribe(ignore -> onProviderDisabled());
    }

    private void addActivityRecognitionListeners() {
        ActivityRecognitionManager
                .getInstance()
                .stationaryListener()
                .subscribe(ignore -> onDeviceStationary());

        ActivityRecognitionManager
                .getInstance()
                .movingListener()
                .subscribe(ignore -> onDeviceMoving());
    }

    private void onDeviceStationary() {
        isDeviceStationary = true;
        stopService();
    }

    private void onDeviceMoving() {
        isDeviceStationary = false;
        startService();
    }

    private void onProviderEnabled() {
        isProviderEnabled = true;
        startService();
    }

    private void onProviderDisabled() {
        isProviderEnabled = false;
        stopService();
    }

    private void startService() {
        if (serviceRunning || isDeviceStationary || !isProviderEnabled) {
            return;
        }
        startService(new Intent(getBaseContext(), TexterService.class));
        serviceRunning = true;
    }

    public void stopService() {
        if (!serviceRunning) {
            return;
        }
        stopService(new Intent(getBaseContext(), TexterService.class));
        serviceRunning = false;
    }
}
