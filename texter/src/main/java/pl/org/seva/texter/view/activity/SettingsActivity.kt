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

package pl.org.seva.texter.view.activity

import pl.org.seva.texter.R
import pl.org.seva.texter.presenter.source.LocationSource
import pl.org.seva.texter.presenter.utils.PermissionsUtils
import pl.org.seva.texter.presenter.utils.SmsSender
import pl.org.seva.texter.view.adapter.TitledPagerAdapter
import pl.org.seva.texter.TexterApplication
import pl.org.seva.texter.view.fragment.SettingsFragment

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
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.WindowManager
import io.reactivex.disposables.CompositeDisposable
import pl.org.seva.texter.view.fragment.PhoneNumberFragment

import java.util.ArrayList

import javax.inject.Inject

class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var permissionsUtils: PermissionsUtils
    @Inject
    lateinit var locationSource: LocationSource
    @Inject
    lateinit var smsSender: SmsSender

    val preferenceListener : (a : SharedPreferences, b : String) -> Unit =
            { _, key -> this.onSharedPreferenceChanged(key) }

    var permissionsCompositeSubscription = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val graph = (application as TexterApplication).graph
        graph.inject(this)

        setContentView(R.layout.activity_settings)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        // Attaching the layout to the toolbar object
        val toolbar = findViewById(R.id.toolbar) as Toolbar

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
        val pager = findViewById(R.id.pager) as ViewPager
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
        permissionsCompositeSubscription.add(permissionsUtils
                .permissionDeniedListener()
                .filter { it.first == PermissionsUtils.SMS_AND_CONTACTS_PERMISSION_REQUEST_ID }
                .filter { it.second == Manifest.permission.READ_CONTACTS }
                .subscribe { onReadContactsPermissionDenied() })
    }

    private fun addLocationPermissionListeners() {
        permissionsCompositeSubscription.addAll(
                permissionsUtils
                    .permissionDeniedListener()
                    .filter {it.first == PermissionsUtils.LOCATION_PERMISSION_REQUEST_ID }
                    .filter { it.second == Manifest.permission.ACCESS_FINE_LOCATION }
                    .subscribe { startHomeLocationActivity() },
                permissionsUtils
                        .permissionGrantedListener()
                        .filter {it.first == PermissionsUtils.LOCATION_PERMISSION_REQUEST_ID }
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

    fun onHomeLocationClicked() {
        if (processLocationPermissions()) {
            startHomeLocationActivity()
        }
    }

    fun onSmsEnabledChanged() {
        if (isSmsEnabled()) {
            processSmsPermissions()
        }
    }

    fun onNumberClicked() {
        PhoneNumberFragment().show(supportFragmentManager, PHONE_NUMBER_FRAGMENT_TAG)
    }

    private fun isSmsEnabled(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SMS_ENABLED, false)
    }

    fun startHomeLocationActivity() {
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
                    PermissionsUtils.SMS_AND_CONTACTS_PERMISSION_REQUEST_ID)
        }
    }

    private fun processLocationPermissions(): Boolean {
        val permissions = ArrayList<String>()
        var locationPermissionGranted = true

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            locationPermissionGranted = false
        }
        if (!locationPermissionGranted) {
            addLocationPermissionListeners()
        }
        if (!permissions.isEmpty()) {
            val arr = permissions.toTypedArray()
            ActivityCompat.requestPermissions(
                    this,
                    arr,
                    PermissionsUtils.LOCATION_PERMISSION_REQUEST_ID)
        }
        return locationPermissionGranted
    }

    fun onHomeLocationChanged() {
        locationSource.onHomeLocationChanged()
        smsSender.resetZones()
    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        permissionsUtils.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun onSharedPreferenceChanged(key: String) {
        when (key) {
            SettingsActivity.HOME_LOCATION -> onHomeLocationChanged()
        }
    }

    private fun onReadContactsPermissionDenied() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_CONTACTS) && permissionsUtils
                .isRationaleNeeded(Manifest.permission.READ_CONTACTS)) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.perm_contacts_rationale)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                permissionsUtils.onRationaleShown(Manifest.permission.READ_CONTACTS)
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
        val SMS_NUMBER = "pref_phone_number"
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
