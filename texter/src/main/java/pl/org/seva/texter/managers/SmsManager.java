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

package pl.org.seva.texter.managers;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import pl.org.seva.texter.R;
import pl.org.seva.texter.activities.SettingsActivity;
import pl.org.seva.texter.listeners.SmsListener;
import pl.org.seva.texter.model.LocationModel;
import pl.org.seva.texter.utils.StringUtils;

public class SmsManager {

	private static SmsManager instance;

    private static final String TEXT_KEY = "pl.org.seva.texter.Text";
    private static final String DISTANCE_KEY = "pl.org.seva.texter.Distance";
    private static final String MINUTES_KEY = "pl.org.seva.texter.Minutes";
    private static final String DIRECTION_KEY = "pl.org.seva.texter.Direction";
    private static final String SPEED_KEY = "pl.org.seva.texter.Speed";

	private static final String SENT = "SMS_SENT";
    private static final String DELIVERED = "SMS_DELIVERED";

	private SharedPreferences preferences;
    private String speedUnit;
    private WeakReference<Context> weakContext;

	private final android.telephony.SmsManager smsManager;

    private final List<BroadcastReceiver> broadcastReceivers = new ArrayList<>();

    private double lastSentDistance;
	
	private boolean initialized;

    public static SmsManager getInstance() {
        if (instance == null ) {
            synchronized (SmsManager.class) {
                if (instance == null) {
                    instance = new SmsManager();
                }
            }
        }
        return instance;
    }

    public static void shutdown() {
        instance.unregisterReceivers();
        synchronized (SmsManager.class) {
            instance = null;
        }
    }

    private SmsManager() {
		smsManager = android.telephony.SmsManager.getDefault();
	}

    private final List<SmsListener> listeners = new ArrayList<>();
	
	public void init(final Context context, String speedUnit) {
		if (initialized) {
			return;
		}
        this.speedUnit = speedUnit;
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
        weakContext = new WeakReference<>(context);
		initialized = true;
	}

    public void addSMSListener(SmsListener listener) {
        synchronized (listeners) {
            removeSMSListener(listener);
            listeners.add(listener);
        }
    }

    public void removeSMSListener(SmsListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

	private String getPhoneNumber() {
		String numberStr = preferences.getString(SettingsActivity.SMS_NUMBER, "");
		return numberStr.length() > 0 ? numberStr : "0";
	}

    public int getMaxSentDistance() {
        String numberStr = preferences.getString(SettingsActivity.MAXIMUM_DISTANCE, "");
        return numberStr.length() > 0 ? Integer.valueOf(numberStr) : 0;
    }

    private void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        synchronized (broadcastReceivers) {
            Context context = weakContext.get();
            if (context != null) {
                context.registerReceiver(receiver, filter);
            }
            broadcastReceivers.add(receiver);
        }
    }

    private void unregisterReceiver(BroadcastReceiver receiver) {
        synchronized (broadcastReceivers) {
            Context context = weakContext.get();
            if (context != null) {
                context.unregisterReceiver(receiver);
            }
            broadcastReceivers.remove(receiver);
        }
    }

    private void unregisterReceivers() {
        synchronized (broadcastReceivers) {
            if (broadcastReceivers.isEmpty()) {
                return;
            }
            Context context = weakContext.get();
            if (context != null) {
                //noinspection Convert2streamapi
                for (BroadcastReceiver receiver : broadcastReceivers) {
                    context.unregisterReceiver(receiver);
                }
            }
            broadcastReceivers.clear();
        }

    }
	
	private void registerBroadcastReceiver(String id) {
        // When the SMS has been sent.
        registerReceiver(new BroadcastReceiver()
        {
            public void onReceive(Context arg0, Intent arg1) {
            	String text = arg1.getStringExtra(TEXT_KEY);
                LocationModel location = new LocationModel().
                    setDistance(arg1.getDoubleExtra(DISTANCE_KEY, 0.0)).
                    setTime(arg1.getIntExtra(MINUTES_KEY, 0)).
                    setDirection(arg1.getIntExtra(DIRECTION_KEY, 0)).
                    setSpeed(arg1.getDoubleExtra(SPEED_KEY, 0.0));
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                    	StringBuilder sentBuilder = new StringBuilder(arg0.getString(R.string.sent));
                    	int length = Toast.LENGTH_SHORT;
                    	if (text != null) {
                    		sentBuilder.append(": ").append(text);
                    		length = Toast.LENGTH_SHORT;
                    	}
                        Toast.makeText(arg0, sentBuilder.toString(), length).show();
                        if (location != null) {
                            HistoryManager.getInstance().add(location);
                            synchronized (listeners) {
                                //noinspection Convert2streamapi
                                for (SmsListener listener : listeners) {
                                    listener.onSMSSent();
                                }
                            }
                        }
                        break;
                    case android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(
                                arg0,
                                arg0.getString(R.string.generic_failure),
                                Toast.LENGTH_SHORT).show();
                        break;
                    case android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(
                                arg0,
                                arg0.getString(R.string.no_service),
                                Toast.LENGTH_SHORT).show();
                        break;
                    case android.telephony.SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(arg0, "Null PDU", Toast.LENGTH_SHORT).show();
                        break;
                    case android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(
                                arg0,
                                arg0.getString(R.string.radio_off),
                                Toast.LENGTH_SHORT).show();
                        break;
                }
                unregisterReceiver(this);
            }
        }, new IntentFilter(SENT + id));

        // When the SMS has been delivered.
        registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
            String text = arg1.getStringExtra(TEXT_KEY);
            switch (getResultCode())
            {
                case Activity.RESULT_OK:
                    StringBuilder deliveredBuilder =
                            new StringBuilder(arg0.getString(R.string.delivered));
                    if (text != null) {
                        deliveredBuilder.append(": ").append(text);
                    }
                    Toast.makeText(arg0, deliveredBuilder.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    StringBuilder notDeliveredBuilder =
                            new StringBuilder(arg0.getString(R.string.not_delivered));
                    if (text != null) {
                        notDeliveredBuilder.append(": ").append(text);
                    }
                    Toast.makeText(arg0, notDeliveredBuilder.toString(), Toast.LENGTH_SHORT).show();
                    break;
            }
            unregisterReceiver(this);
            }
        }, new IntentFilter(DELIVERED + id));        
	}
	
	public void send(LocationModel model) {
        if (getPhoneNumber().equals("0")) {
            return;
        }
        checkInit();
        double distance = model.getDistance();
        @SuppressLint("DefaultLocale")
        String distanceStr = String.format("%.2f", distance) + model.getSign();
        StringBuilder smsBuilder = new StringBuilder(distanceStr + " km");
        if (isSpeedIncluded()) {
            String speedStr = StringUtils.getSpeedString(model.getSpeed(), speedUnit);
            smsBuilder.append(speedStr.startsWith("0 ") ? "" : ", " + speedStr);
        }
        if (isTimeIncluded()) {
            Calendar now = Calendar.getInstance();
            String minuteStr = Integer.toString(now.get(Calendar.MINUTE));
            if (minuteStr.length() == 1) {
                minuteStr = "0" + minuteStr;
            }
            String timeStr = Integer.toString(now.get(Calendar.HOUR_OF_DAY)) + ":" + minuteStr;
            smsBuilder.append(" (").append(timeStr).append(")");
        }
        if (isLocationIncluded()) {
            smsBuilder.append(" ").append(GpsManager.getInstance().getLocationUrl());
        }
        @SuppressLint("DefaultLocale")
        String intentDistanceStr = String.format("%.1f", distance) + model.getSign() + " km";
        String smsStr = smsBuilder.toString();
        send(smsStr, intentDistanceStr, model);
        lastSentDistance = distance;
    }

    public double getLastSentDistance() {
        return lastSentDistance;
    }

    private void send(String text, String intentText, LocationModel location) {
        String id = UUID.randomUUID().toString();
		Intent sentIntent = new Intent(SENT + id);
		sentIntent.putExtra(TEXT_KEY, intentText);
        sentIntent.putExtra(DISTANCE_KEY, location.getDistance());
        sentIntent.putExtra(MINUTES_KEY, location.getMinutes());
        sentIntent.putExtra(DIRECTION_KEY, location.getDirection());
        sentIntent.putExtra(SPEED_KEY, location.getSpeed());

		Intent deliveredIntent = new Intent(DELIVERED + id);
		deliveredIntent.putExtra(TEXT_KEY, intentText);

        synchronized (listeners) {
            //noinspection Convert2streamapi
            for (SmsListener listener : listeners) {
                listener.onSendingSMS();
            }
        }
        Context context = weakContext.get();
        if (context != null) {
            PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, sentIntent, 0);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0, deliveredIntent, 0);
            registerBroadcastReceiver(id);
            try {
                smsManager.sendTextMessage(getPhoneNumber(), null, text, sentPI, deliveredPI);
            } catch (SecurityException ex) {
                // Ignore, as may indicate the app has no permission to send SMS.
            }
        }
	}
	
	private void checkInit() {
		if (!initialized) {
			throw new IllegalStateException("SMS not initialized");
		}
	}

    private boolean isSpeedIncluded() {
        return preferences.getBoolean(SettingsActivity.INCLUDE_SPEED, false);
    }

    private boolean isLocationIncluded() {
        return preferences.getBoolean(SettingsActivity.INCLUDE_LOCATION, false);
    }

    private boolean isTimeIncluded() {
        return preferences.getBoolean(SettingsActivity.INCLUDE_TIME, false);
    }

    public boolean isTextingEnabled() {
        return preferences.getBoolean(SettingsActivity.SMS_ENABLED, false);
    }
}
