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

import io.reactivex.disposables.CompositeDisposable;
import pl.org.seva.texter.R;
import pl.org.seva.texter.databinding.StatsFragmentBinding;
import pl.org.seva.texter.manager.ActivityRecognitionManager;
import pl.org.seva.texter.manager.GpsManager;
import pl.org.seva.texter.manager.PermissionsManager;
import pl.org.seva.texter.manager.SmsManager;
import pl.org.seva.texter.manager.TimerManager;
import pl.org.seva.texter.model.LocationModel;

public class StatsFragment extends Fragment implements
        View.OnClickListener {

    private static String homeString;
    private static String hourString;

    private TextView distanceTextView;
    private TextView intervalTextView;
    private TextView stationaryTextView;
    private TextView speedTextView;
    private Button sendNowButton;

    private CompositeDisposable composite = new CompositeDisposable();

    private double distance;
    private double speed;
    private boolean stationary;

    private Activity activity;

    public static StatsFragment newInstance() {
        return new StatsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        distance = GpsManager.getInstance().getDistance();
        speed = GpsManager.getInstance().getSpeed();

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
                SmsManager.getInstance().isTextingEnabled() &&
                distance != 0.0 &&
                distance != SmsManager.getInstance().getLastSentDistance());

        showStats();
        composite.addAll(
                TimerManager.getInstance().timerListener().subscribe(ignore -> onTimer()),
                SmsManager.getInstance().smsSendingListener().subscribe(ignore -> onSendingSms()),
                GpsManager.getInstance().distanceChangedListener().subscribe(ignore -> onDistanceChanged()),
                GpsManager.getInstance().homeChangedListener().subscribe(ignore -> onHomeChanged()),
                ActivityRecognitionManager
                    .getInstance()
                    .stationaryListener()
                    .subscribe(ignore -> deviceIsStationary()),
                ActivityRecognitionManager
                    .getInstance()
                    .movingListener()
                    .subscribe(ignore -> deviceIsMoving()));

        if (ContextCompat.checkSelfPermission(
                getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            PermissionsManager
                    .getInstance()
                    .permissionGrantedListener()
                    .filter(permission -> permission.equals(Manifest.permission.ACCESS_FINE_LOCATION))
                    .subscribe(ignore -> onLocationPermissionGranted());
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
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    // see http://stackoverflow.com/questions/32083053/android-fragment-onattach-deprecated#32088447
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            this.activity = activity;
        }
    }

    private void showStats() {
        @SuppressLint("DefaultLocale") String distanceStr = String.format("%.3f km", distance);

        if (distance == 0.0) {
            distanceStr = "0 km";
        }
        distanceTextView.setText(distanceStr);
        int seconds = (int) (System.currentTimeMillis() -
                TimerManager.getInstance().getResetTime()) / 1000;
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
        String result = String.format("%.1f", speed) + " " + activity.getString(R.string.speed_unit);
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
                System.currentTimeMillis() - TimerManager.getInstance().getResetTime() > 3 * 3600 * 1000;
        if (distance != SmsManager.getInstance().getLastSentDistance()) {
            sendNowButton.setEnabled(SmsManager.getInstance().isTextingEnabled());
        }

        if (resetValues) {  // reset the values if three hours have passed
            this.speed = 0.0;
            this.distance = 0.0;
        }
        else {
            this.distance = GpsManager.getInstance().getDistance();
            this.speed = GpsManager.getInstance().getSpeed();
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
            calendar.setTimeInMillis(TimerManager.getInstance().getResetTime());
            int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60;
            minutes += calendar.get(Calendar.MINUTE);
            LocationModel location = new LocationModel();
            location.setDistance(distance);
            location.setDirection(0);
            location.setTime(minutes);
            location.setSpeed(speed);
            SmsManager.getInstance().send(location);
        }
    }

    private void onSendingSms() {
        sendNowButton.setEnabled(false);
    }

    private void onHomeChanged() {
        distance = GpsManager.getInstance().getDistance();
        showStats();
    }

    private void onLocationPermissionGranted() {
        sendNowButton.setEnabled(SmsManager.getInstance().isTextingEnabled());
    }
}
