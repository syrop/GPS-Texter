package pl.org.seva.texter.dagger;

import com.google.android.gms.maps.model.LatLng;

import org.mockito.Mockito;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.subjects.PublishSubject;
import pl.org.seva.texter.TestConstants;
import pl.org.seva.texter.manager.ActivityRecognitionManager;
import pl.org.seva.texter.manager.GpsManager;
import pl.org.seva.texter.preference.HomeLocationPreference;
import pl.org.seva.texter.utils.Constants;

@Module
class MockTexterModule {

    @Provides
    @Singleton
    GpsManager provideGpsManager() {
        GpsManager result = Mockito.mock(GpsManager.class);
        mockReturnValues(result);
        return result;
    }

    @Provides
    @Singleton
    ActivityRecognitionManager provideActivityRecognitionManager() {
        ActivityRecognitionManager result = Mockito.mock(ActivityRecognitionManager.class);
        mockReturnValues(result);
        return result;
    }

    private void mockReturnValues(GpsManager gpsManager) {
        String defaultHomeLocation = Constants.DEFAULT_HOME_LOCATION;
        double homeLat = HomeLocationPreference.parseLatitude(defaultHomeLocation);
        double homeLon = HomeLocationPreference.parseLongitude(defaultHomeLocation);

        Mockito.when(gpsManager.getHomeLatLng()).thenReturn(new LatLng(homeLat, homeLon));
        Mockito.when(gpsManager.providerEnabledListener()).thenReturn(PublishSubject.create().hide());
        Mockito.when(gpsManager.providerDisabledListener()).thenReturn(PublishSubject.create().hide());
        Mockito.when(gpsManager.homeChangedListener()).thenReturn(PublishSubject.create().hide());
        Mockito.when(gpsManager.distanceChangedListener()).thenReturn(PublishSubject.create().hide());
        Mockito.when(gpsManager.getDistance()).thenReturn(TestConstants.DISTANCE);
    }

    private void mockReturnValues(ActivityRecognitionManager activityRecognitionManager) {
        Mockito.when(activityRecognitionManager.stationaryListener()).thenReturn(PublishSubject.create().hide());
        Mockito.when(activityRecognitionManager.movingListener()).thenReturn(PublishSubject.create().hide());
    }
}
