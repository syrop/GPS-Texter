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

package pl.org.seva.texter.dagger;

import org.mockito.Mockito;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.subjects.PublishSubject;
import pl.org.seva.texter.mockimplementations.FakeSmsSender;
import pl.org.seva.texter.presenter.source.ActivityRecognitionSource;
import pl.org.seva.texter.presenter.source.LocationSource;
import pl.org.seva.texter.presenter.utils.SmsSender;
import pl.org.seva.texter.presenter.utils.SmsCache;
import pl.org.seva.texter.mockimplementations.FakeLocationSource;
import pl.org.seva.texter.presenter.utils.Timer;
import pl.org.seva.texter.presenter.utils.ZoneCalculator;

@Module
class MockTexterModule {

    @Provides
    @Singleton
    LocationSource provideGpsManager(Timer timer) {
        return new FakeLocationSource(timer);
    }

    @Provides
    @Singleton
    SmsSender provideSmsManager(LocationSource locationSource, SmsCache smsCache, ZoneCalculator zoneCalculator) {
        return new FakeSmsSender(locationSource, smsCache, zoneCalculator);
    }

    @Provides
    @Singleton
    ActivityRecognitionSource provideActivityRecognitionManager() {
        ActivityRecognitionSource result = Mockito.mock(ActivityRecognitionSource.class);
        mockReturnValues(result);
        return result;
    }

    private void mockReturnValues(ActivityRecognitionSource activityRecognitionSource) {
        Mockito.when(activityRecognitionSource.stationaryListener()).thenReturn(PublishSubject.empty());
        Mockito.when(activityRecognitionSource.movingListener()).thenReturn(PublishSubject.empty());
    }
}
