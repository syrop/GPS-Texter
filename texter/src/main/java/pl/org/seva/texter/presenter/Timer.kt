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

package pl.org.seva.texter.presenter

import android.arch.lifecycle.Lifecycle
import java.util.concurrent.TimeUnit

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import pl.org.seva.texter.source.LiveSource

class Timer: LiveSource() {

    var resetTime = System.currentTimeMillis()
    private var timerSubscription = Disposables.empty()
    private val timerSubject = PublishSubject.create<Any>()
    private var start = { reset() }

    private fun createTimerSubscription() {
        timerSubscription.dispose()
        timerSubscription = Observable.timer(1, TimeUnit.SECONDS, Schedulers.io())
                .doOnNext { timerSubject.onNext(0) }
                .repeat()
                .subscribe()
    }

    fun reset() {
        start = {}
        resetTime = System.currentTimeMillis()
        createTimerSubscription()
    }

    fun addTimerListenerUi(lifecycle : Lifecycle, listener: () -> Unit) {
        lifecycle.observe { timerSubject
                .doOnSubscribe { start() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { listener() } }
    }
}
