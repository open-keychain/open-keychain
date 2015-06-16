package org.sufficientlysecure.keychain;


import android.app.Activity;
import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.widget.AdapterView;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.ui.MainActivity;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.sufficientlysecure.keychain.TestHelpers.checkSnackbar;
import static org.sufficientlysecure.keychain.TestHelpers.importKeysFromResource;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withKeyItemId;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
@LargeTest
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

    @Test
    public void test01Edit() throws Exception {
        Activity activity = mActivity.getActivity();

        new KeychainDatabase(activity).clearDatabase();

        // import key for testing, get a stable initial state
        importKeysFromResource(activity, "x.sec.asc");

        // navigate to edit key dialog
        onData(withKeyItemId(0x9D604D2F310716A3L))
                .inAdapterView(allOf(isAssignableFrom(AdapterView.class),
                        isDescendantOfA(withId(R.id.key_list_list))))
                .perform(click());
        onView(withId(R.id.menu_key_view_edit)).perform(click());

        // no-op should yield snackbar
        onView(withText(R.string.btn_save)).perform(click());
        checkSnackbar(Style.ERROR, R.string.msg_mf_error_noop);

    }


}
