package org.sufficientlysecure.keychain.tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.testsupport.PgpVerifyTestingHelper;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class PgpDecryptVerifyTest {

    @Test
    public void testVerifySuccess() throws Exception {

        String testFileName = "/sample.txt";
        int expectedSignatureResult = OpenPgpSignatureResult.SIGNATURE_SUCCESS_UNCERTIFIED;

        int status = new PgpVerifyTestingHelper(Robolectric.application).doTestFile(testFileName);

        Assert.assertEquals(expectedSignatureResult, status);
    }

    @Test
    public void testVerifyFailure() throws Exception {

        String testFileName = "/sample-altered.txt";
        int expectedSignatureResult = OpenPgpSignatureResult.SIGNATURE_ERROR;

        int status = new PgpVerifyTestingHelper(Robolectric.application).doTestFile(testFileName);

        Assert.assertEquals(expectedSignatureResult, status);
    }

}
