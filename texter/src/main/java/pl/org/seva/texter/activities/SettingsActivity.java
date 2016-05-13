package pl.org.seva.texter.activities;

import pl.org.seva.texter.R;
import pl.org.seva.texter.fragments.SettingsFragment;
import pl.org.seva.texter.managers.GPSManager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.WindowManager;

public class SettingsActivity extends AppCompatActivity  implements SharedPreferences.OnSharedPreferenceChangeListener {

    /** If device is not enabled to send SMS< this entire category will be hidden. */
    public static final String CATEGORY_SMS = "category_sms";

    /** Unless true, SMS will be disabled and SMS-related options grayed out. */
    public static final String SMS_ENABLED = "pref_enable_sms";
    /** All text messages will be sent to this number. */
    public static final String SMS_NUMBER = "pref_phone_number";
    /** Beyond this distance from home, no messages will be sent. */
    public static final String MAXIMUM_DISTANCE = "pref_max_distance";
    /** Request updates every so many seconds. */
    public static final String LOCATION_UPDATE_FREQUENCY = "pref_update_frequency";
    /** Location to measure distance from. */
    public static final String HOME_LOCATION = "pref_home_location";
    /** If true, time will be sent with every SMS. */
    public static final String INCLUDE_TIME = "pref_include_time";
    /** If true, speed will be sent with every SMS. */
    public static final String INCLUDE_SPEED = "pref_include_speed";
    /** If true, location will be sent with every SMS. */
    public static final String INCLUDE_LOCATION = "pref_include_location";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // Attaching the layout to the toolbar object
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);

		setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case LOCATION_UPDATE_FREQUENCY:
                GPSManager.getInstance().requestLocationUpdates(this);
                break;
            case HOME_LOCATION:
                GPSManager.getInstance().updateHome();
                break;
        }
    }
}
