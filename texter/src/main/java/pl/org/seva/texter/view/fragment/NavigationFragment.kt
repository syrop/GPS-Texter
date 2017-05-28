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
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.app.Fragment
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

class NavigationFragment : Fragment() {

    @Inject
    lateinit var locationSource: LocationSource
    @Inject
    lateinit var permissionsUtils: PermissionsUtils

    private var distanceTextView: TextView? = null
    private var map: GoogleMap? = null
    private var animateCamera = true
    private var mapContainerId: Int = 0
    private var mapFragment: MapFragment? = null

    private val composite = CompositeDisposable()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_navigation, container, false)

        distanceTextView = view.findViewById(R.id.distance) as TextView
        show(locationSource.distance)

        savedInstanceState?.let {
            animateCamera = false
        }
        MapsInitializer.initialize(activity.applicationContext)
        mapContainerId = view.findViewById(R.id.map_container).id

        return view
    }

    override fun onResume() {
        super.onResume()

        composite.addAll(
                locationSource.distanceChangedListener()
                        .subscribe { activity.runOnUiThread { this.onDistanceChanged() } },
                locationSource.homeChangedListener()
                        .subscribe { onHomeChanged() })

        val fm = fragmentManager
        mapFragment = fm.findFragmentByTag(MAP_TAG) as MapFragment?
        if (mapFragment == null) {
            mapFragment = MapFragment()
            fm.beginTransaction().add(mapContainerId, mapFragment, MAP_TAG).commit()
        }

        mapFragment!!.getMapAsync{ this.onGoogleMapReady(it) }
    }

    private fun onGoogleMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map!!.isMyLocationEnabled = true
        } else {
            permissionsUtils.permissionGrantedListener()
                    .filter { it == Manifest.permission.ACCESS_FINE_LOCATION }
                    .subscribe { onLocationPermissionGranted() }
        }
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity) {
            injectDependencies(context)
        }
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override // http://stackoverflow.com/questions/32083053/android-fragment-onattach-deprecated#32088447
    fun onAttach(activity: Activity) {
        super.onAttach(activity)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            injectDependencies(activity)
        }
    }

    private fun injectDependencies(activity: Activity) {
        val graph = (activity.application as TexterApplication).graph
        graph.inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        composite.clear()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapFragment?.let {
            // see http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit#10261449
            fragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
            mapFragment = null
        }
        super.onSaveInstanceState(outState)
    }

    private fun updateHomeLocation(home: LatLng?) {
        if (map == null || home == null) {
            return
        }
        val marker = MarkerOptions().position(home).title(StatsFragment.homeString)

        // Changing marker icon
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))

        // adding marker
        map!!.clear()
        map!!.addMarker(marker)
    }

    private fun show(distance: Double) {
        @SuppressLint("DefaultLocale") var distanceStr = String.format("%.3f km", distance)
        if (distance == 0.0) {
            distanceStr = "0 km"
        }
        distanceTextView!!.text = distanceStr
    }

    private fun onDistanceChanged() {
        show(locationSource.distance)
    }

    private fun onHomeChanged() {
        updateHomeLocation(locationSource.homeLatLng)
    }

    @SuppressLint("MissingPermission")
    private fun onLocationPermissionGranted() {
        map?.isMyLocationEnabled = true
    }

    companion object {

        private val MAP_TAG = "map"

        fun newInstance(): NavigationFragment {
            return NavigationFragment()
        }
    }
}
