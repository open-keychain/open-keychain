package org.spongycastle.jce.provider.test;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.spongycastle.util.test.SimpleTest;

public class SigTest
    extends SimpleTest
{
    /**
     * signature with a "forged signature" (sig block not at end of plain text)
     */
    private void testBadSig(PrivateKey priv, PublicKey pub) throws Exception
    {
        MessageDigest sha1 = MessageDigest.getInstance("SHA1", "SC");
        Cipher signer = Cipher.getInstance("RSA/ECB/PKCS1Padding", "SC");
        
        signer.init(Cipher.ENCRYPT_MODE, priv);
        
        byte[] block = new byte[signer.getBlockSize()];
        
        sha1.update((byte)0);
        
        byte[] sigHeader = Hex.decode("3021300906052b0e03021a05000414");
        System.arraycopy(sigHeader, 0, block, 0, sigHeader.length);
        
        byte[] dig = sha1.digest();

        System.arraycopy(dig, 0, block, sigHeader.length, dig.length);

        System.arraycopy(sigHeader, 0, block, 
                        sigHeader.length + dig.length, sigHeader.length);
        
        byte[] sig = signer.doFinal(block);
        
        Signature verifier = Signature.getInstance("SHA1WithRSA", "SC");
        
        verifier.initVerify(pub);
        
        verifier.update((byte)0);
        
        if (verifier.verify(sig))
        {
            fail("bad signature passed");
        }
    }

    public void performTest()
        throws Exception
    {   
        Signature           sig = Signature.getInstance("SHA1WithRSAEncryption", "SC");
        KeyPairGenerator    fact;
        KeyPair             keyPair;
        byte[]              data = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };

        fact = KeyPairGenerator.getInstance("RSA", "SC");

        fact.initialize(768, new SecureRandom());

        keyPair = fact.generateKeyPair();

        PrivateKey  signingKey = keyPair.getPrivate();
        PublicKey   verifyKey = keyPair.getPublic();
        
        testBadSig(signingKey, verifyKey);

        sig.initSign(signingKey);

        sig.update(data);

        byte[]  sigBytes = sig.sign();

        sig.initVerify(verifyKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("SHA1 verification failed");
        }

        sig = Signature.getInstance("MD2WithRSAEncryption", "SC");

        sig.initSign(signingKey);

        sig.update(data);

        sigBytes = sig.sign();

        sig.initVerify(verifyKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("MD2 verification failed");
        }

        sig = Signature.getInstance("MD5WithRSAEncryption", "SC");

        sig.initSign(signingKey);

        sig.update(data);

        sigBytes = sig.sign();

        sig.initVerify(verifyKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("MD5 verification failed");
        }

        sig = Signature.getInstance("RIPEMD160WithRSAEncryption", "SC");

        sig.initSign(signingKey);

        sig.update(data);

        sigBytes = sig.sign();

        sig.initVerify(verifyKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("RIPEMD160 verification failed");
        }

        //
        // RIPEMD-128
        //
        sig = Signature.getInstance("RIPEMD128WithRSAEncryption", "SC");

        sig.initSign(signingKey);

        sig.update(data);

        sigBytes = sig.sign();

        sig.initVerify(verifyKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("RIPEMD128 verification failed");
        }

        //
        // RIPEMD256
        //
        sig = Signature.getInstance("RIPEMD256WithRSAEncryption", "SC");

        sig.initSign(signingKey);

        sig.update(data);

        sigBytes = sig.sign();

        sig.initVerify(verifyKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("RIPEMD256 verification failed");
        }

        //
        // SHA-224
        //
        sig = Signature.getInstance("SHA224WithRSAEncryption", "SC");

        sig.initSign(signingKey);

        sig.update(data);

        sigBytes = sig.sign();

        sig.initVerify(verifyKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("SHA224 verification failed");
        }
        
        //
        // SHA-256
        //
        sig = Signature.getInstance("SHA256WithRSAEncryption", "SC");

        sig.initSign(signingKey);

        sig.update(data);

        sigBytes = sig.sign();

        sig.initVerify(verifyKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("SHA256 verification failed");
        }
        
        //
        // SHA-384
        //
        sig = Signature.getInstance("SHA384WithRSAEncryption", "SC");

        sig.initSign(signingKey);

        sig.update(data);

        sigBytes = sig.sign();

        sig.initVerify(verifyKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("SHA384 verification failed");
        }
        
        //
        // SHA-512
        //
        sig = Signature.getInstance("SHA512WithRSAEncryption", "SC");

        sig.initSign(signingKey);

        sig.update(data);

        sigBytes = sig.sign();

        sig.initVerify(verifyKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("SHA512 verification failed");
        }

        //
        // ISO Sigs.
        //
        sig = Signature.getInstance("MD5WithRSA/ISO9796-2", "SC");

        sig.initSign(signingKey);

        sig.update(data);

        sigBytes = sig.sign();

        sig.initVerify(verifyKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("MD5/ISO verification failed");
        }

        sig = Signature.getInstance("SHA1WithRSA/ISO9796-2", "SC");

        sig.initSign(signingKey);

        sig.update(data);

        sigBytes = sig.sign();

        sig.initVerify(verifyKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("SHA1/ISO verification failed");
        }

        sig = Signature.getInstance("RIPEMD160WithRSA/ISO9796-2", "SC");

        sig.initSign(signingKey);

        sig.update(data);

        sigBytes = sig.sign();

        sig.initVerify(verifyKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("RIPEMD160/ISO verification failed");
        }

        //
        // standard vector test - B.1.3 RIPEMD160, implicit.
        //
        BigInteger  mod = new BigInteger("ffffffff78f6c55506c59785e871211ee120b0b5dd644aa796d82413a47b24573f1be5745b5cd9950f6b389b52350d4e01e90009669a8720bf265a2865994190a661dea3c7828e2e7ca1b19651adc2d5", 16);
        BigInteger  pub = new BigInteger("03", 16);
        BigInteger  pri = new BigInteger("2aaaaaaa942920e38120ee965168302fd0301d73a4e60c7143ceb0adf0bf30b9352f50e8b9e4ceedd65343b2179005b2f099915e4b0c37e41314bb0821ad8330d23cba7f589e0f129b04c46b67dfce9d", 16);

        KeyFactory  f = KeyFactory.getInstance("RSA", "SC");

        PrivateKey  privKey = f.generatePrivate(new RSAPrivateKeySpec(mod, pri));
        PublicKey   pubKey = f.generatePublic(new RSAPublicKeySpec(mod, pub));
        byte[]      testSig = Hex.decode("5cf9a01854dbacaec83aae8efc563d74538192e95466babacd361d7c86000fe42dcb4581e48e4feb862d04698da9203b1803b262105104d510b365ee9c660857ba1c001aa57abfd1c8de92e47c275cae");

        data = Hex.decode("fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210");

        sig = Signature.getInstance("RIPEMD160WithRSA/ISO9796-2", "SC");

        sig.initSign(privKey);

        sig.update(data);

        sigBytes = sig.sign();

        if (!Arrays.areEqual(testSig, sigBytes))
        {
            fail("SigTest: failed ISO9796-2 generation Test");
        }

        sig.initVerify(pubKey);

        sig.update(data);

        if (!sig.verify(sigBytes))
        {
            fail("RIPEMD160/ISO verification failed");
        }
    }

    public String getName()
    {
        return "SigTest";
    }

    public static void main(
        String[]    args)
    {
        Security.addProvider(new BouncyCastleProvider());

        runTest(new SigTest());
    }
}
