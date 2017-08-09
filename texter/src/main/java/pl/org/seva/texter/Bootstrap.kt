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

package pl.org.seva.texter

import android.app.Application
import android.content.Intent
import com.github.salomonbrys.kodein.conf.KodeinGlobalAware
import com.github.salomonbrys.kodein.instance
import pl.org.seva.texter.source.ActivityRecognitionSource
import pl.org.seva.texter.source.LocationSource

class Bootstrap(private val application: Application): KodeinGlobalAware {

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
        application.startService(Intent(application.baseContext, TexterService::class.java))
        isServiceRunning = true
    }

    fun stopService() {
        if (!isServiceRunning) {
            return
        }
        application.stopService(Intent(application.baseContext, TexterService::class.java))
        isServiceRunning = false
    }
}
