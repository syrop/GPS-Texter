package pl.org.seva.texter.application;

import android.content.Intent;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import pl.org.seva.texter.dagger.DaggerGraph;
import pl.org.seva.texter.dagger.Graph;
import pl.org.seva.texter.service.TexterService;

public class TexterApplication extends MultiDexApplication {

    private static final String TAG = TexterApplication.class.getSimpleName();

    private boolean serviceRunning;

    private boolean isDeviceStationary;
    private boolean isProviderEnabled;

    private Graph graph;

    @Override
    public void onCreate() {
        super.onCreate();
        graph = DaggerGraph.create();
        addGpsProviderListeners();
        addActivityRecognitionListeners();
    }

    private void addGpsProviderListeners() {
        graph.gpsManager()
                .providerEnabledListener()
                .subscribe(__ -> onProviderEnabled());
        graph.gpsManager()
                .providerDisabledListener()
                .subscribe(__ -> onProviderDisabled());
    }

    private void addActivityRecognitionListeners() {
        graph.activityRecognitionManager()
                .stationaryListener()
                .subscribe(__ -> onDeviceStationary());

        graph.activityRecognitionManager()
                .movingListener()
                .subscribe(__ -> onDeviceMoving());
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

    public Graph getGraph() {
        return graph;
    }
}
