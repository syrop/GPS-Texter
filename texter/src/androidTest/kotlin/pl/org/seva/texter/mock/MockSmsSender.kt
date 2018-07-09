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
 *
 * If you like this program, consider donating bitcoin: 36uxha7sy4mv6c9LdePKjGNmQe8eK16aX6
 */

package pl.org.seva.texter.mock

import android.app.Activity
import android.app.PendingIntent

import pl.org.seva.texter.sms.SmsSender

class MockSmsSender : SmsSender() {

    var messagesSent: Int = 0
        private set

    override val maxSentDistance = MAX_SENT_DISTANCE

    override val isTextingEnabled = true

    override val isCorrectPhoneNumberSet = true

    public override fun needsPermission() = false

    override fun sendTextMessage(
            text: String,
            sentIntent: PendingIntent,
            deliveredIntent: PendingIntent) {
        messagesSent++
        sentIntent.send(Activity.RESULT_OK)
    }

    companion object {
        val MAX_SENT_DISTANCE = 50
    }
}
