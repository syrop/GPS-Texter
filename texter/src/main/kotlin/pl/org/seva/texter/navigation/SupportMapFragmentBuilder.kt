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

package pl.org.seva.texter.navigation

import android.support.v4.app.FragmentManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment

inline fun mapFragment(f: SupportMapFragmentBuilder.() -> Unit): SupportMapFragment =
        SupportMapFragmentBuilder().apply(f).build()

infix fun SupportMapFragment.ready(f: GoogleMap.() -> Unit): SupportMapFragment = apply { getMapAsync(f) }

class SupportMapFragmentBuilder {
    lateinit var fm: FragmentManager
    lateinit var tag: String
    var container: Int = 0

    fun build(): SupportMapFragment {
        var result = fm.findFragmentByTag(tag) as SupportMapFragment?
        result?: let {
            result = SupportMapFragment()
            fm.beginTransaction().add(container, result, tag).commit()
        }

        return result!!
    }
}
