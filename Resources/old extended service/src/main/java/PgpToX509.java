/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.x509.AuthorityKeyIdentifier;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.GeneralNames;
import org.spongycastle.asn1.x509.SubjectKeyIdentifier;
import org.spongycastle.asn1.x509.X509Extensions;
import org.spongycastle.asn1.x509.X509Name;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.x509.X509V3CertificateGenerator;
import org.spongycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.spongycastle.x509.extension.SubjectKeyIdentifierStructure;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class PgpToX509 {
    public static final String DN_COMMON_PART_O = "OpenPGP to X.509 Bridge";
    public static final String DN_COMMON_PART_OU = "OpenPGP Keychain cert";

    /**
     * Creates a self-signed certificate from a public and private key. The (critical) key-usage
     * extension is set up with: digital signature, non-repudiation, key-encipherment, key-agreement
     * and certificate-signing. The (non-critical) Netscape extension is set up with: SSL client and
     * S/MIME. A URI subjectAltName may also be set up.
     *
     * @param pubKey         public key
     * @param privKey        private key
     * @param subject        subject (and issuer) DN for this certificate, RFC 2253 format preferred.
     * @param startDate      date from which the certificate will be valid (defaults to current date and time
     *                       if null)
     * @param endDate        date until which the certificate will be valid (defaults to current date and time
     *                       if null) *
     * @param subjAltNameURI URI to be placed in subjectAltName
     * @return self-signed certificate
     * @throws InvalidKeyException
     * @throws SignatureException
     * @throws NoSuchAlgorithmException
     * @throws IllegalStateException
     * @throws NoSuchProviderException
     * @throws CertificateException
     * @throws Exception
     * @author Bruno Harbulot
     */
    public static X509Certificate createSelfSignedCert(
        PublicKey pubKey, PrivateKey privKey, X509Name subject, Date startDate, Date endDate,
        String subjAltNameURI)
        throws InvalidKeyException, IllegalStateException, NoSuchAlgorithmException,
            SignatureException, CertificateException, NoSuchProviderException {

        X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();

        certGenerator.reset();
        /*
         * Sets up the subject distinguished name. Since it's a self-signed certificate, issuer and
         * subject are the same.
         */
        certGenerator.setIssuerDN(subject);
        certGenerator.setSubjectDN(subject);

        /*
         * Sets up the validity dates.
         */
        if (startDate == null) {
            startDate = new Date(System.currentTimeMillis());
        }
        certGenerator.setNotBefore(startDate);
        if (endDate == null) {
            endDate = new Date(startDate.getTime() + (365L * 24L * 60L * 60L * 1000L));
            Log.d(Constants.TAG, "end date is=" + DateFormat.getDateInstance().format(endDate));
        }

        certGenerator.setNotAfter(endDate);

        /*
         * The serial-number of this certificate is 1. It makes sense because it's self-signed.
         */
        certGenerator.setSerialNumber(BigInteger.ONE);
        /*
         * Sets the public-key to embed in this certificate.
         */
        certGenerator.setPublicKey(pubKey);
        /*
         * Sets the signature algorithm.
         */
        String pubKeyAlgorithm = pubKey.getAlgorithm();
        if (pubKeyAlgorithm.equals("DSA")) {
            certGenerator.setSignatureAlgorithm("SHA1WithDSA");
        } else if (pubKeyAlgorithm.equals("RSA")) {
            certGenerator.setSignatureAlgorithm("SHA1WithRSAEncryption");
        } else {
            RuntimeException re = new RuntimeException("Algorithm not recognised: "
                    + pubKeyAlgorithm);
            Log.e(Constants.TAG, re.getMessage(), re);
            throw re;
        }

        /*
         * Adds the Basic Constraint (CA: true) extension.
         */
        certGenerator.addExtension(X509Extensions.BasicConstraints, true,
                new BasicConstraints(true));

        /*
         * Adds the subject key identifier extension.
         */
        SubjectKeyIdentifier subjectKeyIdentifier = new SubjectKeyIdentifierStructure(pubKey);
        certGenerator
                .addExtension(X509Extensions.SubjectKeyIdentifier, false, subjectKeyIdentifier);

        /*
         * Adds the authority key identifier extension.
         */
        AuthorityKeyIdentifier authorityKeyIdentifier = new AuthorityKeyIdentifierStructure(pubKey);
        certGenerator.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                authorityKeyIdentifier);

        /*
         * Adds the subject alternative-name extension.
         */
        if (subjAltNameURI != null) {
            GeneralNames subjectAltNames = new GeneralNames(new GeneralName(
                    GeneralName.uniformResourceIdentifier, subjAltNameURI));
            certGenerator.addExtension(X509Extensions.SubjectAlternativeName, false,
                    subjectAltNames);
        }

        /*
         * Creates and sign this certificate with the private key corresponding to the public key of
         * the certificate (hence the name "self-signed certificate").
         */
        X509Certificate cert = certGenerator.generate(privKey);

        /*
         * Checks that this certificate has indeed been correctly signed.
         */
        cert.verify(pubKey);

        return cert;
    }

    /**
     * Creates a self-signed certificate from a PGP Secret Key.
     *
     * @param pgpSecKey      PGP Secret Key (from which one can extract the public and private
     *                       keys and other attributes).
     * @param pgpPrivKey     PGP Private Key corresponding to the Secret Key (password callbacks
     *                       should be done before calling this method)
     * @param subjAltNameURI optional URI to embed in the subject alternative-name
     * @return self-signed certificate
     * @throws PGPException
     * @throws NoSuchProviderException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     * @throws CertificateException
     * @author Bruno Harbulot
     */
    public static X509Certificate createSelfSignedCert(
        PGPSecretKey pgpSecKey, PGPPrivateKey pgpPrivKey, String subjAltNameURI)
        throws PGPException, NoSuchProviderException, InvalidKeyException, NoSuchAlgorithmException,
            SignatureException, CertificateException {
        // get public key from secret key
        PGPPublicKey pgpPubKey = pgpSecKey.getPublicKey();

        // LOGGER.info("Key ID: " + Long.toHexString(pgpPubKey.getKeyID() & 0xffffffffL));

        /*
         * The X.509 Name to be the subject DN is prepared. The CN is extracted from the Secret Key
         * user ID.
         */
        Vector<DERObjectIdentifier> x509NameOids = new Vector<DERObjectIdentifier>();
        Vector<String> x509NameValues = new Vector<String>();

        x509NameOids.add(X509Name.O);
        x509NameValues.add(DN_COMMON_PART_O);

        x509NameOids.add(X509Name.OU);
        x509NameValues.add(DN_COMMON_PART_OU);

        for (@SuppressWarnings("unchecked")
             Iterator<Object> it = (Iterator<Object>) pgpSecKey.getUserIDs(); it.hasNext(); ) {
            Object attrib = it.next();
            x509NameOids.add(X509Name.CN);
            x509NameValues.add("CryptoCall");
            // x509NameValues.add(attrib.toString());
        }

        /*
         * Currently unused.
         */
        Log.d(Constants.TAG, "User attributes: ");
        for (@SuppressWarnings("unchecked")
             Iterator<Object> it = (Iterator<Object>) pgpSecKey.getUserAttributes(); it.hasNext(); ) {
            Object attrib = it.next();
            Log.d(Constants.TAG, " - " + attrib + " -- " + attrib.getClass());
        }

        X509Name x509name = new X509Name(x509NameOids, x509NameValues);

        Log.d(Constants.TAG, "Subject DN: " + x509name);

        /*
         * To check the signature from the certificate on the recipient side, the creation time
         * needs to be embedded in the certificate. It seems natural to make this creation time be
         * the "not-before" date of the X.509 certificate. Unlimited PGP keys have a validity of 0
         * second. In this case, the "not-after" date will be the same as the not-before date. This
         * is something that needs to be checked by the service receiving this certificate.
         */
        Date creationTime = pgpPubKey.getCreationTime();
        Log.d(Constants.TAG,
                "pgp pub key creation time=" + DateFormat.getDateInstance().format(creationTime));
        Log.d(Constants.TAG, "pgp valid seconds=" + pgpPubKey.getValidSeconds());
        Date validTo = null;
        if (pgpPubKey.getValidSeconds() > 0) {
            validTo = new Date(creationTime.getTime() + 1000L * pgpPubKey.getValidSeconds());
        }

        X509Certificate selfSignedCert = createSelfSignedCert(
                pgpPubKey.getKey(Constants.BOUNCY_CASTLE_PROVIDER_NAME), pgpPrivKey.getKey(),
                x509name, creationTime, validTo, subjAltNameURI);

        return selfSignedCert;
    }

    /**
     * This is a password callback handler that will fill in a password automatically. Useful to
     * configure passwords in advance, but should be used with caution depending on how much you
     * allow passwords to be stored within your application.
     *
     * @author Bruno Harbulot.
     */
    public static final class PredefinedPasswordCallbackHandler implements CallbackHandler {

        private char[] mPassword;
        private String mPrompt;

        public PredefinedPasswordCallbackHandler(String password) {
            this(password == null ? null : password.toCharArray(), null);
        }

        public PredefinedPasswordCallbackHandler(char[] password) {
            this(password, null);
        }

        public PredefinedPasswordCallbackHandler(String password, String prompt) {
            this(password == null ? null : password.toCharArray(), prompt);
        }

        public PredefinedPasswordCallbackHandler(char[] password, String prompt) {
            this.mPassword = password;
            this.mPrompt = prompt;
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof PasswordCallback) {
                    PasswordCallback pwCallback = (PasswordCallback) callback;
                    if ((this.mPrompt == null) || (this.mPrompt.equals(pwCallback.getPrompt()))) {
                        pwCallback.setPassword(this.mPassword);
                    }
                } else {
                    throw new UnsupportedCallbackException(callback, "Unrecognised callback.");
                }
            }
        }

        protected final Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
    }
}
