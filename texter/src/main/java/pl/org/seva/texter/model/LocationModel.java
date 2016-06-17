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

/**
 * Created by wiktor on 01.08.15.
 */
public class LocationModel implements Parcelable {
    private double distance;  // in kilometers
    private  int minutes; // in minutes since midnight
    private int direction;
    private double speed;

    public LocationModel() {
        // required in order to implement Parcelable
    }

    @Override
    public boolean equals(Object o) {
        // ignore direction
        if (!(o instanceof LocationModel)) {
            return false;
        }
        LocationModel model = (LocationModel) o;
        return model.distance == distance && model.minutes == minutes && model.speed == speed;
    }

    @Override
    public int hashCode() {
        // ignore direction
        long l = Double.doubleToRawLongBits(distance) ^ Double.doubleToLongBits(speed);
        int result = (int) (l >> 32 ^ l & 0x0000ffffL);
        result ^= minutes;
        return result;
    }

    private LocationModel(Parcel in) {
        distance = in.readDouble();
        minutes = in.readInt();
        direction = in.readInt();
        speed = in.readDouble();
    }

    public LocationModel setDistance(double distance) {
        this.distance = distance;
        return this;
    }

    public double getDistance() {
        return distance;
    }

    public LocationModel setSpeed(double speed) {
        this.speed = speed;
        return this;
    }

    public double getSpeed() {
        return speed;
    }

    public String getSign() {
        if (direction == 0) {
            return "";
        }
        else if (direction < 0) {
            return "-";
        }
        else {
            return "+";
        }
    }

    public LocationModel setTime(int minutes) {
        this.minutes = minutes;
        return this;
    }

    public int getMinutes() {
        return minutes;
    }

    public LocationModel setDirection(int direction) {
        this.direction = direction;
        return this;
    }

    public int getDirection() {
        return direction;
    }

    public int describeContents() {
        return  0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(distance);
        out.writeInt(minutes);
        out.writeInt(direction);
        out.writeDouble(speed);
    }

    public static final Parcelable.Creator<LocationModel> CREATOR = new Parcelable.Creator<LocationModel>() {
        public LocationModel createFromParcel(Parcel in) {
            return new LocationModel(in);
        }

        public LocationModel[] newArray(int size) {
            return new LocationModel[size];
        }
    };
}
