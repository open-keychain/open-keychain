package org.spongycastle.openpgp.operator;

import org.spongycastle.bcpg.ContainedPacket;
import org.spongycastle.openpgp.PGPException;

public abstract class PGPKeyEncryptionMethodGenerator
{
    public abstract ContainedPacket generate(int encAlgorithm, byte[] sessionInfo)
        throws PGPException;
}
