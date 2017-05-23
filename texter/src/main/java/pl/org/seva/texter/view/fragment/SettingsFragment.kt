/*
 * Copyright (C) 2016 Wiktor Nizio
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

package pl.org.seva.texter.view.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen

import pl.org.seva.texter.R
import pl.org.seva.texter.view.activity.HomeLocationActivity
import pl.org.seva.texter.view.activity.SettingsActivity

class SettingsFragment : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        if (!activity.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            preferenceScreen.removePreference(findPreference(SettingsActivity.CATEGORY_SMS))
        }
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen, preference: Preference): Boolean {
        if (preference.key == activity.getString(R.string.home_location_key)) {
            val intent = Intent(activity, HomeLocationActivity::class.java)
            startActivity(intent)
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference)
    }

    companion object {

        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}
