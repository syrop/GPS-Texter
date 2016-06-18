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

package pl.org.seva.texter.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

import pl.org.seva.texter.activities.MainActivity;

/**
 * Created by hp1 on 21-01-2015.
 */
public class MainActivityTabAdapter extends FragmentStatePagerAdapter {

    private final CharSequence titles[];
    private List<Fragment> items;

    public MainActivityTabAdapter(FragmentManager fm, CharSequence titles[]) {
        super(fm);

        this.titles = titles;
    }

    public MainActivityTabAdapter setItems(List<Fragment> items) {
        this.items = items;
        return this;
    }

    @Override
    public Fragment getItem(int position) {
        return items.get(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position];
    }

    @Override
    public int getCount() {
        return MainActivity.NUMBER_OF_TABS;
    }
}
