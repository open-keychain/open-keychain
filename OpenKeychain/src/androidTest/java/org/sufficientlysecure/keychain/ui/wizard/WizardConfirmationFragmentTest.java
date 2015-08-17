package org.sufficientlysecure.keychain.ui.wizard;


import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class WizardConfirmationFragmentTest {
    public static final String SAMPLE_EMAIL = "sample_email@gmail.com";
    public static final String SAMPLE_ADDITIONAL_EMAIL = "sample_additional_email@gmail.com";
    public static final String SAMPLE_TWO_EMAILS = SAMPLE_EMAIL + ", " + SAMPLE_ADDITIONAL_EMAIL;

    /**
     * UNIT TESTS
     */

    /**
     * Test main email without additional emails.
     * Second parameter as null
     */
    @Test
    public void testGetAdditionalEmailsNullParam2() {
        String result = String.valueOf(WizardConfirmationFragment.
                generateAdditionalEmails(SAMPLE_EMAIL, null));
        assertTrue(result.equals(SAMPLE_EMAIL));
    }

    /**
     * Test main email without additional emails.
     * Second parameter as empty array list
     */
    @Test
    public void testGetAdditionalEmailsEmptyArrayParam2() {
        String result = String.valueOf(WizardConfirmationFragment.
                generateAdditionalEmails(SAMPLE_EMAIL, new ArrayList<String>()));
        assertTrue(result.equals(SAMPLE_EMAIL));
    }

    /**
     * test main email and one additional email
     */
    @Test
    public void testGetAdditionalEmailsTwoEmails() {
        ArrayList<String> additionalEmails = new ArrayList<>();
        additionalEmails.add(SAMPLE_ADDITIONAL_EMAIL);
        String result = String.valueOf(WizardConfirmationFragment.
                generateAdditionalEmails(SAMPLE_EMAIL, additionalEmails));
        assertTrue(result.equals(SAMPLE_TWO_EMAILS));
    }
}
