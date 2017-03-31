package pl.org.seva.texter.mockmanager;

import android.app.Activity;
import android.app.PendingIntent;

import javax.inject.Singleton;

import pl.org.seva.texter.manager.GpsManager;
import pl.org.seva.texter.manager.HistoryManager;
import pl.org.seva.texter.manager.SmsManager;

@Singleton
public class MockSmsManager extends SmsManager {

    public MockSmsManager(GpsManager gpsManager, HistoryManager historyManager) {
        this.gpsManager = gpsManager;
        this.historyManager = historyManager;
    }

    protected void sendTextMessage(
            String text,
            PendingIntent sentIntent,
            PendingIntent deliveredIntent) throws SecurityException {
        try {
            sentIntent.send(Activity.RESULT_OK);

        }
        catch (PendingIntent.CanceledException ex) {
            ex.printStackTrace();
        }
    }
}
