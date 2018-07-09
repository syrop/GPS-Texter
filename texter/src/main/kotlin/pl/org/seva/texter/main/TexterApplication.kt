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
import android.content.pm.PackageManager
import org.kodein.di.Kodein
import org.kodein.di.conf.global

open class TexterApplication : Application() {

    private val bootstrap: Bootstrap get() = instance()

    init {
        Kodein.global.addImport(module { application = this@TexterApplication })
    }

    override fun onCreate() {
        super.onCreate()
        bootstrap.boot()
    }

    open fun hardwareCanSendSms() = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    fun startService() = bootstrap.startService()

    open fun stopService() = bootstrap.stopService()
}
