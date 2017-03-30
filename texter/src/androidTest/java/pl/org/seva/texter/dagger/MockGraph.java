package pl.org.seva.texter.dagger;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = { pl.org.seva.texter.dagger.MockTexterModule.class })
interface MockGraph extends Graph {
}
