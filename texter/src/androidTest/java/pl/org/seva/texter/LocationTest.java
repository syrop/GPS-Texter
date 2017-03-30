package pl.org.seva.texter;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import pl.org.seva.texter.activity.MainActivity;
import pl.org.seva.texter.manager.GpsManager;

@RunWith(AndroidJUnit4.class)
public class LocationTest {

    @SuppressWarnings("WeakerAccess")
    @Inject
    GpsManager gpsManager;

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(
            MainActivity.class,
            true,
            false);

    @Test
    public void test() {

    }
}
