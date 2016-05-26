package pl.org.seva.texter.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import pl.org.seva.texter.R;
import pl.org.seva.texter.adapters.HistoryAdapter;
import pl.org.seva.texter.listeners.ISMSListener;
import pl.org.seva.texter.managers.HistoryManager;
import pl.org.seva.texter.managers.SMSManager;
import pl.org.seva.texter.model.LocationModel;

/**
 * Created by hp1 on 21-01-2015.
 */
public class HistoryFragment extends Fragment implements ISMSListener, AbsListView.OnScrollListener {

    private HistoryAdapter adapter;
    private ListView historyListView;
    boolean scrollToBottom;

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.history_fragment,container,false);
        historyListView = (ListView) v.findViewById(R.id.listView);
        adapter = new HistoryAdapter(getActivity(), HistoryManager.getInstance().getList());
        historyListView.setAdapter(adapter);
        historyListView.setOnScrollListener(this);
        SMSManager.getInstance().addSMSListener(this);
        scrollToBottom = true;

        return v;
    }

    @Override
    public void onSMSSent(LocationModel model) {
        adapter.notifyDataSetChanged();
        if (scrollToBottom) {
            historyListView.setSelection(adapter.getCount());
        }
    }

    @Override
    public void onSendingSMS(LocationModel location) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (view == historyListView) {
            scrollToBottom = firstVisibleItem + visibleItemCount >= totalItemCount;
        }
    }
}
