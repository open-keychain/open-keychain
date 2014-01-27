package org.spongycastle.cert.test;

import java.io.IOException;

import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.x509.SubjectKeyIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.cert.X509ExtensionUtils;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;
import org.spongycastle.util.test.SimpleTest;

public class X509ExtensionUtilsTest
    extends SimpleTest
{
    private static byte[] pubKeyInfo = Base64.decode(
        "MFgwCwYJKoZIhvcNAQEBA0kAMEYCQQC6wMMmHYMZszT/7bNFMn+gaZoiWJLVP8ODRuu1C2jeAe" +
        "QpxM+5Oe7PaN2GNy3nBE4EOYkB5pMJWA0y9n04FX8NAgED");

    private static byte[] shaID = Hex.decode("d8128a06d6c2feb0865994a2936e7b75b836a021");
    private static byte[] shaTruncID = Hex.decode("436e7b75b836a021");
    private X509ExtensionUtils x509ExtensionUtils = new X509ExtensionUtils(new SHA1DigestCalculator());

    public String getName()
    {
        return "X509ExtensionUtilsTest";
    }

    public void performTest()
        throws IOException
    {
        SubjectPublicKeyInfo pubInfo = SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(pubKeyInfo));

        SubjectKeyIdentifier ski = x509ExtensionUtils.createSubjectKeyIdentifier(pubInfo);

        if (!Arrays.areEqual(shaID, ski.getKeyIdentifier()))
        {
            fail("SHA-1 ID does not match");
        }

        ski = x509ExtensionUtils.createTruncatedSubjectKeyIdentifier(pubInfo);

        if (!Arrays.areEqual(shaTruncID, ski.getKeyIdentifier()))
        {
            fail("truncated SHA-1 ID does not match");
        }
    }

    public static void main(
        String[]    args)
    {
        runTest(new X509ExtensionUtilsTest());
    }
}
