package org.spongycastle.cms.test;

import java.util.Collection;
import java.util.Iterator;

import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cms.CMSEnvelopedData;
import org.spongycastle.cms.CMSEnvelopedDataGenerator;
import org.spongycastle.cms.CMSProcessableByteArray;
import org.spongycastle.cms.RecipientInformation;
import org.spongycastle.cms.RecipientInformationStore;
import org.spongycastle.cms.bc.BcCMSContentEncryptorBuilder;
import org.spongycastle.cms.bc.BcRSAKeyTransEnvelopedRecipient;
import org.spongycastle.cms.bc.BcRSAKeyTransRecipientInfoGenerator;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.test.SimpleTest;

public class BcEnvelopedDataTest
    extends SimpleTest
{
    private static String                   _origDN;
    private static AsymmetricCipherKeyPair  _origKP;
    private static X509CertificateHolder    _origCert;

    private static String                   _signDN;
    private static AsymmetricCipherKeyPair  _signKP;
    private static X509CertificateHolder    _signCert;

    private static String                   _reciDN;
    private static String                   _reciDN2;
    private static AsymmetricCipherKeyPair  _reciKP;
    private static X509CertificateHolder    _reciCert;

    private static boolean _initialised = false;

    public String getName()
    {
        return "BcEnvelopedData";
    }

    private void init()
        throws Exception
    {
        if (!_initialised)
        {
            _initialised = true;

            _origDN   = "O=Bouncy Castle, C=AU";
            _origKP   = CMSTestUtil.makeKeyPair();  
            _origCert = CMSTestUtil.makeCertificate(_origKP, _origDN, _origKP, _origDN);

            _signDN   = "CN=Bob, OU=Sales, O=Bouncy Castle, C=AU";
            _signKP   = CMSTestUtil.makeKeyPair();
            _signCert = CMSTestUtil.makeCertificate(_signKP, _signDN, _origKP, _origDN);

            _reciDN   = "CN=Doug, OU=Sales, O=Bouncy Castle, C=AU";
            _reciDN2  = "CN=Fred, OU=Sales, O=Bouncy Castle, C=AU";
            _reciKP   = CMSTestUtil.makeKeyPair();
            _reciCert = CMSTestUtil.makeCertificate(_reciKP, _reciDN, _signKP, _signDN);
        }
    }

    private void testKeyTransLight128RC4()
            throws Exception
    {
        byte[]          data     = "WallaWallaBouncyCastle".getBytes();

        CMSEnvelopedDataGenerator edGen = new CMSEnvelopedDataGenerator();

        edGen.addRecipientInfoGenerator(new BcRSAKeyTransRecipientInfoGenerator(_reciCert));

        CMSEnvelopedData ed = edGen.generate(
            new CMSProcessableByteArray(data),
            new BcCMSContentEncryptorBuilder(NISTObjectIdentifiers.id_aes128_CBC).build());

        RecipientInformationStore recipients = ed.getRecipientInfos();

        if (!ed.getEncryptionAlgOID().equals(NISTObjectIdentifiers.id_aes128_CBC.getId()))
        {
            fail("enc oid mismatch");
        }

        Collection  c = recipients.getRecipients();
        Iterator    it = c.iterator();

        if (it.hasNext())
        {
            RecipientInformation recipient = (RecipientInformation)it.next();

            byte[] recData = recipient.getContent(new BcRSAKeyTransEnvelopedRecipient((AsymmetricKeyParameter)_reciKP.getPrivate()));

            if (!Arrays.areEqual(data, recData))
            {
                fail("decryption failed");
            }
        }
        else
        {
            fail("no recipient found");
        }
    }

    public void performTest()
        throws Exception
    {
        init();

        testKeyTransLight128RC4();
    }

    public static void main(
        String[]    args)
    {
        runTest(new BcEnvelopedDataTest());
    }
}
