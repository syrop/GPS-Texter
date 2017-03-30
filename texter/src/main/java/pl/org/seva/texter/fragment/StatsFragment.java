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

package pl.org.seva.texter.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Calendar;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import pl.org.seva.texter.R;
import pl.org.seva.texter.application.TexterApplication;
import pl.org.seva.texter.dagger.Graph;
import pl.org.seva.texter.databinding.StatsFragmentBinding;
import pl.org.seva.texter.manager.ActivityRecognitionManager;
import pl.org.seva.texter.manager.GpsManager;
import pl.org.seva.texter.manager.PermissionsManager;
import pl.org.seva.texter.manager.SmsManager;
import pl.org.seva.texter.manager.TimerManager;
import pl.org.seva.texter.model.LocationModel;

public class StatsFragment extends Fragment implements
        View.OnClickListener {

    @Inject GpsManager gpsManager;
    @Inject ActivityRecognitionManager activityRecognitionManager;
    @Inject TimerManager timerManager;
    @Inject SmsManager smsManager;
    @Inject PermissionsManager permissionsManager;

    private static String homeString;
    private static String hourString;

    private TextView distanceTextView;
    private TextView intervalTextView;
    private TextView stationaryTextView;
    private TextView speedTextView;
    private Button sendNowButton;

    private final CompositeDisposable composite = new CompositeDisposable();

    private double distance;
    private double speed;
    private boolean stationary;

    private Activity activity;

    public static StatsFragment newInstance() {
        return new StatsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        distance = gpsManager.getDistance();
        speed = gpsManager.getSpeed();

        homeString = getString(R.string.home);
        hourString = getActivity().getString(R.string.hour);
        StatsFragmentBinding binding =
                DataBindingUtil.inflate(inflater, R.layout.stats_fragment, container, false);
        distanceTextView = binding.distanceValue;
        intervalTextView = binding.updateIntervalValue;
        stationaryTextView = binding.stationary;
        speedTextView = binding.speedValue;
        sendNowButton = binding.sendNowButton;
        sendNowButton.setOnClickListener(this);
        sendNowButton.setEnabled(
                smsManager.isTextingEnabled() &&
                distance != 0.0 &&
                distance != smsManager.getLastSentDistance());

        showStats();
        composite.addAll(
                timerManager.timerListener().subscribe(__ -> onTimer()),
                smsManager.smsSendingListener().subscribe(__ -> onSendingSms()),
                gpsManager.distanceChangedListener().subscribe(__ -> onDistanceChanged()),
                gpsManager.homeChangedListener().subscribe(__ -> onHomeChanged()),
                activityRecognitionManager
                    .stationaryListener()
                    .subscribe(__ -> deviceIsStationary()),
                activityRecognitionManager
                    .movingListener()
                    .subscribe(__ -> deviceIsMoving()));

        if (ContextCompat.checkSelfPermission(
                getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            permissionsManager
                    .permissionGrantedListener()
                    .filter(permission -> permission.equals(Manifest.permission.ACCESS_FINE_LOCATION))
                    .subscribe(__ -> onLocationPermissionGranted());
        }

        return binding.getRoot();
    }

    private void deviceIsStationary() {
        stationary = true;
    }

    private void deviceIsMoving() {
        stationary = false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof  Activity) {
            this.activity = (Activity) context;
            initDependencies();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    // see http://stackoverflow.com/questions/32083053/android-fragment-onattach-deprecated#32088447
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            this.activity = activity;
            initDependencies();
        }
    }

    private void initDependencies() {
        Graph graph = ((TexterApplication) activity.getApplication()).getGraph();
        graph.inject(this);
    }

    private void showStats() {
        @SuppressLint("DefaultLocale") String distanceStr = String.format("%.3f km", distance);

        if (distance == 0.0) {
            distanceStr = "0 km";
        }
        distanceTextView.setText(distanceStr);
        int seconds = (int) (System.currentTimeMillis() -
                timerManager.getResetTime()) / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        int hours = minutes / 60;
        minutes = minutes % 60;
        StringBuilder timeStrBuilder = new StringBuilder();
        if (hours > 0) {
            timeStrBuilder.append(hours).append(" ").append(hourString);
            if (minutes > 0 || seconds > 0) {
                timeStrBuilder.append(" ");
            }
        }
        if (minutes > 0) {
            timeStrBuilder.append(minutes).append(" m");
            if (seconds > 0) {
                timeStrBuilder.append(" ");
            }
        }
        if (seconds > 0) {
            timeStrBuilder.append(seconds).append(" s");
        }
        else if (minutes == 0 && hours == 0) {
            timeStrBuilder.setLength(0);
            timeStrBuilder.append("0 s");
        }
        stationaryTextView.setVisibility(stationary ? View.VISIBLE : View.GONE);
        intervalTextView.setText(timeStrBuilder.toString());
        if (speed == 0.0 || distance == 0.0) {
            speedTextView.setVisibility(View.INVISIBLE);
        }
        else {
            speedTextView.setVisibility(View.VISIBLE);
            speedTextView.setText(getSpeedStr());
        }
    }

    private String getSpeedStr() {
        @SuppressLint("DefaultLocale")
        String result = String.format("%.1f", stationary ? 0.0 : speed) + " " + activity.getString(R.string.speed_unit);
        if (result.contains(".0")) {
            result = result.replace(".0", "");
        }
        else if (result.contains(",0")) {
            result = result.replace(",0", "");
        }
        return result;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        composite.clear();
    }

    private void onDistanceChanged() {
        boolean resetValues =
                System.currentTimeMillis() - timerManager.getResetTime() > 3 * 3600 * 1000;
        if (distance != smsManager.getLastSentDistance()) {
            sendNowButton.setEnabled(smsManager.isTextingEnabled());
        }

        if (resetValues) {  // reset the values if three hours have passed
            this.speed = 0.0;
            this.distance = 0.0;
        }
        else {
            this.distance = gpsManager.getDistance();
            this.speed = gpsManager.getSpeed();
        }
        showStats();
    }

    private void onTimer() {
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(this::showStats);
    }

    public static String getHomeString() {
        return homeString;
    }

    @Override
    public void onClick(View v) {
        if (v == sendNowButton) {
            sendNowButton.setEnabled(false);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timerManager.getResetTime());
            int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60;
            minutes += calendar.get(Calendar.MINUTE);
            LocationModel location = new LocationModel();
            location.setDistance(distance);
            location.setDirection(0);
            location.setTime(minutes);
            location.setSpeed(speed);
            smsManager.send(location);
        }
    }

    private void onSendingSms() {
        sendNowButton.setEnabled(false);
    }

    private void onHomeChanged() {
        distance = gpsManager.getDistance();
        showStats();
    }

    private void onLocationPermissionGranted() {
        sendNowButton.setEnabled(smsManager.isTextingEnabled());
    }
}
