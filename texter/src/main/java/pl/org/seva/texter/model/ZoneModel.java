package pl.org.seva.texter.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import pl.org.seva.texter.utils.StringUtils;

/**
 * Created by wiktor on 28.08.15.
 */
public class ZoneModel {

    private static ZoneModel INSTANCE = new ZoneModel();

    private SparseArray<Zone> zones;

    private ZoneModel() {
        zones = new SparseArray<>();
    }

    public static ZoneModel getInstance() {
        return INSTANCE;
    }

    private void clear() {
        zones.clear();
    }

    public Zone zone(double distance, boolean updateCounters) {
        int check = 0;
        int min = 0;
        int max;
        while (check < distance) {  // calculate min and max
            min = check;
            check += StringUtils.KM_INTERVAL;
        }
        max = check;
        if (!updateCounters) {
            return new Zone(min, max);
        }
        Zone zone = zones.get(min);
        if (zone == null) {
            clear();
            zone = new Zone(min, max);
            zone.counter++;
            zones.put(min, zone);
        }
        else {
            zone.counter++;
        }

        return zone;
    }

    public static class Zone implements Parcelable {
        private int min;
        private int max;
        private int counter;
        private long time;

        private Zone(int min, int max) {
            this.min = min;
            this.max = max;
            time = System.currentTimeMillis();
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public int getCounter() {
            return counter;
        }

        public long getDelay() {
            return System.currentTimeMillis() - time;
        }

        @Override
        public String toString() {
            return "[" + getMin() + " km - " + getMax() + " km]";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(min);
            out.writeInt(max);
            out.writeInt(counter);
            out.writeLong(time);
        }

        public static final Parcelable.Creator<Zone> CREATOR = new Parcelable.Creator<Zone>() {
            public Zone createFromParcel(Parcel in) {
                return new Zone(in);
            }

            public Zone[] newArray(int size) {
                return new Zone[size];
            }
        };

        private Zone(Parcel in) {
            min = in.readInt();
            max = in.readInt();
            counter = in.readInt();
            time = in.readLong();
        }
    }
}
