package org.sufficientlysecure.keychain.util;


import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class Numeric9x4PassphraseUtilTest {
    private static final int RANDOM_SEED = 12345;
    private static final String TRANSFER_CODE_3x3x4 = "1018-5452-1972-0624-7325-1126-8153-1997-1535";

    @Test
    public void generateNumeric9x4Passphrase() {
        Random r = new Random(RANDOM_SEED);

        Passphrase passphrase = Numeric9x4PassphraseUtil.generateNumeric9x4Passphrase(r);

        assertEquals(TRANSFER_CODE_3x3x4, passphrase.toStringUnsafe());
    }

    @Test
    public void isNumeric9x4Passphrase() {
        boolean isValidCodeLayout = Numeric9x4PassphraseUtil.isNumeric9x4Passphrase(TRANSFER_CODE_3x3x4);

        assertTrue(isValidCodeLayout);
    }


    @Test
    public void isNumeric9x4Passphrase_withBadSuffix() {
        boolean isValidCodeLayout = Numeric9x4PassphraseUtil.isNumeric9x4Passphrase(TRANSFER_CODE_3x3x4 + "x");

        assertFalse(isValidCodeLayout);
    }
}