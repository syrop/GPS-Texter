/*
 * Copyright (C) 2016 Wiktor Nizio
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

package pl.org.seva.texter.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import pl.org.seva.texter.utils.StringUtils;

/**
 * Created by wiktor on 28.08.15.
 */
public class ZoneModel implements Parcelable {
    private int min;
    private int max;
    private int counter;
    private long time;

    public ZoneModel(int min, int max) {
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

    public void increaseCounter() {
        counter++;
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

    public static final Parcelable.Creator<ZoneModel> CREATOR =
            new Parcelable.Creator<ZoneModel>() {
        public ZoneModel createFromParcel(Parcel in) {
            return new ZoneModel(in);
        }

        public ZoneModel[] newArray(int size) {
            return new ZoneModel[size];
        }
    };

    private ZoneModel(Parcel in) {
        min = in.readInt();
        max = in.readInt();
        counter = in.readInt();
        time = in.readLong();
    }
}