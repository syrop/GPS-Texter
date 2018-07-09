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

package pl.org.seva.texter

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import pl.org.seva.texter.mock.MockSmsSender
import pl.org.seva.texter.main.MainActivity

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.matcher.ViewMatchers.isRoot
import junit.framework.Assert.assertTrue
import pl.org.seva.texter.action.DelayAction
import pl.org.seva.texter.main.instance
import pl.org.seva.texter.sms.SmsSender

@RunWith(AndroidJUnit4::class)
class LocationTest {

    // https://stackoverflow.com/questions/29945087/kotlin-and-new-activitytestrule-the-rule-must-be-public
    @Suppress("unused")
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, true, true)

    @Test
    fun testLocation() {
        onView(isRoot()).perform(DelayAction.delay(TENTH_SECOND_MS))
        repeat (DURATION_SEC) {
            onView(isRoot()).perform(DelayAction.delay(SECOND_MS))
        }
        val sender = instance<SmsSender>() as MockSmsSender
        assertTrue(sender.messagesSent >= TestConstants.EXPECTED_MESSAGES_SENT)
    }

    companion object {
        private val DURATION_SEC = 50
        private val SECOND_MS = 1000L
        private val TENTH_SECOND_MS = 100L
    }
}
