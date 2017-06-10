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

package pl.org.seva.texter.view.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.arch.lifecycle.LifecycleFragment
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.gms.maps.*

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import javax.inject.Inject

import io.reactivex.disposables.CompositeDisposable
import pl.org.seva.texter.R
import pl.org.seva.texter.TexterApplication
import pl.org.seva.texter.presenter.source.LocationSource
import pl.org.seva.texter.presenter.utils.PermissionsUtils

class NavigationFragment : LifecycleFragment() {

    @Inject
    lateinit var locationSource: LocationSource
    @Inject
    lateinit var permissionsUtils: PermissionsUtils

    private var distanceTextView: TextView? = null
    private var map: GoogleMap? = null
    private var animateCamera = true
    private var mapContainerId: Int = 0
    private var mapFragment: SupportMapFragment? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_navigation, container, false)
        (activity.application as TexterApplication).graph.inject(this)

        distanceTextView = view.findViewById<TextView>(R.id.distance)
        showDistance(locationSource.distance)

        savedInstanceState?.let {
            animateCamera = false
        }
        MapsInitializer.initialize(activity.applicationContext)
        mapContainerId = view.findViewById<View>(R.id.map_container_navigation).id

        return view
    }

    override fun onResume() {
        super.onResume()
        addLocationSubscriptions()
        prepareMaps()
    }

    private fun prepareMaps() {
        val fm = fragmentManager
        mapFragment = fm.findFragmentByTag(MAP_TAG_NAVIGATION) as SupportMapFragment?
        mapFragment?: let {
            mapFragment = SupportMapFragment()
            fm.beginTransaction().add(mapContainerId, mapFragment, MAP_TAG_NAVIGATION).commit()
        }

        mapFragment!!.getMapAsync{ onMapReady(it) }
    }

    private fun addLocationSubscriptions() {
            locationSource.addDistanceChangedListenerUi(lifecycle) { onDistanceChanged() }
            locationSource.addHomeChangedListener(lifecycle) { onHomeChanged() }
    }

    @SuppressLint("MissingPermission")
    private fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        processLocationPermission()
        val homeLatLng = locationSource.homeLatLng
        updateHomeLocation(homeLatLng)
        val cameraPosition = CameraPosition.Builder()
                .target(homeLatLng).zoom(12f).build()
        if (animateCamera) {
            map!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        } else {
            map!!.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
        animateCamera = false
    }

    private fun processLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            onLocationPermissionGranted()
            map?.isMyLocationEnabled = true
        } else {
            permissionsUtils.permissionGrantedListener()
                    .filter { it.first == PermissionsUtils.LOCATION_PERMISSION_REQUEST_ID }
                    .filter { it.second == Manifest.permission.ACCESS_FINE_LOCATION }
                    .subscribe { onLocationPermissionGranted() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapFragment?.let {
            fragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
            mapFragment = null
        }
        super.onSaveInstanceState(outState)
    }

    private fun updateHomeLocation(homeLocation: LatLng?) {
        if (map == null || homeLocation == null) {
            return
        }
        val marker = MarkerOptions().position(homeLocation).title(StatsFragment.homeString)

        // Changing marker icon
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))

        // adding marker
        map!!.clear()
        map!!.addMarker(marker)
    }

    private fun showDistance(distance: Double) {
        @SuppressLint("DefaultLocale") var distanceStr = String.format("%.3f km", distance)
        if (distance == 0.0) {
            distanceStr = "0 km"
        }
        distanceTextView!!.text = distanceStr
    }

    private fun onDistanceChanged() {
        showDistance(locationSource.distance)
    }

    private fun onHomeChanged() {
        updateHomeLocation(locationSource.homeLatLng)
    }

    @SuppressLint("MissingPermission")
    private fun onLocationPermissionGranted() {
        map?.isMyLocationEnabled = true
    }

    companion object {

        private val MAP_TAG_NAVIGATION = "map_navigation"

        fun newInstance(): NavigationFragment {
            return NavigationFragment()
        }
    }
}
