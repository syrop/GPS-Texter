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

import javax.inject.Singleton

import dagger.Component
import pl.org.seva.texter.view.activity.HomeLocationActivity
import pl.org.seva.texter.view.activity.MainActivity
import pl.org.seva.texter.view.activity.SettingsActivity
import pl.org.seva.texter.view.fragment.HistoryFragment
import pl.org.seva.texter.view.fragment.NavigationFragment
import pl.org.seva.texter.view.fragment.StatsFragment

@Singleton
@Component(modules = arrayOf(TexterModule::class))
interface TexterComponent {
    fun inject(texterService: TexterService)
    fun inject(navigationFragment: NavigationFragment)
    fun inject(mainActivity: MainActivity)
    fun inject(homeLocationActivity: HomeLocationActivity)
    fun inject(settingsActivity: SettingsActivity)
    fun inject(statsFragment: StatsFragment)
    fun inject(historyFragment: HistoryFragment)
    fun inject(texterApplication: TexterApplication)
}
