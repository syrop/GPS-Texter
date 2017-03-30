package pl.org.seva.texter.dagger;

import com.google.android.gms.maps.model.LatLng;

import org.mockito.Mockito;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.subjects.PublishSubject;
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

    private void mockReturnValues(GpsManager gpsManager) {
        String defaultHomeLocation = Constants.DEFAULT_HOME_LOCATION;
        double homeLat = HomeLocationPreference.parseLatitude(defaultHomeLocation);
        double homeLon = HomeLocationPreference.parseLongitude(defaultHomeLocation);

        Mockito.when(gpsManager.getHomeLatLng()).thenReturn(new LatLng(homeLat, homeLon));
        Mockito.when(gpsManager.providerEnabledListener()).thenReturn(PublishSubject.create());
        Mockito.when(gpsManager.providerDisabledListener()).thenReturn(PublishSubject.create());
    }
}
