/*
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.pgp;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.Security;
import java.util.ArrayList;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.operations.InputDataOperation;
import org.sufficientlysecure.keychain.operations.results.InputDataResult;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.InputDataParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 21, manifest = "src/main/AndroidManifest.xml")
public class InputDataOperationTest {

    static PrintStream oldShadowStream;

    @BeforeClass
    public static void setUpOnce() throws Exception {

        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        oldShadowStream = ShadowLog.stream;
        // ShadowLog.stream = System.out;

    }

    @Before
    public void setUp() {
        // don't log verbosely here, we're not here to test imports
        ShadowLog.stream = oldShadowStream;

        // ok NOW log verbosely!
        ShadowLog.stream = System.out;
    }

    @Test
    public void testMimeDecoding () throws Exception {

        String mimeMail =
            "Content-Type: multipart/mixed; boundary=\"=-26BafqxfXmhVNMbYdoIi\"\n" +
            "\n" +
            "--=-26BafqxfXmhVNMbYdoIi\n" +
            "Content-Type: text/plain\n" +
            "Content-Transfer-Encoding: quoted-printable\n" +
            "Content-Disposition: attachment; filename=data.txt\n" +
            "\n" +
            "message part 1\n" +
            "\n" +
            "--=-26BafqxfXmhVNMbYdoIi\n" +
            "Content-Type: text/testvalue\n" +
            "Content-Description: Dummy content description\n" +
            "\n" +
            "message part 2.1\n" +
            "message part 2.2\n" +
            "\n" +
            "--=-26BafqxfXmhVNMbYdoIi--";


        ByteArrayOutputStream outStream1 = new ByteArrayOutputStream();
        ByteArrayOutputStream outStream2 = new ByteArrayOutputStream();
        ContentResolver mockResolver = mock(ContentResolver.class);

        // fake openOutputStream first and second
        when(mockResolver.openOutputStream(any(Uri.class), eq("w")))
                .thenReturn(outStream1, outStream2);

        // fake openInputStream
        Uri fakeInputUri = Uri.parse("content://fake/1");
        when(mockResolver.openInputStream(fakeInputUri)).thenReturn(
                new ByteArrayInputStream(mimeMail.getBytes()));

        Uri fakeOutputUri1 = Uri.parse("content://fake/out/1");
        Uri fakeOutputUri2 = Uri.parse("content://fake/out/2");
        when(mockResolver.insert(eq(TemporaryFileProvider.CONTENT_URI), any(ContentValues.class)))
                .thenReturn(fakeOutputUri1, fakeOutputUri2);

        // application which returns mockresolver
        Application spyApplication = spy(RuntimeEnvironment.application);
        when(spyApplication.getContentResolver()).thenReturn(mockResolver);

        InputDataOperation op = new InputDataOperation(spyApplication,
                new ProviderHelper(RuntimeEnvironment.application), null);

        InputDataParcel input = new InputDataParcel(fakeInputUri, null);

        InputDataResult result = op.execute(input, new CryptoInputParcel());

        // must be successful, no verification, have two output URIs
        Assert.assertTrue(result.success());
        Assert.assertNull(result.mDecryptVerifyResult);

        ArrayList<Uri> outUris = result.getOutputUris();
        Assert.assertEquals("must have two output URIs", 2, outUris.size());
        Assert.assertEquals("first uri must be the one we provided", fakeOutputUri1, outUris.get(0));
        verify(mockResolver).openOutputStream(result.getOutputUris().get(0), "w");
        Assert.assertEquals("second uri must be the one we provided", fakeOutputUri2, outUris.get(1));
        verify(mockResolver).openOutputStream(result.getOutputUris().get(1), "w");

        ContentValues contentValues = new ContentValues();
        contentValues.put("name", "data.txt");
        contentValues.put("mimetype", "text/plain");
        verify(mockResolver).insert(TemporaryFileProvider.CONTENT_URI, contentValues);
        contentValues.put("name", (String) null);
        contentValues.put("mimetype", "text/testvalue");
        verify(mockResolver).insert(TemporaryFileProvider.CONTENT_URI, contentValues);

        // quoted-printable returns windows style line endings for some reason?
        Assert.assertEquals("first part must have expected content",
                "message part 1\r\n", new String(outStream1.toByteArray()));
        Assert.assertEquals("second part must have expected content",
                "message part 2.1\nmessage part 2.2\n", new String(outStream2.toByteArray()));


    }

}
