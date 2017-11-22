/*
 * Copyright (C) 2016-2017 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2017 Vincent Breitmoser <look@my.amazin.horse>
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
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.support.annotation.NonNull;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.network.OkHttpClientFactory;
import org.sufficientlysecure.keychain.pgp.PgpAsciiArmorReformatter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableProxy;


public class HkpKeyserverClient implements KeyserverClient {
    /**
     * pub:%keyid%:%algo%:%keylen%:%creationdate%:%expirationdate%:%flags%
     * <ul>
     * <li>%<b>keyid</b>% = this is either the fingerprint or the key ID of the key.
     * Either the 16-digit or 8-digit key IDs are acceptable, but obviously the fingerprint is best.
     * </li>
     * <li>%<b>algo</b>% = the algorithm number, (i.e. 1==RSA, 17==DSA, etc).
     * See <a href="http://tools.ietf.org/html/rfc2440#section-9.1">RFC-2440</a></li>
     * <li>%<b>keylen</b>% = the key length (i.e. 1024, 2048, 4096, etc.)</li>
     * <li>%<b>creationdate</b>% = creation date of the key in standard
     * <a href="http://tools.ietf.org/html/rfc2440#section-9.1">RFC-2440</a> form (i.e. number of
     * seconds since 1/1/1970 UTC time)</li>
     * <li>%<b>expirationdate</b>% = expiration date of the key in standard
     * <a href="http://tools.ietf.org/html/rfc2440#section-9.1">RFC-2440</a> form (i.e. number of
     * seconds since 1/1/1970 UTC time)</li>
     * <li>%<b>flags</b>% = letter codes to indicate details of the key, if any. Flags may be in any
     * order. The meaning of "disabled" is implementation-specific. Note that individual flags may
     * be unimplemented, so the absence of a given flag does not necessarily mean the absence of the
     * detail.
     * <ul>
     * <li>r == revoked</li>
     * <li>d == disabled</li>
     * <li>e == expired</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @see <a href="http://tools.ietf.org/html/draft-shaw-openpgp-hkp-00#section-5.2">
     * 5.2. Machine Readable Indexes</a>
     * in Internet-Draft OpenPGP HTTP Keyserver Protocol Document
     */
    private static final Pattern PUB_KEY_LINE = Pattern
            .compile("pub:([0-9a-fA-F]+):([0-9]+):([0-9]+):([0-9]+):([0-9]*):([rde]*)[ \n\r]*" // pub line
                            + "((uid:([^:]*):([0-9]+):([0-9]*):([rde]*)[ \n\r]*)+)", // one or more uid lines
                    Pattern.CASE_INSENSITIVE
            );

    /**
     * uid:%escaped uid string%:%creationdate%:%expirationdate%:%flags%
     * <ul>
     * <li>%<b>escaped uid string</b>% = the user ID string, with HTTP %-escaping for anything that
     * isn't 7-bit safe as well as for the ":" character.  Any other characters may be escaped, as
     * desired.</li>
     * <li>%<b>creationdate</b>% = creation date of the key in standard
     * <a href="http://tools.ietf.org/html/rfc2440#section-9.1">RFC-2440</a> form (i.e. number of
     * seconds since 1/1/1970 UTC time)</li>
     * <li>%<b>expirationdate</b>% = expiration date of the key in standard
     * <a href="http://tools.ietf.org/html/rfc2440#section-9.1">RFC-2440</a> form (i.e. number of
     * seconds since 1/1/1970 UTC time)</li>
     * <li>%<b>flags</b>% = letter codes to indicate details of the key, if any. Flags may be in any
     * order. The meaning of "disabled" is implementation-specific. Note that individual flags may
     * be unimplemented, so the absence of a given flag does not necessarily mean the absence of
     * the detail.
     * <ul>
     * <li>r == revoked</li>
     * <li>d == disabled</li>
     * <li>e == expired</li>
     * </ul>
     * </li>
     * </ul>
     */
    private static final Pattern UID_LINE = Pattern
            .compile("uid:([^:]*):([0-9]+):([0-9]*):([rde]*)",
                    Pattern.CASE_INSENSITIVE);

    private static final Charset UTF_8 = Charset.forName("utf-8");


    private HkpKeyserverAddress hkpKeyserver;


    public static HkpKeyserverClient fromHkpKeyserverAddress(HkpKeyserverAddress hkpKeyserver) {
        return new HkpKeyserverClient(hkpKeyserver);
    }


    private HkpKeyserverClient(HkpKeyserverAddress hkpKeyserver) {
        this.hkpKeyserver = hkpKeyserver;
    }

    @Override
    public ArrayList<ImportKeysListEntry> search(String query, ParcelableProxy proxy)
            throws KeyserverClient.QueryFailedException, KeyserverClient.QueryNeedsRepairException {
        ArrayList<ImportKeysListEntry> results = new ArrayList<>();

        if (query.length() < 3) {
            throw new KeyserverClient.QueryTooShortException();
        }

        String data;
        try {
            HttpUrl url = getHttpUrl(proxy).newBuilder()
                    .addPathSegment("lookup")
                    .addQueryParameter("op", "index")
                    .addQueryParameter("options", "mr")
                    .addQueryParameter("search", query)
                    .build();

            Log.d(Constants.TAG, "Keyserver search: " + url + " using Proxy: " + proxy.getProxy());

            data = query(url, proxy);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unsupported keyserver URI");
        } catch (HttpError e) {
            if (e.getData() != null) {
                Log.d(Constants.TAG, "returned error data: " + e.getData().toLowerCase(Locale.ENGLISH));

                if (e.getData().toLowerCase(Locale.ENGLISH).contains("no keys found")) {
                    // NOTE: This is also a 404 error for some keyservers!
                    return results;
                } else if (e.getData().toLowerCase(Locale.ENGLISH).contains("too many")) {
                    throw new KeyserverClient.TooManyResponsesException();
                } else if (e.getData().toLowerCase(Locale.ENGLISH).contains("insufficient")) {
                    throw new KeyserverClient.QueryTooShortException();
                } else if (e.getCode() == 404) {
                    // NOTE: handle this 404 at last, maybe it was a "no keys found" error
                    throw new KeyserverClient.QueryFailedException("Keyserver '" + hkpKeyserver.getUrl() + "' not found. Error 404");
                } else {
                    // NOTE: some keyserver do not provide a more detailed error response
                    throw new KeyserverClient.QueryTooShortOrTooManyResponsesException();
                }
            }

            throw new KeyserverClient.QueryFailedException("Querying server(s) for '" + hkpKeyserver.getUrl() + "' failed.");
        }

        final Matcher matcher = PUB_KEY_LINE.matcher(data);
        while (matcher.find()) {
            final ImportKeysListEntry entry = new ImportKeysListEntry();
            entry.setQuery(query);

            // group 1 contains the full fingerprint (v4) or the long key id if available
            // see https://bitbucket.org/skskeyserver/sks-keyserver/pull-request/12/fixes-for-machine-readable-indexes/diff
            String fingerprintOrKeyId = matcher.group(1).toLowerCase(Locale.ENGLISH);
            if (fingerprintOrKeyId.length() == 40) {
                byte[] fingerprint = KeyFormattingUtils.convertFingerprintHexFingerprint(fingerprintOrKeyId);
                entry.setFingerprint(fingerprint);
                entry.setKeyIdHex("0x" + fingerprintOrKeyId.substring(fingerprintOrKeyId.length()
                        - 16, fingerprintOrKeyId.length()));
            } else if (fingerprintOrKeyId.length() == 16) {
                // set key id only
                entry.setKeyIdHex("0x" + fingerprintOrKeyId);
            } else {
                Log.e(Constants.TAG, "Wrong length for fingerprint/long key id.");
                // skip this key
                continue;
            }

            try {
                int bitSize = Integer.parseInt(matcher.group(3));
                entry.setBitStrength(bitSize);
                int algorithmId = Integer.decode(matcher.group(2));
                entry.setAlgorithm(KeyFormattingUtils.getAlgorithmInfo(algorithmId, bitSize, null));

                long creationDate = Long.parseLong(matcher.group(4));
                GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                calendar.setTimeInMillis(creationDate * 1000);
                entry.setDate(calendar.getTime());
            } catch (NumberFormatException e) {
                Log.e(Constants.TAG, "Conversation for bit size, algorithm, or creation date failed.", e);
                // skip this key
                continue;
            }

            try {
                entry.setRevoked(matcher.group(6).contains("r"));
                boolean expired = matcher.group(6).contains("e");

                // It may be expired even without flag, thus check expiration date
                String expiration;
                if (!expired && !(expiration = matcher.group(5)).isEmpty()) {
                    long expirationDate = Long.parseLong(expiration);
                    TimeZone timeZoneUTC = TimeZone.getTimeZone("UTC");
                    GregorianCalendar calendar = new GregorianCalendar(timeZoneUTC);
                    calendar.setTimeInMillis(expirationDate * 1000);
                    expired = new GregorianCalendar(timeZoneUTC).compareTo(calendar) >= 0;
                }
                entry.setExpired(expired);
            } catch (NullPointerException e) {
                Log.e(Constants.TAG, "Check for revocation or expiry failed.", e);
                // skip this key
                continue;
            }

            ArrayList<String> userIds = new ArrayList<>();
            final String uidLines = matcher.group(7);
            final Matcher uidMatcher = UID_LINE.matcher(uidLines);
            while (uidMatcher.find()) {
                String tmp = uidMatcher.group(1).trim();
                if (tmp.contains("%")) {
                    if (tmp.contains("%%")) {
                        // The server encodes a percent sign as %%, so it is swapped out with its
                        // urlencoded counterpart to prevent errors
                        tmp = tmp.replace("%%", "%25");
                    }
                    try {
                        // converts Strings like "Universit%C3%A4t" to a proper encoding form "Universität".
                        tmp = URLDecoder.decode(tmp, "UTF8");
                    } catch (UnsupportedEncodingException ignored) {
                        // will never happen, because "UTF8" is supported
                    } catch (IllegalArgumentException e) {
                        Log.e(Constants.TAG, "User ID encoding broken", e);
                        // skip this user id
                        continue;
                    }
                }
                userIds.add(tmp);
            }
            entry.setUserIds(userIds);
            entry.setPrimaryUserId(userIds.get(0));
            entry.setKeyserver(hkpKeyserver);

            results.add(entry);
        }
        return results;
    }

    @Override
    public String get(String keyIdHex, ParcelableProxy proxy) throws KeyserverClient.QueryFailedException {
        String data;
        try {
            HttpUrl url = getHttpUrl(proxy).newBuilder()
                    .addPathSegment("lookup")
                    .addQueryParameter("op", "get")
                    .addQueryParameter("options", "mr")
                    .addQueryParameter("search", keyIdHex)
                    .build();

            Log.d(Constants.TAG, "Keyserver get: " + url + " using Proxy: " + proxy.getProxy());

            data = query(url, proxy);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unsupported keyserver URI");
        } catch (HttpError httpError) {
            Log.d(Constants.TAG, "Failed to get key at HkpKeyserver", httpError);
            if (httpError.getCode() == 404) {
                throw new KeyserverClient.QueryNotFoundException("not found");
            }
            throw new KeyserverClient.QueryFailedException("not found");
        }
        if (data == null) {
            throw new KeyserverClient.QueryFailedException("data is null");
        }

        Matcher matcher = PgpAsciiArmorReformatter.PGP_PUBLIC_KEY.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new KeyserverClient.QueryFailedException("data is null");
    }

    @Override
    public void add(String armoredKey, ParcelableProxy proxy) throws KeyserverClient.AddKeyException {
        try {
            HttpUrl url = getHttpUrl(proxy).newBuilder()
                    .addPathSegment("add")
                    .build();

            RequestBody formBody = new FormBody.Builder()
                    .add("keytext", armoredKey)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();

            Response response =
                    OkHttpClientFactory.getClientPinnedIfAvailable(url.url(), proxy.getProxy())
                            .newCall(request)
                            .execute();

            String responseBody = getResponseBodyAsUtf8(response);

            Log.d(Constants.TAG, "Adding key with URL: " + url
                    + ", response code: " + response.code()
                    + ", body: " + responseBody);

            if (response.code() != 200) {
                throw new KeyserverClient.AddKeyException();
            }

        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException", e);
            throw new KeyserverClient.AddKeyException();
        } catch (URISyntaxException e) {
            Log.e(Constants.TAG, "Unsupported keyserver URI", e);
            throw new KeyserverClient.AddKeyException();
        }
    }

    private HttpUrl getHttpUrl(ParcelableProxy proxy) throws URISyntaxException {
        URI base = hkpKeyserver.getUrlURI();
        if (proxy.isTorEnabled() && hkpKeyserver.getOnionURI() != null) {
            base = hkpKeyserver.getOnionURI();
        }

        return HttpUrl.get(base).newBuilder()
                .addPathSegment("pks")
                .build();
    }

    private String query(HttpUrl url, @NonNull ParcelableProxy proxy) throws KeyserverClient.QueryFailedException, HttpError {
        try {
            OkHttpClient client = OkHttpClientFactory.getClientPinnedIfAvailable(url.url(), proxy.getProxy());

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client
                    .newCall(request)
                    .execute();

            // contains body both in case of success or failure
            String responseBody = getResponseBodyAsUtf8(response);

            if (response.isSuccessful()) {
                return responseBody;
            } else {
                throw new HttpError(response.code(), responseBody);
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException at HkpKeyserver", e);
            String proxyInfo = proxy.getProxy() == Proxy.NO_PROXY ? "" : " Using proxy " + proxy.getProxy();
            Throwable cause = e.getCause();
            String causeName = cause != null ? cause.getClass().getSimpleName() : "generic";
            throw new KeyserverClient.QueryFailedException(String.format(
                    "Network error (%s) for '%s'. Check your Internet connection! %s",
                    causeName, hkpKeyserver.getUrl(), proxyInfo));
        }
    }

    private String getResponseBodyAsUtf8(Response response) throws IOException {
        String responseBody;
        byte[] responseBytes = response.body().bytes();
        try {
            responseBody = new String(responseBytes, response.body().contentType().charset(UTF_8));
        } catch (UnsupportedCharsetException e) {
            responseBody = new String(responseBytes, UTF_8);
        }

        return responseBody;
    }

    private static class HttpError extends Exception {
        private static final long serialVersionUID = 1718783705229428893L;
        private int code;
        private String data;

        HttpError(int code, String data) {
            super("" + code + ": " + data);
            this.code = code;
            this.data = data;
        }

        public int getCode() {
            return code;
        }

        public String getData() {
            return data;
        }
    }

}
