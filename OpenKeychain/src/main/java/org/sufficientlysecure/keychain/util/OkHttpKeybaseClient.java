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


import com.textuality.keybase.lib.KeybaseUrlConnectionClient;

import okhttp3.OkHttpClient;
import okhttp3.OkUrlFactory;
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
        OkHttpClient client = OkHttpClientFactory.getSimpleClient();

        OkUrlFactory factory = generateUrlFactory();
        try {
            if (isKeybase && proxy != null) {
                client = OkHttpClientFactory.getPinnedClient(url, proxy);
            } else if (proxy != null) {
                client = OkHttpClientFactory.getClient(proxy);
            } else {
                client = OkHttpClientFactory.getSimpleClient();
            }
        } catch (TlsHelper.TlsHelperException e) {
            Log.e(Constants.TAG, "TlsHelper failed", e);
            throw new IOException("TlsHelper failed");
        }
        factory.setClient(client);

        return factory.open(url);
    }

    @Override
    public String getKeybaseBaseUrl() {
        return "https://api.keybase.io/";
    }

}