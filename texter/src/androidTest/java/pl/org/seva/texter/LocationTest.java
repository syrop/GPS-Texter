package pl.org.seva.texter;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import pl.org.seva.texter.activity.MainActivity;
import pl.org.seva.texter.manager.GpsManager;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static pl.org.seva.texter.matcher.DistanceMatcher.distance;

@RunWith(AndroidJUnit4.class)
public class LocationTest {

    @SuppressWarnings("WeakerAccess")
    @Inject
    GpsManager gpsManager;

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(
            MainActivity.class,
            true,
            true);

    @Test
    public void test() {
        onView(withId(R.id.distance_value)).check(matches(withText(distance())));
    }
}
