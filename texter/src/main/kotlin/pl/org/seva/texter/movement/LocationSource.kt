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

package pl.org.seva.texter.movement

import android.annotation.SuppressLint
import android.arch.lifecycle.Lifecycle
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

import io.reactivex.subjects.PublishSubject
import pl.org.seva.texter.stats.Timer
import pl.org.seva.texter.settings.SettingsActivity
import pl.org.seva.texter.settings.HomeLocationPreference
import pl.org.seva.texter.main.Constants
import pl.org.seva.texter.main.observe

open class LocationSource :
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        KodeinGlobalAware {

    private val timer: Timer = instance()

    private lateinit var preferences: SharedPreferences

    private var googleApiClient: GoogleApiClient? = null
    private var locationRequest: LocationRequest? = null

    private val distanceSubject = PublishSubject.create<Any>()
    private val homeChangedSubject = PublishSubject.create<Any>()
    private val locationChangedSubject = PublishSubject.create<Any>()
    var paused = false

    private var location: Location? = null
    var distance: Double = 0.0
    var speed: Double = 0.0

    private var connected: Boolean = false

    protected var homeLat: Double = 0.0
    protected var homeLng: Double = 0.0
    private var time: Long = 0

    val locationUrl
        get() = location?.run { GOOGLE_PREFIX + latitude + "," + longitude } ?: ""

    private val updateFrequency: Long
        get() = Constants.LOCATION_UPDATE_FREQUENCY_MS

    private fun connectGoogleApiClient() = googleApiClient!!.connect()

    fun onHomeLocationChanged() {
        updateDistance()

        val homeLocation = preferences.getString(SettingsActivity.HOME_LOCATION, Constants.DEFAULT_HOME_LOCATION)
        homeLat = HomeLocationPreference.parseLatitude(homeLocation)
        homeLng = HomeLocationPreference.parseLongitude(homeLocation)
        location?.let {
            distance = DistanceCalculator.distanceKm(
                    homeLat,
                    homeLng,
                    it.latitude,
                    it.longitude)
        }
        homeChangedSubject.onNext(0)
    }

    fun initPreferences(context: Context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun initGpsOnLocationGranted(applicationContext: Context) {
        if (googleApiClient != null) {
            return
        }
        googleApiClient = GoogleApiClient.Builder(applicationContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()

        onHomeLocationChanged()
        connectGoogleApiClient()
    }

    fun addDistanceListener(lifecycle: Lifecycle, listener : () -> Unit) =
            lifecycle.observe { distanceSubject
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { request() }
            .doOnDispose { removeRequest() }
            .subscribe { listener() } }

    fun addDistanceChangedListenerUi(lifecycle: Lifecycle, listener : () -> Unit) =
            lifecycle.observe { distanceSubject
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { listener() } }

    fun addHomeChangedListener(lifecycle: Lifecycle, listener: () -> Unit) =
            lifecycle.observe { homeChangedSubject.subscribe { listener() } }

    fun addLocationChangedListener(lifecycle: Lifecycle, listener: () -> Unit) =
            lifecycle.observe { locationChangedSubject.subscribe { listener() } }

    val homeLatLng: LatLng
        get() = LatLng(homeLat, homeLng)

    val latLng: LatLng?
        get() = location?.let { LatLng(it.latitude, it.longitude) }

    val isLocationAvailable: Boolean
        get() = location != null

    override fun onLocationChanged(location: Location) {
        if (this.location == null && location.accuracy >= ACCURACY_THRESHOLD_M) {
            return
        }
        if (!isBetterLocation(location, this.location)) {
            return
        }
        timer.reset()
        val time = System.currentTimeMillis()
        speed = calculateSpeedOrReturnZero(this.location, location, time - this.time)
        this.location = location
        updateDistance()
        this.time = time
        distanceSubject.onNext(0)
        locationChangedSubject.onNext(0)
    }

    private fun updateDistance() = location?.let { distance = calculateCurrentDistance() }

    private fun calculateCurrentDistance() =
        DistanceCalculator.distanceKm(
                location!!.latitude,
                location!!.longitude,
                homeLat,
                homeLng)

    private fun calculateSpeedOrReturnZero(loc1: Location?, loc2: Location?, time: Long): Double {
        if (loc1 == null || loc2 == null || this.time == 0L || time == 0L ||
                loc1.latitude == loc2.latitude && loc1.longitude == loc2.longitude) {
            return 0.0
        }
        if (time.toDouble() == 0.0) {
            return 0.0
        }
        return DistanceCalculator.speedKph(loc1, loc2, time)
    }

    @SuppressLint("MissingPermission")
    override fun onConnected(bundle: Bundle?) {
        connected = true

        location?: let {
            LocationServices.FusedLocationApi.getLastLocation(googleApiClient)?.let { onLocationChanged(it) }
        }

        val updateFrequency = updateFrequency
        removeRequest()
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(updateFrequency)
                .setSmallestDisplacement(MIN_DISTANCE)

        request()
    }

    @SuppressLint("MissingPermission")
    open fun request() {
        if (paused || googleApiClient == null || locationRequest == null) {
            return
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)
    }

    open fun removeRequest() {
        googleApiClient?.let {
            LocationServices.FusedLocationApi.removeLocationUpdates(it, this) }
    }

    override fun onConnectionSuspended(i: Int) = Unit

    override fun onConnectionFailed(connectionResult: ConnectionResult) = Unit

    companion object {
        private val GOOGLE_PREFIX = "http://maps.google.com/?q="
        private val ACCURACY_THRESHOLD_M = 100.0

        /** Minimal distance (in meters) that will be counted between two subsequent updates.  */
        private val MIN_DISTANCE = 5.0f

        private val SIGNIFICANT_TIME_LAPSE = 1000 * 60 * 2

        private fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
            currentBestLocation ?: return true

            val timeDelta = location.time - currentBestLocation.time
            val isSignificantlyNewer = timeDelta > SIGNIFICANT_TIME_LAPSE
            val isSignificantlyOlder = timeDelta < -SIGNIFICANT_TIME_LAPSE
            val isNewer = timeDelta > 0

            if (isSignificantlyNewer) {
                return true
            } else if (isSignificantlyOlder) {
                return false
            }

            val accuracyDelta = (location.accuracy - currentBestLocation.accuracy).toInt()
            val isMoreAccurate = accuracyDelta < 0
            val isSignificantlyLessAccurate = accuracyDelta > 200

            if (isMoreAccurate || isNewer && !isSignificantlyLessAccurate) {
                return true
            }
            return false
        }
    }
}
