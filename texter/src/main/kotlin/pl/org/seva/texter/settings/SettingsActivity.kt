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
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.toolbar.*
import pl.org.seva.texter.main.*
import pl.org.seva.texter.movement.location

import java.util.ArrayList

class SettingsActivity : AppCompatActivity() {

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener {
        _, key -> onSharedPreferenceChanged(key)
    }

    private var permissionsCompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
        }

        val fragments = ArrayList<Fragment>()
        val settingsFragment = SettingsFragment.newInstance()
        settingsFragment.homeLocationClickedListener = { onHomeLocationClicked() }
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

    override fun onDestroy() {
        super.onDestroy()
        permissionsCompositeDisposable.dispose()
    }

    private fun addLocationPermissionListeners() {
            permissions
                    .permissionGrantedListener()
                    .filter {it.first == Permissions.LOCATION_PERMISSION_REQUEST_ID }
                    .filter { it.second == Manifest.permission.ACCESS_FINE_LOCATION }
                    .subscribe { onLocationPermissionGranted() } addTo permissionsCompositeDisposable
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

    private fun startHomeLocationActivity() {
        val intent = Intent(this, HomeLocationActivity::class.java)
        startActivity(intent)
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
        location.onHomeLocationChanged()
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

    private fun onLocationPermissionGranted() {
        location.initGpsOnLocationGranted(applicationContext)
        startHomeLocationActivity()
    }

    companion object {
        /** Location to measure distance from.  */
        const val HOME_LOCATION = "pref_home_location"
        /** If true, time will be sent with every SMS.  */
    }
}
