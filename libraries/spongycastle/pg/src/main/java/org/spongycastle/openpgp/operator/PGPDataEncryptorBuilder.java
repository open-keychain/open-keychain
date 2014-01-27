package org.spongycastle.openpgp.operator;

import java.security.SecureRandom;

import org.spongycastle.openpgp.PGPException;

public interface PGPDataEncryptorBuilder
{
    int getAlgorithm();

    PGPDataEncryptor build(byte[] keyBytes)
        throws PGPException;

    SecureRandom getSecureRandom();
}
