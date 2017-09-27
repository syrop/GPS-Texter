/*
 * Copyright (C) 2017 Wiktor Nizio
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

package pl.org.seva.texter.history

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import pl.org.seva.texter.R
import pl.org.seva.texter.data.SmsLocation
import pl.org.seva.texter.movement.getSpeedString

class HistoryAdapter(private val context: Context, private val values: List<SmsLocation>) :
        RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.adapter_history, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val location = values[position]

        @SuppressLint("DefaultLocale")
        val distanceStr = String.format("%.2f", location.distance) + location.sign + " km"
        holder.distance.text = distanceStr

        val hours = location.minutes / 60
        val minutes = location.minutes % 60
        val builder = StringBuilder(hours.toString() + ":")
        if (minutes < 10) {
            builder.append("0")
        }
        builder.append(minutes)
        holder.time.text = builder.toString()
        holder.speed.text = getSpeedString(
                location.speed,
                context.getString(R.string.speed_unit))
    }

    override fun getItemCount() = values.size

    class DividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

        private val divider: Drawable

        init {
            val styledAttributes = context.obtainStyledAttributes(ATTRS)
            divider = styledAttributes.getDrawable(0)
            styledAttributes.recycle()
        }

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
            val left = parent.paddingLeft
            val right = parent.width - parent.paddingRight

            repeat (parent.childCount) {
                val child = parent.getChildAt(it)

                val params = child.layoutParams as RecyclerView.LayoutParams

                val top = child.bottom + params.bottomMargin
                val bottom = top + divider.intrinsicHeight

                divider.setBounds(left, top, right, bottom)
                divider.draw(c)
            }
        }

        companion object {

            private val ATTRS = intArrayOf(android.R.attr.listDivider)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val distance: TextView = view.findViewById(R.id.distance)
        val time: TextView = view.findViewById(R.id.time)
        val speed: TextView = view.findViewById(R.id.speed)
    }
}
