package pl.org.seva.texter;

import pl.org.seva.texter.utils.Calculator;

public class TestConstants {

    public static final double DISTANCE_TOLERANCE = 0.01; // [km]
    public static final double LATITUDE_STEP = 0.001;
    static final double DISTANCE_STEP = Calculator.calculateDistance(0, 0, LATITUDE_STEP, 0);
}
