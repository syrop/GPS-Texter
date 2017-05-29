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

package pl.org.seva.texter.view.preference

import android.content.Context
import android.content.res.TypedArray
import android.preference.Preference
import android.util.AttributeSet

import pl.org.seva.texter.presenter.utils.Constants
import java.util.*

class HomeLocationPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    private var lat: Double = 0.0
    private var lon: Double = 0.0

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return Constants.DEFAULT_HOME_LOCATION
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        val value: String
        if (restorePersistedValue) {
            value = getPersistedString(HOME_LOCATION)
            lat = parseLatitude(value)
            lon = parseLongitude(value)
        } else {
            value = Constants.DEFAULT_HOME_LOCATION
            persistString(value)
        }
        lat = parseLatitude(value)
        lon = parseLongitude(value)
    }

    companion object {

        private val HOME_LOCATION = "HOME_LOCATION"
        private val GEO_URI_FORMAT = "geo:%.6f,%.6f"

        fun toString(lat: Double, lon: Double): String {
            return String.format(Locale.US, GEO_URI_FORMAT, lat, lon)
        }

        fun parseLatitude(geoUri: String): Double {
            val str = geoUri.substring(geoUri.indexOf(":") + 1, geoUri.indexOf(","))
            return java.lang.Double.valueOf(str)!!
        }

        fun parseLongitude(geoUri: String): Double {
            val str = geoUri.substring(geoUri.indexOf(",") + 1)
            return java.lang.Double.valueOf(str)!!
        }
    }
}
