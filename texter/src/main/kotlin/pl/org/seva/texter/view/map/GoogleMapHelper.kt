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

package pl.org.seva.texter.view.map

import android.support.v4.app.FragmentManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment

fun prepareMap(f: GoogleMapHelper.() -> Unit): SupportMapFragment = GoogleMapHelper().apply(f).prepare()

infix fun SupportMapFragment.ready(f: GoogleMap.() -> Unit) {
    getMapAsync(f)
}

class GoogleMapHelper {
    lateinit var fm: FragmentManager
    lateinit var tag: String
    var container: Int = 0

    fun prepare(): SupportMapFragment {
        var result = fm.findFragmentByTag(tag) as SupportMapFragment?
        result?: let {
            result = SupportMapFragment()
            fm.beginTransaction().add(container, result, tag).commit()
        }

        return result!!
    }
}
