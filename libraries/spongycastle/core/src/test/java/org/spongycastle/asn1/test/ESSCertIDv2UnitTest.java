package org.spongycastle.asn1.test;

import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ess.ESSCertIDv2;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;

public class ESSCertIDv2UnitTest
    extends ASN1UnitTest
{
    public String getName()
    {
        return "ESSCertIDv2";
    }

    public void performTest()
        throws Exception
    {
        // check getInstance on default algorithm.
        byte[] digest = new byte [256];
        ESSCertIDv2 essCertIdv2 = new ESSCertIDv2(new AlgorithmIdentifier(
            NISTObjectIdentifiers.id_sha256), digest);
        ASN1Primitive asn1Object = essCertIdv2.toASN1Primitive();

        ESSCertIDv2.getInstance(asn1Object);
    }

    public static void main(
        String[]    args)
    {
        runTest(new ESSCertIDv2UnitTest());
    }
}