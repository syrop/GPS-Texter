package pl.org.seva.texter.application;

import pl.org.seva.texter.dagger.DaggerMockGraph;
import pl.org.seva.texter.dagger.Graph;

public class MockTexterApplication extends TexterApplication {

    @Override
    protected Graph createGraph() {
        return DaggerMockGraph.create();
    }
}
