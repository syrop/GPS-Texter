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

package pl.org.seva.texter.view.fragment;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import pl.org.seva.texter.R;
import pl.org.seva.texter.presenter.utils.SmsCache;
import pl.org.seva.texter.view.adapter.HistoryAdapter;
import pl.org.seva.texter.TexterApplication;
import pl.org.seva.texter.presenter.dagger.TexterComponent;
import pl.org.seva.texter.databinding.FragmentHistoryBinding;
import pl.org.seva.texter.presenter.utils.SmsSender;

public class HistoryFragment extends Fragment {

    @SuppressWarnings({"CanBeFinal"})
    @Inject
    SmsCache smsCache;
    @SuppressWarnings({"CanBeFinal"})
    @Inject
    SmsSender smsSender;

    private HistoryAdapter adapter;
    private RecyclerView historyRecyclerView;
    private boolean scrollToBottom;
    private Context context;

    private Disposable smsSentSubscription = Disposables.empty();

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof  Activity) {
            initDependencies((Activity) context);
        }
    }

    @Override
    // see http://stackoverflow.com/questions/32083053/android-fragment-onattach-deprecated#32088447
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            this.context = activity;
            initDependencies(activity);
        }
    }

    private void  initDependencies(Activity activity) {
        TexterComponent component = ((TexterApplication) activity.getApplication()).getComponent();
        component.inject(this);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        FragmentHistoryBinding binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_history, container, false);
        historyRecyclerView = binding.recyclerView;
        historyRecyclerView.setHasFixedSize(true);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new HistoryAdapter(getActivity(), smsCache.getList());
        historyRecyclerView.setAdapter(adapter);
        historyRecyclerView.addItemDecoration(new HistoryAdapter.DividerItemDecoration(getActivity()));
        historyRecyclerView.clearOnScrollListeners();
        historyRecyclerView.addOnScrollListener(new OnScrollListener());
        scrollToBottom = true;

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        smsSentSubscription.dispose();
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
        smsSentSubscription = smsSender.smsSentListener().subscribe(__ -> update());
    }

    private void update() {
        adapter.notifyDataSetChanged();
        if (scrollToBottom) {
            historyRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private class OnScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            if (recyclerView == historyRecyclerView) {
                scrollToBottom = recyclerView.computeVerticalScrollOffset() ==
                        recyclerView.computeVerticalScrollRange() -
                                recyclerView.computeVerticalScrollExtent();
            }
        }
    }
}
