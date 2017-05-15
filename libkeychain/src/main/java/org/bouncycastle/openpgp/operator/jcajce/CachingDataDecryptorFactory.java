/*
 * Copyright (c) 2013-2014 Philipp Jakubeit, Signe Rüsch, Dominik Schürmann
 * Copyright (c) 2017 Vincent Breitmoser
 *
 * Licensed under the Bouncy Castle License (MIT license). See LICENSE file for details.
 */

package org.bouncycastle.openpgp.operator.jcajce;


import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.operator.PGPDataDecryptor;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;

public class CachingDataDecryptorFactory implements PublicKeyDataDecryptorFactory
{
    private final PublicKeyDataDecryptorFactory mWrappedDecryptor;
    private final HashMap<ByteBuffer, byte[]> mSessionKeyCache;

    private OperatorHelper mOperatorHelper;

    public CachingDataDecryptorFactory(String providerName, Map<ByteBuffer, byte[]> sessionKeyCache)
    {
        this((PublicKeyDataDecryptorFactory) null, sessionKeyCache);

        mOperatorHelper = new OperatorHelper(new NamedJcaJceHelper(providerName));
    }

    public CachingDataDecryptorFactory(PublicKeyDataDecryptorFactory wrapped,
            Map<ByteBuffer, byte[]> sessionKeyCache)
    {
        mSessionKeyCache = new HashMap<>();
        if (sessionKeyCache != null)
        {
            mSessionKeyCache.putAll(sessionKeyCache);
        }

        mWrappedDecryptor = wrapped;
    }

    public boolean hasCachedSessionData(PGPPublicKeyEncryptedData encData) throws PGPException {
        ByteBuffer bi = ByteBuffer.wrap(encData.getSessionKey()[0]);
        return mSessionKeyCache.containsKey(bi);
    }

    public Map<ByteBuffer, byte[]> getCachedSessionKeys() {
        return Collections.unmodifiableMap(mSessionKeyCache);
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
