package pl.org.seva.texter.managers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.widget.Toast;

import pl.org.seva.texter.R;
import pl.org.seva.texter.activities.SettingsActivity;
import pl.org.seva.texter.listeners.ISMSListener;
import pl.org.seva.texter.model.LocationModel;
import pl.org.seva.texter.utils.StringUtils;

public class SMSManager {

	private static SMSManager instance;

    private static final String TEXT_KEY = "text";
    private static final String MODEL_KEY = "model";
	
	private static final String SENT = "SMS_SENT";
    private static final String DELIVERED = "SMS_DELIVERED";

	private Context context;
	private SharedPreferences preferences;
    private String speedUnit;

	private SmsManager smsManager;

    private final List<BroadcastReceiver> broadcastReceivers = new ArrayList<>();

    private double lastSentDistance;
	
	private boolean initialized;

    public static SMSManager getInstance() {
        if (instance == null ) {
            synchronized (SMSManager.class) {
                if (instance == null) {
                    instance = new SMSManager();
                }
            }
        }
        return instance;
    }

    public static void shutdown() {
        synchronized (SMSManager.class) {
            instance = null;
        }
    }


    private SMSManager() {
		smsManager = SmsManager.getDefault();
	}

    private final List<ISMSListener> listeners = new ArrayList<>();
	
	public void init(final Context context, String speedUnit) {
		if (initialized) {
			return;
		}
		this.context = context;
        this.speedUnit = speedUnit;
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
		initialized = true;
	}

    public void addSMSListener(ISMSListener listener) {
        synchronized (listeners) {
            removeSMSListener(listener);
            listeners.add(listener);
        }
    }

    public void removeSMSListener(ISMSListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void clearSMSListeners() {
        synchronized (listeners) {
            listeners.clear();
        }
    }
	
	public String getPhoneNumber() {
		String numberStr = preferences.getString(SettingsActivity.SMS_NUMBER, "");
		return numberStr.length() > 0 ? numberStr : "0";
	}

    public int getMaxSentDistance() {
        String numberStr = preferences.getString(SettingsActivity.MAXIMUM_DISTANCE, "");
        return numberStr.length() > 0 ? Integer.valueOf(numberStr) : 0;
    }

    private void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        synchronized (broadcastReceivers) {
            context.registerReceiver(receiver, filter);
            broadcastReceivers.add(receiver);
        }
    }

    private void unregisterReceiver(BroadcastReceiver receiver) {
        synchronized (broadcastReceivers) {
            context.unregisterReceiver(receiver);
            broadcastReceivers.remove(receiver);
        }
    }

    public boolean unregisterReceivers() {
        synchronized (broadcastReceivers) {
            if (broadcastReceivers.isEmpty()) {
                return false;
            }
            for (BroadcastReceiver receiver : broadcastReceivers) {
                context.unregisterReceiver(receiver);
            }
            broadcastReceivers.clear();
        }

        return true;
    }
	
	private void registerBroadcastReceiver(String id) {
        // When the SMS has been sent.
        registerReceiver(new BroadcastReceiver()
        {
            public void onReceive(Context arg0, Intent arg1) {
            	String text = arg1.getStringExtra(TEXT_KEY);
                LocationModel location = arg1.getParcelableExtra(MODEL_KEY);
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
                                for (ISMSListener listener : listeners) {
                                    listener.onSMSSent(location);
                                }
                            }
                        }
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(arg0, arg0.getString(R.string.generic_failure), Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(arg0, arg0.getString(R.string.no_service), Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(arg0, "Null PDU", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(arg0, arg0.getString(R.string.radio_off), Toast.LENGTH_SHORT).show();
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
            String text = arg1.getStringExtra("text");
            switch (getResultCode())
            {
                case Activity.RESULT_OK:
                    StringBuilder deliveredBuilder = new StringBuilder(arg0.getString(R.string.delivered));
                    if (text != null) {
                        deliveredBuilder.append(": ").append(text);
                    }
                    Toast.makeText(arg0, deliveredBuilder.toString(), Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    StringBuilder notDeliveredBuilder = new StringBuilder(arg0.getString(R.string.not_delivered));
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
        String distanceStr = String.format("%.2f", distance) + model.getSign();
        StringBuilder smsBuilder = new StringBuilder(distanceStr + " km");
        if (isSpeedIncluded()) {
            String speedStr = StringUtils.getSpeedStr(model.getSpeed(), speedUnit);
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
            smsBuilder.append(" ").append(GPSManager.getInstance().getLocationUrl());
        }

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
        sentIntent.putExtra(MODEL_KEY, location);

		Intent deliveredIntent = new Intent(DELIVERED + id);
		deliveredIntent.putExtra("text", intentText);

		PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, sentIntent, 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0, deliveredIntent, 0);
		registerBroadcastReceiver(id);
        if (location != null) {
            synchronized (listeners) {
                for (ISMSListener listener : listeners) {
                    listener.onSendingSMS(location);
                }
            }
        }
		smsManager.sendTextMessage(getPhoneNumber(), null, text, sentPI, deliveredPI);
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
