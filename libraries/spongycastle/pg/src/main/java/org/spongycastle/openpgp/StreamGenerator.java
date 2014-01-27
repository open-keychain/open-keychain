package org.spongycastle.openpgp;

import java.io.IOException;

interface StreamGenerator
{
    void close()
        throws IOException;
}
