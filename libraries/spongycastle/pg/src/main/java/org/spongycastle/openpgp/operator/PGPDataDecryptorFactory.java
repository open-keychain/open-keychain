package org.spongycastle.openpgp.operator;

import org.spongycastle.openpgp.PGPException;

public interface PGPDataDecryptorFactory
{
    public PGPDataDecryptor createDataDecryptor(boolean withIntegrityPacket, int encAlgorithm, byte[] key)
        throws PGPException;
}
