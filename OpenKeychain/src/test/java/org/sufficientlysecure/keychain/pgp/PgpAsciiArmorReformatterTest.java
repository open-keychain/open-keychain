package org.sufficientlysecure.keychain.pgp;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing.IteratorWithIOThrow;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;


@SuppressWarnings("WeakerAccess")
@RunWith(KeychainTestRunner.class)
public class PgpAsciiArmorReformatterTest {

    static final String INPUT_KEY_BLOCK_TWO_OCTET_LENGTH = "-----BEGIN PGP PUBLIC KEY BLOCK----- Version: GnuPG v2 " +
            "mQENBFnA7Y0BCAC+pdQ1mV9QguWvAyMsKiKkzeP5VxbIDUyQ8OBDFKIrZKZGTzjZ " +
            "xvZs22j5pXWYlyDi4HU4+nuQmAMo6jxJMKQQkW7Y5AmRYijboLN+lZ8L28bYJC4o " +
            "PcAnS3xlib2lE7aNK2BRUbctkhahhb2hohAxiXTUdGmfr9sHgZ2+B9sPTfuhrvtI " +
            "9cFHZ5/0Wp4pLhLB53gduYeLuw4vVfUd7t0C4IhqB+t5HE+F3lolgya7hXxdH0ji " +
            "oFNldCWT2qNdmmehIyY0WaoIrnUm2gVA4LMZ2fQTfsInpec5YT85OZaPEeBs3Opg " +
            "3aGxV4mhXOxHkfREJRuYXcqL1s/UERGNprp9ABEBAAG0E01yLiBNYWxmb3JtZWQg " +
            "QXNjaWmJAVQEEwEIAD4WIQTLHyFqT4uzqqEXR5Zz6vdoZVkMnwUCWcDtjQIbAwUJ " +
            "A8JnAAULCQgHAgYVCAkKCwIEFgIDAQIeAQIXgAAKCRBz6vdoZVkMnwpcB/985UbR " +
            "qxG5pzaLGePdl+Cm6JBra2mC3AfMbJobjHR8YVufDw1tA6qyCMW0emHFi8t/rG8j " +
            "tzPIadcl30rOzaGUTF85G5SqbwgAFHddZ01af36F6em0d5tY3+FCclQR7ynFPQlA " +
            "+KB9k/M5X91ty6Q3/EAaXst5Uwh1WnKNC1js9RAcYL1s1MXKxg2iMmtE0DvwMAWq " +
            "XRR+ADjzqkVdpdrzanTY7b72nuiGfe/H75b7/StIAfyxSZc5BU5535J0wF7Boz4p " +
            "A6zRFXTphabmAE9FIKhgj5X7fbU64Hsrc5OkvWt4dF/6VRE4oXgUYwLKaDEH7A0k " +
            "32GBGOkmnQGLKuqhuQENBFnA7Y0BCADKxQ1APSraxNKMpJv9vEVcXK3Sr91SYpGY " +
            "s/ugYNio2xvIt9Qe2AjYGNSE9+wS6qLRxbbzucIRxl9jbn2QTNbJr7epLVdj3wtL " +
            "JlkKsv13iao77Hg9WMvKh+NHpGoIFn4g5LOeYYG0QkZOvdu6b4Eg0RAryTLBh9jB " +
            "eMLELkZTFDuQAgOSrVY0XgoURNcaDRtVarnVNBIO1N7/7TNXtmL22wR0wpqh4mKv " +
            "vIvhE5itlIrthJHWzTcLDv5BHfyX23wqEpQFEffs1D72k9Ruh60OGgU0XAiVF654 " +
            "WjgZCUoscPCLWcDGDOlcN7FpBxMDi1Ao3+7sLOMi9zES0InJ9q8LABEBAAGJATwE " +
            "GAEIACYWIQTLHyFqT4uzqqEXR5Zz6vdoZVkMnwUCWcDtjQIbDAUJA8JnAAAKCRBz " +
            "6vdoZVkMn3/+B/9B0vrDITV2FpdT+99WVsXJsoiXOsqLfv3WkC0l0RBAcCaUeXxp " +
            "EqzyXQ0dVi6RoXu67dSnah6bdgfVnH14hJE8jc30GJ4QEpPD9kAKyodej15ledR5 " +
            "sbdjeEfsavn9tvACJ0svfu8YVJjUjJLOj5axXy8wUBm5UvCdZuSL4EjPq7hXdq+j " +
            "O/eTJGOfMl6hC4rRxRUbM+piZzbYcQ0lO3R2yPlEwzlO+asM9820V9bkviJUrXiY " +
            "c5EX44mwFdhpXuHbRS18DJjCVcMhEsPG6rQ0Qy/6/dafow5HExRBmZl6ZkfjR2Lb " +
            "alOZH0SNi47bvn6QKqKgiqT4f9mImyEDtSj/ =2V66 -----END PGP PUBLIC KEY BLOCK-----";
    static final String INPUT_KEY_BLOCK_ONE_OCTET_LENGTH = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
            "\n" +
            "mFIEVk2iwRMIKoZIzj0DAQcCAwTBaWEpVYOfZDm85s/zszd4J4CBW8FesYiYQTeX\n" +
            "5WMtwXsqrG5/ZcIgHNBzI0EvUbm/oSBFUJNk7RhmOk6MpS2gtAdNci4gRUNDiHkE\n" +
            "ExMIACEFAlZNosECGwMFCwkIBwIGFQgJCgsCBBYCAwECHgECF4AACgkQt60Zc7T/\n" +
            "SfQTPAD/bZ0ld3UyqAt8oPoHyJduGMkbur5KYoht1w/MMtiogG0BAN8Anhy55kTe\n" +
            "H4VmMWxzK9M+kIFPzqEVHOzsuE5nhJOouFYEVk2iwRIIKoZIzj0DAQcCAwSvfTrq\n" +
            "kkVeD0cVM8FZwhjTaG+B9wgk7yeoMgjIrSuZLiRjGAYC7Kq+6OiczduoItC2oMuK\n" +
            "GpymTF6t+CmQpUfuAwEIB4hhBBgTCAAJBQJWTaLBAhsMAAoJELetGXO0/0n00BwA\n" +
            "/2d1w/A4xMwfIFrKDwHeHALUBaIOuhF2AKd/43HujmuLAQDdcWf3h/0zjgBTjSoB\n" +
            "bcVr5AE/huKUnwKYa7SP7wzoZg==\n" +
            "=ou9N\n" +
            "-----END PGP PUBLIC KEY BLOCK-----\n";
    static final String INPUT_MSG = "-----BEGIN PGP MESSAGE----- Version: GnuPG v2 Header: Value of header " +
            "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVyIGFkaXBpc2NpbmcgZWxpdC4g " +
            "SW50ZWdlciBwb3N1ZXJlIHB1cnVzIG5lYyBsaWJlcm8gaWFjdWxpcywgZWdldCByaG9uY3VzIGxh " +
            "Y3VzIHVsbGFtY29ycGVyLgo= =2V66 -----END PGP MESSAGE-----";

    @Test
    public void reformatPgpPublicKeyBlock() throws Exception {
        String reformattedKey = PgpAsciiArmorReformatter.reformatPgpPublicKeyBlock(INPUT_KEY_BLOCK_TWO_OCTET_LENGTH);

        assertNotNull(reformattedKey);
        UncachedKeyRing.decodeFromData(reformattedKey.getBytes());
    }

    @Test
    public void reformatPgpPublicKeyBlock_consecutiveKeys() throws Exception {
        String reformattedKey = PgpAsciiArmorReformatter.reformatPgpPublicKeyBlock(
                INPUT_KEY_BLOCK_TWO_OCTET_LENGTH + INPUT_KEY_BLOCK_TWO_OCTET_LENGTH);

        assertNotNull(reformattedKey);
        IteratorWithIOThrow<UncachedKeyRing> uncachedKeyRingIteratorWithIOThrow =
                UncachedKeyRing.fromStream(new ByteArrayInputStream(reformattedKey.getBytes()));
        assertNotNull(uncachedKeyRingIteratorWithIOThrow.next());
        assertNotNull(uncachedKeyRingIteratorWithIOThrow.next());
        assertFalse(uncachedKeyRingIteratorWithIOThrow.hasNext());
    }

    @Test
    public void reformatPgpPublicKeyBlock_shouldBeIdempotent() throws Exception {
        String reformattedKey1 = PgpAsciiArmorReformatter.reformatPgpPublicKeyBlock(INPUT_KEY_BLOCK_TWO_OCTET_LENGTH);
        assertNotNull(reformattedKey1);

        String reformattedKey2 = PgpAsciiArmorReformatter.reformatPgpPublicKeyBlock(reformattedKey1);
        assertEquals(reformattedKey1, reformattedKey2);
    }

    @Test
    public void reformatPgpPublicKeyBlock_withOneOctetLengthHeader() throws Exception {
        String reformattedKey = PgpAsciiArmorReformatter.reformatPgpPublicKeyBlock(INPUT_KEY_BLOCK_ONE_OCTET_LENGTH);

        assertNotNull(reformattedKey);
        UncachedKeyRing.decodeFromData(reformattedKey.getBytes());
    }

    @Test
    public void reformatPgpEncryptedMessageBlock() throws Exception {
        String reformattedMsg = PgpAsciiArmorReformatter.reformatPgpEncryptedMessageBlock(INPUT_MSG);

        String expectedMsg = "-----BEGIN PGP MESSAGE-----\n" +
                "Version: GnuPG v2\n" +
                "Header: Value of header\n" +
                "\n" +
                "TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQsIGNvbnNlY3RldHVyIGFkaXBpc2NpbmcgZWxpdC4g\n" +
                "SW50ZWdlciBwb3N1ZXJlIHB1cnVzIG5lYyBsaWJlcm8gaWFjdWxpcywgZWdldCByaG9uY3VzIGxh\n" +
                "Y3VzIHVsbGFtY29ycGVyLgo=\n" +
                "=2V66\n" +
                "-----END PGP MESSAGE-----\n";

        assertEquals(expectedMsg, reformattedMsg);
    }

    @Test
    public void reformatPgpEncryptedMessageBlockIdempotent() throws Exception {
        String reformattedMsg = PgpAsciiArmorReformatter.reformatPgpEncryptedMessageBlock(INPUT_MSG);
        String reformattedMsg2 = PgpAsciiArmorReformatter.reformatPgpEncryptedMessageBlock(reformattedMsg);

        assertEquals(reformattedMsg, reformattedMsg2);
    }
}