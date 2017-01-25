package pl.org.seva.texter.managers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

import java.lang.ref.WeakReference;

import pl.org.seva.texter.services.ActivityRecognitionIntentService;

public class ActivityRecognitionManager implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final long ACTIVITY_RECOGNITION_INTERVAL = 5000;

    private boolean initialized;
    private GoogleApiClient googleApiClient;
    private WeakReference<Context> weakContext;
    private PendingIntent pendingIntent;

    private static ActivityRecognitionManager instance;

    public static ActivityRecognitionManager getInstance() {
        if (instance == null) {
            synchronized (ActivityRecognitionManager.class) {
                if (instance == null) {
                    instance = new ActivityRecognitionManager();
                }
            }
        }
        return instance;
    }

    public static void shutdown(Context context) {
        synchronized (ActivityRecognitionManager.class) {
            if (instance != null) {
                instance.instanceShutdown();
                instance = null;
            }
        }
    }

    private void instanceShutdown() {
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(googleApiClient, pendingIntent);
        googleApiClient.disconnect();
    }

    public void init(Context context) {
        if (initialized) {
            return;
        }
        weakContext = new WeakReference<>(context);
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            googleApiClient.connect();
        }

        initialized = true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Context context = weakContext.get();
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, ActivityRecognitionIntentService.class);
        pendingIntent = PendingIntent.getService(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                googleApiClient,
                ACTIVITY_RECOGNITION_INTERVAL,
                pendingIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
