/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2015 Adithya Abraham Philip <adithyaphilip@gmail.com>
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


import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.network.OkHttpClientFactory;
import org.sufficientlysecure.keychain.pgp.PgpAsciiArmorReformatter;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableProxy;

public class FacebookKeyserverClient implements KeyserverClient {
    private static final String FB_KEY_URL_FORMAT
            = "https://www.facebook.com/%s/publickey/download";
    private static final String FB_HOST = "facebook.com";
    private static final String FB_HOST_WWW = "www." + FB_HOST;


    public static FacebookKeyserverClient getInstance() {
        return new FacebookKeyserverClient();
    }

    private FacebookKeyserverClient() { }


    @Override
    public List<ImportKeysListEntry> search(String fbUsername, ParcelableProxy proxy)
            throws QueryFailedException, QueryNeedsRepairException {
        List<ImportKeysListEntry> entry = new ArrayList<>(1);

        String data = get(fbUsername, proxy);
        // if we're here that means key retrieval succeeded,
        // would have thrown an exception otherwise
        try {
            UncachedKeyRing keyRing = UncachedKeyRing.decodeFromData(data.getBytes());
            try {
                entry.add(getEntry(keyRing, fbUsername));
            } catch (UnsupportedOperationException e) {
                Log.e(Constants.TAG, "Parsing retrieved Facebook key failed!");
            }
        } catch (PgpGeneralException | IOException e) {
            Log.e(Constants.TAG, "Failed parsing key from Facebook during search", e);
            throw new QueryFailedException("No valid key found on Facebook");
        }
        return entry;
    }

    @Override
    public String get(String fbUsername, ParcelableProxy proxy) throws QueryFailedException {
        Log.d(Constants.TAG, "FacebookKeyserver get: " + fbUsername + " using Proxy: " + proxy.getProxy());

        String data = query(fbUsername, proxy);

        if (data == null) {
            throw new QueryFailedException("data is null");
        }

        Matcher matcher = PgpAsciiArmorReformatter.PGP_PUBLIC_KEY.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new QueryFailedException("data is null");
    }

    private String query(String fbUsername, ParcelableProxy proxy) throws QueryFailedException {
        try {
            URL url = new URL(String.format(FB_KEY_URL_FORMAT, fbUsername));
            Log.d(Constants.TAG, "fetching from Facebook with: " + url + " proxy: " + proxy.getProxy());

            /*
             * For some URLs such as https://www.facebook.com/adithya.abraham/publickey/download
             * Facebook redirects to a mobile version (302) and then errors out (500).
             * Using a desktop User-Agent solves that!
             */
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:49.0) Gecko/20100101 Firefox/49.0")
                    .build();

            OkHttpClient client = OkHttpClientFactory.getClientPinnedIfAvailable(url, proxy.getProxy());
            Response response = client.newCall(request).execute();

            // contains body both in case of success or failure
            String responseBody = response.body().string();

            if (response.isSuccessful()) {
                return responseBody;
            } else {
                // probably a 404 indicating that the key does not exist
                throw new QueryFailedException("key for " + fbUsername + " not found on Facebook." +
                        "response:" + response);
            }

        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException at Facebook key download", e);
            throw new QueryFailedException("Cannot connect to Facebook. "
                    + "Check your Internet connection!"
                    + (proxy.getProxy() == Proxy.NO_PROXY ? "" : " Using proxy " + proxy.getProxy()));
        }
    }

    @Override
    public void add(String armoredKey, ParcelableProxy proxy) throws AddKeyException {
        // Implementing will require usage of FB API
        throw new UnsupportedOperationException("Uploading keys not supported yet");
    }

    /**
     * Facebook returns the entire key even during our searching phase.
     *
     * @throws UnsupportedOperationException if the key could not be parsed
     */
    @NonNull
    public static ImportKeysListEntry getEntry(UncachedKeyRing ring, String fbUsername)
            throws UnsupportedOperationException {
        ImportKeysListEntry entry = new ImportKeysListEntry();
        entry.setSecretKey(false); // keys imported from Facebook must be public

        // so we can query for the Facebook username directly, and to identify the source to
        // download the key from
        entry.setFbUsername(fbUsername);

        UncachedPublicKey key = ring.getPublicKey();

        entry.setUserIds(key.getUnorderedUserIds());
        entry.setPrimaryUserId(key.getPrimaryUserIdWithFallback());

        entry.setKeyId(key.getKeyId());
        entry.setFingerprint(key.getFingerprint());

        try {
            if (key.isEC()) { // unsupported key format (ECDH or ECDSA)
                Log.e(Constants.TAG, "ECDH/ECDSA key - not supported.");
                throw new UnsupportedOperationException(
                        "ECDH/ECDSA keys not supported yet");
            }
            entry.setBitStrength(key.getBitStrength());
            final int algorithm = key.getAlgorithm();
            entry.setAlgorithm(KeyFormattingUtils.getAlgorithmInfo(algorithm, key.getBitStrength(),
                    key.getCurveOid()));
        } catch (NumberFormatException | NullPointerException e) {
            Log.e(Constants.TAG, "Conversion for bit size, algorithm, or creation date failed.", e);
            // can't use this key
            throw new UnsupportedOperationException(
                    "Conversion for bit size, algorithm, or creation date failed.");
        }

        return entry;
    }

    public static String getUsernameFromUri(Uri uri) {
        // path pattern is /username/publickey/download
        return uri.getPathSegments().get(0);
    }

    public static boolean isFacebookHost(@Nullable Uri uri) {
        if (uri == null) {
            return false;
        }
        String host = uri.getHost();
        return FB_HOST.equalsIgnoreCase(host) || FB_HOST_WWW.equalsIgnoreCase(host);
    }
}
