package pl.org.seva.texter.dagger;

import org.mockito.Mockito;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.subjects.PublishSubject;
import pl.org.seva.texter.manager.HistoryManager;
import pl.org.seva.texter.manager.SmsManager;
import pl.org.seva.texter.mockmanager.MockGpsManager;
import pl.org.seva.texter.manager.ActivityRecognitionManager;
import pl.org.seva.texter.manager.GpsManager;
import pl.org.seva.texter.manager.TimerManager;
import pl.org.seva.texter.mockmanager.MockSmsManager;

@Module
class MockTexterModule {

    @Provides
    @Singleton
    GpsManager provideGpsManager(TimerManager timerManager) {
        return new MockGpsManager(timerManager);
    }

    @Provides
    @Singleton
    SmsManager provideSmsManager(GpsManager gpsManager, HistoryManager historyManager) {
        return new MockSmsManager(gpsManager, historyManager);
    }

    @Provides
    @Singleton
    ActivityRecognitionManager provideActivityRecognitionManager() {
        ActivityRecognitionManager result = Mockito.mock(ActivityRecognitionManager.class);
        mockReturnValues(result);
        return result;
    }

    private void mockReturnValues(ActivityRecognitionManager activityRecognitionManager) {
        Mockito.when(activityRecognitionManager.stationaryListener()).thenReturn(PublishSubject.create().hide());
        Mockito.when(activityRecognitionManager.movingListener()).thenReturn(PublishSubject.create().hide());
    }
}
