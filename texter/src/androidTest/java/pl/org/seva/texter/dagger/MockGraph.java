package pl.org.seva.texter.dagger;

import javax.inject.Singleton;

import dagger.Component;
import pl.org.seva.texter.LocationTest;

@Singleton
@Component(modules = { pl.org.seva.texter.dagger.MockTexterModule.class })
public interface MockGraph extends Graph {
    void inject(LocationTest locationTest);
}
