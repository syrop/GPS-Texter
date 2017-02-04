package pl.org.seva.texter.application;

import android.content.Intent;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import pl.org.seva.texter.manager.ActivityRecognitionManager;
import pl.org.seva.texter.manager.GpsManager;
import pl.org.seva.texter.service.TexterService;

public class TexterApplication extends MultiDexApplication {

    private static final String TAG = TexterApplication.class.getSimpleName();

    private boolean serviceRunning;

    private boolean isDeviceStationary;
    private boolean isProviderEnabled;

    @Override
    public void onCreate() {
        super.onCreate();
        addGpsProviderListeners();
        addActivityRecognitionListeners();
    }

    private void addGpsProviderListeners() {
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
        if (serviceRunning) {
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
