package pl.org.seva.texter.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

/**
 * Created by hp1 on 21-01-2015.
 */
public class MainActivityTabAdapter extends FragmentStatePagerAdapter {

    private int numberOfTabs;
    private CharSequence titles[]; // This will Store the Titles of the Tabs which are Going to be passed when MainActivityTabAdapter is created
    private List<Fragment> items;

    // Build a Constructor and assign the passed Values to appropriate values in the class
    public MainActivityTabAdapter(FragmentManager fm, CharSequence titles[], int numberOfTabs) {
        super(fm);

        this.titles = titles;
        this.numberOfTabs = numberOfTabs;
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
