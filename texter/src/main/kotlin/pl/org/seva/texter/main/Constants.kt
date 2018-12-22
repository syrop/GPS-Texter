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

package pl.org.seva.texter.main

object Constants {

    /** Geo URI for Warsaw.  */
    const val DEFAULT_HOME_LOCATION = "geo:52.233333,21.016667"

    /** Number when it has not been otherwise set. */
    const val DEFAULT_PHONE_NUMBER = ""

    /** Send an sms each time this value is crossed.  */
    const val KM_THRESHOLD = 2

    /** If the number of measurements in the present zone has reached the trigger, send SMS.  */
    const val SMS_COUNT_TRIGGER = 2

    /** Time spent in zone before an SMS is sent.  */
    const val TIME_IN_ZONE = 11 * 1000

    const val LOCATION_UPDATE_FREQUENCY_MS = 1000L
}
