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
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
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
    public void onDestroy() {
        super.onDestroy();
        SMSManager.getInstance().removeSMSListener(this);
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
    public void onScroll(
            AbsListView view,
            int firstVisibleItem,
            int visibleItemCount,
            int totalItemCount) {
        if (view == historyListView) {
            scrollToBottom = firstVisibleItem + visibleItemCount >= totalItemCount;
        }
    }
}
