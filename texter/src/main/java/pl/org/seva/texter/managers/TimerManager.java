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

package pl.org.seva.texter.managers;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

public class TimerManager {

    private static TimerManager instance;

    private long resetTime = System.currentTimeMillis();
    private Subscription timerSubscription = Subscriptions.empty();
    private final PublishSubject<Void> timerSubject = PublishSubject.create();

    private TimerManager() {
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
        timerSubscription.unsubscribe();
    }

    private void createTimerSubscription() {
        timerSubscription.unsubscribe();
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
        return timerSubject;
    }
}
