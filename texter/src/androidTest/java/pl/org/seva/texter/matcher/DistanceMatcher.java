package pl.org.seva.texter.matcher;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import pl.org.seva.texter.TestConstants;

public class DistanceMatcher extends BaseMatcher<String> {

    public static Matcher<String> distance() {
        return new DistanceMatcher();
    }

    private DistanceMatcher() {

    }

    @Override
    public boolean matches(Object item) {
        if (!(item instanceof String)) {
            return false;
        }
        String str = (String) item;
        double distance = Double.valueOf(str.substring(0, str.indexOf(' ')));
        double delta = Math.abs(TestConstants.DISTANCE - distance);

        return delta < TestConstants.TOLERANCE;
    }

    @Override
    public void describeTo(Description description) {

    }
}
