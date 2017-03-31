package pl.org.seva.texter.dagger;

import org.mockito.Mockito;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.subjects.PublishSubject;
import pl.org.seva.texter.MockGpsManager;
import pl.org.seva.texter.manager.ActivityRecognitionManager;
import pl.org.seva.texter.manager.GpsManager;
import pl.org.seva.texter.manager.TimerManager;

@Module
class MockTexterModule {

    @Provides
    @Singleton
    GpsManager provideGpsManager(TimerManager timerManager) {
        return new MockGpsManager(timerManager);
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
