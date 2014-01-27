package org.spongycastle.jce.provider.test;

import org.spongycastle.util.test.SimpleTest;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.MultiCertStoreParameters;
import org.spongycastle.jce.PrincipalUtil;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.X509CRL;
import org.spongycastle.jce.cert.CollectionCertStoreParameters;
import org.spongycastle.jce.cert.CertStore;
import org.spongycastle.jce.cert.X509CertSelector;
import org.spongycastle.jce.cert.X509CRLSelector;
import java.security.Security;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class MultiCertStoreTest
    extends SimpleTest
{

    public void performTest()
        throws Exception
    {
        basicTest();
    }

    private void basicTest()
        throws Exception
    {
        CertificateFactory cf = CertificateFactory.getInstance("X.509", "SC");

        X509Certificate rootCert = (X509Certificate)cf
                .generateCertificate(new ByteArrayInputStream(
                        CertPathTest.rootCertBin));
        X509Certificate interCert = (X509Certificate)cf
                .generateCertificate(new ByteArrayInputStream(
                        CertPathTest.interCertBin));
        X509Certificate finalCert = (X509Certificate)cf
                .generateCertificate(new ByteArrayInputStream(
                        CertPathTest.finalCertBin));
        X509CRL rootCrl = (X509CRL)cf.generateCRL(new ByteArrayInputStream(
                CertPathTest.rootCrlBin));
        X509CRL interCrl = (X509CRL)cf
                .generateCRL(new ByteArrayInputStream(
                        CertPathTest.interCrlBin));

        // Testing CollectionCertStore generation from List
        List list = new ArrayList();
        list.add(rootCert);
        list.add(interCert);
        list.add(finalCert);
        list.add(rootCrl);
        list.add(interCrl);
        CollectionCertStoreParameters ccsp = new CollectionCertStoreParameters(list);
        CertStore store1 = CertStore.getInstance("Collection", ccsp, "SC");
        CertStore store2 = CertStore.getInstance("Collection", ccsp, "SC");

        List storeList = new ArrayList();
        storeList.add(store1);
        storeList.add(store2);
        CertStore store = CertStore.getInstance("Multi", new MultiCertStoreParameters(storeList));

        // Searching for rootCert by subjectDN
        X509CertSelector targetConstraints = new X509CertSelector();
        targetConstraints.setSubject(PrincipalUtil.getSubjectX509Principal(rootCert).getName());
        Collection certs = store.getCertificates(targetConstraints);

        if (certs.size() != 2 || !certs.contains(rootCert))
        {
            fail("2 rootCerts not found by subjectDN");
        }

        store = CertStore.getInstance("Multi", new MultiCertStoreParameters(storeList, false));
        certs = store.getCertificates(targetConstraints);
        
        if (certs.size() != 1 || !certs.contains(rootCert))
        {
            fail("1 rootCert not found by subjectDN");
        }
    }

    public String getName()
    {
        return "MultiCertStore";
    }

    public static void main(String[] args)
    {
        Security.addProvider(new BouncyCastleProvider());

        runTest(new MultiCertStoreTest());
    }

}
