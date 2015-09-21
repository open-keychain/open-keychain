package org.sufficientlysecure.keychain.util;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
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

import com.textuality.keybase.lib.KeybaseUrlConnectionClient;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper for Keybase Lib
 */
public class OkHttpKeybaseClient implements KeybaseUrlConnectionClient {

    private final OkUrlFactory factory;
    private final OkUrlFactory proxyFactory;

    private static OkUrlFactory generateUrlFactory() {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(5000, TimeUnit.MILLISECONDS);
        client.setReadTimeout(25000, TimeUnit.MILLISECONDS);
        return new OkUrlFactory(client);
    }

    private static OkUrlFactory generateProxyUrlFactory() {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(30000, TimeUnit.MILLISECONDS);
        client.setReadTimeout(40000, TimeUnit.MILLISECONDS);
        return new OkUrlFactory(client);
    }

    public OkHttpKeybaseClient() {
        factory = generateUrlFactory();
        proxyFactory = generateProxyUrlFactory();
    }

    @Override
    public URLConnection openConnection(URL url) throws IOException {
        return openConnection(url, null);
    }

    @Override
    public URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        URLConnection conn;
        if (proxy != null) {
            proxyFactory.client().setProxy(proxy);
            conn = proxyFactory.open(url);
        } else {
            conn = factory.open(url);
        }
        return conn;
    }

}