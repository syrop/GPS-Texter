package pl.org.seva.texter.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Calendar;

import pl.org.seva.texter.R;
import pl.org.seva.texter.listeners.IDistanceChangedListener;
import pl.org.seva.texter.listeners.IHomeChangedListener;
import pl.org.seva.texter.listeners.ISMSListener;
import pl.org.seva.texter.managers.GPSManager;
import pl.org.seva.texter.managers.SMSManager;
import pl.org.seva.texter.model.LocationModel;
import pl.org.seva.texter.utils.Timer;

/**
 * Created by hp1 on 21-01-2015.
 */
public class StatsFragment extends Fragment
        implements IDistanceChangedListener, Timer.TimerListener,
        View.OnClickListener, ISMSListener, IHomeChangedListener {

    private static final String SAVED_TIME = "time";
    private static final String SAVED_SPEED = "speed";
    private static final String SAVED_DISTANCE = "distance";
    private static final String SAVED_LAST_DISTANCE = "last_distance";

    private static String homeString;
    private static String hourString;

    private TextView distanceTextView;
    private TextView intervalLabelTextView;
    private TextView intervalTextView;
    private TextView speedTextView;
    private Button sendNowButton;

    private long time;  // milliseconds
    private double distance;
    private double lastSentDistance;
    private double speed;

    private Activity activity;

    public static StatsFragment newInstance() {
        return new StatsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            time = savedInstanceState.getLong(SAVED_TIME);
            distance = savedInstanceState.getDouble(SAVED_DISTANCE);
            lastSentDistance = savedInstanceState.getDouble(SAVED_LAST_DISTANCE);
            speed = savedInstanceState.getDouble(SAVED_SPEED);
        }
        else {
            time = System.currentTimeMillis();
        }
        homeString = getString(R.string.home);
        hourString = getActivity().getString(R.string.hour);
        View v = inflater.inflate(R.layout.stats_fragment,container,false);
        distanceTextView = (TextView) v.findViewById(R.id.distance_value);
        intervalLabelTextView = (TextView) v.findViewById(R.id.interval_label);
        intervalTextView = (TextView) v.findViewById(R.id.interval_value);
        speedTextView = (TextView) v.findViewById(R.id.speed_value);
        sendNowButton = (Button) v.findViewById(R.id.send_now_button);
        sendNowButton.setOnClickListener(this);
        sendNowButton.setEnabled(distance != 0.0 && distance != lastSentDistance);

        show();
        Timer.getInstance().reset();
        Timer.getInstance().addListener(this);
        SMSManager.getInstance().addSMSListener(this);
        GPSManager.getInstance().addDistanceChangedListener(this);
        GPSManager.getInstance().addHomeChangedListener(this);

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof  Activity) {
            this.activity = (Activity) context;
        }
    }

    private void show() {
        String distanceStr = String.format("%.3f km", distance);

        if (distance == 0.0) {
            distanceStr = "0 km";
        }
        distanceTextView.setText(distanceStr);
        if (time != 0) {
            int seconds = (int) (System.currentTimeMillis() - time) / 1000;
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
            intervalLabelTextView.setVisibility(View.VISIBLE);
            intervalTextView.setVisibility(View.VISIBLE);
            intervalTextView.setText(timeStrBuilder.toString());
        }
        else {
            intervalLabelTextView.setVisibility(View.GONE);
            intervalTextView.setVisibility(View.GONE);
        }
        if (speed == 0.0 || distance == 0.0) {
            speedTextView.setVisibility(View.INVISIBLE);
        }
        else {
            speedTextView.setVisibility(View.VISIBLE);
            speedTextView.setText(getSpeedStr());
        }
    }

    private String getSpeedStr() {
        String result = String.format("%.1f", speed) + " " + getActivity().getString(R.string.speed_unit);
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
        Timer.getInstance().removeListener(this);
        GPSManager.getInstance().removeDistanceListener(this);
        SMSManager.getInstance().removeSMSListener(this);
        GPSManager.getInstance().removeHomeChangedListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(SAVED_TIME, time);
        outState.putDouble(SAVED_SPEED, speed);
        outState.putDouble(SAVED_DISTANCE, distance);
        outState.putDouble(SAVED_LAST_DISTANCE, lastSentDistance);
    }

    @Override
    public void onDistanceChanged(double distance, double speed) {
        time = System.currentTimeMillis();
        Timer.getInstance().reset();
        if (distance != lastSentDistance) {
            sendNowButton.setEnabled(true);
        }

        if (time - this.time > 3 * 3600 * 1000) {  // reset the values if three hours have passed
            this.speed = 0.0;
            this.distance = 0.0;
            this.lastSentDistance = 0.0;
        }

        this.distance = distance;
        this.speed = speed;
        show();
    }

    @Override
    public void onTimer() {
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                show();
            }
        });
    }

    public static String getHomeString() {
        return homeString;
    }

    @Override
    public void onClick(View v) {
        if (v == sendNowButton) {
            sendNowButton.setEnabled(false);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(time);
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
    public void onSendingSMS(LocationModel model) {
        lastSentDistance = model.getDistance();
        sendNowButton.setEnabled(false);
    }

    @Override
    public void onSMSSent(LocationModel model) {
    }

    @Override
    public void onHomeChanged() {
        distance = GPSManager.getInstance().getDistance();
        show();
    }
}
