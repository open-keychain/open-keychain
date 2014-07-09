package org.sufficientlysecure.keychain.tests;

import java.util.Collections;
import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.sufficientlysecure.keychain.testsupport.KeyringTestingHelper;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class ProviderHelperKeyringTest {

    @Test
    public void testSavePublicKeyring() throws Exception {
        Assert.assertTrue(new KeyringTestingHelper(Robolectric.application).addKeyring(Collections.singleton(
                "/public-key-for-sample.blob"
        )));
    }

    @Test
    public void testSavePublicKeyringRsa() throws Exception {
        Assert.assertTrue(new KeyringTestingHelper(Robolectric.application).addKeyring(prependResourcePath(Arrays.asList(
                        "000001-006.public_key",
                        "000002-013.user_id",
                        "000003-002.sig",
                        "000004-012.ring_trust",
                        "000005-002.sig",
                        "000006-012.ring_trust",
                        "000007-002.sig",
                        "000008-012.ring_trust",
                        "000009-002.sig",
                        "000010-012.ring_trust",
                        "000011-002.sig",
                        "000012-012.ring_trust",
                        "000013-014.public_subkey",
                        "000014-002.sig",
                        "000015-012.ring_trust"
                ))));
    }

    @Test
    public void testSavePublicKeyringDsa() throws Exception {
        Assert.assertTrue(new KeyringTestingHelper(Robolectric.application).addKeyring(prependResourcePath(Arrays.asList(
                        "000016-006.public_key",
                        "000017-002.sig",
                        "000018-012.ring_trust",
                        "000019-013.user_id",
                        "000020-002.sig",
                        "000021-012.ring_trust",
                        "000022-002.sig",
                        "000023-012.ring_trust",
                        "000024-014.public_subkey",
                        "000025-002.sig",
                        "000026-012.ring_trust"
                ))));
    }

    @Test
    public void testSavePublicKeyringDsa2() throws Exception {
        Assert.assertTrue(new KeyringTestingHelper(Robolectric.application).addKeyring(prependResourcePath(Arrays.asList(
                        "000027-006.public_key",
                        "000028-002.sig",
                        "000029-012.ring_trust",
                        "000030-013.user_id",
                        "000031-002.sig",
                        "000032-012.ring_trust",
                        "000033-002.sig",
                        "000034-012.ring_trust"
                ))));
    }

    private static Collection<String> prependResourcePath(Collection<String> files) {
        Collection<String> prependedFiles = new ArrayList<String>();
        for (String file: files) {
            prependedFiles.add("/extern/OpenPGP-Haskell/tests/data/" + file);
        }
        return prependedFiles;
    }
}
