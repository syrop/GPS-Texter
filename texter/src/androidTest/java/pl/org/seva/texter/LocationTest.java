package pl.org.seva.texter;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import pl.org.seva.texter.activity.MainActivity;

@RunWith(AndroidJUnit4.class)
public class LocationTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(
            MainActivity.class,
            true,     // initialTouchMode
            false);   // launchActivity. False so we can customize the intent per test method

    @Test
    public void test() {

    }
}
