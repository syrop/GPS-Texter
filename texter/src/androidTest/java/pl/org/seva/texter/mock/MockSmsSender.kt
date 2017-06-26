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

package pl.org.seva.texter.mock

import android.app.Activity
import android.app.PendingIntent

import javax.inject.Singleton

import pl.org.seva.texter.presenter.source.LocationSource
import pl.org.seva.texter.presenter.utils.SmsSender
import pl.org.seva.texter.presenter.utils.SmsHistory
import pl.org.seva.texter.presenter.utils.ZoneCalculator

@Singleton
class MockSmsSender internal constructor(locationSource: LocationSource, smsHistory: SmsHistory, zoneCalculator: ZoneCalculator) : SmsSender() {

    var messagesSent: Int = 0
        private set

    init {
        this.locationSource = locationSource
        this.smsHistory = smsHistory
        this.zoneCalculator = zoneCalculator
    }

    override val isTextingEnabled: Boolean
        get() = true

    override val isCorrectPhoneNumberSet: Boolean
        get() = true

    public override fun needsPermission() = false

    @Throws(SecurityException::class)
    override fun sendTextMessage(
            text: String,
            sentIntent: PendingIntent,
            deliveredIntent: PendingIntent) {
        try {
            messagesSent++
            sentIntent.send(Activity.RESULT_OK)
        } catch (ex: PendingIntent.CanceledException) {
            ex.printStackTrace()
        }

    }
}
