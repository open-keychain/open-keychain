/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2011 Thialfihar <thi@thialfihar.org>
 * Copyright (C) 2011 Senecaso
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.sufficientlysecure.keychain.ui.adapter.ImportKeysListEntry.getAlgorithmFromId;

public class HkpKeyServer extends KeyServer {
    private static class HttpError extends Exception {
        private static final long serialVersionUID = 1718783705229428893L;
        private int mCode;
        private String mData;

        public HttpError(int code, String data) {
            super("" + code + ": " + data);
            mCode = code;
            mData = data;
        }

        public int getCode() {
            return mCode;
        }

        public String getData() {
            return mData;
        }
    }

    private String mHost;
    private short mPort;

    /**
     * pub:%keyid%:%algo%:%keylen%:%creationdate%:%expirationdate%:%flags%
     * <ul>
     * <li>%<b>keyid</b>% = this is either the fingerprint or the key ID of the key. Either the 16-digit or 8-digit
     * key IDs are acceptable, but obviously the fingerprint is best.</li>
     * <li>%<b>algo</b>% = the algorithm number, (i.e. 1==RSA, 17==DSA, etc).
     * See <a href="http://tools.ietf.org/html/rfc2440#section-9.1">RFC-2440</a></li>
     * <li>%<b>keylen</b>% = the key length (i.e. 1024, 2048, 4096, etc.)</li>
     * <li>%<b>creationdate</b>% = creation date of the key in standard
     * <a href="http://tools.ietf.org/html/rfc2440#section-9.1">RFC-2440</a> form (i.e. number of seconds since
     * 1/1/1970 UTC time)</li>
     * <li>%<b>expirationdate</b>% = expiration date of the key in standard
     * <a href="http://tools.ietf.org/html/rfc2440#section-9.1">RFC-2440</a> form (i.e. number of seconds since
     * 1/1/1970 UTC time)</li>
     * <li>%<b>flags</b>% = letter codes to indicate details of the key, if any. Flags may be in any order. The
     * meaning of "disabled" is implementation-specific. Note that individual flags may be unimplemented, so
     * the absence of a given flag does not necessarily mean the absence of the detail.
     * <ul>
     * <li>r == revoked</li>
     * <li>d == disabled</li>
     * <li>e == expired</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @see <a href="http://tools.ietf.org/html/draft-shaw-openpgp-hkp-00#section-5.2">5.2. Machine Readable Indexes</a>
     * in Internet-Draft OpenPGP HTTP Keyserver Protocol Document
     */
    public static final Pattern PUB_KEY_LINE = Pattern
            .compile("pub:([0-9a-fA-F]+):([0-9]+):([0-9]+):([0-9]+):([0-9]*):([rde]*)[ \n\r]*" // pub line
                    + "(uid:(.*):([0-9]+):([0-9]*):([rde]*))+", // one or more uid lines
                    Pattern.CASE_INSENSITIVE);

    /**
     * uid:%escaped uid string%:%creationdate%:%expirationdate%:%flags%
     * <ul>
     * <li>%<b>escaped uid string</b>% = the user ID string, with HTTP %-escaping for anything that isn't 7-bit
     * safe as well as for the ":" character.  Any other characters may be escaped, as desired.</li>
     * <li>%<b>creationdate</b>% = creation date of the key in standard
     * <a href="http://tools.ietf.org/html/rfc2440#section-9.1">RFC-2440</a> form (i.e. number of seconds since
     * 1/1/1970 UTC time)</li>
     * <li>%<b>expirationdate</b>% = expiration date of the key in standard
     * <a href="http://tools.ietf.org/html/rfc2440#section-9.1">RFC-2440</a> form (i.e. number of seconds since
     * 1/1/1970 UTC time)</li>
     * <li>%<b>flags</b>% = letter codes to indicate details of the key, if any. Flags may be in any order. The
     * meaning of "disabled" is implementation-specific. Note that individual flags may be unimplemented, so
     * the absence of a given flag does not necessarily mean the absence of the detail.
     * <ul>
     * <li>r == revoked</li>
     * <li>d == disabled</li>
     * <li>e == expired</li>
     * </ul>
     * </li>
     * </ul>
     */
    public static final Pattern UID_LINE = Pattern
            .compile("uid:(.*):([0-9]+):([0-9]*):([rde]*)",
                    Pattern.CASE_INSENSITIVE);

    private static final short PORT_DEFAULT = 11371;

    /**
     * @param hostAndPort may be just
     *                    "<code>hostname</code>" (eg. "<code>pool.sks-keyservers.net</code>"), then it will
     *                    connect using {@link #PORT_DEFAULT}. However, port may be specified after colon
     *                    ("<code>hostname:port</code>", eg. "<code>p80.pool.sks-keyservers.net:80</code>").
     */
    public HkpKeyServer(String hostAndPort) {
        String host = hostAndPort;
        short port = PORT_DEFAULT;
        final int colonPosition = hostAndPort.lastIndexOf(':');
        if (colonPosition > 0) {
            host = hostAndPort.substring(0, colonPosition);
            final String portStr = hostAndPort.substring(colonPosition + 1);
            port = Short.decode(portStr);
        }
        mHost = host;
        mPort = port;
    }

    public HkpKeyServer(String host, short port) {
        mHost = host;
        mPort = port;
    }

    private static String readAll(InputStream in, String encoding) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();

        byte buffer[] = new byte[1 << 16];
        int n = 0;
        while ((n = in.read(buffer)) != -1) {
            raw.write(buffer, 0, n);
        }

        if (encoding == null) {
            encoding = "utf8";
        }
        return raw.toString(encoding);
    }

    private String query(String request) throws QueryException, HttpError {
        InetAddress ips[];
        try {
            ips = InetAddress.getAllByName(mHost);
        } catch (UnknownHostException e) {
            throw new QueryException(e.toString());
        }
        for (int i = 0; i < ips.length; ++i) {
            try {
                String url = "http://" + ips[i].getHostAddress() + ":" + mPort + request;
                Log.d(Constants.TAG, "hkp keyserver query: " + url);
                URL realUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(25000);
                conn.connect();
                int response = conn.getResponseCode();
                if (response >= 200 && response < 300) {
                    return readAll(conn.getInputStream(), conn.getContentEncoding());
                } else {
                    String data = readAll(conn.getErrorStream(), conn.getContentEncoding());
                    throw new HttpError(response, data);
                }
            } catch (MalformedURLException e) {
                // nothing to do, try next IP
            } catch (IOException e) {
                // nothing to do, try next IP
            }
        }

        throw new QueryException("querying server(s) for '" + mHost + "' failed");
    }

    @Override
    public ArrayList<ImportKeysListEntry> search(String query) throws QueryException, TooManyResponses,
            InsufficientQuery {
        ArrayList<ImportKeysListEntry> results = new ArrayList<ImportKeysListEntry>();

        if (query.length() < 3) {
            throw new InsufficientQuery();
        }

        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(query, "utf8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        String request = "/pks/lookup?op=index&options=mr&search=" + encodedQuery;

        String data;
        try {
            data = query(request);
        } catch (HttpError e) {
            if (e.getCode() == 404) {
                return results;
            } else {
                if (e.getData().toLowerCase(Locale.US).contains("no keys found")) {
                    return results;
                } else if (e.getData().toLowerCase(Locale.US).contains("too many")) {
                    throw new TooManyResponses();
                } else if (e.getData().toLowerCase(Locale.US).contains("insufficient")) {
                    throw new InsufficientQuery();
                }
            }
            throw new QueryException("querying server(s) for '" + mHost + "' failed");
        }

        final Matcher matcher = PUB_KEY_LINE.matcher(data);
        while (matcher.find()) {
            final ImportKeysListEntry entry = new ImportKeysListEntry();

            entry.setBitStrength(Integer.parseInt(matcher.group(3)));

            final int algorithmId = Integer.decode(matcher.group(2));
            entry.setAlgorithm(getAlgorithmFromId(algorithmId));

            // group 1 contains the full fingerprint (v4) or the long key id if available
            // see https://bitbucket.org/skskeyserver/sks-keyserver/pull-request/12/fixes-for-machine-readable-indexes/diff
            // and https://github.com/openpgp-keychain/openpgp-keychain/issues/259#issuecomment-38168176
            String fingerprintOrKeyId = matcher.group(1);
            if (fingerprintOrKeyId.length() > 16) {
                entry.setFingerPrintHex(fingerprintOrKeyId.toLowerCase(Locale.US));
                entry.setKeyIdHex("0x" + fingerprintOrKeyId.substring(fingerprintOrKeyId.length()
                        - 16, fingerprintOrKeyId.length()));
            } else {
                // set key id only
                entry.setKeyIdHex("0x" + fingerprintOrKeyId);
            }

            final long creationDate = Long.parseLong(matcher.group(4));
            final GregorianCalendar tmpGreg = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            tmpGreg.setTimeInMillis(creationDate * 1000);
            entry.setDate(tmpGreg.getTime());

            entry.setRevoked(matcher.group(6).contains("r"));

            ArrayList<String> userIds = new ArrayList<String>();
            final String uidLines = matcher.group(7);
            final Matcher uidMatcher = UID_LINE.matcher(uidLines);
            while (uidMatcher.find()) {
                String tmp = uidMatcher.group(1).trim();
                if (tmp.contains("%")) {
                    try {
                        // converts Strings like "Universit%C3%A4t" to a proper encoding form "Universität".
                        tmp = (URLDecoder.decode(tmp, "UTF8"));
                    } catch (UnsupportedEncodingException ignored) {
                        // will never happen, because "UTF8" is supported
                    }
                }
                userIds.add(tmp);
            }
            entry.setUserIds(userIds);

            results.add(entry);
        }
        return results;
    }

    @Override
    public String get(String keyIdHex) throws QueryException {
        HttpClient client = new DefaultHttpClient();
        try {
            String query = "http://" + mHost + ":" + mPort +
                    "/pks/lookup?op=get&options=mr&search=" + keyIdHex;
            Log.d(Constants.TAG, "hkp keyserver get: " + query);
            HttpGet get = new HttpGet(query);
            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new QueryException("not found");
            }

            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            String data = readAll(is, EntityUtils.getContentCharSet(entity));
            Matcher matcher = PgpHelper.PGP_PUBLIC_KEY.matcher(data);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException e) {
            // nothing to do, better luck on the next keyserver
        } finally {
            client.getConnectionManager().shutdown();
        }

        return null;
    }

    @Override
    public void add(String armoredKey) throws AddKeyException {
        HttpClient client = new DefaultHttpClient();
        try {
            String query = "http://" + mHost + ":" + mPort + "/pks/add";
            HttpPost post = new HttpPost(query);
            Log.d(Constants.TAG, "hkp keyserver add: " + query);
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("keytext", armoredKey));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response = client.execute(post);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new AddKeyException();
            }
        } catch (IOException e) {
            // nothing to do, better luck on the next keyserver
        } finally {
            client.getConnectionManager().shutdown();
        }
    }
}
