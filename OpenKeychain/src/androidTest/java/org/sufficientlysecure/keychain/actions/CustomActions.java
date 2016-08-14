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
 *
 */

package org.sufficientlysecure.keychain.actions;


import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;

import com.tokenautocomplete.TokenCompleteTextView;
import org.hamcrest.Matcher;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.adapter.KeyAdapter;

import static android.support.test.InstrumentationRegistry.getTargetContext;


public abstract class CustomActions {

    public static ViewAction tokenEncryptViewAddToken(long keyId) throws Exception {
        CanonicalizedPublicKeyRing ring =
                new ProviderHelper(getTargetContext()).mReader.getCanonicalizedPublicKeyRing(keyId);
        final Object item = new KeyAdapter.KeyItem(ring);

        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isAssignableFrom(TokenCompleteTextView.class);
            }

            @Override
            public String getDescription() {
                return "add completion token";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((TokenCompleteTextView) view).addObject(item);
            }
        };
    }

    public static ViewAction tokenViewAddToken(final Object item) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isAssignableFrom(TokenCompleteTextView.class);
            }

            @Override
            public String getDescription() {
                return "add completion token";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((TokenCompleteTextView) view).addObject(item);
            }
        };
    }

}