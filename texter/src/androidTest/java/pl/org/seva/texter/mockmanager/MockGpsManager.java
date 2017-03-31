package pl.org.seva.texter.mockmanager;

import android.location.Location;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import pl.org.seva.texter.TestConstants;
import pl.org.seva.texter.manager.GpsManager;
import pl.org.seva.texter.manager.TimerManager;
import pl.org.seva.texter.preference.HomeLocationPreference;
import pl.org.seva.texter.utils.Constants;

@Singleton
public class MockGpsManager extends GpsManager {

    private static final String MOCK_PROVIDER_NAME = "Mock provider";

    private int ticks = -1;

    public MockGpsManager(TimerManager timerManager) {
        this.timerManager = timerManager;
        String defaultHomeLocation = Constants.DEFAULT_HOME_LOCATION;
        homeLat = HomeLocationPreference.parseLatitude(defaultHomeLocation);
        homeLon = HomeLocationPreference.parseLongitude(defaultHomeLocation);

        Observable.timer(1, TimeUnit.SECONDS, Schedulers.computation())
                .doOnNext(__ -> {
                    Location location = new Location(MOCK_PROVIDER_NAME);
                    location.setAccuracy(1.0f);
                    location.setLatitude(getHomeLatLng().latitude + ticks * TestConstants.LATITUDE_STEP);
                    location.setLongitude(getHomeLatLng().longitude);
                    location.setTime(System.currentTimeMillis());
                    onLocationChanged(location);
                    ticks++;
                })
                .repeat()
                .subscribe();
    }
}
