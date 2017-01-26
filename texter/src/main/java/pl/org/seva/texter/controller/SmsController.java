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

package pl.org.seva.texter.controller;

import java.util.Calendar;

import pl.org.seva.texter.managers.GpsManager;
import pl.org.seva.texter.managers.SmsManager;
import pl.org.seva.texter.managers.ZoneManager;
import pl.org.seva.texter.model.LocationModel;
import pl.org.seva.texter.model.ZoneModel;
import pl.org.seva.texter.utils.Constants;

public class SmsController {

    private static final SmsController INSTANCE = new SmsController();

    private LocationModel lastSentLocation;
    private ZoneModel zone;
    private boolean smsInSystem;
    private boolean initialized;

    private SmsController() {
        // do nothing
    }

    public static SmsController getInstance() {
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
        if (!SmsManager.getInstance().isTextingEnabled()) {
            return;
        }
        if (this.lastSentLocation != null && this.lastSentLocation.equals(model)) {
            return;
        }
        this.lastSentLocation = model;
        SmsManager.getInstance().send(model);
    }

    public synchronized void resetZones() {
        ZoneManager.getInstance().clear();
        zone = null;
    }

    public void onDistanceChanged() {
        double distance = GpsManager.getInstance().getDistance();
        double speed = GpsManager.getInstance().getSpeed();

        long time = System.currentTimeMillis();
        int direction = 0; // alternatively (int) Math.signum(this.distance - distance);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        int minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60;
        minutes += calendar.get(Calendar.MINUTE);

        LocationModel location = new LocationModel();
        location.setDistance(distance);
        location.setDirection(direction);
        location.setTime(minutes);
        location.setSpeed(speed);

        synchronized (this) {
            ZoneModel zone = ZoneManager.getInstance().zone(distance);
            if (this.zone == null) {
                this.zone = zone;
            }
            else if (zone.getMin() != this.zone.getMin() &&
                    zone.getCounter() >= Constants.SMS_TRIGGER &&
                    zone.getDelay() >= Constants.TIME_IN_ZONE) {
                if (this.zone.getMin() > zone.getMin()) {
                    direction = -1;
                }
                else {
                    direction = 1;
                }
                location.setDirection(direction);  // calculated specifically for zone border

                if ((direction == 1 ? zone.getMin() : zone.getMax()) <=
                        SmsManager.getInstance().getMaxSentDistance()) {
                    sendSMS(location);
                }
                this.zone = zone;
            }
        }
    }
}
