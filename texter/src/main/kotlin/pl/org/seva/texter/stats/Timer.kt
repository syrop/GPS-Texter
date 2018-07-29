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
 * If you like this program, consider donating bitcoin: 36uxha7sy4mv6c9LdePKjGNmQe8eK16aX6
 */

package pl.org.seva.texter.stats

import android.arch.lifecycle.Lifecycle
import java.util.concurrent.TimeUnit

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import pl.org.seva.texter.main.instance
import pl.org.seva.texter.main.observe

val timer get() = instance<Timer>()

class Timer {

    var resetTime = System.currentTimeMillis()
    private var timerSubscription = Disposables.empty()
    private val timerSubject = PublishSubject.create<Any>()
    private var start = { reset() }

    private val interval = Observable.interval(1, TimeUnit.SECONDS, Schedulers.io())

    private fun createTimerSubscription() {
        timerSubscription.dispose()
        timerSubscription = interval.subscribe { timerSubject.onNext(0) }
    }

    fun reset() {
        start = {}
        resetTime = System.currentTimeMillis()
        createTimerSubscription()
    }

    fun addTimerListenerUi(lifecycle : Lifecycle, listener: () -> Unit) = lifecycle.observe { timerSubject
            .doOnSubscribe { start() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { listener() } }
}
