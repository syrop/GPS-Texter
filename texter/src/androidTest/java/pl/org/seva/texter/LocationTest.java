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
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static pl.org.seva.texter.action.DelayAction.delay;
import static pl.org.seva.texter.matcher.DistanceMatcher.distance;

@RunWith(AndroidJUnit4.class)
public class LocationTest {

    private static final int DURATION_IN_SECONDS = 50;

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
        onView(isRoot()).perform(delay(100));
        for (int i = 1; i <= DURATION_IN_SECONDS; i++) {
            onView(isRoot()).perform(delay(1000));
            onView(withId(R.id.distance_value)).check(matches(withText(distance(i * TestConstants.DISTANCE_STEP))));
        }
    }
}
