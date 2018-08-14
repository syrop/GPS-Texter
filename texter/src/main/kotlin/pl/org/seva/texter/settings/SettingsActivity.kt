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

package pl.org.seva.texter.settings

import pl.org.seva.texter.R
import pl.org.seva.texter.ui.TitledPagerAdapter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.core.app.ActivityCompat
import android.content.Intent
import android.content.SharedPreferences
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.view.WindowManager
import kotlinx.android.synthetic.main.toolbar.*
import pl.org.seva.texter.main.*
import pl.org.seva.texter.movement.locationSource
import pl.org.seva.texter.sms.smsSender

import java.util.ArrayList

class SettingsActivity : AppCompatActivity() {

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener {
        _, key -> onSharedPreferenceChanged(key)
    }

    private var permissionsCompositeSubscription = liveDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SMS_ENABLED, false)) {
            processSmsPermissions()
        }

        val fragments = ArrayList<Fragment>()
        val settingsFragment = SettingsFragment.newInstance()
        settingsFragment.smsEnabledClickedListener = { onSmsEnabledChanged() }
        settingsFragment.homeLocationClickedListener = { onHomeLocationClicked() }
        settingsFragment.numberClickedListener = { onNumberClicked() }
        fragments.add(settingsFragment)

        val adapter = TitledPagerAdapter(supportFragmentManager, null).setItems(fragments)
        val pager = findViewById<ViewPager>(R.id.pager)
        pager.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }

    private fun addReadContactsPermissionListeners() {
        permissions
                .permissionDeniedListener()
                .filter { it.first == Permissions.SMS_AND_CONTACTS_PERMISSION_REQUEST_ID }
                .filter { it.second == Manifest.permission.READ_CONTACTS }
                .subscribe { onReadContactsPermissionDenied() } addTo permissionsCompositeSubscription
    }

    private fun addLocationPermissionListeners() {
            permissions
                    .permissionGrantedListener()
                    .filter {it.first == Permissions.LOCATION_PERMISSION_REQUEST_ID }
                    .filter { it.second == Manifest.permission.ACCESS_FINE_LOCATION }
                    .subscribe { onLocationPermissionGranted() } addTo permissionsCompositeSubscription
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onHomeLocationClicked() = processLocationPermissions()

    private fun onSmsEnabledChanged() {
        if (isSmsEnabled()) {
            processSmsPermissions()
        }
    }

    private fun onNumberClicked() =
            PhoneNumberFragment().show(supportFragmentManager, PHONE_NUMBER_FRAGMENT_TAG)

    private fun isSmsEnabled() =
            PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SMS_ENABLED, false)

    private fun startHomeLocationActivity() {
        val intent = Intent(this, HomeLocationActivity::class.java)
        startActivity(intent)
    }

    private fun processSmsPermissions() {
        val permissions = ArrayList<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_CONTACTS)
            addReadContactsPermissionListeners()
        }
        permissions.addAll(smsSender.permissionsToRequest())
        if (!permissions.isEmpty()) {
            val arr = permissions.toTypedArray()
            ActivityCompat.requestPermissions(
                    this,
                    arr,
                    Permissions.SMS_AND_CONTACTS_PERMISSION_REQUEST_ID)
        }
    }

    private fun processLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            addLocationPermissionListeners()
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    Permissions.LOCATION_PERMISSION_REQUEST_ID)
            return
        }
        onLocationPermissionGranted()
    }

    private fun onHomeLocationChanged() {
        locationSource.onHomeLocationChanged()
        smsSender.resetZones()
    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissionsArray: Array<String>,
            grantResults: IntArray) =
            permissions.onRequestPermissionsResult(requestCode, permissionsArray, grantResults)

    private fun onSharedPreferenceChanged(key: String) {
        when (key) {
            HOME_LOCATION -> onHomeLocationChanged()
        }
    }

    private fun onReadContactsPermissionDenied() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_CONTACTS) && permissions
                .isRationaleNeeded(Manifest.permission.READ_CONTACTS)) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.perm_contacts_rationale)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                permissions.onRationaleShown(Manifest.permission.READ_CONTACTS)
                processSmsPermissions()
            }
            builder.create().show()
        }
    }

    private fun onLocationPermissionGranted() {
        locationSource.initGpsOnLocationGranted(applicationContext)
        startHomeLocationActivity()
    }

    companion object {

        const val PHONE_NUMBER_FRAGMENT_TAG = "number"

        /** If device is not enabled to send SMS, this entire category will be hidden.  */
        const val CATEGORY_SMS = "category_sms"

        /** Unless true, SMS will be disabled and SMS-related options grayed out.  */
        const val SMS_ENABLED = "pref_enable_sms"
        /** All text messages will be sent to this number.  */
        const val PHONE_NUMBER = "pref_phone_number"
        /** Beyond this distance from home, no messages will be sent.  */
        const val MAXIMUM_DISTANCE = "pref_max_distance"
        /** Location to measure distance from.  */
        const val HOME_LOCATION = "pref_home_location"
        /** If true, time will be sent with every SMS.  */
        const val INCLUDE_TIME = "pref_include_time"
        /** If true, speed will be sent with every SMS.  */
        const val INCLUDE_SPEED = "pref_include_speed"
        /** If true, location will be sent with every SMS.  */
        const val INCLUDE_LOCATION = "pref_include_location"
    }
}
