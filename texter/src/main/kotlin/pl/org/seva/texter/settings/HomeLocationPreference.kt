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

package pl.org.seva.texter.settings

import android.content.Context
import android.content.res.TypedArray
import androidx.preference.Preference
import android.util.AttributeSet

import pl.org.seva.texter.main.Constants
import java.util.*

class HomeLocationPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any = Constants.DEFAULT_HOME_LOCATION

    override fun onSetInitialValue(defaultValue: Any?) {
        persistString(Constants.DEFAULT_HOME_LOCATION)
    }

    companion object {
        private const val GEO_URI_FORMAT = "geo:%.6f,%.6f"

        fun toString(lat: Double, lon: Double) = String.format(Locale.US, GEO_URI_FORMAT, lat, lon)

        fun parseLatitude(geoUri: String): Double {
            return geoUri.substring(geoUri.indexOf(":") + 1, geoUri.indexOf(",")).toDouble()
        }

        fun parseLongitude(geoUri: String): Double {
            return geoUri.substring(geoUri.indexOf(",") + 1).toDouble()
        }
    }
}
