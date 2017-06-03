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

package pl.org.seva.texter.presenter.utils

import java.util.concurrent.TimeUnit

import javax.inject.Inject
import javax.inject.Singleton

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

@Singleton
class Timer @Inject
internal constructor() {

    var resetTime = System.currentTimeMillis()
        private set
    private var timerSubscription = Disposables.empty()
    private val timerSubject = PublishSubject.create<Any>()

    init {
        createTimerSubscription()
    }

    private fun createTimerSubscription() {
        timerSubscription.dispose()
        timerSubscription = Observable.timer(1, TimeUnit.SECONDS, Schedulers.computation())
                .doOnNext { timerSubject.onNext(0) }
                .repeat()
                .subscribe()
    }

    fun reset() {
        resetTime = System.currentTimeMillis()
        createTimerSubscription()
    }

    fun addTimerListenerUi(listener: () -> Unit): Disposable {
        return timerSubject.observeOn(AndroidSchedulers.mainThread()).subscribe { listener() }
    }
}
