package pl.org.seva.texter.fragments;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;

import pl.org.seva.texter.R;
import pl.org.seva.texter.activities.SettingsActivity;

/**
 * Created by wiktor on 16.08.15.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            getPreferenceScreen().removePreference(findPreference(SettingsActivity.CATEGORY_SMS));
        }
    }
}
