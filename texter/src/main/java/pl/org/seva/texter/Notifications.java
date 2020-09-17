package pl.org.seva.texter;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

public class Notifications {

    private static final String NOTIFICATION_CHANNEL_ID = "notifications";

    public static final String NOTIFICATION_CHANNEL_NAME = "notifications";

    static void createNotificationChannels(Context ctx) {
        final NotificationManager notificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setSound(null, null);
        notificationManager.createNotificationChannel(channel);
    }
}
