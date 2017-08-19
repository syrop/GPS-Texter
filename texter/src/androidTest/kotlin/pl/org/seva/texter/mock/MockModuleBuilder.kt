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

package pl.org.seva.texter.mock

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.singleton
import pl.org.seva.texter.presenter.SmsSender
import pl.org.seva.texter.source.ActivityRecognitionSource
import pl.org.seva.texter.source.LocationSource

fun module(f: MockModuleBuilder.() -> Unit) =
        MockModuleBuilder().apply { f() }.build()

class MockModuleBuilder {
    fun build() = Kodein.Module {
        bind<LocationSource>(overrides = true) with singleton { MockLocationSource() }
        bind<SmsSender>(overrides = true) with singleton { MockSmsSender() }
        bind<ActivityRecognitionSource>(overrides = true) with singleton { MockActivityRecognitionSource() }
    }
}
