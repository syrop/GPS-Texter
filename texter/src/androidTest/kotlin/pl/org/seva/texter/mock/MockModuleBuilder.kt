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

package pl.org.seva.texter.mock

import android.content.Context
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton
import pl.org.seva.texter.sms.SmsSender
import pl.org.seva.texter.movement.ActivityRecognitionObservable
import pl.org.seva.texter.movement.LocationObservable

val Context.mockModule get() = MockModuleBuilder().build(this)

class MockModuleBuilder {
    fun build(ctx: Context) = Kodein.Module("test") {
        bind<LocationObservable>(overrides = true) with singleton { MockLocationObservable() }
        bind<SmsSender>(overrides = true) with singleton { MockSmsSender(ctx) }
        bind<ActivityRecognitionObservable>(overrides = true) with singleton { MockActivityRecognitionObservable() }
    }
}
