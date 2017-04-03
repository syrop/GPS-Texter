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

import android.content.Intent;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import javax.inject.Inject;

import pl.org.seva.texter.presenter.dagger.DaggerGraph;
import pl.org.seva.texter.presenter.dagger.Graph;
import pl.org.seva.texter.presenter.manager.ActivityRecognitionManager;
import pl.org.seva.texter.presenter.manager.GpsManager;
import pl.org.seva.texter.presenter.service.TexterService;

public class TexterApplication extends MultiDexApplication {

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject GpsManager gpsManager;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
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
