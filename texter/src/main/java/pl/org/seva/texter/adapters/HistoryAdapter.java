package pl.org.seva.texter.adapters;

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
    private Context context;

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
        String distanceStr = String.format("%.2f", location.getDistance()) + location.getSign() + " km";
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
        private TextView distance;
        private TextView time;
        private TextView speed;

        private HistoryViewHolder(View v) {
            distance = (TextView) v.findViewById(R.id.distance);
            time = (TextView) v.findViewById(R.id.time);
            speed = (TextView) v.findViewById(R.id.speed);
        }
    }
}
