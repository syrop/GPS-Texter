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

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.maps.*

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import javax.inject.Inject

import io.reactivex.disposables.Disposables
import pl.org.seva.texter.R
import pl.org.seva.texter.TexterApplication
import pl.org.seva.texter.presenter.source.LocationSource
import pl.org.seva.texter.presenter.utils.PermissionsUtils
import pl.org.seva.texter.presenter.utils.Constants

class HomeLocationActivity : AppCompatActivity() {

    @Inject
    lateinit var locationSource: LocationSource
    @Inject
    lateinit var permissionsUtils: PermissionsUtils

    private var locationChangedSubscription = Disposables.empty()

    private var lat: Double = 0.toDouble()
    private var lon: Double = 0.toDouble()
    private var toastShown: Boolean = false
    private var zoom: Float = 0.toFloat()
    private var isCurrentLocationAvailable: Boolean = false

    private var map: GoogleMap? = null
    private var useCurrentButton: Button? = null
    private var animateCamera = true
    private var mapContainerId: Int = 0
    private var mapFragment: MapFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            val myState = it.get(SAVED_STATE) as HomeLocationActivity.SavedState
            lat = myState.lat
            lon = myState.lon
            toastShown = myState.toastShown
            zoom = myState.zoom
            animateCamera = false
        }

        val graph = (application as TexterApplication).graph
        graph.inject(this)

        val value = persistedString
        lat = parseLatitude(value)
        lon = parseLongitude(value)

        setContentView(R.layout.activity_home_location)

        mapContainerId = findViewById<View>(R.id.map_container).id
        MapsInitializer.initialize(this)
        useCurrentButton = findViewById<Button>(R.id.current_location_button)

        val locationPermitted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val locationAvailable = locationPermitted && locationSource.isLocationAvailable
        useCurrentButton!!.isEnabled = locationAvailable
        if (!toastShown) {
            Toast.makeText(
                    this,
                    R.string.long_press,
                    Toast.LENGTH_SHORT).show()
            toastShown = true
        }
        locationChangedSubscription = locationSource.locationChangedListener().subscribe {
            onLocationChanged() }

        if (!locationPermitted) {
            useCurrentButton!!.isEnabled = false
            setLocationPermissionListeners()
        }
    }

    private fun setLocationPermissionListeners() {
        permissionsUtils
                .permissionGrantedListener()
                .filter { it == Manifest.permission.ACCESS_FINE_LOCATION }
                .subscribe { onLocationPermissionGranted() }
    }

    override fun onResume() {
        super.onResume()
        val fm = fragmentManager
        mapFragment = fm.findFragmentByTag("map") as MapFragment?
        if (mapFragment == null) {
            mapFragment = MapFragment()
            fm.beginTransaction().add(mapContainerId, mapFragment, "map").commit()
        }
        mapFragment!!.getMapAsync({ this.onMapReady(it) })
    }

    private fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map!!.isMyLocationEnabled = true
        } else {
            setLocationPermissionListeners()
        }
        zoom = PreferenceManager.getDefaultSharedPreferences(this).getFloat(ZOOM_PROPERTY_NAME, ZOOM_DEFAULT_VALUE)

        updateMarker()
        val cameraPosition = CameraPosition.Builder()
                .target(LatLng(lat, lon)).zoom(zoom).build()
        if (animateCamera) {
            map!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        } else {
            map!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
        animateCamera = false

        map!!.setOnMapLongClickListener({ this.onMapLongClick(it) })
        map!!.setOnCameraIdleListener({ this.onCameraIdle() })
    }

    override fun onBackPressed() {
        locationChangedSubscription.dispose()
        toastShown = false

        persistString(toString())
        PreferenceManager.getDefaultSharedPreferences(this).edit().putFloat(ZOOM_PROPERTY_NAME, zoom).apply()
        locationSource.onHomeLocationChanged()

        mapFragment?.let {
            // Without enclosing in the if, throws:
            // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
            fragmentManager.beginTransaction().remove(it).commit()
        }
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val myState = HomeLocationActivity.SavedState()
        myState.lat = lat
        myState.lon = lon
        myState.toastShown = toastShown
        myState.zoom = zoom

        mapFragment?.let {
            fragmentManager.beginTransaction().remove(it).commit()
            mapFragment = null
        }
        super.onSaveInstanceState(outState)
    }

    override fun toString(): String {
        return toString(lat, lon)
    }

    private fun persistString(`val`: String) {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putString(SettingsActivity.HOME_LOCATION, `val`).apply()
    }

    private val persistedString: String
        get() = PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.HOME_LOCATION, Constants.DEFAULT_HOME_LOCATION)

    private fun updateMarker() {
        map!!.clear()
        val marker = MarkerOptions().position(
                LatLng(lat, lon)).title(getString(R.string.home))
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
        map!!.addMarker(marker)
    }

    private fun onLocationChanged() {
        if (map == null || useCurrentButton == null) {
            return
        }
        isCurrentLocationAvailable = true
        useCurrentButton!!.isEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun onLocationPermissionGranted() {
        map?.let {
            it.isMyLocationEnabled = true
            useCurrentButton!!.isEnabled = false
        }
    }

    private fun onMapLongClick(latLng: LatLng) {
        lat = latLng.latitude
        lon = latLng.longitude
        updateMarker()
        if (isCurrentLocationAvailable) {
            useCurrentButton!!.isEnabled = true
        }
    }

    private fun onCameraIdle() {
        zoom = map!!.cameraPosition.zoom
    }

    fun onUseCurrentLocationButtonClicked() {
        useCurrentButton!!.isEnabled = false
        val loc = locationSource.latLng
        loc?.let {
            lat = it.latitude
            lon = it.longitude
            updateMarker()
            val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(lat, lon)).zoom(zoom).build()
            map!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }


    private class SavedState internal constructor() : Parcelable {
        var lat: Double = 0.0
        var lon: Double = 0.0
        var toastShown: Boolean = false
        var zoom: Float = 0.0f

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeDouble(lat)
            dest.writeDouble(lon)
            dest.writeInt(if (toastShown) 1 else 0)
            dest.writeFloat(zoom)
        }
    }

    companion object {

        private val SAVED_STATE = "saved_state"
        private val ZOOM_PROPERTY_NAME = "map_preference_gui_zoom"
        private val ZOOM_DEFAULT_VALUE = 7.5f

        private fun toString(lat: Double, lon: Double): String {
            return "geo:" +
                    lat.toInt() + "." +
                    java.lang.Double.toString(lat - lat.toInt()).substring(2, 8) + "," +
                    lon.toInt() + "." +
                    java.lang.Double.toString(lon - lon.toInt()).substring(2, 8)
        }

        private fun parseLatitude(uri: String): Double {
            val str = uri.substring(uri.indexOf(":") + 1, uri.indexOf(","))
            return java.lang.Double.valueOf(str)!!
        }

        private fun parseLongitude(uri: String): Double {
            val str = uri.substring(uri.indexOf(",") + 1)
            return java.lang.Double.valueOf(str)!!
        }
    }
}
