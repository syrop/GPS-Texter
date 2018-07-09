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
 * If you like this program, consider donating bitcoin: 36uxha7sy4mv6c9LdePKjGNmQe8eK16aX6
 */

package pl.org.seva.texter.main

import android.app.Application
import android.content.Intent
import android.os.Build
import pl.org.seva.texter.movement.ActivityRecognitionSource
import pl.org.seva.texter.movement.LocationSource

class Bootstrap(private val application: Application) {

    private var isServiceRunning = false

    fun boot() {
        val locationSource: LocationSource = instance()
        val activityRecognitionSource: ActivityRecognitionSource = instance()
        locationSource.initPreferences(application)
        activityRecognitionSource.initWithContext(application)
    }

    fun startService() {
        if (isServiceRunning) {
            return
        }
        startService(Intent(application.baseContext, TexterService::class.java))
        isServiceRunning = true
    }

    private fun startService(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    fun stopService() {
        if (!isServiceRunning) {
            return
        }
        application.stopService(Intent(application.baseContext, TexterService::class.java))
        isServiceRunning = false
    }
}
