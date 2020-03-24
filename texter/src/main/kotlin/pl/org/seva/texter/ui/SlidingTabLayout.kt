/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.org.seva.texter.ui

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.viewpager.widget.ViewPager
import android.util.AttributeSet
import android.util.SparseArray
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

import pl.org.seva.texter.R

/**
 * To be used with ViewPager to provide a tab indicator component which give constant feedback as to
 * the user's scroll progress.
 *
 *
 * To use the component, simply add it to your view hierarchy. Then in your
 * [android.app.Activity] or [android.support.v4.app.Fragment] call
 * [.setViewPager] providing it the ViewPager this layout is being used for.
 */
class SlidingTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : HorizontalScrollView(context, attrs, defStyle) {

    private val mTitleOffset: Int

    private var mDistributeEvenly: Boolean = false

    private lateinit var viewPager: ViewPager
    private val mContentDescriptions = SparseArray<String>()

    private val mTabStrip: SlidingTabStrip

    init {
        // Disable the Scroll Bar
        isHorizontalScrollBarEnabled = false
        // Make sure that the Tab Strips fills this View
        isFillViewport = true

        mTitleOffset = (TITLE_OFFSET_DIPS * resources.displayMetrics.density).toInt()

        mTabStrip = SlidingTabStrip(context)
        addView(mTabStrip, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    fun setCustomTabColorizer(tabColorizer: (Any) -> Int) = mTabStrip.setCustomTabColorizer(tabColorizer)

    fun setDistributeEvenly() {
        mDistributeEvenly = true
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content
     * (number of tabs and tab titles) does not change after this call has been made.
     */
    fun setViewPager(viewPager: ViewPager) {
        mTabStrip.removeAllViews()

        this.viewPager = viewPager
        viewPager.clearOnPageChangeListeners()
        viewPager.addOnPageChangeListener(InternalViewPagerListener())
        populateTabStrip()
        scrollToTab(viewPager.currentItem, 0)
    }

    /**
     * Create a default view to be used for tabs.
     */
    private fun createDefaultTabView(context: Context): TextView {
        val textView = TextView(context)
        textView.gravity = Gravity.CENTER
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP.toFloat())
        textView.typeface = Typeface.DEFAULT_BOLD
        textView.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val outValue = TypedValue()
        getContext().theme.resolveAttribute(
                android.R.attr.selectableItemBackground,
                outValue,
                true)
        textView.setBackgroundResource(outValue.resourceId)
        textView.setSingleLine()
        textView.setAllCaps(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textView.setTextColor(resources.getColorStateList(R.color.selector, context.theme))
        } else {
            @Suppress("DEPRECATION")
            textView.setTextColor(resources.getColorStateList(R.color.selector))
        }
        textView.textSize = 14f

        val padding = (TAB_VIEW_PADDING_DIPS * resources.displayMetrics.density).toInt()
        textView.setPadding(padding, padding, padding, padding)

        return textView
    }

    private fun populateTabStrip() {
        val adapter = viewPager.adapter
        val tabClickListener = OnClickListener { this.onTabClicked(it) }

        repeat (checkNotNull(adapter).count) {
            var tabTitleView: TextView? = null

            val tabView = createDefaultTabView(context)
            if (TextView::class.java.isInstance(tabView)) {
                tabTitleView = tabView
            }

            if (mDistributeEvenly) {
                val lp = tabView.layoutParams as LinearLayout.LayoutParams
                lp.width = 0
                lp.weight = 1f
            }
            tabTitleView?.text = adapter.getPageTitle(it)
            tabView.setOnClickListener(tabClickListener)
            val desc = mContentDescriptions.get(it, null)
            desc?.let {
                tabView.contentDescription = it
            }

            mTabStrip.addView(tabView)
            if (it == viewPager.currentItem) {
                tabView.isSelected = true
            }
        }
    }

    private fun scrollToTab(tabIndex: Int, positionOffset: Int) {
        val tabStripChildCount = mTabStrip.childCount
        if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
            return
        }

        val selectedChild = mTabStrip.getChildAt(tabIndex)
        var targetScrollX = selectedChild.left + positionOffset

        if (tabIndex > 0 || positionOffset > 0) {
            // If we're not at the first child and are mid-scroll, make sure we obey the offset
            targetScrollX -= mTitleOffset
        }

        scrollTo(targetScrollX, 0)
    }

    private inner class InternalViewPagerListener : ViewPager.OnPageChangeListener {
        private var mScrollState: Int = 0

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            val tabStripChildCount = mTabStrip.childCount
            if (tabStripChildCount == 0 || position < 0 || position >= tabStripChildCount) {
                return
            }

            mTabStrip.onViewPagerPageChanged(position, positionOffset)

            val selectedTitle = mTabStrip.getChildAt(position)
            val extraOffset = if (selectedTitle != null)
                (positionOffset * selectedTitle.width).toInt()
            else
                0
            scrollToTab(position, extraOffset)
        }

        override fun onPageScrollStateChanged(state: Int) {
            mScrollState = state
        }

        override fun onPageSelected(position: Int) {
            if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
                mTabStrip.onViewPagerPageChanged(position, 0f)
                scrollToTab(position, 0)
            }
            repeat (mTabStrip.childCount) {
                mTabStrip.getChildAt(it).isSelected = position == it
            }
        }
    }

    private fun onTabClicked(v: View) = repeat (mTabStrip.childCount) {
        if (v === mTabStrip.getChildAt(it)) {
            viewPager.currentItem = it
            return
        }
    }

    companion object {
        private const val TITLE_OFFSET_DIPS = 24
        private const val TAB_VIEW_PADDING_DIPS = 16
        private const val TAB_VIEW_TEXT_SIZE_SP = 12
    }
}
