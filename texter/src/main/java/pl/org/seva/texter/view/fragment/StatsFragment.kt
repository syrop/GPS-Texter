/*
 * Copyright (C) 2016 Wiktor Nizio
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
import android.app.Fragment
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

import java.util.Calendar

import javax.inject.Inject

import io.reactivex.disposables.CompositeDisposable
import pl.org.seva.texter.R
import pl.org.seva.texter.TexterApplication
import pl.org.seva.texter.presenter.source.ActivityRecognitionSource
import pl.org.seva.texter.presenter.source.LocationSource
import pl.org.seva.texter.presenter.utils.PermissionsUtils
import pl.org.seva.texter.presenter.utils.SmsSender
import pl.org.seva.texter.presenter.utils.Timer
import pl.org.seva.texter.model.SmsLocation
import pl.org.seva.texter.presenter.listener.ActivityRecognitionListener

class StatsFragment : Fragment(), ActivityRecognitionListener {

    @Inject
    lateinit var locationSource: LocationSource
    @Inject
    lateinit var activityRecognitionSource: ActivityRecognitionSource
    @Inject
    lateinit var timer: Timer
    @Inject
    lateinit var smsSender: SmsSender
    @Inject
    lateinit var permissionsUtils: PermissionsUtils

    private lateinit var distanceTextView: TextView
    private lateinit var intervalTextView: TextView
    private lateinit var stationaryTextView: TextView
    private lateinit var speedTextView: TextView
    private lateinit var sendNowButton: Button

    private val composite = CompositeDisposable()

    private var distance: Double = 0.toDouble()
    private var speed: Double = 0.toDouble()
    private var stationary: Boolean = false

    private lateinit var fragmentActivity: Activity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        distance = locationSource.distance
        speed = locationSource.speed

        homeString = getString(R.string.home)
        hourString = getActivity().getString(R.string.hour)
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        distanceTextView = view.findViewById<TextView>(R.id.distance_value)
        intervalTextView = view.findViewById<TextView>(R.id.update_interval_value)
        stationaryTextView = view.findViewById<TextView>(R.id.stationary)
        speedTextView = view.findViewById<TextView>(R.id.speed_value)
        sendNowButton = view.findViewById<Button>(R.id.send_now_button)
        sendNowButton.setOnClickListener { _ -> onSendNowClicked() }
        sendNowButton.isEnabled = smsSender.isTextingEnabled &&
                distance != 0.0 &&
                distance != smsSender.lastSentDistance

        showStats()
        composite.addAll(
                timer.timerListener().subscribe { _ -> onTimer() },
                smsSender.smsSendingListener().subscribe {
                    _ -> activity.runOnUiThread { this.onSendingSms() } },
                locationSource.distanceChangedListener().subscribe {
                    _ -> activity.runOnUiThread { this.onDistanceChanged() } },
                locationSource.homeChangedListener().subscribe { _ -> onHomeChanged() },
                activityRecognitionSource.addActivityRecognitionListener(this))

        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsUtils
                    .permissionGrantedListener()
                    .filter { permission -> permission == Manifest.permission.ACCESS_FINE_LOCATION }
                    .subscribe { _ -> onLocationPermissionGranted() }
        }

        return view
    }

    override fun onDeviceStationary() {
        stationary = true
    }

    override fun onDeviceMoving() {
        stationary = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity) {
            fragmentActivity = context
            initDependencies()
        }
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override // see http://stackoverflow.com/questions/32083053/android-fragment-onattach-deprecated#32088447
    fun onAttach(activity: Activity) {
        super.onAttach(activity)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            fragmentActivity = activity
            initDependencies()
        }
    }

    private fun initDependencies() {
        val graph = (activity!!.application as TexterApplication).graph
        graph.inject(this)
    }

    private fun showStats() {
        @SuppressLint("DefaultLocale") var distanceStr = String.format("%.3f km", distance)

        if (distance == 0.0) {
            distanceStr = "0 km"
        }
        distanceTextView.text = distanceStr
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
        stationaryTextView.visibility = if (stationary) View.VISIBLE else View.GONE
        intervalTextView.text = timeStrBuilder.toString()
        if (speed == 0.0 || distance == 0.0) {
            speedTextView.visibility = View.INVISIBLE
        } else {
            speedTextView.visibility = View.VISIBLE
            speedTextView.text = speedStr
        }
    }

    private val speedStr: String
        get() {
            @SuppressLint("DefaultLocale")
            var result = String.format("%.1f", if (stationary) 0.0 else speed) + " " + activity!!.getString(R.string.speed_unit)
            if (result.contains(".0")) {
                result = result.replace(".0", "")
            } else if (result.contains(",0")) {
                result = result.replace(",0", "")
            }
            return result
        }

    override fun onDestroy() {
        super.onDestroy()
        composite.clear()
    }

    private fun onDistanceChanged() {
        val resetValues = System.currentTimeMillis() - timer.resetTime > 3 * 3600 * 1000
        if (distance != smsSender.lastSentDistance) {
            sendNowButton.isEnabled = smsSender.isTextingEnabled
        }

        if (resetValues) {  // reset the values if three hours have passed
            this.speed = 0.0
            this.distance = 0.0
        } else {
            this.distance = locationSource.distance
            this.speed = locationSource.speed
        }
        showStats()
    }

    private fun onTimer() {
        if (activity == null) {
            return
        }
        activity!!.runOnUiThread { this.showStats() }
    }

    @SuppressLint("WrongConstant")
    private fun onSendNowClicked() {
        sendNowButton.isEnabled = false
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timer.resetTime
        @SuppressLint("WrongConstant") var minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60
        minutes += calendar.get(Calendar.MINUTE)
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
        distance = locationSource.distance
        showStats()
    }

    private fun onLocationPermissionGranted() {
        sendNowButton.isEnabled = smsSender.isTextingEnabled
    }

    companion object {

        var homeString: String? = null
        private var hourString: String? = null

        fun newInstance(): StatsFragment {
            return StatsFragment()
        }
    }
}
