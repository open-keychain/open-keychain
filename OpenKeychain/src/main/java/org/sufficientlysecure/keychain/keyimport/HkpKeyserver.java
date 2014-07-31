/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2011-2014 Thialfihar <thi@thialfihar.org>
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

package org.sufficientlysecure.keychain.keyimport;

import de.measite.minidns.Client;
import de.measite.minidns.Question;
import de.measite.minidns.Record;
import de.measite.minidns.record.SRV;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.helper.TlsHelper;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HkpKeyserver extends Keyserver {
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
    private boolean mSecure;

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
    public static final Pattern PUB_KEY_LINE = Pattern
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
    public static final Pattern UID_LINE = Pattern
            .compile("uid:([^:]*):([0-9]+):([0-9]*):([rde]*)",
                    Pattern.CASE_INSENSITIVE);

    private static final short PORT_DEFAULT = 11371;
    private static final short PORT_DEFAULT_HKPS = 443;

    /**
     * @param hostAndPort may be just
     *                    "<code>hostname</code>" (eg. "<code>pool.sks-keyservers.net</code>"), then it will
     *                    connect using {@link #PORT_DEFAULT}. However, port may be specified after colon
     *                    ("<code>hostname:port</code>", eg. "<code>p80.pool.sks-keyservers.net:80</code>").
     */
    public HkpKeyserver(String hostAndPort) {
        String host = hostAndPort;
        short port = PORT_DEFAULT;
        boolean secure = false;
        String[] parts = hostAndPort.split(":");
        if (parts.length > 1) {
            if (!parts[0].contains(".")) { // This is not a domain or ip, so it must be a protocol name
                if (parts[0].equalsIgnoreCase("hkps") || parts[0].equalsIgnoreCase("https")) {
                    secure = true;
                    port = PORT_DEFAULT_HKPS;
                } else if (!parts[0].equalsIgnoreCase("hkp") && !parts[0].equalsIgnoreCase("http")) {
                    throw new IllegalArgumentException("Protocol " + parts[0] + " is unknown");
                }
                host = parts[1];
                if (host.startsWith("//")) { // People tend to type https:// and hkps://, so we'll support that as well
                    host = host.substring(2);
                }
                if (parts.length > 2) {
                    port = Short.decode(parts[2]);
                }
            } else {
                host = parts[0];
                port = Short.decode(parts[1]);
            }
        }
        mHost = host;
        mPort = port;
        mSecure = secure;
    }

    public HkpKeyserver(String host, short port) {
        this(host, port, false);
    }

    public HkpKeyserver(String host, short port, boolean secure) {
        mHost = host;
        mPort = port;
        mSecure = secure;
    }

    private String getUrlPrefix() {
        return mSecure ? "https://" : "http://";
    }

    private HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) TlsHelper.openConnection(url);
        } catch (TlsHelper.TlsHelperException e) {
            Log.w(Constants.TAG, e);
        }
        if (conn == null) {
            conn = (HttpURLConnection) url.openConnection();
        }
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(25000);
        return conn;
    }

    private String query(String request) throws QueryFailedException, HttpError {
        try {
            URL url = new URL(getUrlPrefix() + mHost + ":" + mPort + request);
            Log.d(Constants.TAG, "hkp keyserver query: " + url);
            HttpURLConnection conn = openConnection(url);
            conn.connect();
            int response = conn.getResponseCode();
            if (response >= 200 && response < 300) {
                return readAll(conn.getInputStream(), conn.getContentEncoding());
            } else {
                String data = readAll(conn.getErrorStream(), conn.getContentEncoding());
                throw new HttpError(response, data);
            }
        } catch (IOException e) {
            throw new QueryFailedException("Keyserver '" + mHost + "' is unavailable. Check your Internet connection!");
        }
    }

    /**
     * Results are sorted by creation date of key!
     *
     * @param query
     * @return
     * @throws QueryFailedException
     * @throws QueryNeedsRepairException
     */
    @Override
    public ArrayList<ImportKeysListEntry> search(String query) throws QueryFailedException,
            QueryNeedsRepairException {
        ArrayList<ImportKeysListEntry> results = new ArrayList<ImportKeysListEntry>();

        if (query.length() < 3) {
            throw new QueryTooShortException();
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
            if (e.getData() != null) {
                Log.d(Constants.TAG, "returned error data: " + e.getData().toLowerCase(Locale.US));

                if (e.getData().toLowerCase(Locale.US).contains("no keys found")) {
                    // NOTE: This is also a 404 error for some keyservers!
                    return results;
                } else if (e.getData().toLowerCase(Locale.US).contains("too many")) {
                    throw new TooManyResponsesException();
                } else if (e.getData().toLowerCase(Locale.US).contains("insufficient")) {
                    throw new QueryTooShortException();
                } else if (e.getCode() == 404) {
                    // NOTE: handle this 404 at last, maybe it was a "no keys found" error
                    throw new QueryFailedException("Keyserver '" + mHost + "' not found. Error 404");
                } else {
                    // NOTE: some keyserver do not provide a more detailed error response
                    throw new QueryFailedException("Either no keys or too many have been found. Please improve your query!");
                }
            }

            throw new QueryFailedException("Querying server(s) for '" + mHost + "' failed.");
        }

        final Matcher matcher = PUB_KEY_LINE.matcher(data);
        while (matcher.find()) {
            final ImportKeysListEntry entry = new ImportKeysListEntry();
            entry.setQuery(query);
            entry.setOrigin(getUrlPrefix() + mHost + ":" + mPort);

            entry.setBitStrength(Integer.parseInt(matcher.group(3)));

            final int algorithmId = Integer.decode(matcher.group(2));
            entry.setAlgorithm(PgpKeyHelper.getAlgorithmInfo(algorithmId));

            // group 1 contains the full fingerprint (v4) or the long key id if available
            // see http://bit.ly/1d4bxbk and http://bit.ly/1gD1wwr
            String fingerprintOrKeyId = matcher.group(1);
            if (fingerprintOrKeyId.length() > 16) {
                entry.setFingerprintHex(fingerprintOrKeyId.toLowerCase(Locale.US));
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
            entry.setExpired(matcher.group(6).contains("e"));

            ArrayList<String> userIds = new ArrayList<String>();
            final String uidLines = matcher.group(7);
            final Matcher uidMatcher = UID_LINE.matcher(uidLines);
            while (uidMatcher.find()) {
                String tmp = uidMatcher.group(1).trim();
                if (tmp.contains("%")) {
                    if (tmp.contains("%%")) {
                        // This is a fix for issue #683
                        // The server encodes a percent sign as %%, so it is swapped out with its
                        // urlencoded counterpart to prevent errors
                        tmp = tmp.replace("%%", "%25");
                    }
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
            entry.setPrimaryUserId(userIds.get(0));

            results.add(entry);
        }
        return results;
    }

    @Override
    public String get(String keyIdHex) throws QueryFailedException {
        String request = "/pks/lookup?op=get&options=mr&search=" + keyIdHex;
        Log.d(Constants.TAG, "hkp keyserver get: " + request);
        String data;
        try {
            data = query(request);
        } catch (HttpError httpError) {
            throw new QueryFailedException("not found");
        }
        Matcher matcher = PgpHelper.PGP_PUBLIC_KEY.matcher(data);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    public void add(String armoredKey) throws AddKeyException {
        try {
            String query = getUrlPrefix() + mHost + ":" + mPort + "/pks/add";
            String params;
            try {
                params = "keytext=" + URLEncoder.encode(armoredKey, "utf8");
            } catch (UnsupportedEncodingException e) {
                throw new AddKeyException();
            }
            Log.d(Constants.TAG, "hkp keyserver add: " + query);

            HttpURLConnection connection = openConnection(new URL(query));
            connection.setRequestMethod("POST");
            connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Integer.toString(params.getBytes().length));
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(params);
            wr.flush();
            wr.close();
        } catch (IOException e) {
            throw new AddKeyException();
        }
    }

    @Override
    public String toString() {
        return mHost + ":" + mPort;
    }

    /**
     * Tries to find a server responsible for a given domain
     *
     * @return A responsible Keyserver or null if not found.
     */
    public static HkpKeyserver resolve(String domain) {
        try {
            Record[] records = new Client().query(new Question("_hkp._tcp." + domain, Record.TYPE.SRV)).getAnswers();
            if (records.length > 0) {
                Arrays.sort(records, new Comparator<Record>() {
                    @Override
                    public int compare(Record lhs, Record rhs) {
                        if (lhs.getPayload().getType() != Record.TYPE.SRV) return 1;
                        if (rhs.getPayload().getType() != Record.TYPE.SRV) return -1;
                        return ((SRV) lhs.getPayload()).getPriority() - ((SRV) rhs.getPayload()).getPriority();
                    }
                });
                Record record = records[0]; // This is our best choice
                if (record.getPayload().getType() == Record.TYPE.SRV) {
                    return new HkpKeyserver(((SRV) record.getPayload()).getName(),
                            (short) ((SRV) record.getPayload()).getPort());
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
