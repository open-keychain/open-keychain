/*
 * Copyright (C) 2013-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.network;

import android.content.res.AssetManager;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class TlsCertificatePinning {

    private static Map<String, byte[]> sPinnedCertificates = new HashMap<>();

    /**
     * Add certificate from assets to pinned certificate map.
     */
    public static void addPinnedCertificate(String host, AssetManager assetManager, String cerFilename) {
        try {
            InputStream is = assetManager.open(cerFilename);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int reads = is.read();

            while (reads != -1) {
                baos.write(reads);
                reads = is.read();
            }

            is.close();

            sPinnedCertificates.put(host, baos.toByteArray());
        } catch (IOException e) {
            Log.w(Constants.TAG, e);
        }
    }

    /**
     * Use pinned certificate for OkHttpClient if we have one.
     *
     * @return true, if certificate is available, false if not
     */
    public static SSLSocketFactory getPinnedSslSocketFactory(URL url) {
        if (url.getProtocol().equals("https")) {
            // use certificate PIN from assets if we have one
            for (String host : sPinnedCertificates.keySet()) {
                if (url.getHost().endsWith(host)) {
                    return pinCertificate(sPinnedCertificates.get(host));
                }
            }
        }
        return null;
    }

    /**
     * Modifies the builder to accept only requests with a given certificate.
     * Applies to all URLs requested by the builder.
     * Therefore a builder that is pinned this way should be used to only make requests
     * to URLs with passed certificate.
     *
     * @param certificate certificate to pin
     */
    private static SSLSocketFactory pinCertificate(byte[] certificate) {
        // We don't use OkHttp's CertificatePinner since it can not be used to pin self-signed
        // certificate if such certificate is not accepted by TrustManager.
        // (Refer to note at end of description:
        // http://square.github.io/okhttp/javadoc/com/squareup/okhttp/CertificatePinner.html )
        // Creating our own TrustManager that trusts only our certificate eliminates the need for certificate pinning
        try {
            // Load CA
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate ca = cf.generateCertificate(new ByteArrayInputStream(certificate));

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            return context.getSocketFactory();
        } catch (CertificateException | KeyStoreException |
                KeyManagementException | NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
