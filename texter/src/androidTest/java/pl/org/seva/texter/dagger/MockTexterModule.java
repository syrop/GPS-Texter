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

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import pl.org.seva.texter.mockimplementations.MockSmsSender;
import pl.org.seva.texter.presenter.source.LocationSource;
import pl.org.seva.texter.presenter.utils.SmsSender;
import pl.org.seva.texter.presenter.utils.SmsHistory;
import pl.org.seva.texter.mockimplementations.MockLocationSource;
import pl.org.seva.texter.presenter.utils.Timer;
import pl.org.seva.texter.presenter.utils.ZoneCalculator;

@Module
class MockTexterModule {

    @Provides
    @Singleton
    LocationSource provideGpsManager(Timer timer) {
        return new MockLocationSource(timer);
    }

    @Provides
    @Singleton
    SmsSender provideSmsManager(LocationSource locationSource, SmsHistory smsHistory, ZoneCalculator zoneCalculator) {
        return new MockSmsSender(locationSource, smsHistory, zoneCalculator);
    }
}
