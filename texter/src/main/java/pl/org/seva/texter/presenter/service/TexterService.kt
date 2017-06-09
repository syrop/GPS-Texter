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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        (application as TexterApplication).graph.inject(this)

        startForeground(ONGOING_NOTIFICATION_ID, createOngoingNotification())
        createDistanceSubscription()
        locationSource.resumeUpdates(this)

        return Service.START_STICKY
    }

    private fun createOngoingNotification(): Notification {
        val mainActivityIntent = Intent(this, MainActivity::class.java)

        val pi = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                mainActivityIntent,
                0)
        return Notification.Builder(this)
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

    override fun onDestroy() {
        removeDistanceSubscription()
        locationSource.pauseUpdates()
        super.onDestroy()
    }

    private fun hardwareCanSendSms(): Boolean {
        return (application as TexterApplication).hardwareCanSendSms()
    }

    private fun createDistanceSubscription() {
        if (!hardwareCanSendSms()) {
            return
        }
        distanceSubscription = locationSource.addDistanceChangedListener { smsSender.onDistanceChanged() }
    }

    private fun removeDistanceSubscription() {
        distanceSubscription.dispose()
    }

    companion object {

        private val ONGOING_NOTIFICATION_ID = 1
    }
}
