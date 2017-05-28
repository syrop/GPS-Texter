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
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.WindowManager

import java.util.ArrayList

import javax.inject.Inject

class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var permissionsUtils: PermissionsUtils
    @Inject
    lateinit var locationSource: LocationSource
    @Inject
    lateinit var smsSender: SmsSender

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
            if (!processPermissions()) {
                setReadContactsPermissionListeners()
            }
        }

        val fragments = ArrayList<Fragment>()
        fragments.add(SettingsFragment.newInstance())

        val adapter = TitledPagerAdapter(fragmentManager, null).setItems(fragments)
        val pager = findViewById(R.id.pager) as ViewPager
        pager.adapter = adapter
    }

    private fun setReadContactsPermissionListeners() {
        permissionsUtils
                .permissionDeniedListener()
                .filter { it == Manifest.permission.READ_CONTACTS }
                .subscribe { onShowContactsPermissionDenied() }
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

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener {
                    sharedPreferences, key -> this.onSharedPreferenceChanged(sharedPreferences, key) }
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener {
                    sharedPreferences, key -> this.onSharedPreferenceChanged(sharedPreferences, key) }
    }

    /**
     * All actions that require permissions must be placed here. The methods performs them or
     * asks for permissions if they haven't been granted already.

     * @return false if particularly READ_CONTACTS was't granted previously
     */
    private fun processPermissions(): Boolean {
        val permissions = ArrayList<String>()
        var result = true

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_CONTACTS)
            result = false
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS)
        }
        if (!permissions.isEmpty()) {
            val arr = arrayOfNulls<String>(permissions.size)
            permissions.toTypedArray()
            ActivityCompat.requestPermissions(
                    this,
                    arr,
                    PermissionsUtils.PERMISSION_READ_CONTACTS_REQUEST)
        }
        return result
    }

    private fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            SMS_ENABLED  // off by default
            -> if (sharedPreferences.getBoolean(SMS_ENABLED, false)) {
                if (!processPermissions()) {
                    setReadContactsPermissionListeners()
                }
            }
            HOME_LOCATION -> {
                locationSource.onHomeLocationChanged()
                smsSender.resetZones()
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        if (requestCode == PermissionsUtils.PERMISSION_READ_CONTACTS_REQUEST) {
            permissionsUtils.onRequestPermissionsResult(permissions, grantResults)
        }
    }

    private fun onShowContactsPermissionDenied() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_CONTACTS) && permissionsUtils
                .isRationaleNeeded(Manifest.permission.READ_CONTACTS)) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.perm_contacts_rationale)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                permissionsUtils.onRationaleShown(Manifest.permission.READ_CONTACTS)
                processPermissions()
            }
            builder.create().show()
        }
    }

    companion object {

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
