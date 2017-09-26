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

package pl.org.seva.texter.ui.activity

import pl.org.seva.texter.R
import pl.org.seva.texter.movement.LocationSource
import pl.org.seva.texter.application.Permissions
import pl.org.seva.texter.sms.SmsSender
import pl.org.seva.texter.ui.adapter.TitledPagerAdapter
import pl.org.seva.texter.ui.fragment.SettingsFragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.content.Intent
import android.content.SharedPreferences
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.WindowManager
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.toolbar.*
import pl.org.seva.texter.ui.fragment.PhoneNumberFragment

import java.util.ArrayList

class SettingsActivity : AppCompatActivity(), KodeinGlobalAware {

    private val permissionsHelper: Permissions = instance()
    private val locationSource: LocationSource = instance()
    private val smsSender: SmsSender = instance()

    private val preferenceListener : (a : SharedPreferences, b : String) -> Unit =
            { _, key -> this.onSharedPreferenceChanged(key) }

    private var permissionsCompositeSubscription = CompositeDisposable()

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
        permissionsCompositeSubscription.add(permissionsHelper
                .permissionDeniedListener()
                .filter { it.first == Permissions.SMS_AND_CONTACTS_PERMISSION_REQUEST_ID }
                .filter { it.second == Manifest.permission.READ_CONTACTS }
                .subscribe { onReadContactsPermissionDenied() })
    }

    private fun addLocationPermissionListeners() {
        permissionsCompositeSubscription.addAll(
                permissionsHelper
                        .permissionGrantedListener()
                        .filter {it.first == Permissions.LOCATION_PERMISSION_REQUEST_ID }
                        .filter { it.second == Manifest.permission.ACCESS_FINE_LOCATION }
                        .subscribe { onLocationPermissionGranted() })
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

    private fun isSmsEnabled(): Boolean =
            PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SMS_ENABLED, false)

    private fun startHomeLocationActivity() {
        val intent = Intent(this, HomeLocationActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        permissionsCompositeSubscription.dispose()
        super.onDestroy()
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
            permissions: Array<String>,
            grantResults: IntArray) =
            permissionsHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)

    private fun onSharedPreferenceChanged(key: String) {
        when (key) {
            SettingsActivity.HOME_LOCATION -> onHomeLocationChanged()
        }
    }

    private fun onReadContactsPermissionDenied() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_CONTACTS) && permissionsHelper
                .isRationaleNeeded(Manifest.permission.READ_CONTACTS)) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.perm_contacts_rationale)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                permissionsHelper.onRationaleShown(Manifest.permission.READ_CONTACTS)
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

        val PHONE_NUMBER_FRAGMENT_TAG = "number"

        /** If device is not enabled to send SMS, this entire category will be hidden.  */
        val CATEGORY_SMS = "category_sms"

        /** Unless true, SMS will be disabled and SMS-related options grayed out.  */
        val SMS_ENABLED = "pref_enable_sms"
        /** All text messages will be sent to this number.  */
        val PHONE_NUMBER = "pref_phone_number"
        /** Beyond this distance from home, no messages will be sent.  */
        val MAXIMUM_DISTANCE = "pref_max_distance"
        /** Location to measure distance from.  */
        val HOME_LOCATION = "pref_home_location"
        /** If true, time will be sent with every SMS.  */
        val INCLUDE_TIME = "pref_include_time"
        /** If true, speed will be sent with every SMS.  */
        val INCLUDE_SPEED = "pref_include_speed"
        /** If true, location will be sent with every SMS.  */
        val INCLUDE_LOCATION = "pref_include_location"
    }
}
