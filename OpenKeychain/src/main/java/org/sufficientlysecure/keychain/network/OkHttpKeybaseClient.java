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


import java.io.IOException;
import java.net.Proxy;
import java.net.URL;

import com.textuality.keybase.lib.KeybaseUrlConnectionClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Wrapper for Keybase Lib
 */
public class OkHttpKeybaseClient implements KeybaseUrlConnectionClient {

    @Override
    public Response getUrlResponse(URL url, Proxy proxy, boolean isKeybase) throws IOException {
        OkHttpClient client;

        if (proxy != null) {
            client = OkHttpClientFactory.getClientPinnedIfAvailable(url, proxy);
        } else {
            client = OkHttpClientFactory.getSimpleClient();
        }

        Request request = new Request.Builder()
                .url(url).build();
        okhttp3.Response okResponse = client.newCall(request).execute();
        return new Response(okResponse.body().byteStream(), okResponse.code(), okResponse.message(), okResponse.headers().toMultimap());
    }

    @Override
    public String getKeybaseBaseUrl() {
        return "https://api.keybase.io/";
    }

}