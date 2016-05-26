package pl.org.seva.texter.managers;

import java.util.ArrayList;
import java.util.List;

import pl.org.seva.texter.listeners.ITimerListener;

/**
 * Created by wiktor on 08.09.15.
 */
public class TimerManager extends Thread {

    private static TimerManager instance = new TimerManager();

    private long resetTime;

    private final List<ITimerListener> listeners = new ArrayList<>();

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
        synchronized (TimerManager.class) {
            instance = null;
        }
    }

    public void run() {
        try {
            synchronized (listeners) {
            //noinspection InfiniteLoopStatement
                while (true) {
                    listeners.wait(1000);
                    for (ITimerListener listener : listeners) {
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

    public boolean addListener(ITimerListener listener) {
        synchronized (listeners) {
            return listeners.add(listener);
        }
    }

    public boolean removeListener(ITimerListener listener) {
        synchronized (listeners) {
            return listeners.remove(listener);
        }
    }
}
