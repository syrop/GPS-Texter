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

package pl.org.seva.texter.mock;

import android.app.Activity;
import android.app.PendingIntent;
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import pl.org.seva.texter.presenter.source.LocationSource;
import pl.org.seva.texter.presenter.utils.SmsSender;
import pl.org.seva.texter.presenter.utils.SmsHistory;
import pl.org.seva.texter.presenter.utils.ZoneCalculator;

@Singleton
public class MockSmsSender extends SmsSender {

    private int messagesSent;

    public MockSmsSender(LocationSource locationSource, SmsHistory smsHistory, ZoneCalculator zoneCalculator) {
        this.setLocationSource(locationSource);
        this.setSmsHistory(smsHistory);
        this.setZoneCalculator(zoneCalculator);
    }

    @SuppressWarnings({"SameReturnValue", "unused"})
    @Override
    public boolean isTextingEnabled() {
        return true;
    }

    @SuppressWarnings({"SameReturnValue", "unused"})
    @Override
    protected boolean isCorrectPhoneNumberSet() {
        return true;
    }

    @SuppressWarnings({"SameReturnValue", "unused"})
    @Override
    public boolean needsPermission() {
        return false;
    }

    @SuppressWarnings("unused")
    protected void sendTextMessage(
            @NonNull String text,
            @NonNull PendingIntent sentIntent,
            @NonNull PendingIntent deliveredIntent)
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
