package pl.org.seva.texter.managers;

import java.util.ArrayList;
import java.util.List;

import pl.org.seva.texter.model.LocationModel;

/**
 * Created by wiktor on 01.08.15.
 */
public class HistoryManager {
    private static HistoryManager instance;

    private boolean mock = true;

    private List<LocationModel> list;

    private HistoryManager() {
        list = new ArrayList<>();
        list.add(new LocationModel());
    }

    public static HistoryManager getInstance() {
        if (instance == null ) {
            synchronized (HistoryManager.class) {
                if (instance == null) {
                    instance = new HistoryManager();
                }
            }
        }
        return instance;
    }

    public static void shutdown() {
        synchronized (HistoryManager.class) {
            instance = null;
        }
    }

    public List<LocationModel> getList() {
        return list;
    }

    public HistoryManager add(LocationModel model) {
        if (mock) {
            list.clear();
            mock = false;
        }
        list.add(model);
        return this;
    }
}
