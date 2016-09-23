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

package pl.org.seva.texter.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Calendar;

import pl.org.seva.texter.R;
import pl.org.seva.texter.databinding.StatsFragmentBinding;
import pl.org.seva.texter.listeners.IDistanceChangedListener;
import pl.org.seva.texter.listeners.IHomeChangedListener;
import pl.org.seva.texter.listeners.IPermissionGrantedListener;
import pl.org.seva.texter.listeners.ISMSListener;
import pl.org.seva.texter.listeners.ITimerListener;
import pl.org.seva.texter.managers.GPSManager;
import pl.org.seva.texter.managers.PermissionsManager;
import pl.org.seva.texter.managers.SMSManager;
import pl.org.seva.texter.model.LocationModel;
import pl.org.seva.texter.managers.TimerManager;

/**
 * Created by hp1 on 21-01-2015.
 */
public class StatsFragment extends Fragment
        implements IDistanceChangedListener, ITimerListener,
        View.OnClickListener, ISMSListener, IHomeChangedListener,
        IPermissionGrantedListener {

    private static String homeString;
    private static String hourString;

    private TextView distanceTextView;
    private TextView intervalTextView;
    private TextView speedTextView;
    private Button sendNowButton;

    private double distance;
    private double speed;

    private Activity activity;

    public static StatsFragment newInstance() {
        return new StatsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        distance = GPSManager.getInstance().getDistance();
        speed = GPSManager.getInstance().getSpeed();

        homeString = getString(R.string.home);
        hourString = getActivity().getString(R.string.hour);
        StatsFragmentBinding binding =
                DataBindingUtil.inflate(inflater, R.layout.stats_fragment, container, false);
        distanceTextView = binding.distanceValue;
        intervalTextView = binding.intervalValue;
        speedTextView = binding.speedValue;
        sendNowButton = binding.sendNowButton;
        sendNowButton.setOnClickListener(this);
        sendNowButton.setEnabled(distance != 0.0 &&
                distance != SMSManager.getInstance().getLastSentDistance());

        show();
        TimerManager.getInstance().addListener(this);
        SMSManager.getInstance().addSMSListener(this);
        GPSManager.getInstance().addDistanceChangedListener(this);
        GPSManager.getInstance().addHomeChangedListener(this);

        if (ContextCompat.checkSelfPermission(
                getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            PermissionsManager.getInstance().addPermissionGrantedListener(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    this);
        }

        return binding.getRoot();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof  Activity) {
            this.activity = (Activity) context;
        }
    }

    private void show() {
        @SuppressLint("DefaultLocale") String distanceStr = String.format("%.3f km", distance);

        if (distance == 0.0) {
            distanceStr = "0 km";
        }
        distanceTextView.setText(distanceStr);
        int seconds = (int) (System.currentTimeMillis() - TimerManager.getInstance().getResetTime()) / 1000;
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
        TimerManager.getInstance().removeListener(this);
        GPSManager.getInstance().removeDistanceChangedListener(this);
        SMSManager.getInstance().removeSMSListener(this);
        GPSManager.getInstance().removeHomeChangedListener(this);
    }

    @Override
    public void onDistanceChanged() {
        boolean resetValues =
                System.currentTimeMillis() - TimerManager.getInstance().getResetTime() > 3 * 3600 * 1000;
        if (distance != SMSManager.getInstance().getLastSentDistance()) {
            sendNowButton.setEnabled(true);
        }

        if (resetValues) {  // reset the values if three hours have passed
            this.speed = 0.0;
            this.distance = 0.0;
        }
        else {
            this.distance = GPSManager.getInstance().getDistance();
            this.speed = GPSManager.getInstance().getSpeed();
        }
        show();
    }

    @Override
    public void onTimer() {
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(this::show);
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
            SMSManager.getInstance().send(location);
        }
    }

    @Override
    public void onSendingSMS() {
        sendNowButton.setEnabled(false);
    }

    @Override
    public void onSMSSent() {
    }

    @Override
    public void onHomeChanged() {
        distance = GPSManager.getInstance().getDistance();
        show();
    }

    @Override
    public void onPermissionGranted(String permission) {
        if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            sendNowButton.setEnabled(true);
            PermissionsManager.getInstance().
                    removePermissionGrantedListener(Manifest.permission.ACCESS_FINE_LOCATION, this);
        }
    }
}
