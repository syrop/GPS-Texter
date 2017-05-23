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

package pl.org.seva.texter.view.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import pl.org.seva.texter.R;
import pl.org.seva.texter.model.SmsLocation;
import pl.org.seva.texter.presenter.utils.StringUtils;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private final Context context;
    private final List<SmsLocation> values;

    public HistoryAdapter(Context context, List<SmsLocation> values) {
        this.context = context;
        this.values = values;
    }

    @Override
    public HistoryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_history, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        SmsLocation location = values.get(position);

        @SuppressLint("DefaultLocale")
        String distanceStr =
                String.format("%.2f", location.getDistance()) + location.getSign() + " km";
        holder.distance.setText(distanceStr);

        int hours = location.getMinutes() / 60;
        int minutes = location.getMinutes() % 60;
        StringBuilder builder = new StringBuilder(hours + ":");
        if (minutes < 10) {
            builder.append("0");
        }
        builder.append(minutes);
        holder.time.setText(builder.toString());
        holder.speed.setText(
            StringUtils.getSpeedString(location.getSpeed(),
            context.getString(R.string.speed_unit)));
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    public static class DividerItemDecoration extends RecyclerView.ItemDecoration {

        private static final int[] ATTRS = new int[]{android.R.attr.listDivider};

        private final Drawable mDivider;

        public DividerItemDecoration(Context context) {
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

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView distance;
        private final TextView time;
        private final TextView speed;

        ViewHolder(View view) {
            super(view);
            distance = view.findViewById(R.id.distance);
            time = view.findViewById(R.id.time);
            speed = view.findViewById(R.id.speed);
        }
    }
}
