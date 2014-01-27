package org.spongycastle.crypto.test;

import java.security.SecureRandom;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.engines.ChaChaEngine;
import org.spongycastle.crypto.engines.Grain128Engine;
import org.spongycastle.crypto.engines.Grainv1Engine;
import org.spongycastle.crypto.engines.HC128Engine;
import org.spongycastle.crypto.engines.HC256Engine;
import org.spongycastle.crypto.engines.ISAACEngine;
import org.spongycastle.crypto.engines.RC4Engine;
import org.spongycastle.crypto.engines.Salsa20Engine;
import org.spongycastle.crypto.engines.XSalsa20Engine;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.spongycastle.util.test.SimpleTest;

/**
 * Test whether block ciphers implement reset contract on init, encrypt/decrypt and reset.
 */
public class StreamCipherResetTest
    extends SimpleTest
{
    public String getName()
    {
        return "Stream Cipher Reset";
    }

    public void performTest()
        throws Exception
    {
        testReset(new Salsa20Engine(), new Salsa20Engine(), new ParametersWithIV(new KeyParameter(random(32)),
            random(8)));
        testReset(new Salsa20Engine(), new Salsa20Engine(), new ParametersWithIV(new KeyParameter(random(16)),
            random(8)));
        testReset(new XSalsa20Engine(), new XSalsa20Engine(), new ParametersWithIV(new KeyParameter(random(32)),
            random(24)));
        testReset(new ChaChaEngine(), new ChaChaEngine(), new ParametersWithIV(new KeyParameter(random(32)), random(8)));
        testReset(new ChaChaEngine(), new ChaChaEngine(), new ParametersWithIV(new KeyParameter(random(16)), random(8)));
        testReset(new RC4Engine(), new RC4Engine(), new KeyParameter(random(16)));
        testReset(new ISAACEngine(), new ISAACEngine(), new KeyParameter(random(16)));
        testReset(new HC128Engine(), new HC128Engine(), new ParametersWithIV(new KeyParameter(random(16)), random(16)));
        testReset(new HC256Engine(), new HC256Engine(), new ParametersWithIV(new KeyParameter(random(16)), random(16)));
        testReset(new Grainv1Engine(), new Grainv1Engine(), new ParametersWithIV(new KeyParameter(random(16)),
            random(8)));
        testReset(new Grain128Engine(), new Grain128Engine(), new ParametersWithIV(new KeyParameter(random(16)),
            random(12)));
    }

    private static final SecureRandom RAND = new SecureRandom();

    private byte[] random(int size)
    {
        final byte[] data = new byte[size];
        RAND.nextBytes(data);
        return data;
    }

    private void testReset(StreamCipher cipher1, StreamCipher cipher2, CipherParameters params)
        throws InvalidCipherTextException
    {
        cipher1.init(true, params);

        byte[] plaintext = new byte[1023];
        byte[] ciphertext = new byte[plaintext.length];

        // Establish baseline answer
        cipher1.processBytes(plaintext, 0, plaintext.length, ciphertext, 0);

        // Test encryption resets
        checkReset(cipher1, params, true, plaintext, ciphertext);

        // Test decryption resets with fresh instance
        cipher2.init(false, params);
        checkReset(cipher2, params, false, ciphertext, plaintext);
    }

    private void checkReset(StreamCipher cipher,
                            CipherParameters params,
                            boolean encrypt,
                            byte[] pretext,
                            byte[] posttext)
        throws InvalidCipherTextException
    {
        // Do initial run
        byte[] output = new byte[posttext.length];
        cipher.processBytes(pretext, 0, pretext.length, output, 0);

        // Check encrypt resets cipher
        cipher.init(encrypt, params);

        try
        {
            cipher.processBytes(pretext, 0, pretext.length, output, 0);
        }
        catch (Exception e)
        {
            fail(cipher.getAlgorithmName() + " init did not reset: " + e.getMessage());
        }
        if (!Arrays.areEqual(output, posttext))
        {
            fail(cipher.getAlgorithmName() + " init did not reset.", new String(Hex.encode(posttext)),
                new String(Hex.encode(output)));
        }

        // Check reset resets data
        cipher.reset();

        try
        {
            cipher.processBytes(pretext, 0, pretext.length, output, 0);
        }
        catch (Exception e)
        {
            fail(cipher.getAlgorithmName() + " reset did not reset: " + e.getMessage());
        }
        if (!Arrays.areEqual(output, posttext))
        {
            fail(cipher.getAlgorithmName() + " reset did not reset.");
        }
    }

    public static void main(String[] args)
    {
        runTest(new StreamCipherResetTest());
    }

}
