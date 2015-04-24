/**
 * Copyright (c) 2013-2014 Philipp Jakubeit, Signe Rüsch, Dominik Schürmann
 * Copyright (c) 2000-2013 The Legion of the Bouncy Castle Inc. (http://www.bouncycastle.org)
 *
 * Licensed under the Bouncy Castle License (MIT license). See LICENSE file for details.
 */

package org.spongycastle.openpgp.operator.jcajce;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.operator.PGPContentSigner;
import org.spongycastle.openpgp.operator.PGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.PGPDigestCalculator;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.Provider;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * This class is based on JcaPGPContentSignerBuilder.
 *
 * Instead of using a Signature object based on a privateKey, this class only calculates the digest
 * of the output stream and gives the result back using a RuntimeException.
 */
public class NfcSyncPGPContentSignerBuilder
    implements PGPContentSignerBuilder
{
    private JcaPGPDigestCalculatorProviderBuilder digestCalculatorProviderBuilder = new JcaPGPDigestCalculatorProviderBuilder();
    private int     hashAlgorithm;
    private int     keyAlgorithm;
    private long    keyID;

    private Map signedHashes;

    public static class NfcInteractionNeeded extends RuntimeException
    {
        public byte[] hashToSign;
        public int hashAlgo;

        public NfcInteractionNeeded(byte[] hashToSign, int hashAlgo)
        {
            super("NFC interaction required!");
            this.hashToSign = hashToSign;
            this.hashAlgo = hashAlgo;
        }
    }

    public NfcSyncPGPContentSignerBuilder(int keyAlgorithm, int hashAlgorithm, long keyID, Map signedHashes)
    {
        this.keyAlgorithm = keyAlgorithm;
        this.hashAlgorithm = hashAlgorithm;
        this.keyID = keyID;
        this.signedHashes = signedHashes;
    }

    public NfcSyncPGPContentSignerBuilder setProvider(Provider provider)
    {
        digestCalculatorProviderBuilder.setProvider(provider);

        return this;
    }

    public NfcSyncPGPContentSignerBuilder setProvider(String providerName)
    {
        digestCalculatorProviderBuilder.setProvider(providerName);

        return this;
    }

    public NfcSyncPGPContentSignerBuilder setDigestProvider(Provider provider)
    {
        digestCalculatorProviderBuilder.setProvider(provider);

        return this;
    }

    public NfcSyncPGPContentSignerBuilder setDigestProvider(String providerName)
    {
        digestCalculatorProviderBuilder.setProvider(providerName);

        return this;
    }

    public PGPContentSigner build(final int signatureType, PGPPrivateKey privateKey)
        throws PGPException {
        // NOTE: privateKey is null in this case!
        return build(signatureType, keyID);
    }

    public PGPContentSigner build(final int signatureType, final long keyID)
        throws PGPException
    {
        final PGPDigestCalculator digestCalculator = digestCalculatorProviderBuilder.build().get(hashAlgorithm);

        return new PGPContentSigner()
        {
            public int getType()
            {
                return signatureType;
            }

            public int getHashAlgorithm()
            {
                return hashAlgorithm;
            }

            public int getKeyAlgorithm()
            {
                return keyAlgorithm;
            }

            public long getKeyID()
            {
                return keyID;
            }

            public OutputStream getOutputStream()
            {
                return digestCalculator.getOutputStream();
            }

            public byte[] getSignature() {
                byte[] digest = digestCalculator.getDigest();
                ByteBuffer buf = ByteBuffer.wrap(digest);
                if (signedHashes.containsKey(buf)) {
                    return (byte[]) signedHashes.get(buf);
                }
                // catch this when signatureGenerator.generate() is executed and divert digest to card,
                // when doing the operation again reuse creationTimestamp (this will be hashed)
                throw new NfcInteractionNeeded(digest, getHashAlgorithm());
            }

            public byte[] getDigest()
            {
                return digestCalculator.getDigest();
            }
        };
    }
}
