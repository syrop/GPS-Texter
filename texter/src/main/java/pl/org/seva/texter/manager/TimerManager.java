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

package pl.org.seva.texter.manager;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class TimerManager {

    private static TimerManager instance;

    private long resetTime = System.currentTimeMillis();
    private Disposable timerSubscription = Disposables.empty();
    private final PublishSubject<Void> timerSubject = PublishSubject.create();

    private TimerManager() {
        createTimerSubscription();
    }

    public static TimerManager getInstance() {
        if (instance == null) {
            synchronized (TimerManager.class) {
                if (instance == null) {
                    instance = new TimerManager();
                }
            }
        }
        return instance;
    }

    public static void shutdown() {
        if (instance != null) {
            instance.instanceShutdown();
            instance = null;
        }
    }

    private void instanceShutdown() {
        timerSubscription.dispose();
    }

    private void createTimerSubscription() {
        timerSubscription.dispose();
        timerSubscription = Observable.timer(1, TimeUnit.SECONDS, Schedulers.computation())
                .observeOn(Schedulers.io())
                .doOnNext(ignore -> timerSubject.onNext(null))
                .repeat()
                .subscribe();
    }

    public void reset() {
        resetTime = System.currentTimeMillis();
        createTimerSubscription();
    }

    public long getResetTime() {
        return resetTime;
    }

    public Observable<Void> timerListener() {
        return timerSubject.hide();
    }
}
