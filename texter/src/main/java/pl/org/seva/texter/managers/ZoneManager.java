package pl.org.seva.texter.managers;

import android.util.SparseArray;

import pl.org.seva.texter.model.ZoneModel;
import pl.org.seva.texter.utils.StringUtils;

/**
 * Created by wiktor on 5/26/16.
 */
public class ZoneManager {

    private static ZoneManager instance;

    private SparseArray<ZoneModel> zones;

    public static ZoneManager getInstance() {
        if (instance == null ) {
            synchronized (ZoneManager.class) {
                if (instance == null) {
                    instance = new ZoneManager();
                }
            }
        }
        return instance;
    }

    public static void shutdown() {
        synchronized (ZoneManager.class) {
            instance = null;
        }
    }

    private ZoneManager() {
        zones = new SparseArray<>();
    }

    // Needs to be called from a synchronized block.
    public void clear() {
        zones.clear();
    }

    // Needs to be called from a synchronized block.
    public ZoneModel zone(double distance, boolean updateCounters) {
        int check = 0;
        int min = 0;
        int max;
        while (check < distance) {  // Calculate min and max.
            min = check;
            check += StringUtils.KM_INTERVAL;
        }
        max = check;
        if (!updateCounters) {
            return new ZoneModel(min, max);
        }
        ZoneModel zone = zones.get(min);
        if (zone == null) {
            clear();
            zone = new ZoneModel(min, max);
            zone.increaseCounter();
            zones.put(min, zone);
        }
        else {
            zone.increaseCounter();
        }

        return zone;
    }
}
