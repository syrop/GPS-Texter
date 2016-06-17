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

package pl.org.seva.texter.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import pl.org.seva.texter.R;
import pl.org.seva.texter.model.LocationModel;
import pl.org.seva.texter.utils.StringUtils;

/**
 * Created by wiktor on 01.08.15.
 */
public class HistoryAdapter extends ArrayAdapter<LocationModel> {
    private final Context context;

    public HistoryAdapter(Context context, List<LocationModel> values) {
        super(context, -1, values);
        this.context = context;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LocationModel location = getItem(position);
        HistoryViewHolder holder;
        if (convertView == null) {
            convertView = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).
                    inflate(R.layout.history_adapter, parent, false);
            holder = new HistoryViewHolder(convertView);
            convertView.setTag(holder);
        }
        else {
            holder = (HistoryViewHolder) convertView.getTag();
        }
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
        holder.speed.setText(StringUtils.getSpeedStr(location.getSpeed(), context.getString(R.string.speed_unit)));
        return convertView;
    }

    private static class HistoryViewHolder {
        private final TextView distance;
        private final TextView time;
        private final TextView speed;

        private HistoryViewHolder(View v) {
            distance = (TextView) v.findViewById(R.id.distance);
            time = (TextView) v.findViewById(R.id.time);
            speed = (TextView) v.findViewById(R.id.speed);
        }
    }
}
