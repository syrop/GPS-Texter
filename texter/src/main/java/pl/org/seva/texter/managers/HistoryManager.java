package pl.org.seva.texter.managers;

import java.util.ArrayList;
import java.util.List;

import pl.org.seva.texter.model.LocationModel;

/**
 * Created by wiktor on 01.08.15.
 */
public class HistoryManager {
    private static final HistoryManager INSTANCE = new HistoryManager();

    private boolean mock = true;

    private List<LocationModel> list;

    private HistoryManager() {
        list = new ArrayList<>();
        list.add(new LocationModel());
    }

    public static HistoryManager getInstance() {
        return INSTANCE;
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
