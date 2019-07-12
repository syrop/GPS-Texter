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

package pl.org.seva.texter.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


import kotlinx.android.synthetic.main.fragment_navigation.*

import pl.org.seva.texter.R
import pl.org.seva.texter.main.Permissions
import pl.org.seva.texter.main.permissions
import pl.org.seva.texter.movement.location
import pl.org.seva.texter.stats.StatsFragment

class NavigationFragment : Fragment() {

    private var map: GoogleMap? = null
    private var animateCamera = true
    private var mapFragment: SupportMapFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLocationSubscriptions()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_navigation, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showDistance(location.distance)

        savedInstanceState?.let {
            animateCamera = false
        }
        MapsInitializer.initialize(requireActivity().applicationContext)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { it.onReady() }
    }

    private fun createLocationSubscriptions() {
            location.addDistanceChangedListenerUi(lifecycle) { onDistanceChanged() }
            location.addHomeChangedListener(lifecycle) { onHomeChanged() }
    }

    private fun GoogleMap.onReady() {
        map = this
        processLocationPermission()
        val homeLatLng = location.homeLatLng
        updateHomeLocation(homeLatLng)
        val cameraPosition = CameraPosition.Builder()
                .target(homeLatLng).zoom(12f).build()
        if (animateCamera) {
            checkNotNull(map).animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        } else {
            checkNotNull(map).moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
        animateCamera = false
    }

    @SuppressLint("CheckResult")
    private fun processLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            onLocationPermissionGranted()
            map?.isMyLocationEnabled = true
        } else {
            permissions.permissionGrantedListener()
                    .filter { it.first == Permissions.LOCATION_PERMISSION_REQUEST_ID }
                    .filter { it.second == Manifest.permission.ACCESS_FINE_LOCATION }
                    .subscribe { onLocationPermissionGranted() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapFragment?.let {
            checkNotNull(fragmentManager).beginTransaction().remove(it).commitAllowingStateLoss()
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
        checkNotNull(map).clear()
        checkNotNull(map).addMarker(marker)
    }

    private fun showDistance(distance: Double) {
        @SuppressLint("DefaultLocale") var distanceStr = String.format("%.3f km", distance)
        if (distance == 0.0) {
            distanceStr = "0 km"
        }
        distance_view.text = distanceStr
    }

    private fun onDistanceChanged() = showDistance(location.distance)

    private fun onHomeChanged() = updateHomeLocation(location.homeLatLng)

    @SuppressLint("MissingPermission")
    private fun onLocationPermissionGranted() {
        map?.isMyLocationEnabled = true
    }

    companion object {
        fun newInstance() = NavigationFragment()
    }
}
