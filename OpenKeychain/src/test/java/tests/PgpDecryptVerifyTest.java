/*
 * Copyright (C) Art O Cathain
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

package tests;

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
