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

/**
 * Created by hp1 on 21-01-2015.
 */
public class MainActivityTabAdapter extends FragmentStatePagerAdapter {

    private final int numberOfTabs;
    private final CharSequence titles[]; // This will Store the Titles of the Tabs which are Going to be passed when MainActivityTabAdapter is created
    private List<Fragment> items;

    // Build a Constructor and assign the passed Values to appropriate values in the class
    public MainActivityTabAdapter(FragmentManager fm, CharSequence titles[]) {
        super(fm);

        this.titles = titles;
        this.numberOfTabs = pl.org.seva.texter.activities.MainActivity.NUMBER_OF_TABS;
    }

    public MainActivityTabAdapter setItems(List<Fragment> items) {
        this.items = items;
        return this;
    }

    //This method return the fragment for the every position in the View Pager
    @Override
    public Fragment getItem(int position) {
        return items.get(position);
    }

    // This method return the titles for the Tabs in the Tab Strip
    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position];
    }

    // This method return the Number of tabs for the tabs Strip

    @Override
    public int getCount() {
        return numberOfTabs;
    }
}
