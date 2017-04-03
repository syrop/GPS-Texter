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
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import pl.org.seva.texter.R;
import pl.org.seva.texter.databinding.HistoryAdapterBinding;
import pl.org.seva.texter.model.LocationModel;
import pl.org.seva.texter.presenter.utils.StringUtils;

/**
 * Created by wiktor on 01.08.15.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private final Context context;
    private final List<LocationModel> values;

    public HistoryAdapter(Context context, List<LocationModel> values) {
        this.context = context;
        this.values = values;
    }

    @Override
    public HistoryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        HistoryAdapterBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.history_adapter, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        LocationModel location = values.get(position);

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

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView distance;
        private final TextView time;
        private final TextView speed;

        ViewHolder(HistoryAdapterBinding binding) {
            super(binding.getRoot());
            distance = binding.distance;
            time = binding.time;
            speed = binding.speed;
        }
    }
}
