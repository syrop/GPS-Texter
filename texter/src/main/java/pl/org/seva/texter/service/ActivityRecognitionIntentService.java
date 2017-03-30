package pl.org.seva.texter.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import javax.inject.Inject;

import pl.org.seva.texter.application.TexterApplication;
import pl.org.seva.texter.manager.ActivityRecognitionManager;

public class ActivityRecognitionIntentService extends IntentService {

    @Inject ActivityRecognitionManager activityRecognitionManager;

    private final Handler handler;

    public ActivityRecognitionIntentService() {
        super(ActivityRecognitionIntentService.class.getSimpleName());
        handler = new Handler();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        ((TexterApplication) getApplication()).getGraph().inject(this);
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            if (result.getMostProbableActivity().getType() == DetectedActivity.STILL) {
                onDeviceStationary();
            }
            else {
                onDeviceMoving();
            }
        }
    }

    private void onDeviceStationary() {
        handler.post(() -> activityRecognitionManager.stationary());
    }

    private void onDeviceMoving() {
        handler.post(() -> activityRecognitionManager.moving());
    }
}
