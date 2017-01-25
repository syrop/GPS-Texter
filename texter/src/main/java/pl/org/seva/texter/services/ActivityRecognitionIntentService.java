package pl.org.seva.texter.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import pl.org.seva.texter.managers.GpsManager;

public class ActivityRecognitionIntentService extends IntentService {

    public ActivityRecognitionIntentService() {
        super(ActivityRecognitionIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            if (result.getMostProbableActivity().getType() == DetectedActivity.STILL) {
                GpsManager.getInstance().pauseUpdates();
            }
            else {
                GpsManager.getInstance().resumeUpdates();
            }
        }
    }
}
