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
import android.content.Context
import android.support.test.runner.AndroidJUnitRunner

import pl.org.seva.texter.mock.MockApplication

@Suppress("unused")  // Declared in build.gradle
class MockTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
            cl: ClassLoader, className: String, context: Context): Application =
            super.newApplication(cl, MockApplication::class.java.name, context)
}
