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

package org.sufficientlysecure.keychain.keyimport;


import androidx.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.sufficientlysecure.keychain.network.OkHttpClientFactory;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.WebKeyDirectoryUtil;
import timber.log.Timber;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;


/**
 * Searches for keys using Web Key Directory protocol.
 *
 * @see <a href="https://tools.ietf.org/html/draft-koch-openpgp-webkey-service-05#section-3.1">Key Discovery</a>
 */
public class WebKeyDirectoryClient implements KeyserverClient {

    public static WebKeyDirectoryClient getInstance() {
        return new WebKeyDirectoryClient();
    }

    private WebKeyDirectoryClient() {
    }

    @Override
    public List<ImportKeysListEntry> search(String name, ParcelableProxy proxy)
            throws QueryFailedException {
        URL webKeyDirectoryURL = WebKeyDirectoryUtil.toWebKeyDirectoryURL(name, true);

        if (webKeyDirectoryURL == null) {
            Timber.d("Name not supported by Web Key Directory Client: " + name);
            return Collections.emptyList();
        }

        Timber.d("Web Key Directory import: " + name + " using Proxy: " + proxy.getProxy());

        Timber.d("Query Web Key Directory Advanced method for: " + name);
        byte[] data = query(webKeyDirectoryURL, proxy.getProxy());

        if (data == null) {
            // Retry with direct mode
            URL webKeyDirectoryURLDirect = WebKeyDirectoryUtil.toWebKeyDirectoryURL(name, false);

            Timber.d("Query Web Key Directory fallback Direct method for: " + name);
            byte[] dataDirect = query(webKeyDirectoryURLDirect, proxy.getProxy());

            if (dataDirect == null) {
                Timber.d("No Web Key Directory endpoint for: " + name);
                return Collections.emptyList();
            } else {
                data = dataDirect;
            }
        }

        // if we're here that means key retrieval succeeded,
        // would have thrown an exception otherwise
        try {
            UncachedKeyRing ring = UncachedKeyRing.decodeFromData(data);
            return Collections.singletonList(new ImportKeysListEntry(null, ring));
        } catch (PgpGeneralException | IOException e) {
            Timber.e(e, "Failed parsing key from Web Key Directory during search");
            throw new QueryFailedException("No valid key found on Web Key Directory");
        }
    }

    @Override
    public String get(String name, ParcelableProxy proxy) {
        throw new UnsupportedOperationException("Returning armored key from Web Key Directory not supported");
    }

    @Nullable
    private byte[] query(URL url, Proxy proxy) throws QueryFailedException {
        try {
            Timber.d("fetching from Web Key Directory with: %s proxy: %s", url, proxy);

            Request request = new Request.Builder().url(url).build();

            OkHttpClient client = OkHttpClientFactory.getClientPinnedIfAvailableWithRedirects(url, proxy);
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                return response.body().bytes();
            } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                return null;
            } else {
                throw new QueryFailedException("Error while fetching key from Web Key Directory. " +
                        "Response:" + response);
            }

        } catch (UnknownHostException e) {
            Timber.e(e, "Unknown host at Web Key Directory key download");
            return null;
        } catch (IOException e) {
            Timber.e(e, "IOException at Web Key Directory key download");
            throw new QueryFailedException("Cannot connect to Web Key Directory. "
                    + "Check your Internet connection!"
                    + (proxy == Proxy.NO_PROXY ? "" : " Using proxy " + proxy));
        }
    }

    @Override
    public void add(String armoredKey, ParcelableProxy proxy) {
        throw new UnsupportedOperationException("Uploading keys to Web Key Directory is not supported");
    }
}
