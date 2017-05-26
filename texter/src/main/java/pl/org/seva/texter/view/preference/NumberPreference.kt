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

package pl.org.seva.texter.view.preference

import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.preference.DialogPreference
import android.preference.Preference
import android.util.AttributeSet
import android.view.View

import pl.org.seva.texter.R
import pl.org.seva.texter.view.fragment.PhoneNumberFragment

class NumberPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {

    private var numberFragment: PhoneNumberFragment? = null
    private lateinit var number: String

    init {
        dialogLayoutResource = R.layout.dialog_number
        dialogIcon = null
    }

    override fun onCreateDialogView(): View {
        val result = super.onCreateDialogView()
        numberFragment = (context as android.support.v4.app.FragmentActivity)
                .supportFragmentManager.findFragmentById(R.id.number_fragment) as PhoneNumberFragment
        numberFragment!!.setNumber(number)

        return result
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val myState = SavedState(superState)

        numberFragment?.let {
            number = it.toString()
            myState.number = number
            // If called after onSaveInstanceState, throws:
            // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
            (context as android.support.v4.app.FragmentActivity)
                    .supportFragmentManager.beginTransaction().remove(it).commit()
            numberFragment = null
        }

        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || state.javaClass != SavedState::class.java) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state)
            return
        }

        // Cast state to custom BaseSavedState and pass to superclass
        val myState = state as SavedState?
        number = myState!!.number

        super.onRestoreInstanceState(myState.superState)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return ""
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        number = if (restorePersistedValue) getPersistedString("") else defaultValue.toString()
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        numberFragment?.let {
            if (positiveResult) {
                number = it.toString()
                persistString(number)
            } else {
                number = getPersistedString("")
            }
            (context as android.support.v4.app.FragmentActivity)
                    .supportFragmentManager.beginTransaction().remove(it).commit()
        }
        super.onDialogClosed(positiveResult)
    }

    private class SavedState internal constructor(superState: Parcelable) :
            Preference.BaseSavedState(superState) {
        lateinit var number: String

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(number)
        }
    }
}
