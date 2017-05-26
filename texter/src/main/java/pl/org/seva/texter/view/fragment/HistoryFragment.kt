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

package pl.org.seva.texter.view.fragment

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.support.v7.widget.RecyclerView

import javax.inject.Inject

import io.reactivex.disposables.Disposables
import pl.org.seva.texter.R
import pl.org.seva.texter.presenter.utils.SmsHistory
import pl.org.seva.texter.view.adapter.HistoryAdapter
import pl.org.seva.texter.TexterApplication
import pl.org.seva.texter.presenter.utils.SmsSender

class HistoryFragment : Fragment() {

    @Inject
    lateinit var smsHistory: SmsHistory
    @Inject
    lateinit var smsSender: SmsSender

    private var adapter: HistoryAdapter? = null
    private lateinit var historyRecyclerView: RecyclerView
    private var scrollToBottom: Boolean = false
    private lateinit var fragmentContext: Context

    private var smsSentSubscription = Disposables.empty()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentContext = context
        if (context is Activity) {
            initDependencies(context)
        }
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override // see http://stackoverflow.com/questions/32083053/android-fragment-onattach-deprecated#32088447
    fun onAttach(activity: Activity) {
        super.onAttach(activity)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            fragmentContext = activity
            initDependencies(activity)
        }
    }

    private fun initDependencies(activity: Activity) {
        val graph = (activity.application as TexterApplication).graph
        graph.inject(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        historyRecyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        historyRecyclerView.setHasFixedSize(true)
        historyRecyclerView.layoutManager = LinearLayoutManager(fragmentContext)
        adapter = HistoryAdapter(activity, smsHistory.list)
        historyRecyclerView.adapter = adapter
        historyRecyclerView.addItemDecoration(HistoryAdapter.DividerItemDecoration(activity))
        historyRecyclerView.clearOnScrollListeners()
        historyRecyclerView.addOnScrollListener(OnScrollListener())
        scrollToBottom = true

        return view
    }

    override fun onPause() {
        super.onPause()
        smsSentSubscription.dispose()
    }

    override fun onResume() {
        super.onResume()
        update()
        smsSentSubscription = smsSender.smsSentListener().subscribe { update() }
    }

    private fun update() {
        adapter!!.notifyDataSetChanged()
        if (scrollToBottom) {
            historyRecyclerView.scrollToPosition(adapter!!.itemCount - 1)
        }
    }

    private inner class OnScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            if (recyclerView === historyRecyclerView) {
                scrollToBottom = recyclerView.computeVerticalScrollOffset() ==
                        recyclerView.computeVerticalScrollRange() - recyclerView.computeVerticalScrollExtent()
            }
        }
    }

    companion object {

        fun newInstance(): HistoryFragment {
            return HistoryFragment()
        }
    }
}
