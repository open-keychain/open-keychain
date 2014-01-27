package org.spongycastle.tsp.test;

import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.cmp.PKIFailureInfo;
import org.spongycastle.asn1.cmp.PKIStatus;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.ess.ESSCertID;
import org.spongycastle.asn1.ess.ESSCertIDv2;
import org.spongycastle.asn1.ess.SigningCertificate;
import org.spongycastle.asn1.ess.SigningCertificateV2;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.GeneralNames;
import org.spongycastle.asn1.x509.IssuerSerial;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.jcajce.JcaCertStore;
import org.spongycastle.cms.CMSAttributeTableGenerationException;
import org.spongycastle.cms.CMSAttributeTableGenerator;
import org.spongycastle.cms.DefaultSignedAttributeTableGenerator;
import org.spongycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.spongycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.spongycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.spongycastle.tsp.GenTimeAccuracy;
import org.spongycastle.tsp.TSPAlgorithms;
import org.spongycastle.tsp.TSPException;
import org.spongycastle.tsp.TSPValidationException;
import org.spongycastle.tsp.TimeStampRequest;
import org.spongycastle.tsp.TimeStampRequestGenerator;
import org.spongycastle.tsp.TimeStampResponse;
import org.spongycastle.tsp.TimeStampResponseGenerator;
import org.spongycastle.tsp.TimeStampToken;
import org.spongycastle.tsp.TimeStampTokenGenerator;
import org.spongycastle.tsp.TimeStampTokenInfo;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.Store;

public class NewTSPTest
    extends TestCase
{
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    public void setUp()
    {
        Security.addProvider(new BouncyCastleProvider());
    }

    public void testGeneral()
        throws Exception
    {
            String signDN = "O=Bouncy Castle, C=AU";
            KeyPair signKP = TSPTestUtil.makeKeyPair();
            X509Certificate signCert = TSPTestUtil.makeCACertificate(signKP,
                    signDN, signKP, signDN);

            String origDN = "CN=Eric H. Echidna, E=eric@bouncycastle.org, O=Bouncy Castle, C=AU";
            KeyPair origKP = TSPTestUtil.makeKeyPair();
            X509Certificate origCert = TSPTestUtil.makeCertificate(origKP,
                    origDN, signKP, signDN);


            
            List certList = new ArrayList();
            certList.add(origCert);
            certList.add(signCert);

            Store certs = new JcaCertStore(certList);
            
            basicTest(origKP.getPrivate(), origCert, certs);
            basicSha256Test(origKP.getPrivate(), origCert, certs);
            basicTestWithTSA(origKP.getPrivate(), origCert, certs);
            overrideAttrsTest(origKP.getPrivate(), origCert, certs);
            responseValidationTest(origKP.getPrivate(), origCert, certs);
            incorrectHashTest(origKP.getPrivate(), origCert, certs);
            badAlgorithmTest(origKP.getPrivate(), origCert, certs);
            timeNotAvailableTest(origKP.getPrivate(), origCert, certs);
            badPolicyTest(origKP.getPrivate(), origCert, certs);
            tokenEncodingTest(origKP.getPrivate(), origCert, certs);
            certReqTest(origKP.getPrivate(), origCert, certs);
            testAccuracyZeroCerts(origKP.getPrivate(), origCert, certs);
            testAccuracyWithCertsAndOrdering(origKP.getPrivate(), origCert, certs);
            testNoNonse(origKP.getPrivate(), origCert, certs);
    }
    
    private void basicTest(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store certs)
        throws Exception
    {
        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(
                new JcaSimpleSignerInfoGeneratorBuilder().build("SHA1withRSA", privateKey, cert), new SHA1DigestCalculator(), new ASN1ObjectIdentifier("1.2"));
        
        tsTokenGen.addCertificates(certs);

        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        TimeStampRequest          request = reqGen.generate(TSPAlgorithms.SHA1, new byte[20], BigInteger.valueOf(100));

        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED);

        TimeStampResponse tsResp = tsRespGen.generate(request, new BigInteger("23"), new Date());

        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampToken  tsToken = tsResp.getTimeStampToken();

        tsToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert));

        AttributeTable  table = tsToken.getSignedAttributes();

        assertNotNull("no signingCertificate attribute found", table.get(PKCSObjectIdentifiers.id_aa_signingCertificate));
    }

    private void basicSha256Test(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store certs)
        throws Exception
    {
        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(
                new JcaSimpleSignerInfoGeneratorBuilder().build("SHA256withRSA", privateKey, cert), new SHA256DigestCalculator(), new ASN1ObjectIdentifier("1.2"));

        tsTokenGen.addCertificates(certs);

        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        TimeStampRequest          request = reqGen.generate(TSPAlgorithms.SHA256, new byte[32], BigInteger.valueOf(100));

        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED);

        TimeStampResponse tsResp = tsRespGen.generate(request, new BigInteger("23"), new Date());

        assertEquals(PKIStatus.GRANTED, tsResp.getStatus());

        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampToken  tsToken = tsResp.getTimeStampToken();

        tsToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert));

        AttributeTable  table = tsToken.getSignedAttributes();

        assertNotNull("no signingCertificate attribute found", table.get(PKCSObjectIdentifiers.id_aa_signingCertificateV2));

        DigestCalculator digCalc = new SHA256DigestCalculator();

        OutputStream dOut = digCalc.getOutputStream();

        dOut.write(cert.getEncoded());

        dOut.close();

        byte[] certHash = digCalc.getDigest();

        SigningCertificateV2 sigCertV2 = SigningCertificateV2.getInstance(table.get(PKCSObjectIdentifiers.id_aa_signingCertificateV2).getAttributeValues()[0]);

        assertTrue(Arrays.areEqual(certHash, sigCertV2.getCerts()[0].getCertHash()));
    }

    private void overrideAttrsTest(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store certs)
        throws Exception
    {
        JcaSimpleSignerInfoGeneratorBuilder signerInfoGenBuilder = new JcaSimpleSignerInfoGeneratorBuilder().setProvider("SC");

        IssuerSerial issuerSerial = new IssuerSerial(new GeneralNames(new GeneralName(new X509CertificateHolder(cert.getEncoded()).getIssuer())), cert.getSerialNumber());

        DigestCalculator digCalc = new SHA1DigestCalculator();

        OutputStream dOut = digCalc.getOutputStream();

        dOut.write(cert.getEncoded());

        dOut.close();

        byte[] certHash = digCalc.getDigest();

        digCalc = new SHA256DigestCalculator();

        dOut = digCalc.getOutputStream();

        dOut.write(cert.getEncoded());

        dOut.close();

        byte[] certHash256 = digCalc.getDigest();

        final ESSCertID essCertid = new ESSCertID(certHash, issuerSerial);
        final ESSCertIDv2 essCertidV2 = new ESSCertIDv2(certHash256, issuerSerial);

        signerInfoGenBuilder.setSignedAttributeGenerator(new CMSAttributeTableGenerator()
        {
            public AttributeTable getAttributes(Map parameters)
                throws CMSAttributeTableGenerationException
            {
                CMSAttributeTableGenerator attrGen = new DefaultSignedAttributeTableGenerator();

                AttributeTable table = attrGen.getAttributes(parameters);
                table = table.add(PKCSObjectIdentifiers.id_aa_signingCertificate, new SigningCertificate(essCertid));
                table = table.add(PKCSObjectIdentifiers.id_aa_signingCertificateV2, new SigningCertificateV2(new ESSCertIDv2[]{essCertidV2}));

                return table;
            }
        });

        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(signerInfoGenBuilder.build("SHA1withRSA", privateKey, cert), new SHA1DigestCalculator(), new ASN1ObjectIdentifier("1.2"));

        tsTokenGen.addCertificates(certs);

        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        TimeStampRequest          request = reqGen.generate(TSPAlgorithms.SHA1, new byte[20], BigInteger.valueOf(100));

        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED);

        TimeStampResponse tsResp = tsRespGen.generate(request, new BigInteger("23"), new Date());

        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampToken  tsToken = tsResp.getTimeStampToken();

        tsToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert));

        AttributeTable  table = tsToken.getSignedAttributes();

        assertNotNull("no signingCertificate attribute found", table.get(PKCSObjectIdentifiers.id_aa_signingCertificate));
        assertNotNull("no signingCertificateV2 attribute found", table.get(PKCSObjectIdentifiers.id_aa_signingCertificateV2));

        SigningCertificate sigCert = SigningCertificate.getInstance(table.get(PKCSObjectIdentifiers.id_aa_signingCertificate).getAttributeValues()[0]);

        assertEquals(new X509CertificateHolder(cert.getEncoded()).getIssuer(), sigCert.getCerts()[0].getIssuerSerial().getIssuer().getNames()[0].getName());
        assertEquals(cert.getSerialNumber(), sigCert.getCerts()[0].getIssuerSerial().getSerial().getValue());
        assertTrue(Arrays.areEqual(certHash, sigCert.getCerts()[0].getCertHash()));

        SigningCertificateV2 sigCertV2 = SigningCertificateV2.getInstance(table.get(PKCSObjectIdentifiers.id_aa_signingCertificateV2).getAttributeValues()[0]);

        assertEquals(new X509CertificateHolder(cert.getEncoded()).getIssuer(), sigCertV2.getCerts()[0].getIssuerSerial().getIssuer().getNames()[0].getName());
        assertEquals(cert.getSerialNumber(), sigCertV2.getCerts()[0].getIssuerSerial().getSerial().getValue());
        assertTrue(Arrays.areEqual(certHash256, sigCertV2.getCerts()[0].getCertHash()));
    }

    private void basicTestWithTSA(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store certs)
        throws Exception
    {
        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(
                new JcaSimpleSignerInfoGeneratorBuilder().build("SHA1withRSA", privateKey, cert), new SHA1DigestCalculator(), new ASN1ObjectIdentifier("1.2"));

        tsTokenGen.addCertificates(certs);
        tsTokenGen.setTSA(new GeneralName(new X500Name("CN=Test")));

        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        TimeStampRequest          request = reqGen.generate(TSPAlgorithms.SHA1, new byte[20], BigInteger.valueOf(100));

        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED);

        TimeStampResponse tsResp = tsRespGen.generate(request, new BigInteger("23"), new Date());

        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampToken  tsToken = tsResp.getTimeStampToken();

        tsToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert));

        AttributeTable  table = tsToken.getSignedAttributes();

        assertNotNull("no signingCertificate attribute found", table.get(PKCSObjectIdentifiers.id_aa_signingCertificate));
    }

    private void responseValidationTest(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store       certs)
        throws Exception
    {
        JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider(BC).build());

        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(
            infoGeneratorBuilder.build(new JcaContentSignerBuilder("MD5withRSA").setProvider(BC).build(privateKey), cert), new SHA1DigestCalculator(), new ASN1ObjectIdentifier("1.2"));

        tsTokenGen.addCertificates(certs);

        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        TimeStampRequest          request = reqGen.generate(TSPAlgorithms.SHA1, new byte[20], BigInteger.valueOf(100));

        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED);

        TimeStampResponse tsResp = tsRespGen.generate(request, new BigInteger("23"), new Date());

        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampToken  tsToken = tsResp.getTimeStampToken();

        tsToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider("SC").build(cert));
        
        //
        // check validation
        //
        tsResp.validate(request);
        
        try
        {
            request = reqGen.generate(TSPAlgorithms.SHA1, new byte[20], BigInteger.valueOf(101));
            
            tsResp.validate(request);
            
            fail("response validation failed on invalid nonce.");
        }
        catch (TSPValidationException e)
        {
            // ignore
        }

        try
        {
            request = reqGen.generate(TSPAlgorithms.SHA1, new byte[22], BigInteger.valueOf(100));
            
            tsResp.validate(request);
            
            fail("response validation failed on wrong digest.");
        }
        catch (TSPValidationException e)
        {
            // ignore
        }
        
        try
        {
            request = reqGen.generate(TSPAlgorithms.MD5, new byte[20], BigInteger.valueOf(100));
            
            tsResp.validate(request);
            
            fail("response validation failed on wrong digest.");
        }
        catch (TSPValidationException e)
        {
            // ignore
        }
    }
    
    private void incorrectHashTest(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store       certs)
        throws Exception
    {
        JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider(BC).build());

        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(infoGeneratorBuilder.build(new JcaContentSignerBuilder("SHA1withRSA").setProvider(BC).build(privateKey), cert), new SHA1DigestCalculator(), new ASN1ObjectIdentifier("1.2"));
        
        tsTokenGen.addCertificates(certs);

        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        TimeStampRequest          request = reqGen.generate(TSPAlgorithms.SHA1, new byte[16]);

        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED);

        TimeStampResponse tsResp = tsRespGen.generate(request, new BigInteger("23"), new Date());

        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampToken  tsToken = tsResp.getTimeStampToken();

        if (tsToken != null)
        {
            fail("incorrectHash - token not null.");
        }
        
        PKIFailureInfo  failInfo = tsResp.getFailInfo();
        
        if (failInfo == null)
        {
            fail("incorrectHash - failInfo set to null.");
        }
        
        if (failInfo.intValue() != PKIFailureInfo.badDataFormat)
        {
            fail("incorrectHash - wrong failure info returned.");
        }
    }
    
    private void badAlgorithmTest(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store       certs)
        throws Exception
    {
        JcaSimpleSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSimpleSignerInfoGeneratorBuilder().setProvider(BC);

        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(infoGeneratorBuilder.build("SHA1withRSA", privateKey, cert), new SHA1DigestCalculator(), new ASN1ObjectIdentifier("1.2"));

        tsTokenGen.addCertificates(certs);

        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        TimeStampRequest            request = reqGen.generate(new ASN1ObjectIdentifier("1.2.3.4.5"), new byte[20]);

        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED);

        TimeStampResponse tsResp = tsRespGen.generate(request, new BigInteger("23"), new Date());

        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampToken  tsToken = tsResp.getTimeStampToken();

        if (tsToken != null)
        {
            fail("badAlgorithm - token not null.");
        }

        PKIFailureInfo  failInfo = tsResp.getFailInfo();
        
        if (failInfo == null)
        {
            fail("badAlgorithm - failInfo set to null.");
        }
        
        if (failInfo.intValue() != PKIFailureInfo.badAlg)
        {
            fail("badAlgorithm - wrong failure info returned.");
        }
    }

    private void timeNotAvailableTest(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store       certs)
        throws Exception
    {
        JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider(BC).build());

        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(infoGeneratorBuilder.build(new JcaContentSignerBuilder("SHA1withRSA").setProvider(BC).build(privateKey), cert), new SHA1DigestCalculator(), new ASN1ObjectIdentifier("1.2"));

        tsTokenGen.addCertificates(certs);

        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        TimeStampRequest            request = reqGen.generate(new ASN1ObjectIdentifier("1.2.3.4.5"), new byte[20]);

        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED);

        TimeStampResponse tsResp;

        try
        {
            tsResp = tsRespGen.generateGrantedResponse(request, new BigInteger("23"), null);
        }
        catch (TSPException e)
        {
            tsResp = tsRespGen.generateRejectedResponse(e);
        }

        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampToken  tsToken = tsResp.getTimeStampToken();

        if (tsToken != null)
        {
            fail("timeNotAvailable - token not null.");
        }

        PKIFailureInfo  failInfo = tsResp.getFailInfo();

        if (failInfo == null)
        {
            fail("timeNotAvailable - failInfo set to null.");
        }

        if (failInfo.intValue() != PKIFailureInfo.timeNotAvailable)
        {
            fail("timeNotAvailable - wrong failure info returned.");
        }
    }

    private void badPolicyTest(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store       certs)
        throws Exception
    {
        JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider(BC).build());

        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(infoGeneratorBuilder.build(new JcaContentSignerBuilder("SHA1withRSA").setProvider(BC).build(privateKey), cert), new SHA1DigestCalculator(), new ASN1ObjectIdentifier("1.2"));

        tsTokenGen.addCertificates(certs);

        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        
        reqGen.setReqPolicy(new ASN1ObjectIdentifier("1.1"));
        
        TimeStampRequest            request = reqGen.generate(TSPAlgorithms.SHA1, new byte[20]);

        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED, new HashSet());

        TimeStampResponse tsResp;

        try
        {
            tsResp = tsRespGen.generateGrantedResponse(request, new BigInteger("23"), new Date());
        }
        catch (TSPException e)
        {
            tsResp = tsRespGen.generateRejectedResponse(e);
        }

        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampToken  tsToken = tsResp.getTimeStampToken();

        if (tsToken != null)
        {
            fail("badPolicy - token not null.");
        }

        PKIFailureInfo  failInfo = tsResp.getFailInfo();
        
        if (failInfo == null)
        {
            fail("badPolicy - failInfo set to null.");
        }
        
        if (failInfo.intValue() != PKIFailureInfo.unacceptedPolicy)
        {
            fail("badPolicy - wrong failure info returned.");
        }
    }
    
    private void certReqTest(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store       certs)
        throws Exception
    {
        JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider(BC).build());

        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(infoGeneratorBuilder.build(new JcaContentSignerBuilder("MD5withRSA").setProvider(BC).build(privateKey), cert), new SHA1DigestCalculator(), new ASN1ObjectIdentifier("1.2"));

        tsTokenGen.addCertificates(certs);
        
        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        
        //
        // request with certReq false
        //
        reqGen.setCertReq(false);
        
        TimeStampRequest          request = reqGen.generate(TSPAlgorithms.SHA1, new byte[20], BigInteger.valueOf(100));

        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED);

        TimeStampResponse tsResp = tsRespGen.generateGrantedResponse(request, new BigInteger("23"), new Date());
        
        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampToken  tsToken = tsResp.getTimeStampToken();
        
        assertNull(tsToken.getTimeStampInfo().getGenTimeAccuracy());  // check for abscence of accuracy
        
        assertEquals("1.2", tsToken.getTimeStampInfo().getPolicy().getId());
        
        try
        {
            tsToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(cert));
        }
        catch (TSPValidationException e)
        {
            fail("certReq(false) verification of token failed.");
        }

        Store   respCerts = tsToken.getCertificates();
        
        Collection  certsColl = respCerts.getMatches(null);
        
        if (!certsColl.isEmpty())
        {
            fail("certReq(false) found certificates in response.");
        }
    }
    
    
    private void tokenEncodingTest(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store       certs)
        throws Exception
    {
        JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider(BC).build());

        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(infoGeneratorBuilder.build(new JcaContentSignerBuilder("SHA1withRSA").setProvider(BC).build(privateKey), cert), new SHA1DigestCalculator(), new ASN1ObjectIdentifier("1.2.3.4.5.6"));

        tsTokenGen.addCertificates(certs);

        TimeStampRequestGenerator  reqGen = new TimeStampRequestGenerator();
        TimeStampRequest           request = reqGen.generate(TSPAlgorithms.SHA1, new byte[20], BigInteger.valueOf(100));
        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED);
        TimeStampResponse          tsResp = tsRespGen.generate(request, new BigInteger("23"), new Date());

        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampResponse tsResponse = new TimeStampResponse(tsResp.getEncoded());

        if (!Arrays.areEqual(tsResponse.getEncoded(), tsResp.getEncoded())
            || !Arrays.areEqual(tsResponse.getTimeStampToken().getEncoded(),
                        tsResp.getTimeStampToken().getEncoded()))
        {
            fail();
        }
    }
    
    private void testAccuracyZeroCerts(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store       certs)
        throws Exception
    {
        JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider(BC).build());

        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(infoGeneratorBuilder.build(new JcaContentSignerBuilder("MD5withRSA").setProvider(BC).build(privateKey), cert), new SHA1DigestCalculator(), new ASN1ObjectIdentifier("1.2"));

        tsTokenGen.addCertificates(certs);

        tsTokenGen.setAccuracySeconds(1);
        tsTokenGen.setAccuracyMillis(2);
        tsTokenGen.setAccuracyMicros(3);
        
        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        TimeStampRequest          request = reqGen.generate(TSPAlgorithms.SHA1, new byte[20], BigInteger.valueOf(100));

        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED);

        TimeStampResponse tsResp = tsRespGen.generate(request, new BigInteger("23"), new Date());

        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampToken  tsToken = tsResp.getTimeStampToken();

        tsToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider("SC").build(cert));
        
        //
        // check validation
        //
        tsResp.validate(request);

        //
        // check tstInfo
        //
        TimeStampTokenInfo tstInfo = tsToken.getTimeStampInfo();
        
        //
        // check accuracy
        //
        GenTimeAccuracy accuracy = tstInfo.getGenTimeAccuracy();
        
        assertEquals(1, accuracy.getSeconds());
        assertEquals(2, accuracy.getMillis());
        assertEquals(3, accuracy.getMicros());
        
        assertEquals(new BigInteger("23"), tstInfo.getSerialNumber());
        
        assertEquals("1.2", tstInfo.getPolicy().getId());
        
        //
        // test certReq
        //
        Store store = tsToken.getCertificates();
        
        Collection certificates = store.getMatches(null);
        
        assertEquals(0, certificates.size());
    }
    
    private void testAccuracyWithCertsAndOrdering(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store       certs)
        throws Exception
    {
        JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider(BC).build());

        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(infoGeneratorBuilder.build(new JcaContentSignerBuilder("MD5withRSA").setProvider(BC).build(privateKey), cert), new SHA1DigestCalculator(), new ASN1ObjectIdentifier("1.2.3"));

        tsTokenGen.addCertificates(certs);

        tsTokenGen.setAccuracySeconds(3);
        tsTokenGen.setAccuracyMillis(1);
        tsTokenGen.setAccuracyMicros(2);
        
        tsTokenGen.setOrdering(true);
        
        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        
        reqGen.setCertReq(true);
        
        TimeStampRequest          request = reqGen.generate(TSPAlgorithms.SHA1, new byte[20], BigInteger.valueOf(100));

        assertTrue(request.getCertReq());
        
        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED);

        TimeStampResponse tsResp;

        try
        {
            tsResp = tsRespGen.generateGrantedResponse(request, new BigInteger("23"), new Date());
        }
        catch (TSPException e)
        {
            tsResp = tsRespGen.generateRejectedResponse(e);
        }

        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampToken  tsToken = tsResp.getTimeStampToken();

        tsToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider("SC").build(cert));
        
        //
        // check validation
        //
        tsResp.validate(request);

        //
        // check tstInfo
        //
        TimeStampTokenInfo tstInfo = tsToken.getTimeStampInfo();
        
        //
        // check accuracy
        //
        GenTimeAccuracy accuracy = tstInfo.getGenTimeAccuracy();
        
        assertEquals(3, accuracy.getSeconds());
        assertEquals(1, accuracy.getMillis());
        assertEquals(2, accuracy.getMicros());
        
        assertEquals(new BigInteger("23"), tstInfo.getSerialNumber());
        
        assertEquals("1.2.3", tstInfo.getPolicy().getId());
        
        assertEquals(true, tstInfo.isOrdered());
        
        assertEquals(tstInfo.getNonce(), BigInteger.valueOf(100));
        
        //
        // test certReq
        //
        Store store = tsToken.getCertificates();
        
        Collection certificates = store.getMatches(null);
        
        assertEquals(2, certificates.size());
    }   
    
    private void testNoNonse(
        PrivateKey      privateKey,
        X509Certificate cert,
        Store       certs)
        throws Exception
    {
        JcaSignerInfoGeneratorBuilder infoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider(BC).build());

        TimeStampTokenGenerator tsTokenGen = new TimeStampTokenGenerator(infoGeneratorBuilder.build(new JcaContentSignerBuilder("MD5withRSA").setProvider(BC).build(privateKey), cert), new SHA1DigestCalculator(), new ASN1ObjectIdentifier("1.2.3"));

        tsTokenGen.addCertificates(certs);
        
        TimeStampRequestGenerator reqGen = new TimeStampRequestGenerator();
        TimeStampRequest          request = reqGen.generate(TSPAlgorithms.SHA1, new byte[20]);

        assertFalse(request.getCertReq());
        
        TimeStampResponseGenerator tsRespGen = new TimeStampResponseGenerator(tsTokenGen, TSPAlgorithms.ALLOWED);

        TimeStampResponse tsResp = tsRespGen.generate(request, new BigInteger("24"), new Date());

        tsResp = new TimeStampResponse(tsResp.getEncoded());

        TimeStampToken  tsToken = tsResp.getTimeStampToken();

        tsToken.validate(new JcaSimpleSignerInfoVerifierBuilder().setProvider("SC").build(cert));
        
        //
        // check validation
        //
        tsResp.validate(request);

        //
        // check tstInfo
        //
        TimeStampTokenInfo tstInfo = tsToken.getTimeStampInfo();
        
        //
        // check accuracy
        //
        GenTimeAccuracy accuracy = tstInfo.getGenTimeAccuracy();
        
        assertNull(accuracy);
        
        assertEquals(new BigInteger("24"), tstInfo.getSerialNumber());
        
        assertEquals("1.2.3", tstInfo.getPolicy().getId());
        
        assertEquals(false, tstInfo.isOrdered());
        
        assertNull(tstInfo.getNonce());
        
        //
        // test certReq
        //
        Store store = tsToken.getCertificates();
        
        Collection certificates = store.getMatches(null);
        
        assertEquals(0, certificates.size());
    } 
}
