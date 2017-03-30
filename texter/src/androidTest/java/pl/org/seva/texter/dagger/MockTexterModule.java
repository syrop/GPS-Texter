package pl.org.seva.texter.dagger;

import org.mockito.Mockito;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import pl.org.seva.texter.manager.GpsManager;

@Module
public class MockTexterModule {

    @Provides
    @Singleton
    GpsManager provideGpsManager() {
        return Mockito.mock(GpsManager.class);
    }
}
