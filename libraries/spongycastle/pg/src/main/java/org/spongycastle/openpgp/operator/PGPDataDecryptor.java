package org.spongycastle.openpgp.operator;

import java.io.InputStream;

public interface PGPDataDecryptor
{
    InputStream getInputStream(InputStream in);

    int getBlockSize();

    PGPDigestCalculator getIntegrityCalculator();
}
