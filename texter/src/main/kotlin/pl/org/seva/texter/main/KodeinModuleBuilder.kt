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

package pl.org.seva.texter.main

import android.app.Application
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.conf.global
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import pl.org.seva.texter.history.SmsHistory
import pl.org.seva.texter.sms.*
import pl.org.seva.texter.movement.ActivityRecognitionSource
import pl.org.seva.texter.movement.LocationSource
import pl.org.seva.texter.movement.ZoneCalculator
import pl.org.seva.texter.stats.Timer

fun module(f: KodeinModuleBuilder.() -> Unit) = KodeinModuleBuilder().apply { f() }.build()

inline fun <reified T : Any> instance() = Kodein.global.instance<T>()

class KodeinModuleBuilder {

    lateinit var application: Application

    fun build() = Kodein.Module {
        bind<Bootstrap>() with singleton { Bootstrap(application) }
        bind<LocationSource>() with singleton { LocationSource() }
        bind<SmsSender>() with singleton { SmsSender() }
        bind<Timer>() with singleton { Timer() }
        bind<Permissions>() with singleton { Permissions() }
        bind<SmsHistory>() with singleton { SmsHistory() }
        bind<ActivityRecognitionSource>() with singleton { ActivityRecognitionSource() }
        bind<ZoneCalculator>() with singleton { ZoneCalculator() }
    }
}
