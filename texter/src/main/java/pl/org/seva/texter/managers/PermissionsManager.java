package pl.org.seva.texter.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.org.seva.texter.listeners.IPermissionGrantedListener;

/**
 * Created by wiktor on 5/13/16.
 */
public class PermissionsManager {

    public static int PERMISSION_ACCESS_FINE_LOCATION = 0;

    private static final PermissionsManager INSTANCE = new PermissionsManager();

    private final Map<String, List<IPermissionGrantedListener>> map;

    public static PermissionsManager getInstance() {
        return INSTANCE;
    }

    private PermissionsManager() {
        map = new HashMap<>();
    }

    public void addPermissionListener(
            final String permission,
            final IPermissionGrantedListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (map) {
                    List<IPermissionGrantedListener> list = map.get(permission);
                    if (list == null) {
                        list = new ArrayList<>();
                        map.put(permission, list);
                    }
                    if (!list.contains(listener) && listener != null) {
                        list.add(listener);
                    }
                }
            }
        }).start();
    }

    public void removePermissionListener(
            final String permission,
            final IPermissionGrantedListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (map) {
                    final List<IPermissionGrantedListener> list = map.get(permission);
                    if (list != null) {
                        list.remove(listener);
                    }
                }
            }
        }).start();
    }

    public void permissionGranted(String permission) {
        synchronized (map) {
            List<IPermissionGrantedListener> list = map.get(permission);
            if (list != null) {
                for (IPermissionGrantedListener listener : list) {
                    listener.onPermissionGranted(permission);
                }
            }
        }
    }
}
