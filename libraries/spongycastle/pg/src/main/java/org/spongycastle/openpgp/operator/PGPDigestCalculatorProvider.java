package org.spongycastle.openpgp.operator;

import org.spongycastle.openpgp.PGPException;

public interface PGPDigestCalculatorProvider
{
    PGPDigestCalculator get(int algorithm)
        throws PGPException;
}
