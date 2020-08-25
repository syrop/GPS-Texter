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
 * If you like this program, consider donating bitcoin: bc1qncxh5xs6erq6w4qz3a7xl7f50agrgn3w58dsfp
 */

package pl.org.seva.texter

import org.junit.Test
import org.junit.runner.RunWith

import pl.org.seva.texter.mock.MockSmsSender

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import pl.org.seva.texter.action.DelayAction
import pl.org.seva.texter.main.MainActivity
import pl.org.seva.texter.sms.smsSender

@RunWith(AndroidJUnit4::class)
class LocationTest {

    // https://stackoverflow.com/questions/29945087/kotlin-and-new-activitytestrule-the-rule-must-be-public
    @Suppress("unused", "BooleanLiteralArgument")
    @get:Rule
    val activityRule =
        ActivityTestRule(MainActivity::class.java, true, true)

    @Test
    fun testLocation() {
        onView(isRoot()).perform(DelayAction.delay(TENTH_SECOND_MS))
        repeat (DURATION_SEC) {
            onView(isRoot()).perform(DelayAction.delay(SECOND_MS))
        }
        val sender = smsSender as MockSmsSender
        assertTrue(sender.messagesSent >= TestConstants.EXPECTED_MESSAGES_SENT)
    }

    companion object {
        private const val DURATION_SEC = 50
        private const val SECOND_MS = 1000L
        private const val TENTH_SECOND_MS = 100L
    }
}
