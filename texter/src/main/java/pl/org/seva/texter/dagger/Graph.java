package pl.org.seva.texter.dagger;

import javax.inject.Singleton;

import dagger.Component;
import pl.org.seva.texter.controller.SmsController;
import pl.org.seva.texter.manager.ActivityRecognitionManager;
import pl.org.seva.texter.manager.GpsManager;
import pl.org.seva.texter.manager.HistoryManager;
import pl.org.seva.texter.manager.LastLocationManager;
import pl.org.seva.texter.manager.PermissionsManager;
import pl.org.seva.texter.manager.SmsManager;
import pl.org.seva.texter.manager.TimerManager;

@Singleton
@Component(modules = { pl.org.seva.texter.dagger.TexterModule.class })
public interface Graph {
    ActivityRecognitionManager activityRecognitionManager();
    GpsManager gpsManager();
    HistoryManager historyManager();
    PermissionsManager permissionsManager();
    SmsManager smsManager();
    TimerManager timerManager();
    SmsController smsController();
    LastLocationManager lastLocationManager();
}
