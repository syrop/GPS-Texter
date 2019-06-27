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

package pl.org.seva.texter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import pl.org.seva.texter.dagger.MockTexterComponent;
import pl.org.seva.texter.mockimplementations.FakeSmsSender;
import pl.org.seva.texter.view.activity.MainActivity;
import pl.org.seva.texter.presenter.utils.SmsSender;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static pl.org.seva.texter.action.DelayAction.delay;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LocationTest {

    private static final int DURATION_IN_SECONDS = 50;

    @SuppressWarnings({"CanBeFinal", "WeakerAccess"})
    @Inject SmsSender smsSender;

    @Rule
    public final ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(
            MainActivity.class,
            true,
            true);

    @Before
    public void setUp() {
        MockTexterComponent graph = (MockTexterComponent) ((TexterApplication) activityRule.getActivity().getApplication()).getComponent();
        graph.inject(this);
    }

    @Test
    public void testLocation() {
        onView(isRoot()).perform(delay(100));
        for (int i = 0; i <= DURATION_IN_SECONDS; i++) {
            onView(isRoot()).perform(delay(1000));
        }
        assertTrue(((FakeSmsSender) smsSender).getMessagesSent() >= TestConstants.EXPECTED_MESSAGES_SENT);
    }
}
