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

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

import java.lang.ref.WeakReference

import javax.inject.Inject
import javax.inject.Singleton

import io.reactivex.subjects.PublishSubject
import pl.org.seva.texter.presenter.listener.ActivityRecognitionListener

@Singleton
open class ActivityRecognitionSource @Inject
internal constructor() : GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private var initialized: Boolean = false
    private var googleApiClient: GoogleApiClient? = null
    private var weakContext: WeakReference<Context>? = null
    private var activityRecognitionReceiver : BroadcastReceiver? = null

    fun init(context: Context) {
        if (initialized) {
            return
        }
        weakContext = WeakReference(context)
        if (googleApiClient == null) {
            googleApiClient = GoogleApiClient.Builder(context)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build()
            googleApiClient!!.connect()
        }

        initialized = true
    }

    override fun onConnected(bundle: Bundle?) {
        val context = weakContext!!.get() ?: return
        registerReceiver()
        val intent = Intent(ACTIVITY_RECOGNITION_INTENT)

        val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                googleApiClient,
                ACTIVITY_RECOGNITION_INTERVAL,
                pendingIntent)
    }

    override fun onConnectionSuspended(i: Int) {
        unregisterReceiver()
    }

    private fun registerReceiver() {
        val context = weakContext!!.get() ?: return
        activityRecognitionReceiver = activityRecognitionReceiver?: ActivityRecognitionReceiver()
        context.registerReceiver(activityRecognitionReceiver, IntentFilter(ACTIVITY_RECOGNITION_INTENT))
    }

    private fun unregisterReceiver() {
        val context = weakContext!!.get() ?: return
        activityRecognitionReceiver?: return
        context.unregisterReceiver(activityRecognitionReceiver)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {

    }

    fun addActivityRecognitionListener(activityRecognitionListener: ActivityRecognitionListener) : Disposable {
        val result = CompositeDisposable()
        result.addAll(
                stationarySubject
                    .filter { it >= STATIONARY_CONFIDENCE_THRESHOLD }
                    .subscribe { _ -> activityRecognitionListener.onDeviceStationary() },
                movingSubject.subscribe { _ -> activityRecognitionListener.onDeviceMoving() })
        return result
    }

    private fun onDeviceStationary(confidence: Int) {
        stationarySubject.onNext(confidence)
    }

    private fun onDeviceMoving() {
        movingSubject.onNext(0)
    }

    private inner class ActivityRecognitionReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (ActivityRecognitionResult.hasResult(intent)) {
                val result = ActivityRecognitionResult.extractResult(intent)
                if (result.mostProbableActivity.type == DetectedActivity.STILL) {
                    onDeviceStationary(result.getActivityConfidence(DetectedActivity.STILL))
                } else {
                    onDeviceMoving()
                }
            }
        }
    }

    companion object {

        private val ACTIVITY_RECOGNITION_INTENT = "activity_recognition_intent"
        private val ACTIVITY_RECOGNITION_INTERVAL = 1000L  // [ms]
        private val STATIONARY_CONFIDENCE_THRESHOLD = 55

        private val stationarySubject = PublishSubject.create<Int>()
        private val movingSubject = PublishSubject.create<Any>()
    }
}
