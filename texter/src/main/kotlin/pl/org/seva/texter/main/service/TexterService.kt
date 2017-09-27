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

package pl.org.seva.texter.main.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build

import pl.org.seva.texter.movement.LocationSource
import pl.org.seva.texter.sms.SmsSender
import pl.org.seva.texter.main.MainActivity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.arch.lifecycle.LifecycleService
import android.content.Context
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance
import pl.org.seva.texter.R
import pl.org.seva.texter.main.TexterApplication
import pl.org.seva.texter.movement.ActivityRecognitionSource


class TexterService : LifecycleService(), KodeinGlobalAware {

    private val locationSource: LocationSource = instance()
    private val smsSender: SmsSender = instance()
    private val activityRecognitionSource: ActivityRecognitionSource = instance()

    private val notificationBuilder by lazy { createNotificationBuilder() }
    private var activityRecognitionListenersAdded = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(ONGOING_NOTIFICATION_ID, createOngoingNotification())
        addDistanceListeners()
        addActivityRecognitionListeners()
        locationSource.request()

        return Service.START_STICKY
    }

    private fun addActivityRecognitionListeners() {
        if (activityRecognitionListenersAdded) {
            return
        }
        activityRecognitionSource.addActivityRecognitionListener(
                lifecycle, stationary = this::onDeviceStationary, moving = this::onDeviceMoving)
        activityRecognitionListenersAdded = true
    }

    private fun onDeviceStationary() {
        locationSource.paused = true
        locationSource.removeRequest()
    }

    private fun onDeviceMoving() {
        locationSource.paused = false
        locationSource.request()
    }

    private fun createOngoingNotification(): Notification {
        val mainActivityIntent = Intent(this, MainActivity::class.java)

        val pi = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                mainActivityIntent,
                0)
        return notificationBuilder
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            R.drawable.notification
                        else
                            R.mipmap.ic_launcher)
                .setContentIntent(pi)
                .setAutoCancel(false)
                .build()
    }

    private fun createNotificationBuilder() : Notification.Builder =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
            }
            else {
                val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                // The id of the channel.
                val id = NOTIFICATION_CHANNEL_ID
                // The user-visible name of the channel.
                val name = getString(R.string.channel_name)
                // The user-visible description of the channel.
                val description = getString(R.string.channel_description)
                val importance = NotificationManager.IMPORTANCE_LOW
                val mChannel = NotificationChannel(id, name, importance)
                // Configure the notification channel.
                mChannel.description = description
                mNotificationManager.createNotificationChannel(mChannel)
                Notification.Builder(this, id)
            }

    private fun hardwareCanSendSms(): Boolean = (application as TexterApplication).hardwareCanSendSms()

    private fun addDistanceListeners() {
        if (!hardwareCanSendSms()) {
            return
        }
        locationSource.addDistanceListener(lifecycle) { smsSender.onDistanceChanged() }
    }

    companion object {
        private val NOTIFICATION_CHANNEL_ID = "my_channel_01"
        private val ONGOING_NOTIFICATION_ID = 1
    }
}
