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

package pl.org.seva.texter.stats

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import java.util.Calendar

import pl.org.seva.texter.R
import pl.org.seva.texter.main.Permissions
import pl.org.seva.texter.data.SmsLocation
import pl.org.seva.texter.main.permissions
import pl.org.seva.texter.movement.activityRecognition
import pl.org.seva.texter.movement.location
import pl.org.seva.texter.sms.smsSender

class StatsFragment : Fragment() {

    private var distance: Double = 0.0
    private var speed: Double = 0.0
    private var isStationary: Boolean = false

    private var mapFragment: SupportMapFragment? = null
    private var map: GoogleMap? = null
    private var locationPermissionGranted = false

    private var zoom = 0.0f

    private val sendNowButton by lazy { requireActivity().findViewById<Button>(R.id.send_now_button) }
    private val distanceValue by lazy { requireActivity().findViewById<TextView>(R.id.distance_value) }
    private val stationary by lazy { requireActivity().findViewById<View>(R.id.stationary) }
    private val updateIntervalValue by lazy { requireActivity().findViewById<TextView>(R.id.update_interval_value) }
    private val speedValue by lazy { requireActivity().findViewById<TextView>(R.id.speed_value) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createSubscriptions()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        distance = location.distance
        speed = location.speed
        zoom = PreferenceManager.getDefaultSharedPreferences(activity)
                .getFloat(ZOOM_PROPERTY_NAME, DEFAULT_ZOOM)

        homeString = getString(R.string.home)
        hourString = requireActivity().getString(R.string.hour)
        speedUnitStr = getString(R.string.speed_unit)
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sendNowButton.setOnClickListener { onSendNowClicked() }
        sendNowButton.isEnabled = smsSender.isTextingEnabled &&
                distance != 0.0 &&
                distance != smsSender.lastSentDistance

        showStats()
        MapsInitializer.initialize(requireActivity().applicationContext)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { it.onReady() }
    }

    @SuppressLint("MissingPermission")
    private fun GoogleMap.onReady() {
        map = this
        processLocationPermission()
        val homeLatLng = location.homeLatLng
        updateHomeLocation(homeLatLng)
        val cameraPosition = CameraPosition.Builder()
                .target(homeLatLng).zoom(zoom).build()
        checkNotNull(map).moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        if (locationPermissionGranted) {
            checkNotNull(map).isMyLocationEnabled = true
        }
        checkNotNull(map).setOnCameraIdleListener { onCameraIdle() }
    }

    private fun onCameraIdle() {
        zoom = checkNotNull(map).cameraPosition.zoom
        PreferenceManager.getDefaultSharedPreferences(activity).edit().putFloat(ZOOM_PROPERTY_NAME, zoom).apply()
    }

    private fun updateHomeLocation(homeLocation: LatLng?) {
        if (map == null || homeLocation == null) {
            return
        }
        val marker = MarkerOptions().position(homeLocation).title(homeString)

        // Changing marker icon
        marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))

        // adding marker
        checkNotNull(map).clear()
        checkNotNull(map).addMarker(marker)
    }

    @SuppressLint("CheckResult")
    private fun processLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
            map?.isMyLocationEnabled = true
            onLocationPermissionGranted()
        } else {
            permissions.permissionGrantedListener()
                    .filter { it.first == Permissions.LOCATION_PERMISSION_REQUEST_ID }
                    .filter { it.second == Manifest.permission.ACCESS_FINE_LOCATION }
                    .subscribe { onLocationPermissionGranted() }
        }
    }

    @SuppressLint("MissingPermission")
    private fun onLocationPermissionGranted() {
        locationPermissionGranted = true
        map?.isMyLocationEnabled = true
    }

    private fun createSubscriptions() {
        location.addDistanceChangedListenerUi(lifecycle) { onDistanceChanged() }
        location.addHomeChangedListener(lifecycle) { onHomeChanged() }
        timer.addTimerListenerUi(lifecycle) { showStats() }
        smsSender.addSmsSendingListenerUi(lifecycle) { onSendingSms() }
        activityRecognition.addActivityRecognitionListener(
                lifecycle,
                stationary = ::onDeviceStationary,
                moving = ::onDeviceMoving)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        deleteMapFragment()
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        deleteMapFragment()
        super.onStop()
    }

    private fun deleteMapFragment() {
        mapFragment?.let {
            checkNotNull(fragmentManager).beginTransaction().remove(it).commitAllowingStateLoss()
            mapFragment = null
        }
    }

    private fun onDeviceStationary() {
        isStationary = true
    }

    private fun onDeviceMoving() {
        isStationary = false
    }

    private fun showStats() {
        distanceValue.text = if (distance == 0.0) {
            "0 km"
        } else {
            formattedDistanceStr
        }

        stationary.visibility = if (isStationary) View.VISIBLE else View.INVISIBLE
        updateIntervalValue.text = formattedTimeStr
        if (speed == 0.0 || distance == 0.0) {
            speedValue.visibility = View.INVISIBLE
        } else {
            speedValue.visibility = View.VISIBLE
            speedValue.text = formattedSpeedStr
        }
    }

    private val formattedDistanceStr : String
        get() = String.format("%.3f km", distance)

    private val formattedSpeedStr: String
        get() {
            @SuppressLint("DefaultLocale")
            var result = String.format("%.1f", if (isStationary) 0.0 else speed) + " " + speedUnitStr
            if (result.contains(".0")) {
                result = result.replace(".0", "")
            } else if (result.contains(",0")) {
                result = result.replace(",0", "")
            }
            return result
        }

    private val formattedTimeStr: String
        get() {
            var seconds = (System.currentTimeMillis() - timer.resetTime).toInt() / 1000
            var minutes = seconds / 60
            seconds %= 60
            val hours = minutes / 60
            minutes %= 60
            val timeStrBuilder = StringBuilder()
            if (hours > 0) {
                timeStrBuilder.append(hours).append(" ").append(hourString)
                if (minutes > 0 || seconds > 0) {
                    timeStrBuilder.append(" ")
                }
            }
            if (minutes > 0) {
                timeStrBuilder.append(minutes).append(" m")
                if (seconds > 0) {
                    timeStrBuilder.append(" ")
                }
            }
            if (seconds > 0) {
                timeStrBuilder.append(seconds).append(" s")
            } else if (minutes == 0 && hours == 0) {
                timeStrBuilder.setLength(0)
                timeStrBuilder.append("0 s")
            }
            return timeStrBuilder.toString()
        }

    private fun onDistanceChanged() {
        if (distance != smsSender.lastSentDistance) {
            sendNowButton.isEnabled = smsSender.isTextingEnabled
        }

        val threeHoursPassed = System.currentTimeMillis() - timer.resetTime > 3 * 3600 * 1000
        if (threeHoursPassed) {
            this.speed = 0.0
            this.distance = 0.0
        } else {
            this.distance = location.distance
            this.speed = location.speed
        }
        showStats()
    }

    @SuppressLint("WrongConstant")
    private fun onSendNowClicked() {
        sendNowButton.isEnabled = false
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timer.resetTime
        val minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val location = SmsLocation()
        location.distance = distance
        location.direction = 0
        location.setTime(minutes)
        location.speed = speed
        smsSender.send(location)
    }

    private fun onSendingSms() {
        sendNowButton.isEnabled = false
    }

    private fun onHomeChanged() {
        distance = location.distance
        showStats()
    }

    companion object {
        private const val ZOOM_PROPERTY_NAME = "stats_map_zoom"
        private const val DEFAULT_ZOOM = 7.5f

        var homeString: String? = null
        private lateinit var hourString: String
        private lateinit var speedUnitStr: String

        fun newInstance() = StatsFragment()
    }
}
