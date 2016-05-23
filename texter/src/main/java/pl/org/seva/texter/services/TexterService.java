package pl.org.seva.texter.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import pl.org.seva.texter.R;
import pl.org.seva.texter.activities.MainActivity;

/**
 * Created by wiktor on 5/17/16.
 */
public class TexterService extends Service{

    private static final int ONGOING_NOTIFICATION_ID = 1;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped.

        Intent mainActivityIntent = new Intent(this, MainActivity.class);

        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                mainActivityIntent,
                0);
        Notification n = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pIntent)
                .setAutoCancel(false).build();

        startForeground(ONGOING_NOTIFICATION_ID, n);

        return START_STICKY;
    }

}
