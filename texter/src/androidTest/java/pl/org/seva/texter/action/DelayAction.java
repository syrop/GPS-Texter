package pl.org.seva.texter.action;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;

import org.hamcrest.Matcher;

import static android.support.test.espresso.matcher.ViewMatchers.isRoot;

public class DelayAction implements ViewAction {

    private long millis;

    public static ViewAction delay(long millis) {
        return new DelayAction(millis);
    }

    private DelayAction(long millis) {
        this.millis = millis;
    }
    @Override
    public Matcher<View> getConstraints() {
        return isRoot();
    }

    @Override
    public String getDescription() {
        return "wait " + millis + " milliseconds";
    }

    @Override
    public void perform(UiController uiController, View view) {
        uiController.loopMainThreadUntilIdle();
        uiController.loopMainThreadForAtLeast(millis);
    }
}
