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

    private static final PermissionsManager INSTANCE = new PermissionsManager();

    private final Map<String, List<IPermissionGrantedListener>> grantedMap = new HashMap<>();
    private final Map<String, List<IPermissionDeniedListener>> deniedMap = new HashMap<>();

    public static PermissionsManager getInstance() {
        return INSTANCE;
    }

    private PermissionsManager() {
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
