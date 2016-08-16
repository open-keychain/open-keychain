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

package org.sufficientlysecure.keychain;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.view.View;

import com.nispok.snackbar.Snackbar;
import org.hamcrest.Matcher;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing.IteratorWithIOThrow;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withSnackbarLineColor;


public class TestHelpers {

    public static void dismissSnackbar() {
        onView(withClassName(endsWith("Snackbar")))
            .perform(new ViewAction() {
                @Override
                public Matcher<View> getConstraints() {
                    return ViewMatchers.isAssignableFrom(Snackbar.class);
                }

                @Override
                public String getDescription() {
                    return "dismiss snackbar";
                }

                @Override
                public void perform(UiController uiController, View view) {
                    ((Snackbar) view).dismiss();
                }
            });
    }

    public static void checkSnackbar(Style style, @StringRes Integer text) {

        onView(withClassName(endsWith("Snackbar")))
                .check(matches(withSnackbarLineColor(style.mLineColor)));

        if (text != null) {
            onView(withClassName(endsWith("Snackbar")))
                    .check(matches(hasDescendant(withText(text))));
        }

    }

    public static void checkAndDismissSnackbar(Style style, @StringRes Integer text) {
        checkSnackbar(style, text);
        dismissSnackbar();
    }

    public static void importKeysFromResource(Context context, String name) throws Exception {
        IteratorWithIOThrow<UncachedKeyRing> stream = UncachedKeyRing.fromStream(
                getInstrumentation().getContext().getAssets().open(name));

        ProviderHelper helper = new ProviderHelper(context);
        while(stream.hasNext()) {
            UncachedKeyRing ring = stream.next();
            if (ring.isSecret()) {
                helper.write().saveSecretKeyRingForTest(ring);
            } else {
                helper.write().savePublicKeyRing(ring);
            }
        }

    }

    public static void copyFiles() throws IOException {
        File cacheDir = getInstrumentation().getTargetContext().getFilesDir();
        byte[] buf = new byte[256];
        for (String filename : FILES) {
            File outFile = new File(cacheDir, filename);
            if (outFile.exists()) {
                continue;
            }
            InputStream in = new BufferedInputStream(getInstrumentation().getContext().getAssets().open(filename));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
            int len;
            while( (len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    public static final String[] FILES = new String[] { "pa.png", "re.png", "ci.png" };
    public static File[] getImageNames() {
        File cacheDir = getInstrumentation().getTargetContext().getFilesDir();
        File[] ret = new File[FILES.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new File(cacheDir, FILES[i]);
        }
        return ret;
    }

    public static <T> T pickRandom(T[] haystack) {
        return haystack[new Random().nextInt(haystack.length)];
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

    public static void cleanupForTests(Context context) throws Exception {

        new KeychainDatabase(context).clearDatabase();

        // import these two, make sure they're there
        importKeysFromResource(context, "x.sec.asc");

        // make sure no passphrases are cached
        PassphraseCacheService.clearAllCachedPassphrases(context);

    }

}
