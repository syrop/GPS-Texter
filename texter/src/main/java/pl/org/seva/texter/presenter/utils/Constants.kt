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

package pl.org.seva.texter.presenter.utils

object Constants {

    /** Geo URI for Warsaw.  */
    val DEFAULT_HOME_LOCATION = "geo:52.233333,21.016667"  // Warsaw

    /** Send an sms each time this value is passed.  */
    val KM_INTERVAL = 2  // two kilometers

    /** If the number of measurements in the present calculateZone has reached the trigger, send SMS.  */
    val SMS_TRIGGER = 2
    /** Time spend in calculateZone before an SMS is sent.  */
    val TIME_IN_ZONE = 11 * 1000

    val LOCATION_UPDATE_FREQUENCY: Long = 1000  // [ms]
}
