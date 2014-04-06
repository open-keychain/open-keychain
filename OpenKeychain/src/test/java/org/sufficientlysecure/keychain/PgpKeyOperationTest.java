package org.sufficientlysecure.keychain;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import org.sufficientlysecure.keychain.pgp.*;
import org.spongycastle.openpgp.*;

@RunWith(RobolectricGradleTestRunner.class)
public class PgpKeyOperationTest {

    PGPSecretKey key;

    @Before
    public void setUp() throws Exception {

        /* Input */
        int algorithm = Id.choice.algorithm.dsa;
        String passphrase = "swag";
        int keysize = 2048;
        boolean masterKey = true;

        /* Operation */
        PgpKeyOperation keyOperations = new PgpKeyOperation(null);
        key = keyOperations.createKey(algorithm, keysize, passphrase, masterKey);

        System.err.println("initialized, test key: " + PgpKeyHelper.convertKeyIdToHex(key.getKeyID()));
    }

    @After
    public void tearDown() {
    }

    @Test
    public void createTest() {
    }

    @Test
    public void certifyKey() {
        System.err.println("swag");
    }

}
