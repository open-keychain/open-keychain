/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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


import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sufficientlysecure.keychain.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.sufficientlysecure.keychain.matcher.EditTextMatchers.withError;
import static org.sufficientlysecure.keychain.matcher.EditTextMatchers.withTransformationMethod;

//TODO This test is disabled because it needs to be fixed to work with updated code
//@RunWith(AndroidJUnit4.class)
//@LargeTest
public class CreateKeyActivityTest {

    public static final String SAMPLE_NAME = "Sample Name";
    public static final String SAMPLE_EMAIL = "sample_email@gmail.com";
    public static final String SAMPLE_ADDITIONAL_EMAIL = "sample_additional_email@gmail.com";
    public static final String SAMPLE_PASSWORD = "sample_password";

    @Rule
    public final ActivityTestRule<CreateKeyActivity> mActivity
            = new ActivityTestRule<>(CreateKeyActivity.class);

    //@Test
    public void testCreateMyKey() {
        /*

        mActivity.getActivity();

        // Clicks create my key
        onView(ViewMatchers.withId(R.id.create_key_create_key_button))
                .perform(click());

        // Clicks next with empty name
        onView(withId(R.id.create_key_next_button))
                .perform(click());
        onView(withId(R.id.create_key_name))
                .check(matches(withError(R.string.create_key_empty)));

        // Types name and clicks next
        onView(withId(R.id.create_key_name))
                .perform(typeText(SAMPLE_NAME));
        onView(withId(R.id.create_key_next_button))
                .perform(click());

        // Clicks next with empty email
        onView(withId(R.id.create_key_next_button))
                .perform(click());
        onView(withId(R.id.create_key_email))
                .check(matches(withError(R.string.create_key_empty)));

        // Types email
        onView(withId(R.id.create_key_email))
                .perform(typeText(SAMPLE_EMAIL));

        // Adds same email as additional email and dismisses the snackbar
        onView(withId(R.id.create_key_add_email))
                .perform(click());
        onView(withId(R.id.add_email_address))
                .perform(typeText(SAMPLE_EMAIL));
        onView(withText(android.R.string.ok))
                .inRoot(isDialog())
                .perform(click());
        onView(allOf(withId(R.id.sb__text), withText(R.string.create_key_email_already_exists_text)))
                .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.sb__text), withText(R.string.create_key_email_already_exists_text)))
                .perform(swipeLeft());

        // Adds additional email
        onView(withId(R.id.create_key_add_email))
                .perform(click());
        onView(withId(R.id.add_email_address))
                .perform(typeText(SAMPLE_ADDITIONAL_EMAIL));
        onView(withText(android.R.string.ok))
                .inRoot(isDialog())
                .perform(click());
        onView(withId(R.id.create_key_emails))
                .check(matches(hasDescendant(allOf(withId(R.id.create_key_email_item_email), withText(SAMPLE_ADDITIONAL_EMAIL)))));

        // Removes additional email and clicks next
        onView(allOf(withId(R.id.create_key_email_item_delete_button), hasSibling(allOf(withId(R.id.create_key_email_item_email), withText(SAMPLE_ADDITIONAL_EMAIL)))))
                .perform(click())
                .check(doesNotExist());
        onView(withId(R.id.create_key_next_button))
                .perform(click(click()));

        // Clicks next with empty password
        onView(withId(R.id.create_key_next_button))
                .perform(click());
        onView(withId(R.id.create_key_passphrase))
                .check(matches(withError(R.string.create_key_empty)));

        // Types password
        onView(withId(R.id.create_key_passphrase))
                .perform(typeText(SAMPLE_PASSWORD));

        // Clicks next with empty confirm password
        onView(withId(R.id.create_key_next_button))
                .perform(click());
        onView(withId(R.id.create_key_passphrase_again))
                .check(matches(withError(R.string.create_key_passphrases_not_equal)));

        // Types confirm password
        onView(withId(R.id.create_key_passphrase_again))
                .perform(typeText(SAMPLE_PASSWORD));

        // Clicks show password twice and clicks next
        onView(withId(R.id.create_key_show_passphrase))
                .perform(click());
        onView(withId(R.id.create_key_passphrase))
                .check(matches(withTransformationMethod(HideReturnsTransformationMethod.class)));
        onView(withId(R.id.create_key_passphrase_again))
                .check(matches(withTransformationMethod(HideReturnsTransformationMethod.class)));
        onView(withId(R.id.create_key_show_passphrase))
                .perform(click());
        onView(withId(R.id.create_key_passphrase))
                .check(matches(withTransformationMethod(PasswordTransformationMethod.class)));
        onView(withId(R.id.create_key_passphrase_again))
                .check(matches(withTransformationMethod(PasswordTransformationMethod.class)));
        onView(withId(R.id.create_key_next_button))
                .perform(click());

        // Verifies name and email
        onView(withId(R.id.name))
                .check(matches(withText(SAMPLE_NAME)));
        onView(withId(R.id.email))
                .check(matches(withText(SAMPLE_EMAIL)));

        // Verifies backstack
        onView(withId(R.id.create_key_back_button))
                .perform(click());
        onView(withId(R.id.create_key_back_button))
                .perform(click());
        onView(withId(R.id.create_key_back_button))
                .perform(click());

        onView(withId(R.id.create_key_name))
                .check(matches(withText(SAMPLE_NAME)));
        onView(withId(R.id.create_key_next_button))
                .perform(click());

        onView(withId(R.id.create_key_email))
                .check(matches(withText(SAMPLE_EMAIL)));
        onView(withId(R.id.create_key_next_button))
                .perform(click());

        // TODO: Uncomment when fixed in main
//        onView(withId(R.id.create_key_passphrase))
//                .check(matches(withText(SAMPLE_PASSWORD)));
//        onView(withId(R.id.create_key_passphrase_again))
//                .check(matches(withText(SAMPLE_PASSWORD)));
        onView(withId(R.id.create_key_next_button))
                .perform(click());

        onView(withId(R.id.name))
                .check(matches(withText(SAMPLE_NAME)));
        onView(withId(R.id.email))
                .check(matches(withText(SAMPLE_EMAIL)));

        // Clicks create key
        onView(withId(R.id.create_key_next_button))
                .perform(click());
        */
    }

}
