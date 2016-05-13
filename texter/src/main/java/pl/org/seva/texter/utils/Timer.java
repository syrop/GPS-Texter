package pl.org.seva.texter.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wiktor on 08.09.15.
 */
public class Timer extends Thread {

    private static final Timer INSTANCE = new Timer();

    private boolean ended;

    private List<TimerListener> listeners = new ArrayList<>();

    private Timer() {
    }

    public static Timer getInstance() {
        return INSTANCE;
    }

    public void run() {
        try {
            while (!ended) {
                synchronized (INSTANCE) {
                    INSTANCE.wait(1000);
                }
                for (TimerListener listener : listeners) {
                    listener.onTimer();
                }
            }

        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void reset() {
        synchronized (INSTANCE) {
            INSTANCE.notify();
        }
    }

    public boolean addListener(TimerListener listener) {
        return listeners.add(listener);
    }

    public boolean removeListener(TimerListener listener) {
        return listeners.remove(listener);
    }

    public void end() {
        ended = true;
        reset();
    }

    public interface TimerListener {
        void onTimer();
    }
}
