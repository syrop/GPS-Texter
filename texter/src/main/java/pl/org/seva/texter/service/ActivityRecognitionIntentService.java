/*
 * Copyright (C) 2017 Wiktor Nizio
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
