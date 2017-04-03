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

package pl.org.seva.texter.mockmanager;

import android.app.Activity;
import android.app.PendingIntent;

import javax.inject.Singleton;

import pl.org.seva.texter.presenter.manager.GpsManager;
import pl.org.seva.texter.presenter.utils.SmsCache;
import pl.org.seva.texter.presenter.manager.SmsManager;
import pl.org.seva.texter.presenter.utils.ZoneCalculator;

@Singleton
public class MockSmsManager extends SmsManager {

    private int messagesSent;

    public MockSmsManager(GpsManager gpsManager, SmsCache smsCache, ZoneCalculator zoneCalculator) {
        this.gpsManager = gpsManager;
        this.smsCache = smsCache;
        this.zoneCalculator = zoneCalculator;
    }

    protected void sendTextMessage(String text, PendingIntent sentIntent, PendingIntent deliveredIntent)
            throws SecurityException {
        try {
            messagesSent++;
            sentIntent.send(Activity.RESULT_OK);
        }
        catch (PendingIntent.CanceledException ex) {
            ex.printStackTrace();
        }
    }

    public int getMessagesSent() {
        return messagesSent;
    }
}