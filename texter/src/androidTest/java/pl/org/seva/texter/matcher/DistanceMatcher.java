package pl.org.seva.texter.matcher;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import pl.org.seva.texter.TestConstants;

public class DistanceMatcher extends BaseMatcher<String> {

    private double expectedDistance;

    public static Matcher<String> distance(double expectedDistance) {
        return new DistanceMatcher(expectedDistance);
    }

    private DistanceMatcher(double expectedDistance) {
        this.expectedDistance = expectedDistance;
    }

    @Override
    public boolean matches(Object item) {
        if (!(item instanceof String)) {
            return false;
        }
        String str = (String) item;
        double distance = Double.valueOf(str.substring(0, str.indexOf(' ')));
        double delta = Math.abs(expectedDistance - distance);

        return delta < TestConstants.DISTANCE_TOLERANCE;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("is in appropriate range from " + expectedDistance);
    }
}
