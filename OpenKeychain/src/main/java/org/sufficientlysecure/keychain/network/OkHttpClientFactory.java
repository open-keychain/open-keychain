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


import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;

public class OkHttpClientFactory {
    private static OkHttpClient client;

    public static OkHttpClient getSimpleClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(5000, TimeUnit.MILLISECONDS)
                    .readTimeout(25000, TimeUnit.MILLISECONDS)
                    .build();
        }
        return client;
    }

    public static OkHttpClient getSimpleClientPinned(CertificatePinner pinner) {
        return new OkHttpClient.Builder()
                .connectTimeout(5000, TimeUnit.MILLISECONDS)
                .readTimeout(25000, TimeUnit.MILLISECONDS)
                .certificatePinner(pinner)
                .build();
    }

    public static OkHttpClient getClientPinnedIfAvailable(URL url, Proxy proxy) {
        // don't follow any redirects for keyservers, as discussed in the security audit
        return getClientPinnedIfAvailable(url, proxy, false);
    }

    public static OkHttpClient getClientPinnedIfAvailableWithRedirects(URL url, Proxy proxy) {
        return getClientPinnedIfAvailable(url, proxy, true);
    }

    private static OkHttpClient getClientPinnedIfAvailable(URL url, Proxy proxy, boolean followRedirects) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.followRedirects(followRedirects)
                .followSslRedirects(false);

        if (proxy != null) {
            // set proxy and higher timeouts for Tor
            builder.proxy(proxy);
            builder.connectTimeout(30000, TimeUnit.MILLISECONDS)
                    .readTimeout(45000, TimeUnit.MILLISECONDS);
        } else {
            builder.connectTimeout(5000, TimeUnit.MILLISECONDS)
                    .readTimeout(25000, TimeUnit.MILLISECONDS);
        }

        // If a pinned cert is available, use it!
        // NOTE: this fails gracefully back to "no pinning" if no cert is available.
        TlsCertificatePinning tlsCertificatePinning = new TlsCertificatePinning(url);
        boolean isHttpsProtocol = "https".equals(url.getProtocol());
        boolean isPinAvailable = tlsCertificatePinning.isPinAvailable();
        if (isHttpsProtocol && isPinAvailable) {
            tlsCertificatePinning.pinCertificate(builder);
        }

        return builder.build();
    }

}
