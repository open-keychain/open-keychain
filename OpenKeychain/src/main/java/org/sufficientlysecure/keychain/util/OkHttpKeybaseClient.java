/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.util;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import com.textuality.keybase.lib.KeybaseUrlConnectionClient;

import org.sufficientlysecure.keychain.Constants;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper for Keybase Lib
 */
public class OkHttpKeybaseClient implements KeybaseUrlConnectionClient {

    private OkUrlFactory generateUrlFactory() {
        OkHttpClient client = new OkHttpClient();
        return new OkUrlFactory(client);
    }

    @Override
    public URLConnection openConnection(URL url, Proxy proxy, boolean isKeybase) throws IOException {
        OkUrlFactory factory = generateUrlFactory();
        if (proxy != null) {
            factory.client().setProxy(proxy);
            factory.client().setConnectTimeout(30000, TimeUnit.MILLISECONDS);
            factory.client().setReadTimeout(40000, TimeUnit.MILLISECONDS);
        } else {
            factory.client().setConnectTimeout(5000, TimeUnit.MILLISECONDS);
            factory.client().setReadTimeout(25000, TimeUnit.MILLISECONDS);
        }

        factory.client().setFollowSslRedirects(false);

        // forced the usage of api.keybase.io pinned certificate
        if (isKeybase) {
            try {
                if (!TlsHelper.usePinnedCertificateIfAvailable(factory.client(), url)) {
                    throw new IOException("no pinned certificate found for URL!");
                }
            } catch (TlsHelper.TlsHelperException e) {
                Log.e(Constants.TAG, "TlsHelper failed", e);
                throw new IOException("TlsHelper failed");
            }
        }

        return factory.open(url);
    }

    @Override
    public String getKeybaseBaseUrl() {
        return "https://api.keybase.io/";
    }

}