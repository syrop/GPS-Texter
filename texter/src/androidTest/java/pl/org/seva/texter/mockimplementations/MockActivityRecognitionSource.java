package pl.org.seva.texter.mockimplementations;


import org.jetbrains.annotations.NotNull;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import pl.org.seva.texter.presenter.source.ActivityRecognitionSource;

public class MockActivityRecognitionSource extends ActivityRecognitionSource {

    @NotNull
    @Override
    public Observable<Object> stationaryListener() {
        return PublishSubject.empty();
    }

    @NotNull
    @Override
    public Observable<Object> movingListener() {
        return PublishSubject.empty();
    }
}

