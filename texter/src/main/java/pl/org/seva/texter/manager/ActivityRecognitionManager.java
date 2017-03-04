package pl.org.seva.texter.manager;

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

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import pl.org.seva.texter.service.ActivityRecognitionIntentService;

public class ActivityRecognitionManager implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final long ACTIVITY_RECOGNITION_INTERVAL = 1000;  // [ms]

    private static final PublishSubject<Void> stationarySubject = PublishSubject.create();
    private static final PublishSubject<Void> movingSubject = PublishSubject.create();

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

    static void shutdown() {
        synchronized (ActivityRecognitionManager.class) {
            if (instance != null) {
                instance.instanceShutdown();
                instance = null;
            }
        }
    }

    private void instanceShutdown() {
        if (googleApiClient != null) {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(googleApiClient, pendingIntent);
            googleApiClient.disconnect();
        }
    }

    void init(Context context) {
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

    public Observable<Void> stationaryListener() {
        return stationarySubject.hide();
    }

    public Observable<Void> movingListener() {
        return movingSubject.hide();
    }

    public void stationary() {
        stationarySubject.onNext(null);
    }

    public void moving() {
        movingSubject.onNext(null);
    }
}
