/*
 * Copyright (C) 2017 Wiktor Nizio
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
