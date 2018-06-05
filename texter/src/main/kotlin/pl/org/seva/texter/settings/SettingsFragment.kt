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
 * If you like this program, consider donating bitcoin: 37vHXbpPcDBwcCTpZfjGL63JRwn6FPiXTS
 */

package pl.org.seva.texter.settings

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat

import pl.org.seva.texter.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) =
            addPreferencesFromResource(R.xml.preferences)

    var homeLocationClickedListener: (() -> Unit)? = null
    var smsEnabledClickedListener: (() -> Unit)? = null
    var numberClickedListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!activity!!.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            preferenceScreen.removePreference(findPreference(SettingsActivity.CATEGORY_SMS))
        }
    }

    override fun onPreferenceTreeClick(preference: Preference) = when (preference.key) {
        SettingsActivity.HOME_LOCATION -> {
            homeLocationClickedListener?.invoke()
            true
        }
        SettingsActivity.SMS_ENABLED -> {
            smsEnabledClickedListener?.invoke()
            true
        }
        SettingsActivity.PHONE_NUMBER -> {
            numberClickedListener?.invoke()
            true
        }
        else -> super.onPreferenceTreeClick(preference)
    }

    companion object {
        fun newInstance(): SettingsFragment = SettingsFragment()
    }
}
