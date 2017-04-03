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
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.RecyclerView;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import pl.org.seva.texter.R;
import pl.org.seva.texter.presenter.utils.SmsCache;
import pl.org.seva.texter.view.adapter.HistoryAdapter;
import pl.org.seva.texter.TexterApplication;
import pl.org.seva.texter.presenter.dagger.Graph;
import pl.org.seva.texter.databinding.HistoryFragmentBinding;
import pl.org.seva.texter.presenter.utils.SmsSender;

public class HistoryFragment extends Fragment {

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    SmsCache smsCache;
    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
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

    @SuppressWarnings("deprecation")
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
        Graph graph = ((TexterApplication) activity.getApplication()).getGraph();
        graph.inject(this);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        HistoryFragmentBinding binding =
                DataBindingUtil.inflate(inflater, R.layout.history_fragment, container, false);
        historyRecyclerView = binding.listView;
        historyRecyclerView.setHasFixedSize(true);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new HistoryAdapter(getActivity(), smsCache.getList());
        historyRecyclerView.setAdapter(adapter);
        historyRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));
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
        smsSentSubscription = smsSender.smsSentListener().subscribe(
                __ -> onSMsSent());
    }

    private void onSMsSent() {
        adapter.notifyDataSetChanged();
        if (scrollToBottom) {
            historyRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private class OnScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (recyclerView == historyRecyclerView) {
                scrollToBottom = recyclerView.computeVerticalScrollOffset() ==
                        recyclerView.computeVerticalScrollRange() -
                                recyclerView.computeVerticalScrollExtent();
            }
        }
    }

    private static class DividerItemDecoration extends RecyclerView.ItemDecoration {

        private static final int[] ATTRS = new int[]{android.R.attr.listDivider};

        private final Drawable mDivider;

        DividerItemDecoration(Context context) {
            final TypedArray styledAttributes = context.obtainStyledAttributes(ATTRS);
            mDivider = styledAttributes.getDrawable(0);
            styledAttributes.recycle();
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }
}
