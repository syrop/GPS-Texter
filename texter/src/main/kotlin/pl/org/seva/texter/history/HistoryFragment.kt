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
 *
 * If you like this program, consider donating bitcoin: bc1qncxh5xs6erq6w4qz3a7xl7f50agrgn3w58dsfp
 */

package pl.org.seva.texter.history

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import pl.org.seva.texter.R
import pl.org.seva.texter.sms.smsSender

class HistoryFragment : Fragment() {

    private lateinit var adapter: HistoryAdapter
    private var scrollToBottom: Boolean = false
    private val recyclerView by lazy<RecyclerView> { requireActivity().findViewById(R.id.recycler_view)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createSubscription()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = HistoryAdapter(requireActivity(), smsHistory.list)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(HistoryAdapter.DividerItemDecoration(requireActivity()))
        recyclerView.clearOnScrollListeners()
        recyclerView.addOnScrollListener(OnScrollListener())
        scrollToBottom = true
    }

    private fun createSubscription() = smsSender.addSmsSentListener(lifecycle) { update() }

    override fun onResume() {
        super.onResume()
        update()
    }

    private fun update() {
        adapter.notifyDataSetChanged()
        if (scrollToBottom) {
            recyclerView.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private inner class OnScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (recyclerView === this@HistoryFragment.recyclerView) {
                scrollToBottom = recyclerView.computeVerticalScrollOffset() ==
                        recyclerView.computeVerticalScrollRange() - recyclerView.computeVerticalScrollExtent()
            }
        }
    }

    companion object {

        fun newInstance() = HistoryFragment()
    }
}
