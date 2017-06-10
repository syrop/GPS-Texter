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

package pl.org.seva.texter.presenter.source

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.Disposable

open class LiveSource protected constructor() {

    fun Lifecycle.observe(disposable: Disposable) {
        addObserver(RxLifecycleObserver(disposable))
    }

    @Suppress("unused")
    private class RxLifecycleObserver(val disposable: Disposable) : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY)
        private fun dispose() = disposable.dispose()
    }
}