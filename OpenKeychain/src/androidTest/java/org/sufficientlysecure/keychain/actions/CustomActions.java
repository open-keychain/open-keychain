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
                new ProviderHelper(getTargetContext()).getCanonicalizedPublicKeyRing(keyId);
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