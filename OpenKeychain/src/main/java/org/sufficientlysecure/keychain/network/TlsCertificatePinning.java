/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import timber.log.Timber;


public class TlsCertificatePinning {

    private static Map<String, byte[]> sCertificatePins = new HashMap<>();

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

            sCertificatePins.put(host, baos.toByteArray());
        } catch (IOException e) {
            Timber.w(e);
        }
    }

    private final URL url;

    public TlsCertificatePinning(URL url) {
        this.url = url;
    }

    public boolean isPinAvailable() {
        return sCertificatePins.containsKey(url.getHost());
    }

    /**
     * Modifies the builder to accept only requests with a given certificate.
     * Applies to all URLs requested by the builder.
     * Therefore a builder that is pinned this way should be used to only make requests
     * to URLs with passed certificate.
     */
    void pinCertificate(OkHttpClient.Builder builder) {
        Timber.d("Pinning certificate for " + url);

        // We don't use OkHttp's CertificatePinner since it can not be used to pin self-signed
        // certificate if such certificate is not accepted by TrustManager.
        // (Refer to note at end of description:
        // http://square.github.io/okhttp/javadoc/com/squareup/okhttp/CertificatePinner.html )
        // Creating our own TrustManager that trusts only our certificate eliminates the need for certificate pinning
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            byte[] certificate = sCertificatePins.get(url.getHost());
            Certificate ca = cf.generateCertificate(new ByteArrayInputStream(certificate));

            KeyStore keyStore = createSingleCertificateKeyStore(ca);
            X509TrustManager trustManager = createTrustManager(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, trustManager);
        } catch (CertificateException | KeyStoreException |
                KeyManagementException | NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private KeyStore createSingleCertificateKeyStore(Certificate ca) throws KeyStoreException,
            CertificateException, NoSuchAlgorithmException, IOException {
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        return keyStore;
    }

    private X509TrustManager createTrustManager(KeyStore keyStore) throws NoSuchAlgorithmException,
            KeyStoreException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers: "
                    + Arrays.toString(trustManagers));
        }

        return (X509TrustManager) trustManagers[0];
    }
}
