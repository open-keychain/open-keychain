package org.spongycastle.openpgp.operator;

import org.spongycastle.bcpg.PublicKeyPacket;
import org.spongycastle.openpgp.PGPException;

public interface KeyFingerPrintCalculator
{
    byte[] calculateFingerprint(PublicKeyPacket publicPk)
        throws PGPException;
}
