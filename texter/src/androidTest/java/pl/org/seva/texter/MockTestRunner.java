package pl.org.seva.texter;

import android.app.Application;
import android.content.Context;
import android.support.test.runner.AndroidJUnitRunner;

import pl.org.seva.texter.application.MockTexterApplication;

public class MockTestRunner extends AndroidJUnitRunner {
    @Override
    public Application newApplication(
            ClassLoader cl, String className, Context context)
            throws InstantiationException,
            IllegalAccessException,
            ClassNotFoundException {
        return super.newApplication(cl, MockTexterApplication.class.getName(), context);
    }
}
