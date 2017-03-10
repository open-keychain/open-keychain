/**
 * Copyright (c) 2013-2014 Philipp Jakubeit, Signe Rüsch, Dominik Schürmann
 *
 * Licensed under the Bouncy Castle License (MIT license). See LICENSE file for details.
 */

package org.bouncycastle.openpgp.operator.jcajce;


import java.nio.ByteBuffer;
import java.util.Map;

import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.operator.PGPDataDecryptor;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;

public class CachingDataDecryptorFactory implements PublicKeyDataDecryptorFactory
{
    private final PublicKeyDataDecryptorFactory mWrappedDecryptor;
    private final Map<ByteBuffer, byte[]> mSessionKeyCache;

    private OperatorHelper mOperatorHelper;

    public CachingDataDecryptorFactory(String providerName,
            final Map<ByteBuffer,byte[]> sessionKeyCache)
    {
        mWrappedDecryptor = null;
        mSessionKeyCache = sessionKeyCache;

        mOperatorHelper = new OperatorHelper(new NamedJcaJceHelper(providerName));
    }

    public CachingDataDecryptorFactory(PublicKeyDataDecryptorFactory wrapped,
            final Map<ByteBuffer,byte[]> sessionKeyCache)
    {
        mWrappedDecryptor = wrapped;
        mSessionKeyCache = sessionKeyCache;

    }

    public boolean hasCachedSessionData(PGPPublicKeyEncryptedData encData) throws PGPException {
        ByteBuffer bi = ByteBuffer.wrap(encData.getSessionKey()[0]);
        return mSessionKeyCache.containsKey(bi);
    }

    public Map<ByteBuffer, byte[]> getCachedSessionKeys() {
        return mSessionKeyCache;
    }

    public boolean canDecrypt() {
        return mWrappedDecryptor != null;
    }

    @Override
    public byte[] recoverSessionData(int keyAlgorithm, byte[][] secKeyData) throws PGPException {
        ByteBuffer bi = ByteBuffer.wrap(secKeyData[0]);  // encoded MPI
        if (mSessionKeyCache.containsKey(bi)) {
            return mSessionKeyCache.get(bi);
        }

        if (mWrappedDecryptor == null) {
            throw new IllegalStateException("tried to decrypt without wrapped decryptor, this is a bug!");
        }

        byte[] sessionData = mWrappedDecryptor.recoverSessionData(keyAlgorithm, secKeyData);
        mSessionKeyCache.put(bi, sessionData);
        return sessionData;
    }

    @Override
    public PGPDataDecryptor createDataDecryptor(boolean withIntegrityPacket, int encAlgorithm, byte[] key)
            throws PGPException {
        if (mWrappedDecryptor != null) {
            return mWrappedDecryptor.createDataDecryptor(withIntegrityPacket, encAlgorithm, key);
        }
        return mOperatorHelper.createDataDecryptor(withIntegrityPacket, encAlgorithm, key);
    }

}
