/*
 * Copyright (C) 2016 Wiktor Nizio
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

package pl.org.seva.texter.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import java.util.List;

/**
 * Created by hp1 on 21-01-2015.
 */
public class TitledPagerAdapter extends FragmentPagerAdapter {

    private final CharSequence titles[];
    private List<Fragment> items;

    public TitledPagerAdapter(FragmentManager fm, CharSequence titles[]) {
        super(fm);

        this.titles = titles;
    }

    public TitledPagerAdapter setItems(List<Fragment> items) {
        this.items = items;
        return this;
    }

    @Override
    public Fragment getItem(int position) {
        return items.get(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (titles == null || position > titles.length) {
            return null;
        }
        return titles[position];
    }

    @Override
    public int getCount() {
        return items.size();
    }
}
