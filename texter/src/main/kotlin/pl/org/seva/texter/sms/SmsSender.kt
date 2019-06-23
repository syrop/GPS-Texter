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

package pl.org.seva.texter.sms

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import androidx.lifecycle.Lifecycle
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.preference.PreferenceManager
import androidx.core.content.ContextCompat
import android.widget.Toast

import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.UUID

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import pl.org.seva.texter.main.Constants
import pl.org.seva.texter.R
import pl.org.seva.texter.main.observe
import pl.org.seva.texter.data.DistanceZone
import pl.org.seva.texter.movement.getSpeedString
import pl.org.seva.texter.settings.SettingsActivity
import pl.org.seva.texter.data.SmsLocation
import pl.org.seva.texter.history.smsHistory
import pl.org.seva.texter.main.instance
import pl.org.seva.texter.movement.location
import pl.org.seva.texter.movement.zoneCalculator

val smsSender by instance<SmsSender>()

open class SmsSender {

    private lateinit var preferences: SharedPreferences
    private lateinit var speedUnit: String
    private lateinit var weakContext: WeakReference<Context>

    private val smsManager: android.telephony.SmsManager = android.telephony.SmsManager.getDefault()

    private val smsSendingSubject: PublishSubject<Any> = PublishSubject.create<Any>()
    private val smsSentSubject: PublishSubject<Any> = PublishSubject.create<Any>()

    var lastSentDistance: Double = 0.0

    private var initialized: Boolean = false

    private var lastSentLocation: SmsLocation? = null
    private var zone: DistanceZone? = null


    fun init(context: Context, speedUnit: String) {
        if (initialized) {
            return
        }
        this.speedUnit = speedUnit
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        weakContext = WeakReference(context)
        initialized = true
    }

    fun permissionsToRequest() : List<String> {
        val result = ArrayList<String>()
        weakContext.get()?.let {
            if (needsPermission() &&
                    ContextCompat.checkSelfPermission(
                            it,
                            Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                result.add(Manifest.permission.SEND_SMS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && needsPermission() &&
                    ContextCompat.checkSelfPermission(
                            it,
                            Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                result.add(Manifest.permission.READ_PHONE_STATE)
            }
        }

        return result
    }

    fun onDistanceChanged() {
        val distance = location.distance
        val speed = location.speed

        val time = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        var minutes = calendar.get(Calendar.HOUR_OF_DAY) * 60
        minutes += calendar.get(Calendar.MINUTE)

        val smsLocation = SmsLocation()
        smsLocation.distance = distance
        smsLocation.setTime(minutes)
        smsLocation.speed = speed

        val zone = zoneCalculator.calculateZone(distance)
        this.zone?.let {
            if (canSendZone(zone)) {
                val direction = calculateDirection(zone)
                smsLocation.direction = direction

                if ((if (direction == 1) zone.min else zone.max) <= maxSentDistance) {
                    send(smsLocation)
                }
                this.zone = zone
            }
        } ?:let { this.zone = zone }
    }

    private fun canSendZone(zone: DistanceZone): Boolean = zone.min != this.zone!!.min &&
            zone.counter >= Constants.SMS_COUNT_TRIGGER &&
            zone.delay >= Constants.TIME_IN_ZONE

    private fun calculateDirection(zone: DistanceZone) = if (this.zone!!.min > zone.min) -1 else 1

    @Synchronized fun resetZones() {
        zoneCalculator.clearCache()
        zone = null
    }

    fun addSmsSendingListenerUi(lifecycle: Lifecycle, listener: () -> Unit) =
            lifecycle.observe { smsSendingSubject
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { listener() } }

    fun addSmsSentListener(lifecycle: Lifecycle, listener: () -> Unit) =
            lifecycle.observe { smsSentSubject.subscribe { listener() } }

    private val phoneNumber: String
        get() {
            val numberStr = preferences.getString(SettingsActivity.PHONE_NUMBER, "")
            return if (numberStr!!.isNotEmpty()) numberStr else "0"
        }

    protected open val maxSentDistance: Int
        get() {
            val numberStr = preferences.getString(SettingsActivity.MAXIMUM_DISTANCE, "")
            return if (numberStr!!.isNotEmpty()) Integer.valueOf(numberStr) else 0
        }

    private fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
        weakContext.get()?.registerReceiver(receiver, filter)
    }

    private fun unregisterReceiver(receiver: BroadcastReceiver) {
        weakContext.get()?.unregisterReceiver(receiver)
    }

    private fun registerBroadcastReceiver(id: String) =// When the SMS has been sent.
            registerReceiver(SmsSentReceiver(), IntentFilter(SENT + id))

    @SuppressLint("WrongConstant")
    fun send(model: SmsLocation) {
        if (!isTextingEnabled) {
            return
        }
        if (this.lastSentLocation != null && this.lastSentLocation == model) {
            return
        }
        this.lastSentLocation = model
        if (!isCorrectPhoneNumberSet) {
            return
        }
        checkInit()
        val distance = model.distance
        @SuppressLint("DefaultLocale")
        val distanceStr = String.format("%.2f", distance) + model.sign
        val smsBuilder = StringBuilder("$distanceStr km")
        if (isSpeedIncluded) {
            val speedStr = getSpeedString(model.speed, speedUnit)
            smsBuilder.append(if (speedStr.startsWith("0 ")) "" else ", $speedStr")
        }
        if (isTimeIncluded) {
            val now = Calendar.getInstance()
            var minuteStr = Integer.toString(now.get(Calendar.MINUTE))
            if (minuteStr.length == 1) {
                minuteStr = "0$minuteStr"
            }
            val timeStr = Integer.toString(now.get(Calendar.HOUR_OF_DAY)) + ":" + minuteStr
            smsBuilder.append(" (").append(timeStr).append(")")
        }
        if (isLocationIncluded) {
            smsBuilder.append(" ").append(location.locationUrl)
        }
        @SuppressLint("DefaultLocale")
        val intentDistanceStr = String.format("%.1f", distance) + model.sign + " km"
        val smsStr = smsBuilder.toString()
        send(smsStr, intentDistanceStr, model)
        lastSentDistance = distance
    }

    protected open val isCorrectPhoneNumberSet: Boolean
        get() = phoneNumber != "0"

    private fun send(text: String, intentText: String, location: SmsLocation) {
        val id = UUID.randomUUID().toString()
        val sentIntent = Intent(SENT + id)
        sentIntent.putExtra(TEXT_KEY, intentText)
        sentIntent.putExtra(DISTANCE_KEY, location.distance)
        sentIntent.putExtra(MINUTES_KEY, location.minutes)
        sentIntent.putExtra(DIRECTION_KEY, location.direction)
        sentIntent.putExtra(SPEED_KEY, location.speed)

        val deliveredIntent = Intent(DELIVERED + id)
        deliveredIntent.putExtra(TEXT_KEY, intentText)

        smsSendingSubject.onNext(0)

        weakContext.get()?.let {
            val sentPI = PendingIntent.getBroadcast(it, 0, sentIntent, 0)
            val deliveredPI = PendingIntent.getBroadcast(it, 0, deliveredIntent, 0)
            registerBroadcastReceiver(id)
            sendTextMessage(text, sentPI, deliveredPI)
        }
    }

    protected open fun sendTextMessage(
            text: String,
            sentIntent: PendingIntent,
            deliveredIntent: PendingIntent) =
            smsManager.sendTextMessage(phoneNumber, null, text, sentIntent, deliveredIntent)

    private fun checkInit() {
        if (!initialized) {
            throw IllegalStateException("SMS not initialized")
        }
    }

    private val isSpeedIncluded: Boolean
        get() = preferences.getBoolean(SettingsActivity.INCLUDE_SPEED, false)

    private val isLocationIncluded: Boolean
        get() = preferences.getBoolean(SettingsActivity.INCLUDE_LOCATION, false)

    private val isTimeIncluded: Boolean
        get() = preferences.getBoolean(SettingsActivity.INCLUDE_TIME, false)

    open val isTextingEnabled: Boolean
        get() = preferences.getBoolean(SettingsActivity.SMS_ENABLED, false)

    protected open fun needsPermission() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    private inner class SmsSentReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val text = intent.getStringExtra(TEXT_KEY)
            val location = SmsLocation()
            location.distance = intent.getDoubleExtra(DISTANCE_KEY, 0.0)
            location.setTime(intent.getIntExtra(MINUTES_KEY, 0))
            location.direction = intent.getIntExtra(DIRECTION_KEY, 0)
            location.speed = intent.getDoubleExtra(SPEED_KEY, 0.0)
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val sentBuilder = StringBuilder(context.getString(R.string.sent))
                    var length = Toast.LENGTH_SHORT
                    text?.let {
                        sentBuilder.append(": ").append(it)
                        length = Toast.LENGTH_SHORT
                    }
                    Toast.makeText(context, sentBuilder.toString(), length).show()
                    smsHistory.add(location)
                    smsSentSubject.onNext(0)
                }
                android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Toast.makeText(
                        context,
                        context.getString(R.string.generic_failure),
                        Toast.LENGTH_SHORT).show()
                android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE -> Toast.makeText(
                        context,
                        context.getString(R.string.no_service),
                        Toast.LENGTH_SHORT).show()
                android.telephony.SmsManager.RESULT_ERROR_NULL_PDU -> Toast.makeText(context, "Null PDU", Toast.LENGTH_SHORT).show()
                android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF -> Toast.makeText(
                        context,
                        context.getString(R.string.radio_off),
                        Toast.LENGTH_SHORT).show()
            }
            unregisterReceiver(this)
        }
    }

    companion object {

        private const val TEXT_KEY = "pl.org.seva.texter.Text"
        private const val DISTANCE_KEY = "pl.org.seva.texter.Distance"
        private const val MINUTES_KEY = "pl.org.seva.texter.Minutes"
        private const val DIRECTION_KEY = "pl.org.seva.texter.Direction"
        private const val SPEED_KEY = "pl.org.seva.texter.Speed"

        private const val SENT = "SMS_SENT"
        private const val DELIVERED = "SMS_DELIVERED"
    }
}
