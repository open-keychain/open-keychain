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

package org.sufficientlysecure.keychain.ui;


import android.app.Activity;
import android.content.Intent;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.RecyclerView;
import android.widget.AdapterView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.runners.MethodSorters;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.matcher.CustomMatchers;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnHolderItem;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.sufficientlysecure.keychain.TestHelpers.checkSnackbar;
import static org.sufficientlysecure.keychain.TestHelpers.importKeysFromResource;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withKeyHolderId;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withKeyItemId;

//TODO This test is disabled because it needs to be fixed to work with updated code
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//@RunWith(AndroidJUnit4.class)
//@LargeTest
public class EditKeyTest {

    @Rule
    public final ActivityTestRule<MainActivity> mActivity
            = new ActivityTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected Intent getActivityIntent() {
            Intent intent = super.getActivityIntent();
            intent.putExtra(MainActivity.EXTRA_SKIP_FIRST_TIME, true);
            return intent;
        }
    };

    //@Test
    public void test01Edit() throws Exception {
        Activity activity = mActivity.getActivity();

        new KeychainDatabase(activity).clearDatabase();

        // import key for testing, get a stable initial state
        importKeysFromResource(activity, "x.sec.asc");

        // navigate to edit key dialog
        onView(allOf(
                isAssignableFrom(RecyclerView.class),
                withId(android.R.id.list)))
                .perform(actionOnHolderItem(
                        withKeyHolderId(0x9D604D2F310716A3L), click()));

        onView(withId(R.id.view_key_card_user_ids_edit)).perform(click());

        // no-op should yield snackbar
        onView(withText(R.string.btn_save)).perform(click());
        checkSnackbar(Style.ERROR, R.string.msg_mf_error_noop);

    }


}
