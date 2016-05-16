package pl.org.seva.texter.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.List;

import pl.org.seva.texter.R;
import pl.org.seva.texter.adapters.MainActivityTabAdapter;
import pl.org.seva.texter.controller.SMSController;
import pl.org.seva.texter.fragments.HistoryFragment;
import pl.org.seva.texter.fragments.StatsFragment;
import pl.org.seva.texter.fragments.NavigationFragment;
import pl.org.seva.texter.layouts.SlidingTabLayout;
import pl.org.seva.texter.listeners.IPermissionGrantedListener;
import pl.org.seva.texter.managers.GPSManager;
import pl.org.seva.texter.managers.PermissionsManager;
import pl.org.seva.texter.managers.SMSManager;
import pl.org.seva.texter.utils.Timer;

public class MainActivity extends AppCompatActivity implements IPermissionGrantedListener {

    public static final int STATS_TAB_POSITION = 0;
    public static final int MAP_TAB_POSITION = 1;
    public static final int HISTORY_TAB_POSITION = 2;

    private static final int NUMBER_OF_TABS = 3;

    /**
     * Number of milliseconds that will be taken for a double click.
     */
    private static final long DOUBLE_CLICK_MILLIS = 5000;
    /**
     * Used when counting a double click.
     */
    private long clickTime;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            int color;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                color = getResources().getColor(R.color.ColorPrimaryDark, getTheme());
            }
            else {
                color = getResources().getColor(R.color.ColorPrimaryDark);
            }
            window.setStatusBarColor(color);
        }
        setContentView(R.layout.activity_main);

        CharSequence titles[] = new CharSequence[NUMBER_OF_TABS];
        titles[STATS_TAB_POSITION] = getString(R.string.stats_tab_name);
        titles[MAP_TAB_POSITION] = getString(R.string.map_tab_name);
        titles[HISTORY_TAB_POSITION] = getString(R.string.history_tab_name);

        SMSController.getInstance().init(getPackageManager().
                hasSystemFeature(PackageManager.FEATURE_TELEPHONY));

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(StatsFragment.newInstance());
        fragments.add(NavigationFragment.newInstance());
        fragments.add(HistoryFragment.newInstance());

        MainActivityTabAdapter adapter =
                new MainActivityTabAdapter(getSupportFragmentManager(), titles, NUMBER_OF_TABS).
                        setItems(fragments);

        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        if (pager != null) {
            pager.setAdapter(adapter);
        }

        SlidingTabLayout tabs = (SlidingTabLayout) findViewById(R.id.tabs);
        if (tabs != null) {
            tabs.setDistributeEvenly(true);
        }

        final int tabColor;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            tabColor = getResources().getColor(R.color.tabsScrollColor, getTheme());
        }
        else {
            tabColor = getResources().getColor(R.color.tabsScrollColor);
        }
        if (tabs != null) {
            tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
                @Override
                public int getIndicatorColor(int position) {
                    return tabColor;
                }
            });
            tabs.setViewPager(pager);
        }
        if (Timer.getInstance().getState() == Thread.State.NEW) {
            Timer.getInstance().start();
        }

        SMSManager.getInstance().init(this, getString(R.string.speed_unit));
        GPSManager.getInstance().init(this);
        GPSManager.getInstance().addDistanceChangedListener(SMSController.getInstance());

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            PermissionsManager.getInstance().addPermissionListener(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    this);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String permissions[],
            @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            for (String permission : permissions) {
                PermissionsManager.getInstance().permissionGranted(permission);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timer.getInstance().end();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - clickTime < DOUBLE_CLICK_MILLIS) {
            super.onBackPressed();
        }
        else {
            Toast.makeText(this, R.string.tap_back_second_time, Toast.LENGTH_SHORT).
                    show();
            clickTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPermissionGranted(String permission) {
        if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            GPSManager.getInstance().init(this);
            PermissionsManager.getInstance().
                    removePermissionListener(Manifest.permission.ACCESS_FINE_LOCATION, this);
        }
    }
}
