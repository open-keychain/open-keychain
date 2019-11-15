/*
 * Copyright (C) 2015 Vincent Breitmoser <look@my.amazin.horse>
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

package org.sufficientlysecure.keychain.ui.widget;


import android.app.Activity;
import android.content.Intent;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.ActivityTestRule;
import android.view.KeyEvent;
import android.widget.AdapterView;

import org.junit.Rule;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.EncryptTextActivity;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.allOf;
import static org.sufficientlysecure.keychain.AndroidTestHelpers.importKeysFromResource;
import static org.sufficientlysecure.keychain.actions.CustomActions.tokenEncryptViewAddToken;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withKeyItemId;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withKeyToken;


//TODO This test is disabled because it needs to be fixed to work with updated code
//@RunWith(AndroidJUnit4.class)
//@LargeTest
public class EncryptKeyCompletionViewTest {

    @Rule
    public final ActivityTestRule<EncryptTextActivity> mActivity
            = new ActivityTestRule<>(EncryptTextActivity.class);

    //@Test
    public void testTextEncryptDecryptFromToken() throws Exception {

        Intent intent = new Intent();
        intent.putExtra(EncryptTextActivity.EXTRA_ENCRYPTION_KEY_IDS, new long[]{0x9D604D2F310716A3L});
        Activity activity = mActivity.launchActivity(intent);

        // import these two, make sure they're there
        importKeysFromResource(activity, "x.sec.asc");

        // check if the element passed in from intent
        onView(ViewMatchers.withId(R.id.recipient_list)).check(matches(withKeyToken(0x9D604D2F310716A3L)));
        onView(withId(R.id.recipient_list)).perform(ViewActions.pressKey(KeyEvent.KEYCODE_DEL));

        // type X, select from list, check if it's there
        onView(withId(R.id.recipient_list)).perform(typeText("x"));
        onData(withKeyItemId(0x9D604D2F310716A3L)).inRoot(RootMatchers.isPlatformPopup())
                .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
                        hasDescendant(withId(R.id.key_list_item_name)))).perform(click());
        onView(withId(R.id.recipient_list)).check(matches(withKeyToken(0x9D604D2F310716A3L)));
        onView(withId(R.id.recipient_list)).perform(ViewActions.pressKey(KeyEvent.KEYCODE_DEL));
        onView(withId(R.id.recipient_list)).perform(ViewActions.pressKey(KeyEvent.KEYCODE_DEL));

        // add directly, check if it's there
        onView(withId(R.id.recipient_list)).perform(tokenEncryptViewAddToken(0x9D604D2F310716A3L));
        onView(withId(R.id.recipient_list)).check(matches(withKeyToken(0x9D604D2F310716A3L)));
        onView(withId(R.id.recipient_list)).perform(ViewActions.pressKey(KeyEvent.KEYCODE_DEL));

    }

}
