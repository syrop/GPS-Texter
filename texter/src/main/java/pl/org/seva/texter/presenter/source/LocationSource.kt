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

package pl.org.seva.texter.presenter.source

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.model.LatLng

import javax.inject.Inject
import javax.inject.Singleton

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import pl.org.seva.texter.presenter.listener.ProviderListener
import pl.org.seva.texter.presenter.utils.Timer
import pl.org.seva.texter.view.activity.SettingsActivity
import pl.org.seva.texter.view.preference.HomeLocationPreference
import pl.org.seva.texter.presenter.utils.DistanceCalculator
import pl.org.seva.texter.presenter.utils.Constants

@Singleton
open class LocationSource @Inject
constructor() : GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    @Inject
    lateinit var timer: Timer

    private var preferences: SharedPreferences? = null

    private var googleApiClient: GoogleApiClient? = null
    private var locationRequest: LocationRequest? = null

    private val distanceSubject = PublishSubject.create<Any>()
    private val homeChangedSubject = PublishSubject.create<Any>()
    private val providerEnabledSubject = PublishSubject.create<Any>()
    private val providerDisabledSubject = PublishSubject.create<Any>()
    private val locationChangedSubject = PublishSubject.create<Any>()

    /** Location last received from the update.  */
    private var location: Location? = null
    /** Last calculated distance.  */
    var distance: Double = 0.toDouble()
    /** Last calculated speed.  */
    var speed: Double = 0.toDouble()

    private var connected: Boolean = false
    private var paused: Boolean = false

    protected var homeLat: Double = 0.toDouble()
    protected var homeLng: Double = 0.toDouble()
    private var time: Long = 0

    val locationUrl: String
        get() {
            if (location == null) {
                return ""
            }
            return "http://maps.google.com/?q=" + location!!.latitude + "," + location!!.longitude
        }

    /**
     * @return minimum time between two subsequent updates from one provider (in millis)
     */
    private val updateFrequency: Long
        get() = Constants.LOCATION_UPDATE_FREQUENCY

    private fun requestLocationUpdates(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleApiClient!!.connect()
        }
    }

    fun onHomeLocationChanged() {
        updateDistance()

        val homeLocation = preferences!!.getString(SettingsActivity.HOME_LOCATION, Constants.DEFAULT_HOME_LOCATION)
        homeLat = HomeLocationPreference.parseLatitude(homeLocation)
        homeLng = HomeLocationPreference.parseLongitude(homeLocation)
        location?.let {
            distance = DistanceCalculator.distanceInKm(
                    homeLat,
                    homeLng,
                    it.latitude,
                    it.longitude)
        }
        homeChangedSubject.onNext(0)
    }

    /**
     * Initializes the GPS.

     * Actions taken depend on whether the app possesses the permission. If not, has to be
     * called again.

     * @param context application context
     */
    fun init(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun initGps(context: Context) {
        if (googleApiClient == null) {
            googleApiClient = GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build()
        }

        context.applicationContext.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                locationSettingsChanged()
            }
        }, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))

        onHomeLocationChanged()
        requestLocationUpdates(context)
    }

    open fun addProviderListener(providerListener: ProviderListener) {
        providerEnabledSubject.subscribe { providerListener.onProviderEnabled() }
        providerDisabledSubject.subscribe { providerListener.onProviderDisabled() }
    }

    fun distanceChangedListener(): Observable<Any> {
        return distanceSubject.hide()
    }

    fun homeChangedListener(): Observable<Any> {
        return homeChangedSubject.hide()
    }

    fun locationChangedListener(): Observable<Any> {
        return locationChangedSubject.hide()
    }

    val homeLatLng: LatLng
        get() = LatLng(homeLat, homeLng)

    val latLng: LatLng?
        get() {
            return if (location == null) {
                null
            }
            else LatLng(location!!.latitude, location!!.longitude)
        }

    val isLocationAvailable: Boolean
        get() = location != null

    override fun onLocationChanged(location: Location) {
        if (this.location == null && location.accuracy >= ACCURACY_THRESHOLD) {
            return
        }
        if (!isBetterLocation(location, this.location)) {
            return
        }
        timer.reset()
        val time = System.currentTimeMillis()
        speed = calculateSpeedOrReturnZero(this.location, location, time - this.time)
        this.location = location
        this.distance = calculateCurrentDistance()  // distance in kilometres
        this.time = time
        distanceSubject.onNext(0)
        locationChangedSubject.onNext(0)
    }

    private fun updateDistance() {
        if (location == null) {
            return
        }
        distance = calculateCurrentDistance()
    }

    private fun calculateCurrentDistance(): Double {
        return DistanceCalculator.distanceInKm(
                location!!.latitude,
                location!!.longitude,
                homeLat,
                homeLng)
    }

    private fun calculateSpeedOrReturnZero(loc1: Location?, loc2: Location?, time: Long): Double {
        if (loc1 == null || loc2 == null || this.time == 0L || time == 0L ||
                loc1.latitude == loc2.latitude && loc1.longitude == loc2.longitude) {
            return 0.0
        }
        if (time.toDouble() == 0.0) {
            return 0.0
        }
        return DistanceCalculator.speedInKph(loc1, loc2, time)
    }

    @SuppressLint("MissingPermission")
    override fun onConnected(bundle: Bundle?) {
        connected = true

        if (location == null) {
            onLocationChanged(LocationServices.FusedLocationApi.getLastLocation(googleApiClient))
        }

        val updateFrequency = updateFrequency
        removeLocationUpdates()
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(updateFrequency)
                .setSmallestDisplacement(MIN_DISTANCE)

        requestLocationUpdates()
        callProviderListener()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (googleApiClient == null || locationRequest == null) {
            return
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
    }

    private fun removeLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
    }

    fun callProviderListener() {
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
        val pendingResult = LocationServices.SettingsApi.checkLocationSettings(
                googleApiClient,
                builder.build())
        pendingResult.setResultCallback {
            connected = it.status.isSuccess &&
                    it.locationSettingsStates.isLocationUsable
            if (connected) {
                providerEnabledSubject.onNext(0)
            } else {
                providerDisabledSubject.onNext(0)
            }
        }
    }

    fun pauseUpdates() {
        if (paused || googleApiClient == null) {
            return
        }
        Log.d(TAG, "Pause updates.")
        removeLocationUpdates()

        paused = true
    }

    fun resumeUpdates(context: Context) {
        if (!paused || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        Log.d(TAG, "Resume updates.")

        requestLocationUpdates()

        paused = false
    }

    private fun locationSettingsChanged() {
        if (locationRequest == null) {
            return
        }

        callProviderListener()
    }

    override fun onConnectionSuspended(i: Int) {}

    override fun onConnectionFailed(connectionResult: ConnectionResult) {}

    companion object {

        private val TAG = LocationSource::class.java.simpleName

        private val ACCURACY_THRESHOLD = 100.0  // [m]

        /** Minimal distance (in meters) that will be counted between two subsequent updates.  */
        private val MIN_DISTANCE = 5.0f

        private val SIGNIFICANT_TIME_LAPSE = 1000 * 60 * 2

        private fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
            if (currentBestLocation == null) {
                // A new location is always better than no location
                return true
            }

            // Check whether the new location fix is newer or older
            val timeDelta = location.time - currentBestLocation.time
            val isSignificantlyNewer = timeDelta > SIGNIFICANT_TIME_LAPSE
            val isSignificantlyOlder = timeDelta < -SIGNIFICANT_TIME_LAPSE
            val isNewer = timeDelta > 0

            // If it's been more than two minutes since the current location, use the new location
            // because the user has likely moved
            if (isSignificantlyNewer) {
                return true
                // If the new location is more than two minutes older, it must be worse
            } else if (isSignificantlyOlder) {
                return false
            }

            // Check whether the new location fix is more or less accurate
            val accuracyDelta = (location.accuracy - currentBestLocation.accuracy).toInt()
            val isMoreAccurate = accuracyDelta < 0
            val isSignificantlyLessAccurate = accuracyDelta > 200

            // Determine location quality using a combination of timeliness and accuracy
            if (isMoreAccurate) {
                return true
            } else if (isNewer && !isSignificantlyLessAccurate) {
                return true
            }
            return false
        }
    }
}
