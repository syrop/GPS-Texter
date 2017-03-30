package pl.org.seva.texter.application;

import android.content.Intent;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import javax.inject.Inject;

import pl.org.seva.texter.dagger.DaggerGraph;
import pl.org.seva.texter.dagger.Graph;
import pl.org.seva.texter.manager.ActivityRecognitionManager;
import pl.org.seva.texter.manager.GpsManager;
import pl.org.seva.texter.service.TexterService;

public class TexterApplication extends MultiDexApplication {

    @Inject GpsManager gpsManager;
    @Inject ActivityRecognitionManager activityRecognitionManager;

    private static final String TAG = TexterApplication.class.getSimpleName();

    private boolean serviceRunning;

    private boolean isDeviceStationary;
    private boolean isProviderEnabled;

    private Graph graph;

    @Override
    public void onCreate() {
        super.onCreate();
        graph = createGraph();
        graph.inject(this);
        addGpsProviderListeners();
        addActivityRecognitionListeners();
    }

    private void addGpsProviderListeners() {
        gpsManager
                .providerEnabledListener()
                .subscribe(__ -> onProviderEnabled());
        gpsManager
                .providerDisabledListener()
                .subscribe(__ -> onProviderDisabled());
    }

    private void addActivityRecognitionListeners() {
        activityRecognitionManager
                .stationaryListener()
                .subscribe(__ -> onDeviceStationary());

        activityRecognitionManager
                .movingListener()
                .subscribe(__ -> onDeviceMoving());
    }

    protected Graph createGraph() {
        return DaggerGraph.create();
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
