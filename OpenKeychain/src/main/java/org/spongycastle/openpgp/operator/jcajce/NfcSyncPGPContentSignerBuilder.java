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
import java.security.Provider;
import java.util.Date;

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

    private byte[] signedHash;
    private Date creationTimestamp;

    public static class NfcInteractionNeeded extends RuntimeException
    {
        public byte[] hashToSign;
        public Date creationTimestamp;
        public int hashAlgo;

        public NfcInteractionNeeded(byte[] hashToSign, int hashAlgo, Date creationTimestamp)
        {
            super("NFC interaction required!");
            this.hashToSign = hashToSign;
            this.hashAlgo = hashAlgo;
            this.creationTimestamp = creationTimestamp;
        }
    }

    public NfcSyncPGPContentSignerBuilder(int keyAlgorithm, int hashAlgorithm, long keyID, byte[] signedHash, Date creationTimestamp)
    {
        this.keyAlgorithm = keyAlgorithm;
        this.hashAlgorithm = hashAlgorithm;
        this.keyID = keyID;
        this.signedHash = signedHash;
        this.creationTimestamp = creationTimestamp;
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
                if (signedHash != null) {
                    // we already have the signed hash from a previous execution, return this!
                    return signedHash;
                } else {
                    // catch this when signatureGenerator.generate() is executed and divert digest to card,
                    // when doing the operation again reuse creationTimestamp (this will be hashed)
                    throw new NfcInteractionNeeded(digestCalculator.getDigest(), getHashAlgorithm(), creationTimestamp);
                }
            }

            public byte[] getDigest()
            {
                return digestCalculator.getDigest();
            }
        };
    }
}
