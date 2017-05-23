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

package pl.org.seva.texter.view.layout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.widget.LinearLayout

internal class SlidingTabStrip(context: Context) : LinearLayout(context, null) {

    private val bottomBorderThickness: Int
    private val bottomBorderPaint: Paint

    private val selectedIndicatorThickness: Int
    private val selectedIndicatorPaint: Paint

    private var selectedPosition: Int = 0
    private var selectionOffset: Float = 0.toFloat()

    private lateinit var customTabColorizer: (Any) -> Int

    init {
        setWillNotDraw(false)

        val density = resources.displayMetrics.density

        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorForeground, outValue, true)
        val themeForegroundColor = outValue.data

        val defaultBottomBorderColor = setColorAlpha(themeForegroundColor)

        bottomBorderThickness = (DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS * density).toInt()
        bottomBorderPaint = Paint()
        bottomBorderPaint.color = defaultBottomBorderColor

        selectedIndicatorThickness = (SELECTED_INDICATOR_THICKNESS_DIPS * density).toInt()
        selectedIndicatorPaint = Paint()
    }

    fun setCustomTabColorizer(customTabColorizer: (Any) -> Int) {
        this.customTabColorizer = customTabColorizer
        invalidate()
    }

    fun onViewPagerPageChanged(position: Int, positionOffset: Float) {
        selectedPosition = position
        selectionOffset = positionOffset
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val height = height
        val childCount = childCount
        val tabColorizer = customTabColorizer

        // Thick colored underline below the current selection
        if (childCount > 0) {
            val selectedTitle = getChildAt(selectedPosition)
            var left = selectedTitle.left
            var right = selectedTitle.right
            var color = tabColorizer.invoke(selectedPosition)

            if (selectionOffset > 0f && selectedPosition < getChildCount() - 1) {
                val nextColor = tabColorizer.invoke(selectedPosition + 1)
                if (color != nextColor) {
                    color = blendColors(nextColor, color, selectionOffset)
                }

                // Draw the selection partway between the tabs
                val nextTitle = getChildAt(selectedPosition + 1)
                left = (selectionOffset * nextTitle.left + (1.0f - selectionOffset) * left).toInt()
                right = (selectionOffset * nextTitle.right + (1.0f - selectionOffset) * right).toInt()
            }

            selectedIndicatorPaint.color = color

            canvas.drawRect(left.toFloat(),
                    (height - selectedIndicatorThickness).toFloat(),
                    right.toFloat(),
                    height.toFloat(), selectedIndicatorPaint)
        }

        // Thin underline along the entire bottom edge
        canvas.drawRect(0f, (height - bottomBorderThickness).toFloat(), width.toFloat(), height.toFloat(), bottomBorderPaint)
    }

    companion object {

        private val DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS = 0
        private val DEFAULT_BOTTOM_BORDER_COLOR_ALPHA: Byte = 0x26
        private val SELECTED_INDICATOR_THICKNESS_DIPS = 3
        private val DEFAULT_SELECTED_INDICATOR_COLOR = 0xFF33B5E5.toInt()

        /**
         * Set the alpha value of the `color` to be the given `alpha` value.
         */
        private fun setColorAlpha(color: Int): Int {
            return Color.argb(
                    DEFAULT_BOTTOM_BORDER_COLOR_ALPHA.toInt(),
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color))
        }

        /**
         * Blend `color1` and `color2` using the given ratio.

         * @param ratio of which to blend. 1.0 will return `color1`, 0.5 will give an even blend,
         * *              0.0 will return `color2`.
         */
        private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
            val inverseRation = 1f - ratio
            val r = Color.red(color1) * ratio + Color.red(color2) * inverseRation
            val g = Color.green(color1) * ratio + Color.green(color2) * inverseRation
            val b = Color.blue(color1) * ratio + Color.blue(color2) * inverseRation
            return Color.rgb(r.toInt(), g.toInt(), b.toInt())
        }
    }
}


