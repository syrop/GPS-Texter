package pl.org.seva.texter.controller;

import java.util.Calendar;

import pl.org.seva.texter.listeners.IDistanceChangedListener;
import pl.org.seva.texter.managers.GPSManager;
import pl.org.seva.texter.managers.SMSManager;
import pl.org.seva.texter.model.LocationModel;
import pl.org.seva.texter.model.ZoneModel;

/**
 * Created by wiktor on 11.01.16.
 */
public class SMSController implements IDistanceChangedListener {

    private static final SMSController INSTANCE = new SMSController();

    /** If the number of measurements in the present zone has reached the trigger, send SMS. */
    private static final int SMS_TRIGGER = 2;
    /** Time spend in zone before an SMS is sent. */
    private static final long TIME_IN_ZONE = 11 * 1000;

    private LocationModel lastSentLocation;
    private ZoneModel.Zone zone;
    private boolean smsInSystem;
    private boolean initialized;

    private SMSController() {

    }

    public static SMSController getInstance() {
        return INSTANCE;
    }

    public void init(boolean smsInSystem) {
        if (initialized) {
            return;
        }
        initialized = true;
        this.smsInSystem = smsInSystem;
    }

    private void sendSMS(LocationModel model) {
        if (!smsInSystem) {
            return;
        }
        if (!SMSManager.getInstance().isTextingEnabled()) {
            return;
        }
        if (this.lastSentLocation != null && this.lastSentLocation.equals(model)) {
            return;
        }
        this.lastSentLocation = model;
        SMSManager.getInstance().send(model);
    }

    @Override
    public void onDistanceChanged() {
        double distance = GPSManager.getInstance().getDistance();
        double speed = GPSManager.getInstance().getSpeed();

        long time = System.currentTimeMillis();
        int direction = 0 ; // alternatively (int) Math.signum(this.distance - distance);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60;
        minutes += calendar.get(Calendar.MINUTE);

        LocationModel location = new LocationModel();
        location.setDistance(distance);
        location.setDirection(direction);
        location.setTime(minutes);
        location.setSpeed(speed);

        ZoneModel.Zone zone = ZoneModel.getInstance().zone(distance, true);
        if (this.zone == null) {
            this.zone = zone;
        }
        else if (zone.getMin() != this.zone.getMin() &&
                zone.getCounter() >= SMS_TRIGGER &&
                zone.getDelay() >= TIME_IN_ZONE) {
            if (this.zone.getMin() > zone.getMin()) {
                direction = -1;
            }
            else {
                direction = 1;
            }
            location.setDirection(direction);  // calculated specifically for zone border

            if ((direction == 1 ? zone.getMin() : zone.getMax()) <=
                    SMSManager.getInstance().getMaxSentDistance()) {
                sendSMS(location);
            }
            this.zone = zone;
        }
    }
}
