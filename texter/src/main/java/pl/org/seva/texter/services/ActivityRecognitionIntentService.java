package pl.org.seva.texter.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import pl.org.seva.texter.managers.ActivityRecognitionManager;

public class ActivityRecognitionIntentService extends IntentService {

    private final Handler handler;

    public ActivityRecognitionIntentService() {
        super(ActivityRecognitionIntentService.class.getSimpleName());
        handler = new Handler();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
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
        handler.post(() -> ActivityRecognitionManager.getInstance().stationary());
    }

    private void onDeviceMoving() {
        handler.post(() -> ActivityRecognitionManager.getInstance().moving());
    }
}
