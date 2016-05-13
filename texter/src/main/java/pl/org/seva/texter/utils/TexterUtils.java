package pl.org.seva.texter.utils;

/**
 * Created by wiktor on 28.08.15.
 */
public class TexterUtils {

    /** Send an sms each time this value is passed. */
    public static final int KM_INTERVAL = 2;  // two kilometers

    public static String getSpeedStr(double speed, String speedUnit) {
        String result = String.format("%.1f", speed) + " " + speedUnit;
        if (result.contains(".0")) {
            result = result.replace(".0", "");
        }
        else if (result.contains(",0")) {
            result = result.replace(",0", "");
        }
        return result;
    }

}
