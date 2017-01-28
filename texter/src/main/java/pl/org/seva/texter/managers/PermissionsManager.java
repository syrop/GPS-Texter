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

package pl.org.seva.texter.managers;

import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import rx.subjects.PublishSubject;

public class PermissionsManager {

    public static final int PERMISSION_ACCESS_FINE_LOCATION_REQUEST = 0;
    public static final int PERMISSION_READ_CONTACTS_REQUEST = 1;

    private static PermissionsManager instance;

    private final PublishSubject<String> permissionGrantedSubject = PublishSubject.create();
    private final PublishSubject<String> permissionDeniedSubject = PublishSubject.create();

    private final List<String> rationalesShown = new ArrayList<>();

    private PermissionsManager() {
    }

    public static PermissionsManager getInstance() {
        if (instance == null ) {
            synchronized (PermissionsManager.class) {
                if (instance == null) {
                    instance = new PermissionsManager();
                }
            }
        }
        return instance;
    }

    public static void shutdown() {
        synchronized (PermissionsManager.class) {
            instance = null;
        }
    }

    public PublishSubject<String> permissionGrantedListener() {
        return permissionGrantedSubject;
    }

    public boolean isRationaleNeeded(String permission) {
        return !rationalesShown.contains(permission);
    }

    public void onRationaleShown(String permission) {
        if (isRationaleNeeded(permission)) {
            rationalesShown.add(permission);
        }
    }

    public PublishSubject<String> permissionDeniedListener() {
        return permissionDeniedSubject;
    }

    private void onPermissionGranted(String permission) {
        permissionGrantedSubject.onNext(permission);
        permissionGrantedSubject.onCompleted();
    }

    private void onPermissionDenied(String permission) {
        permissionDeniedSubject.onNext(permission);
        permissionDeniedSubject.onCompleted();
    }

    public void onRequestPermissionsResult(
            @NonNull String permissions[],
            @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            for (String permission : permissions) {
                onPermissionGranted(permission);
            }
        }
        else {
            for (String permission : permissions) {
                onPermissionDenied(permission);
            }
        }
    }
}
