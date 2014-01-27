package org.spongycastle.openpgp.test;

import java.security.Security;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPPublicKeyRingCollection;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.util.test.SimpleTest;

public class PGPParsingTest
    extends SimpleTest
{
    public void performTest()
        throws Exception
    {
        PGPPublicKeyRingCollection pubRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(this.getClass().getResourceAsStream("bigpub.asc")));
    }

    public String getName()
    {
        return "PGPParsingTest";
    }

    public static void main(
        String[]    args)
    {
        Security.addProvider(new BouncyCastleProvider());

        runTest(new PGPParsingTest());
    }
}
