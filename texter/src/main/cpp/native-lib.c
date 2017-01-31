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

#include <math.h>
#include <jni.h>

__const double EARTH_RADIUS = 6371.0;  // [km]

double toRad(double deg) {
    return deg / 180.0 * M_PI;
}

double dist(double lat1, double lon1, double lat2, double lon2) {
    double dLat = toRad(lat2 - lat1);
    double dLon = toRad(lon2 - lon1);
    double a = pow(sin(dLat / 2), 2.0) + cos(toRad(lat1)) * cos(toRad(lat2)) * pow(sin(dLon / 2), 2.0);
    return 2.0 * atan2(sqrt(a), sqrt(1 - a)) * EARTH_RADIUS;
}

JNIEXPORT jdouble JNICALL Java_pl_org_seva_texter_utils_Calculator_distance
        (JNIEnv * env, jobject obj, jdouble lat1, jdouble lon1, jdouble lat2, jdouble lon2) {
    return dist(lat1, lon1, lat2, lon2);
}

JNIEXPORT jdouble JNICALL Java_pl_org_seva_texter_utils_Calculator_speed
        (JNIEnv * env, jobject obj, jdouble lat1, jdouble lon1, jdouble lat2, jdouble lon2, jlong time) {
    double seconds = time / 1000.0;
    double hours = seconds / 3600.0;
    double distance = dist(lat1, lon1, lat2, lon2);
    return distance / hours;
}
