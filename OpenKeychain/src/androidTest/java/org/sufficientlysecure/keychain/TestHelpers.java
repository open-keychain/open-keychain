package org.sufficientlysecure.keychain;


import java.util.Random;

import android.content.Context;
import android.support.annotation.StringRes;

import org.hamcrest.CoreMatchers;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing.IteratorWithIOThrow;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withSnackbarLineColor;


public class TestHelpers {


    public static void checkSnackbar(Style style, @StringRes Integer text) {

        onView(withClassName(CoreMatchers.endsWith("Snackbar")))
                .check(matches(withSnackbarLineColor(style.mLineColor)));

        if (text != null) {
            onView(withClassName(CoreMatchers.endsWith("Snackbar")))
                    .check(matches(hasDescendant(withText(text))));
        }

    }


    static void importKeysFromResource(Context context, String name) throws Exception {
        IteratorWithIOThrow<UncachedKeyRing> stream = UncachedKeyRing.fromStream(
                getInstrumentation().getContext().getAssets().open(name));

        ProviderHelper helper = new ProviderHelper(context);
        while(stream.hasNext()) {
            UncachedKeyRing ring = stream.next();
            if (ring.isSecret()) {
                helper.saveSecretKeyRing(ring, new ProgressScaler());
            } else {
                helper.savePublicKeyRing(ring, new ProgressScaler());
            }
        }

    }

    public static String randomString(int min, int max) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789!@#$%^&*()-_=";
        Random r = new Random();
        StringBuilder passbuilder = new StringBuilder();
        // 5% chance for an empty string
        for(int i = 0, j = r.nextInt(max)+min; i < j; i++) {
            passbuilder.append(chars.charAt(r.nextInt(chars.length())));
        }
        return passbuilder.toString();
    }


}
