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
 * If you like this program, consider donating bitcoin: 3JVNWUeVH118S3pzU4hDgkUNwEeNarZySf
 */

package pl.org.seva.texter.settings

import android.content.Context
import android.content.res.TypedArray
import android.support.v7.preference.Preference
import android.util.AttributeSet
import pl.org.seva.texter.main.Constants

class NumberPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    override fun onGetDefaultValue(a: TypedArray, index: Int) = Constants.DEFAULT_PHONE_NUMBER

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        if (!restorePersistedValue) {
            persistString(Constants.DEFAULT_PHONE_NUMBER)
        }
    }
}
