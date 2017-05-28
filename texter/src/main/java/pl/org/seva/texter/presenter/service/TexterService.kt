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

package pl.org.seva.texter.presenter.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

import javax.inject.Inject

import io.reactivex.disposables.Disposables
import pl.org.seva.texter.R
import pl.org.seva.texter.presenter.source.LocationSource
import pl.org.seva.texter.presenter.utils.SmsSender
import pl.org.seva.texter.view.activity.MainActivity
import pl.org.seva.texter.TexterApplication

class TexterService : Service() {

    @Inject
    lateinit var locationSource: LocationSource
    @Inject
    lateinit var smsSender: SmsSender

    private var distanceSubscription = Disposables.empty()

    override fun onBind(arg0: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        (application as TexterApplication).graph.inject(this)
        val mainActivityIntent = Intent(this, MainActivity::class.java)

        // Use System.currentTimeMillis() to have a unique ID for the pending intent.
        val pIntent = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                mainActivityIntent,
                0)
        @Suppress("DEPRECATION")
        val n = Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            R.drawable.notification
                        else
                            R.mipmap.ic_launcher)
                .setContentIntent(pIntent)
                .setAutoCancel(false)
                .build()
        createDistanceSubscription()
        locationSource.resumeUpdates(this)

        startForeground(ONGOING_NOTIFICATION_ID, n)

        return Service.START_STICKY
    }

    override fun onDestroy() {
        removeDistanceSubscription()
        locationSource.pauseUpdates()
        super.onDestroy()
    }

    private fun hardwareCanSendSms(): Boolean {
        return (application as TexterApplication).hardwareCanSendSms()
    }

    private fun createDistanceSubscription() {
        distanceSubscription = locationSource
                .distanceChangedListener()
                .filter { hardwareCanSendSms() }
                .subscribe { smsSender.onDistanceChanged() }
    }

    private fun removeDistanceSubscription() {
        distanceSubscription.dispose()
    }

    companion object {

        private val ONGOING_NOTIFICATION_ID = 1
    }
}