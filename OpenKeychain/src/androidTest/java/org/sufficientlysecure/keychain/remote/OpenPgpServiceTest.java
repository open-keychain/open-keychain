package org.sufficientlysecure.keychain.remote;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.widget.AdapterView;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static android.support.test.espresso.Espresso.closeSoftKeyboard;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.sufficientlysecure.keychain.TestHelpers.cleanupForTests;
import static org.sufficientlysecure.keychain.matcher.CustomMatchers.withKeyItemId;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class OpenPgpServiceTest {

    public static final int ACTIVITY_WAIT_TIME = 2 * 1000;

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();

    private OpenPgpApi mApi;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();

        cleanupForTests(context);

        Intent serviceIntent = new Intent(context, OpenPgpService2.class);
        IBinder binder = mServiceRule.bindService(serviceIntent);

        mApi = new OpenPgpApi(context, IOpenPgpService2.Stub.asInterface(binder));
    }

    @Test
    public void testStuff() throws Exception {
        // TODO why does this not ask for general usage permissions?!

        {
            Intent intent = new Intent();
            intent.setAction(OpenPgpApi.ACTION_ENCRYPT);
            intent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
            intent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, new long[]{0x9D604D2F310716A3L});

            ByteArrayInputStream is = new ByteArrayInputStream("swag".getBytes());
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            Intent result = mApi.executeApi(intent, is, os);

            assertThat("result is pending accept",
                    result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR),
                    is(OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED));

            PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
            pi.send();

            Thread.sleep(ACTIVITY_WAIT_TIME); // Wait for activity to start
            onView(withText(R.string.button_allow)).perform(click());
        }

        byte[] ciphertext;
        {
            Intent intent = new Intent();
            intent.setAction(OpenPgpApi.ACTION_ENCRYPT);
            intent.putExtra(OpenPgpApi.EXTRA_REQUEST_ASCII_ARMOR, true);
            intent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, new long[]{0x9D604D2F310716A3L});

            ByteArrayInputStream is = new ByteArrayInputStream("swag".getBytes());
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            Intent result = mApi.executeApi(intent, is, os);

            assertThat("result is encrypt ok",
                    result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR),
                    is(OpenPgpApi.RESULT_CODE_SUCCESS));

            ciphertext = os.toByteArray();
        }

        { // decrypt
            Intent intent = new Intent();
            intent.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);

            ByteArrayInputStream is = new ByteArrayInputStream(ciphertext);
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            Intent result = mApi.executeApi(intent, is, os);

            assertThat("result is pending input",
                    result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR),
                    is(OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED));

            PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
            pi.send();

            Thread.sleep(ACTIVITY_WAIT_TIME); // Wait for activity to start
            onView(withText(R.string.button_allow)).perform(click());
        }

        { // decrypt again, this time pending passphrase
            Intent intent = new Intent();
            intent.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);

            ByteArrayInputStream is = new ByteArrayInputStream(ciphertext);
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            Intent result = mApi.executeApi(intent, is, os);

            assertThat("result is pending passphrase",
                    result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR),
                    is(OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED));

            PendingIntent pi = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
            pi.send();

            Thread.sleep(ACTIVITY_WAIT_TIME); // Wait for activity to start
            onView(withId(R.id.passphrase_passphrase)).perform(typeText("x"));

            // Needed to correctly execute test on Travis
            closeSoftKeyboard();
            Thread.sleep(1 * 1000);

            onView(withText(R.string.btn_unlock)).perform(click());
        }

        { // decrypt again, NOW it should work with passphrase cached =)
            Intent intent = new Intent();
            intent.setAction(OpenPgpApi.ACTION_DECRYPT_VERIFY);

            ByteArrayInputStream is = new ByteArrayInputStream(ciphertext);
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            Intent result = mApi.executeApi(intent, is, os);

            assertThat("result is decrypt ok",
                    result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR),
                    is(OpenPgpApi.RESULT_CODE_SUCCESS));

            byte[] plaintext = os.toByteArray();
            assertThat("decrypted plaintext matches plaintext", new String(plaintext), is("swag"));
        }
    }

}
