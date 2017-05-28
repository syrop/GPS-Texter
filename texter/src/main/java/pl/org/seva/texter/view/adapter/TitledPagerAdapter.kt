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

package pl.org.seva.texter.view.adapter

import android.app.Fragment
import android.app.FragmentManager
import android.support.v13.app.FragmentPagerAdapter

class TitledPagerAdapter(fm: FragmentManager, private val titles: Array<CharSequence>?) :
        FragmentPagerAdapter(fm) {
    private lateinit var items: List<Fragment>

    fun setItems(items: List<Fragment>): TitledPagerAdapter {
        this.items = items
        return this
    }

    override fun getItem(position: Int): Fragment {
        return items[position]
    }

    override fun getPageTitle(position: Int): CharSequence? {
        if (titles == null || position > titles.size) {
            return null
        }
        return titles[position]
    }

    override fun getCount(): Int {
        return items.size
    }
}
