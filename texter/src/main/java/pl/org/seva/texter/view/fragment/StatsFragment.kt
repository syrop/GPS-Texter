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

import android.annotation.SuppressLint
import android.app.Fragment
import android.os.Bundle
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

    private var distance: Double = 0.0
    private var speed: Double = 0.0
    private var stationary: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity.application as TexterApplication).graph.inject(this)
        distance = locationSource.distance
        speed = locationSource.speed

        homeString = getString(R.string.home)
        hourString = activity.getString(R.string.hour)
        speedUnitStr = getString(R.string.speed_unit)
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        distanceTextView = view.findViewById(R.id.distance_value) as TextView
        intervalTextView = view.findViewById(R.id.update_interval_value) as TextView
        stationaryTextView = view.findViewById(R.id.stationary) as TextView
        speedTextView = view.findViewById(R.id.speed_value) as TextView
        sendNowButton = view.findViewById(R.id.send_now_button) as Button
        sendNowButton.setOnClickListener { onSendNowClicked() }
        sendNowButton.isEnabled = smsSender.isTextingEnabled &&
                distance != 0.0 &&
                distance != smsSender.lastSentDistance

        showStats()
        createSubscriptions()

        return view
    }

    private fun createSubscriptions() {
        composite.addAll(
                timer.timerListener().subscribe { onSecondsTimer() },
                smsSender.smsSendingListener().subscribe {
                    activity.runOnUiThread { onSendingSms() } },
                locationSource.addDistanceChangedListener {
                    activity.runOnUiThread { onDistanceChanged() } },
                locationSource.addHomeChangedListener { onHomeChanged() },
                activityRecognitionSource.addActivityRecognitionListener(this))
    }

    override fun onDeviceStationary() {
        stationary = true
    }

    override fun onDeviceMoving() {
        stationary = false
    }

    private fun showStats() {
        distanceTextView.text = if (distance == 0.0) {
            "0 km"
        } else {
            formattedDistanceStr
        }

        stationaryTextView.visibility = if (stationary) View.VISIBLE else View.GONE
        intervalTextView.text = formattedTimeStr
        if (speed == 0.0 || distance == 0.0) {
            speedTextView.visibility = View.INVISIBLE
        } else {
            speedTextView.visibility = View.VISIBLE
            speedTextView.text = formattedSpeedStr
        }
    }

    private val formattedDistanceStr : String
        get() {
            return String.format("%.3f km", distance)
        }

    private val formattedSpeedStr: String
        get() {
            @SuppressLint("DefaultLocale")
            var result = String.format("%.1f", if (stationary) 0.0 else speed) + " " + speedUnitStr
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

    override fun onDestroy() {
        super.onDestroy()
        composite.clear()
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
            this.distance = locationSource.distance
            this.speed = locationSource.speed
        }
        showStats()
    }

    private fun onSecondsTimer() {
        activity?.runOnUiThread { this.showStats() }
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
        distance = locationSource.distance
        showStats()
    }

    companion object {

        var homeString: String? = null
        private lateinit var hourString: String
        private lateinit var speedUnitStr: String

        fun newInstance(): StatsFragment {
            return StatsFragment()
        }
    }
}
