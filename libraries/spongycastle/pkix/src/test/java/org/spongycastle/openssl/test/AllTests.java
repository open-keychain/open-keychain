package org.spongycastle.openssl.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openssl.PEMParser;
import org.spongycastle.openssl.PEMWriter;
import org.spongycastle.openssl.PKCS8Generator;
import org.spongycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.spongycastle.openssl.jcajce.JcaPKCS8Generator;
import org.spongycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.spongycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.spongycastle.pkcs.PKCSException;
import org.spongycastle.util.test.SimpleTestResult;

public class
    AllTests
    extends TestCase
{
    public void testOpenSSL()
    {
        if (Security.getProvider("SC") == null)
        {
            Security.addProvider(new BouncyCastleProvider());
        }
        
        org.spongycastle.util.test.Test[] tests = new org.spongycastle.util.test.Test[]
        {
            new WriterTest(),
            new ParserTest()
        };

        for (int i = 0; i != tests.length; i++)
        {
            SimpleTestResult  result = (SimpleTestResult)tests[i].perform();
            
            if (!result.isSuccessful())
            {
                fail(result.toString());
            }
        }
    }

    public void testPKCS8Encrypted()
        throws Exception
    {
        if (Security.getProvider("SC") == null)
        {
            Security.addProvider(new BouncyCastleProvider());
        }

        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "SC");

        kpGen.initialize(1024);

        PrivateKey key = kpGen.generateKeyPair().getPrivate();

        encryptedTestNew(key, PKCS8Generator.AES_256_CBC);
        encryptedTestNew(key, PKCS8Generator.DES3_CBC);
        encryptedTestNew(key, PKCS8Generator.PBE_SHA1_3DES);
    }

    private void encryptedTestNew(PrivateKey key, ASN1ObjectIdentifier algorithm)
        throws NoSuchProviderException, NoSuchAlgorithmException, IOException, OperatorCreationException, PKCSException
    {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PEMWriter pWrt = new PEMWriter(new OutputStreamWriter(bOut));

        JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder = new JceOpenSSLPKCS8EncryptorBuilder(algorithm);

        encryptorBuilder.setProvider("SC");
        encryptorBuilder.setPasssword("hello".toCharArray());

        PKCS8Generator pkcs8 = new JcaPKCS8Generator(key, encryptorBuilder.build());

        pWrt.writeObject(pkcs8);

        pWrt.close();

        PEMParser pRd = new PEMParser(new InputStreamReader(new ByteArrayInputStream(bOut.toByteArray())));

        PKCS8EncryptedPrivateKeyInfo pInfo = (PKCS8EncryptedPrivateKeyInfo)pRd.readObject();

        PrivateKey rdKey = new JcaPEMKeyConverter().setProvider("SC").getPrivateKey(pInfo.decryptPrivateKeyInfo(new JceOpenSSLPKCS8DecryptorProviderBuilder().setProvider("SC").build("hello".toCharArray())));


        assertEquals(key, rdKey);
    }

    public void testPKCS8PlainNew()
        throws Exception
    {
        if (Security.getProvider("SC") == null)
        {
            Security.addProvider(new BouncyCastleProvider());
        }

        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "SC");

        kpGen.initialize(1024);

        PrivateKey key = kpGen.generateKeyPair().getPrivate();
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PEMWriter pWrt = new PEMWriter(new OutputStreamWriter(bOut));
        PKCS8Generator pkcs8 = new JcaPKCS8Generator(key, null);

        pWrt.writeObject(pkcs8);

        pWrt.close();

        PEMParser pRd = new PEMParser(new InputStreamReader(new ByteArrayInputStream(bOut.toByteArray())));

        PrivateKeyInfo kp = (PrivateKeyInfo)pRd.readObject();

        PrivateKey rdKey = new JcaPEMKeyConverter().setProvider("SC").getPrivateKey(kp);

        assertEquals(key, rdKey);
    }

    public static void main (String[] args)
    {
        Security.addProvider(new BouncyCastleProvider());
        
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite()
    {
        TestSuite suite = new TestSuite("OpenSSL Tests");
        
        suite.addTestSuite(AllTests.class);
        
        return suite;
    }
}
