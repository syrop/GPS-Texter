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

package pl.org.seva.texter.mockmanager;

import android.location.Location;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import pl.org.seva.texter.TestConstants;
import pl.org.seva.texter.presenter.manager.GpsManager;
import pl.org.seva.texter.presenter.manager.TimerManager;
import pl.org.seva.texter.presenter.preference.HomeLocationPreference;
import pl.org.seva.texter.presenter.utils.Constants;

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

    @Override
    public Observable<Object> providerEnabledListener() {
        return Observable.just(0);
    }

    @Override
    public Observable<Object> providerDisabledListener() {
        return Observable.empty();
    }
}
