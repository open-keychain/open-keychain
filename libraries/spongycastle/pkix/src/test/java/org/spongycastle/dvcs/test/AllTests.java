package org.spongycastle.dvcs.test;

import java.io.IOException;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.spongycastle.asn1.dvcs.CertEtcToken;
import org.spongycastle.asn1.dvcs.TargetEtcChain;
import org.spongycastle.cert.jcajce.JcaX509CertificateHolder;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.cms.CMSSignedDataGenerator;
import org.spongycastle.cms.SignerId;
import org.spongycastle.cms.SignerInformationVerifier;
import org.spongycastle.cms.SignerInformationVerifierProvider;
import org.spongycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.spongycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.spongycastle.cms.test.CMSTestUtil;
import org.spongycastle.dvcs.CCPDRequestBuilder;
import org.spongycastle.dvcs.CCPDRequestData;
import org.spongycastle.dvcs.CPDRequestBuilder;
import org.spongycastle.dvcs.CPDRequestData;
import org.spongycastle.dvcs.DVCSException;
import org.spongycastle.dvcs.DVCSRequest;
import org.spongycastle.dvcs.MessageImprint;
import org.spongycastle.dvcs.MessageImprintBuilder;
import org.spongycastle.dvcs.SignedDVCSMessageGenerator;
import org.spongycastle.dvcs.TargetChain;
import org.spongycastle.dvcs.VPKCRequestBuilder;
import org.spongycastle.dvcs.VPKCRequestData;
import org.spongycastle.dvcs.VSDRequestBuilder;
import org.spongycastle.dvcs.VSDRequestData;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.io.Streams;

public class AllTests
    extends TestCase
{
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    private static boolean initialised = false;

    private static String origDN;
    private static KeyPair origKP;
    private static X509Certificate origCert;

    private static String signDN;
    private static KeyPair signKP;
    private static X509Certificate signCert;

    private static void init()
        throws Exception
    {
        if (!initialised)
        {
            initialised = true;

            if (Security.getProvider(BC) == null)
            {
                Security.addProvider(new BouncyCastleProvider());
            }
            origDN = "O=Bouncy Castle, C=AU";
            origKP = CMSTestUtil.makeKeyPair();
            origCert = CMSTestUtil.makeCertificate(origKP, origDN, origKP, origDN);

            signDN = "CN=Bob, OU=Sales, O=Bouncy Castle, C=AU";
            signKP = CMSTestUtil.makeKeyPair();
            signCert = CMSTestUtil.makeCertificate(signKP, signDN, origKP, origDN);
        }
    }

    public void setUp()
        throws Exception
    {
        init();
    }

    private byte[] getInput(String name)
        throws IOException
    {
        return Streams.readAll(getClass().getResourceAsStream(name));
    }

    public void testCCPDRequest()
        throws Exception
    {
        SignedDVCSMessageGenerator gen = getSignedDVCSMessageGenerator();

        CCPDRequestBuilder reqBuilder = new CCPDRequestBuilder();

        MessageImprintBuilder imprintBuilder = new MessageImprintBuilder(new SHA1DigestCalculator());

        MessageImprint messageImprint = imprintBuilder.build(new byte[100]);

        CMSSignedData reqMsg = gen.build(reqBuilder.build(messageImprint));

        assertTrue(reqMsg.verifySignatures(new SignerInformationVerifierProvider()
        {
            public SignerInformationVerifier get(SignerId sid)
                throws OperatorCreationException
            {
                return new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(signCert);
            }
        }));

        DVCSRequest request = new DVCSRequest(reqMsg);

        CCPDRequestData reqData = (CCPDRequestData)request.getData();

        assertEquals(messageImprint, reqData.getMessageImprint());
    }

    private CMSSignedData getWrappedCPDRequest()
        throws OperatorCreationException, CertificateEncodingException, DVCSException, IOException
    {
        SignedDVCSMessageGenerator gen = getSignedDVCSMessageGenerator();

        CPDRequestBuilder reqBuilder = new CPDRequestBuilder();

        return gen.build(reqBuilder.build(new byte[100]));
    }

    public void testCPDRequest()
        throws Exception
    {
        CMSSignedData reqMsg = getWrappedCPDRequest();

        assertTrue(reqMsg.verifySignatures(new SignerInformationVerifierProvider()
        {
            public SignerInformationVerifier get(SignerId sid)
                throws OperatorCreationException
            {
                return new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(signCert);
            }
        }));

        DVCSRequest request = new DVCSRequest(reqMsg);

        CPDRequestData reqData = (CPDRequestData)request.getData();

        assertTrue(Arrays.areEqual(new byte[100], reqData.getMessage()));
    }

    public void testVPKCRequest()
        throws Exception
    {
        SignedDVCSMessageGenerator gen = getSignedDVCSMessageGenerator();

        VPKCRequestBuilder reqBuilder = new VPKCRequestBuilder();

        reqBuilder.addTargetChain(new JcaX509CertificateHolder(signCert));

        CMSSignedData reqMsg = gen.build(reqBuilder.build());

        assertTrue(reqMsg.verifySignatures(new SignerInformationVerifierProvider()
        {
            public SignerInformationVerifier get(SignerId sid)
                throws OperatorCreationException
            {
                return new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(signCert);
            }
        }));

        DVCSRequest request = new DVCSRequest(reqMsg);

        VPKCRequestData reqData = (VPKCRequestData)request.getData();

        assertEquals(new TargetEtcChain(new CertEtcToken(CertEtcToken.TAG_CERTIFICATE, new JcaX509CertificateHolder(signCert).toASN1Structure())), ((TargetChain)reqData.getCerts().get(0)).toASN1Structure());
    }

    public void testVSDRequest()
        throws Exception
    {
        CMSSignedData message = getWrappedCPDRequest();

        SignedDVCSMessageGenerator gen = getSignedDVCSMessageGenerator();

        VSDRequestBuilder reqBuilder = new VSDRequestBuilder();

        CMSSignedData reqMsg = gen.build(reqBuilder.build(message));

        assertTrue(reqMsg.verifySignatures(new SignerInformationVerifierProvider()
        {
            public SignerInformationVerifier get(SignerId sid)
                throws OperatorCreationException
            {
                return new JcaSimpleSignerInfoVerifierBuilder().setProvider(BC).build(signCert);
            }
        }));

        DVCSRequest request = new DVCSRequest(reqMsg);

        VSDRequestData reqData = (VSDRequestData)request.getData();

        assertEquals(message.toASN1Structure().getContentType(), reqData.getParsedMessage().toASN1Structure().getContentType());
    }

    private SignedDVCSMessageGenerator getSignedDVCSMessageGenerator()
        throws OperatorCreationException, CertificateEncodingException
    {
        CMSSignedDataGenerator sigDataGen = new CMSSignedDataGenerator();

        JcaDigestCalculatorProviderBuilder calculatorProviderBuilder = new JcaDigestCalculatorProviderBuilder().setProvider(BC);

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA1withRSA").setProvider(BC).build(signKP.getPrivate());

        sigDataGen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(calculatorProviderBuilder.build()).build(contentSigner, signCert));

        return new SignedDVCSMessageGenerator(sigDataGen);
    }

    public static void main(String[] args)
        throws Exception
    {
        Security.addProvider(new BouncyCastleProvider());

        junit.textui.TestRunner.run(suite());
    }

    public static Test suite()
        throws Exception
    {
        TestSuite suite= new TestSuite("EAC tests");

        suite.addTestSuite(AllTests.class);
        suite.addTestSuite(DVCSParseTest.class);

        return new DVCSTestSetup(suite);
    }
}
