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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.maps.*

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import io.reactivex.disposables.Disposables
import pl.org.seva.texter.R
import pl.org.seva.texter.main.Constants
import pl.org.seva.texter.movement.location
import androidx.core.content.edit

@Suppress("DEPRECATION")
class HomeLocationActivity : AppCompatActivity() {

    private var locationChangedSubscription = Disposables.empty()

    private var lat: Double = 0.0
    private var lon: Double = 0.0
    private var toastShown: Boolean = false
    private var zoom: Float = 0.0f
    private var isCurrentLocationAvailable: Boolean = false

    private var map: GoogleMap? = null
    private var animateCamera = true
    private var mapContainerId: Int = 0
    private var mapFragment: MapFragment? = null

    private val currentLocationButton by lazy { findViewById<Button>(R.id.current_location_button)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val value = persistedString
        lat = HomeLocationPreference.parseLatitude(value)
        lon = HomeLocationPreference.parseLongitude(value)

        setContentView(R.layout.activity_home_location)

        mapContainerId = R.id.map_container
        MapsInitializer.initialize(this)

        val locationPermitted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val locationAvailable = locationPermitted && location.isLocationAvailable
        currentLocationButton.isEnabled = locationAvailable
        currentLocationButton.setOnClickListener { onUseCurrentLocationClicked() }
        if (!toastShown) {
            Toast.makeText(
                    this,
                    R.string.long_press,
                    Toast.LENGTH_SHORT).show()
            toastShown = true
        }
        location.addLocationChangedListener(lifecycle) { onLocationChanged() }

        if (!locationPermitted) {
            currentLocationButton.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        val fm = fragmentManager
        mapFragment = fm.findFragmentByTag(MAP_TAG_HOME_LOCATION) as MapFragment?
        mapFragment?:let {
            mapFragment = MapFragment()
            fm.beginTransaction().add(mapContainerId, mapFragment, MAP_TAG_HOME_LOCATION).commit()
        }
        checkNotNull(mapFragment).getMapAsync { it.onReady() }
    }

    private fun GoogleMap.onReady() {
        fun onMapLongClick(latLng: LatLng) {
            lat = latLng.latitude
            lon = latLng.longitude
            updateMarker()
            if (isCurrentLocationAvailable) {
                currentLocationButton.isEnabled = true
            }
        }

        fun onCameraIdle() {
            zoom = checkNotNull(map).cameraPosition.zoom
        }

        map = this
        if (ContextCompat.checkSelfPermission(
                this@HomeLocationActivity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkNotNull(map).isMyLocationEnabled = true
        }
        zoom = PreferenceManager.getDefaultSharedPreferences(this@HomeLocationActivity)
                .getFloat(ZOOM_PROPERTY_NAME, ZOOM_DEFAULT_VALUE)

        updateMarker()
        val cameraPosition = CameraPosition.Builder()
                .target(LatLng(lat, lon)).zoom(zoom).build()
        if (animateCamera) {
            checkNotNull(map).animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        } else {
            checkNotNull(map).moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
        animateCamera = false

        checkNotNull(map).setOnMapLongClickListener { onMapLongClick(it) }
        checkNotNull(map).setOnCameraIdleListener { onCameraIdle() }
    }

    override fun onBackPressed() {
        locationChangedSubscription.dispose()
        toastShown = false

        persistHomeLocation(latLngToString())
        PreferenceManager.getDefaultSharedPreferences(this).edit {
                putFloat(ZOOM_PROPERTY_NAME, zoom)
        }
        location.onHomeLocationChanged()

        mapFragment?.let {
            fragmentManager.beginTransaction().remove(it).commit()
        }
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapFragment?.let {
            fragmentManager.beginTransaction().remove(it).commit()
            mapFragment = null
        }
        super.onSaveInstanceState(outState)
    }

    private fun latLngToString(): String = HomeLocationPreference.toString(lat, lon)

    private fun persistHomeLocation(`val`: String) =
            PreferenceManager.getDefaultSharedPreferences(this).edit {
                putString(SettingsActivity.HOME_LOCATION, `val`)
            }

    private val persistedString: String
        get() = checkNotNull(PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsActivity.HOME_LOCATION, Constants.DEFAULT_HOME_LOCATION))

    private fun updateMarker() {
        checkNotNull(map).clear()
        val marker = MarkerOptions().position(
                LatLng(lat, lon)).title(getString(R.string.home))
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
        checkNotNull(map).addMarker(marker)
    }

    private fun onLocationChanged() {
        if (map == null) {
            return
        }
        isCurrentLocationAvailable = true
        currentLocationButton.isEnabled = true
    }

    private fun onUseCurrentLocationClicked() {
        currentLocationButton.isEnabled = false
        val loc = location.latLng
        loc?.let {
            lat = it.latitude
            lon = it.longitude
            updateMarker()
            val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(lat, lon)).zoom(zoom).build()
            checkNotNull(map).animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    companion object {
        private const val MAP_TAG_HOME_LOCATION = "map_home_location"
        private const val ZOOM_PROPERTY_NAME = "map_preference_gui_zoom"
        private const val ZOOM_DEFAULT_VALUE = 7.5f
    }
}
