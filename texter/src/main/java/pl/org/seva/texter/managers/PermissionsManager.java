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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.org.seva.texter.listeners.IPermissionDeniedListener;
import pl.org.seva.texter.listeners.IPermissionGrantedListener;

/**
 * Created by wiktor on 5/13/16.
 */
public class PermissionsManager {

    public static int PERMISSION_ACCESS_FINE_LOCATION_REQUEST = 0;
    public static int PERMISSION_READ_CONTACTS_REQUEST = 1;

    private static PermissionsManager instance;

    private final Map<String, List<IPermissionGrantedListener>> grantedMap = new HashMap<>();
    private final Map<String, List<IPermissionDeniedListener>> deniedMap = new HashMap<>();
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

    public void addPermissionGrantedListener(
            final String permission,
            final IPermissionGrantedListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (grantedMap) {
                    List<IPermissionGrantedListener> list = grantedMap.get(permission);
                    if (list == null) {
                        list = new ArrayList<>();
                        grantedMap.put(permission, list);
                    }
                    if (!list.contains(listener) && listener != null) {
                        list.add(listener);
                    }
                }
            }
        }).start();
    }

    public void removePermissionGrantedListener(
            final String permission,
            final IPermissionGrantedListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (grantedMap) {
                    final List<IPermissionGrantedListener> list = grantedMap.get(permission);
                    if (list != null) {
                        list.remove(listener);
                    }
                }
            }
        }).start();
    }

    public boolean rationaleShown(String permission) {
        return rationalesShown.contains(permission);
    }

    public void onRationaleShown(String permission) {
        if (!rationaleShown(permission)) {
            rationalesShown.add(permission);
        }
    }

    public void addPermissionDeniedListener(
            final String permission,
            final IPermissionDeniedListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (deniedMap) {
                    List<IPermissionDeniedListener> list = deniedMap.get(permission);
                    if (list == null) {
                        list = new ArrayList<>();
                        deniedMap.put(permission, list);
                    }
                    if (!list.contains(listener) && listener != null) {
                        list.add(listener);
                    }
                }
            }
        }).start();
    }

    public void removePermissionDeniedListener(
            final String permission,
            final IPermissionDeniedListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (grantedMap) {
                    final List<IPermissionDeniedListener> list = deniedMap.get(permission);
                    if (list != null) {
                        list.remove(listener);
                    }
                }
            }
        }).start();
    }

    private void permissionGranted(String permission) {
        synchronized (grantedMap) {
            List<IPermissionGrantedListener> list = grantedMap.get(permission);
            if (list != null) {
                for (IPermissionGrantedListener listener : list) {
                    listener.onPermissionGranted(permission);
                }
            }
        }
    }

    private void permissionDenied(String permission) {
        synchronized (grantedMap) {
            List<IPermissionDeniedListener> list = deniedMap.get(permission);
            if (list != null) {
                for (IPermissionDeniedListener listener : list) {
                    listener.onPermissionDenied(permission);
                }
            }
        }
    }

    public void onRequestPermissionsResult(
            @NonNull String permissions[],
            @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            for (String permission : permissions) {
                permissionGranted(permission);
            }
        }
        else {
            for (String permission : permissions) {
                permissionDenied(permission);
            }
        }
    }
}
