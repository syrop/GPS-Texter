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

package pl.org.seva.texter.main

import android.content.Context
import org.kodein.di.Kodein
import org.kodein.di.conf.global
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import pl.org.seva.texter.history.SmsHistory
import pl.org.seva.texter.sms.*
import pl.org.seva.texter.movement.ActivityRecognitionSource
import pl.org.seva.texter.movement.LocationSource
import pl.org.seva.texter.movement.ZoneCalculator
import pl.org.seva.texter.stats.Timer

val Context.module get() = KodeinModuleBuilder(this).build()

inline fun <reified R : Any> instance(): R {
    val result by Kodein.global.instance<R>()
    return result
}

class KodeinModuleBuilder(private val ctx: Context) {

    fun build() = Kodein.Module("main") {
        bind<Bootstrap>() with singleton { Bootstrap(ctx) }
        bind<LocationSource>() with singleton { LocationSource() }
        bind<SmsSender>() with singleton { SmsSender() }
        bind<Timer>() with singleton { Timer() }
        bind<Permissions>() with singleton { Permissions() }
        bind<SmsHistory>() with singleton { SmsHistory() }
        bind<ActivityRecognitionSource>() with singleton { ActivityRecognitionSource() }
        bind<ZoneCalculator>() with singleton { ZoneCalculator() }
    }
}
