/**
 * Copyright (c) 2013-2014 Philipp Jakubeit, Signe Rüsch, Dominik Schürmann
 *
 * Licensed under the Bouncy Castle License (MIT license). See LICENSE file for details.
 */

package org.spongycastle.openpgp.operator.jcajce;

import org.spongycastle.bcpg.PublicKeyAlgorithmTags;
import org.spongycastle.jcajce.util.DefaultJcaJceHelper;
import org.spongycastle.jcajce.util.NamedJcaJceHelper;
import org.spongycastle.jcajce.util.ProviderJcaJceHelper;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.operator.PGPDataDecryptor;
import org.spongycastle.openpgp.operator.PublicKeyDataDecryptorFactory;

import java.nio.ByteBuffer;
import java.security.Provider;
import java.util.Map;


/**
 * This class is based on JcePublicKeyDataDecryptorFactoryBuilder
 *
 */
public class NfcSyncPublicKeyDataDecryptorFactoryBuilder
{
    private OperatorHelper helper = new OperatorHelper(new DefaultJcaJceHelper());
    private OperatorHelper contentHelper = new OperatorHelper(new DefaultJcaJceHelper());
    private JcaPGPKeyConverter keyConverter = new JcaPGPKeyConverter();
//    private JcaPGPDigestCalculatorProviderBuilder digestCalculatorProviderBuilder = new JcaPGPDigestCalculatorProviderBuilder();
//    private JcaKeyFingerprintCalculator fingerprintCalculator = new JcaKeyFingerprintCalculator();

    public static class NfcInteractionNeeded extends RuntimeException
    {
        public byte[] encryptedSessionKey;

        public NfcInteractionNeeded(byte[] encryptedSessionKey)
        {
            super("NFC interaction required!");
            this.encryptedSessionKey = encryptedSessionKey;
        }
    }

    public NfcSyncPublicKeyDataDecryptorFactoryBuilder()
    {
    }

    /**
     * Set the provider object to use for creating cryptographic primitives in the resulting factory the builder produces.
     *
     * @param provider  provider object for cryptographic primitives.
     * @return  the current builder.
     */
    public NfcSyncPublicKeyDataDecryptorFactoryBuilder setProvider(Provider provider)
    {
        this.helper = new OperatorHelper(new ProviderJcaJceHelper(provider));
        keyConverter.setProvider(provider);
        this.contentHelper = helper;

        return this;
    }

    /**
     * Set the provider name to use for creating cryptographic primitives in the resulting factory the builder produces.
     *
     * @param providerName  the name of the provider to reference for cryptographic primitives.
     * @return  the current builder.
     */
    public NfcSyncPublicKeyDataDecryptorFactoryBuilder setProvider(String providerName)
    {
        this.helper = new OperatorHelper(new NamedJcaJceHelper(providerName));
        keyConverter.setProvider(providerName);
        this.contentHelper = helper;

        return this;
    }

    public NfcSyncPublicKeyDataDecryptorFactoryBuilder setContentProvider(Provider provider)
    {
        this.contentHelper = new OperatorHelper(new ProviderJcaJceHelper(provider));

        return this;
    }

    public NfcSyncPublicKeyDataDecryptorFactoryBuilder setContentProvider(String providerName)
    {
        this.contentHelper = new OperatorHelper(new NamedJcaJceHelper(providerName));

        return this;
    }

    public PublicKeyDataDecryptorFactory build(final Map<ByteBuffer,byte[]> nfcDecryptedMap) {
        return new PublicKeyDataDecryptorFactory()
        {
            public byte[] recoverSessionData(int keyAlgorithm, byte[][] secKeyData)
                    throws PGPException
            {
                if (keyAlgorithm == PublicKeyAlgorithmTags.ECDH)
                {
                    throw new PGPException("ECDH not supported!");
                }

                return decryptSessionData(keyAlgorithm, secKeyData, nfcDecryptedMap);
            }

            public PGPDataDecryptor createDataDecryptor(boolean withIntegrityPacket, int encAlgorithm, byte[] key)
                    throws PGPException
            {
                return contentHelper.createDataDecryptor(withIntegrityPacket, encAlgorithm, key);
            }
        };
    }

//    public PublicKeyDataDecryptorFactory build(final PrivateKey privKey)
//    {
//        return new PublicKeyDataDecryptorFactory()
//        {
//            public byte[] recoverSessionData(int keyAlgorithm, byte[][] secKeyData)
//                    throws PGPException
//            {
//                if (keyAlgorithm == PublicKeyAlgorithmTags.ECDH)
//                {
//                    throw new PGPException("ECDH requires use of PGPPrivateKey for decryption");
//                }
//                return decryptSessionData(keyAlgorithm, privKey, secKeyData);
//            }
//
//            public PGPDataDecryptor createDataDecryptor(boolean withIntegrityPacket, int encAlgorithm, byte[] key)
//                    throws PGPException
//            {
//                return contentHelper.createDataDecryptor(withIntegrityPacket, encAlgorithm, key);
//            }
//        };
//    }

//    public PublicKeyDataDecryptorFactory build(final PGPPrivateKey privKey, final byte[] nfcDecrypted)
//    {
//        return new PublicKeyDataDecryptorFactory()
//        {
//            public byte[] recoverSessionData(int keyAlgorithm, byte[][] secKeyData)
//                    throws PGPException
//            {
//                if (keyAlgorithm == PublicKeyAlgorithmTags.ECDH)
//                {
//                    return decryptSessionData(privKey.getPrivateKeyDataPacket(), privKey.getPublicKeyPacket(), secKeyData);
//                }
//
//                return decryptSessionData(keyAlgorithm, keyConverter.getPrivateKey(privKey), secKeyData, nfcDecrypted);
//            }
//
//            public PGPDataDecryptor createDataDecryptor(boolean withIntegrityPacket, int encAlgorithm, byte[] key)
//                    throws PGPException
//            {
//                return contentHelper.createDataDecryptor(withIntegrityPacket, encAlgorithm, key);
//            }
//        };
//    }

//    private byte[] decryptSessionData(BCPGKey privateKeyPacket, PublicKeyPacket pubKeyData, byte[][] secKeyData)
//            throws PGPException
//    {
//        ECDHPublicBCPGKey ecKey = (ECDHPublicBCPGKey)pubKeyData.getKey();
//        X9ECParameters x9Params = NISTNamedCurves.getByOID(ecKey.getCurveOID());
//
//        byte[] enc = secKeyData[0];
//
//        int pLen = ((((enc[0] & 0xff) << 8) + (enc[1] & 0xff)) + 7) / 8;
//        byte[] pEnc = new byte[pLen];
//
//        System.arraycopy(enc, 2, pEnc, 0, pLen);
//
//        byte[] keyEnc = new byte[enc[pLen + 2]];
//
//        System.arraycopy(enc, 2 + pLen + 1, keyEnc, 0, keyEnc.length);
//
//        Cipher c = helper.createKeyWrapper(ecKey.getSymmetricKeyAlgorithm());
//
//        ECPoint S = x9Params.getCurve().decodePoint(pEnc).multiply(((ECSecretBCPGKey)privateKeyPacket).getX()).normalize();
//
//        RFC6637KDFCalculator rfc6637KDFCalculator = new RFC6637KDFCalculator(digestCalculatorProviderBuilder.build().get(ecKey.getHashAlgorithm()), ecKey.getSymmetricKeyAlgorithm());
//        Key key = new SecretKeySpec(rfc6637KDFCalculator.createKey(ecKey.getCurveOID(), S, fingerprintCalculator.calculateFingerprint(pubKeyData)), "AESWrap");
//
//        try
//        {
//            c.init(Cipher.UNWRAP_MODE, key);
//
//            Key paddedSessionKey = c.unwrap(keyEnc, "Session", Cipher.SECRET_KEY);
//
//            return PGPPad.unpadSessionData(paddedSessionKey.getEncoded());
//        }
//        catch (InvalidKeyException e)
//        {
//            throw new PGPException("error setting asymmetric cipher", e);
//        }
//        catch (NoSuchAlgorithmException e)
//        {
//            throw new PGPException("error setting asymmetric cipher", e);
//        }
//    }

    private byte[] decryptSessionData(int keyAlgorithm, byte[][] secKeyData,
            Map<ByteBuffer,byte[]> nfcDecryptedMap)
        throws PGPException
    {
//        Cipher c1 = helper.createPublicKeyCipher(keyAlgorithm);
//
//        try
//        {
//            c1.init(Cipher.DECRYPT_MODE, privKey);
//        }
//        catch (InvalidKeyException e)
//        {
//            throw new PGPException("error setting asymmetric cipher", e);
//        }

        if (keyAlgorithm == PGPPublicKey.RSA_ENCRYPT
                || keyAlgorithm == PGPPublicKey.RSA_GENERAL)
        {
            ByteBuffer bi = ByteBuffer.wrap(secKeyData[0]);  // encoded MPI

            if (nfcDecryptedMap.containsKey(bi)) {
                return nfcDecryptedMap.get(bi);
            } else {
                // catch this when decryptSessionData() is executed and divert digest to card,
                // when doing the operation again reuse nfcDecrypted
                throw new NfcInteractionNeeded(bi.array());
            }

//            c1.update(bi, 2, bi.length - 2);
        }
        else
        {
            throw new PGPException("ElGamal not supported!");

//            ElGamalKey k = (ElGamalKey)privKey;
//            int size = (k.getParameters().getP().bitLength() + 7) / 8;
//            byte[] tmp = new byte[size];
//
//            byte[] bi = secKeyData[0]; // encoded MPI
//            if (bi.length - 2 > size)  // leading Zero? Shouldn't happen but...
//            {
//                c1.update(bi, 3, bi.length - 3);
//            }
//            else
//            {
//                System.arraycopy(bi, 2, tmp, tmp.length - (bi.length - 2), bi.length - 2);
//                c1.update(tmp);
//            }
//
//            bi = secKeyData[1];  // encoded MPI
//            for (int i = 0; i != tmp.length; i++)
//            {
//                tmp[i] = 0;
//            }
//
//            if (bi.length - 2 > size) // leading Zero? Shouldn't happen but...
//            {
//                c1.update(bi, 3, bi.length - 3);
//            }
//            else
//            {
//                System.arraycopy(bi, 2, tmp, tmp.length - (bi.length - 2), bi.length - 2);
//                c1.update(tmp);
//            }
        }

//        try
//        {
//            return c1.doFinal();
//        }
//        catch (Exception e)
//        {
//            throw new PGPException("exception decrypting session data", e);
//        }
    }
}
