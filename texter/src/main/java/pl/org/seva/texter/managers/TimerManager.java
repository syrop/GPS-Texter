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

import java.util.ArrayList;
import java.util.List;

import pl.org.seva.texter.listeners.TimerListener;

public class TimerManager extends Thread {

    private static TimerManager instance;

    private long resetTime = System.currentTimeMillis();

    private final List<TimerListener> listeners = new ArrayList<>();

    private boolean running = true;

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
        instance.end();
        synchronized (TimerManager.class) {
            instance = null;
        }
    }

    private void end() {
        synchronized (listeners) {
            running = false;
            listeners.notify();
        }
    }

    public void run() {
        try {
            synchronized (listeners) {
                while (running) {
                    listeners.wait(1000);
                    //noinspection Convert2streamapi
                    for (TimerListener listener : listeners) {
                        listener.onTimer();
                    }
                }
            }
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void reset() {
        resetTime = System.currentTimeMillis();
        synchronized (listeners) {
            listeners.notify();
        }
    }

    public long getResetTime() {
        return resetTime;
    }

    public void addListener(TimerListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(TimerListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
}
