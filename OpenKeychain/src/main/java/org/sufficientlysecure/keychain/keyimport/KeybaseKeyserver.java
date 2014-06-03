/*
 * Copyright (C) 2014 Tim Bray <tbray@textuality.com>
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.util.JWalk;
import org.sufficientlysecure.keychain.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class KeybaseKeyserver extends Keyserver {
    private String mQuery;

    @Override
    public ArrayList<ImportKeysListEntry> search(String query) throws QueryFailedException,
            QueryNeedsRepairException {
        ArrayList<ImportKeysListEntry> results = new ArrayList<ImportKeysListEntry>();

        if (query.startsWith("0x")) {
            // cut off "0x" if a user is searching for a key id
            query = query.substring(2);
        }

        JSONObject fromQuery = getFromKeybase("_/api/1.0/user/autocomplete.json?q=", query);
        try {

            JSONArray matches = JWalk.getArray(fromQuery, "completions");
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);

                // only list them if they have a key
                if (JWalk.optObject(match, "components", "key_fingerprint") != null) {
                    String keybaseId = JWalk.getString(match, "components", "username", "val");
                    String fingerprint = JWalk.getString(match, "components", "key_fingerprint", "val");
                    fingerprint = fingerprint.replace(" ", "").toUpperCase();

                    if (keybaseId.equals(query) || fingerprint.startsWith(query.toUpperCase())) {
                        results.add(makeEntry(match));
                    } else {
                        results.add(makeEntry(match));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "keybase result parsing error", e);
            throw new QueryFailedException("Unexpected structure in keybase search result: " + e.getMessage());
        }

        return results;
    }

    private JSONObject getUser(String keybaseId) throws QueryFailedException {
        try {
            return getFromKeybase("_/api/1.0/user/lookup.json?username=", keybaseId);
        } catch (Exception e) {
            String detail = "";
            if (keybaseId != null) {
                detail = ". Query was for user '" + keybaseId + "'";
            }
            throw new QueryFailedException(e.getMessage() + detail);
        }
    }

    private ImportKeysListEntry makeEntry(JSONObject match) throws QueryFailedException, JSONException {

        final ImportKeysListEntry entry = new ImportKeysListEntry();
        entry.setQuery(mQuery);

        String keybaseId = JWalk.getString(match, "components", "username", "val");
        String fullName = JWalk.getString(match, "components", "full_name", "val");
        String fingerprint = JWalk.getString(match, "components", "key_fingerprint", "val");
        fingerprint = fingerprint.replace(" ", "").toUpperCase(); // not strictly necessary but doesn't hurt
        entry.setFingerprintHex(fingerprint);

        entry.setKeyIdHex("0x" + fingerprint.substring(Math.max(0, fingerprint.length() - 16)));
        // store extra info, so we can query for the keybase id directly
        entry.setExtraData(keybaseId);

        final int algorithmId = JWalk.getInt(match, "components", "key_fingerprint", "algo");
        entry.setAlgorithm(PgpKeyHelper.getAlgorithmInfo(algorithmId));
        final int bitStrength = JWalk.getInt(match, "components", "key_fingerprint", "nbits");
        entry.setBitStrength(bitStrength);

        ArrayList<String> userIds = new ArrayList<String>();
        String name = fullName + " <keybase.io/" + keybaseId + ">";
        userIds.add(name);
        try {
            userIds.add("github.com/" + JWalk.getString(match, "components", "github", "val"));
        } catch (JSONException e) {
            // ignore
        }
        try {
            userIds.add("twitter.com/" + JWalk.getString(match, "components", "twitter", "val"));
        } catch (JSONException e) {
            // ignore
        }
        try {
            JSONArray array = JWalk.getArray(match, "components", "websites");
            JSONObject website = array.getJSONObject(0);
            userIds.add(JWalk.getString(website, "val"));
        } catch (JSONException e) {
            // ignore
        }
        entry.setUserIds(userIds);
        entry.setPrimaryUserId(name);
        return entry;
    }

    private JSONObject getFromKeybase(String path, String query) throws QueryFailedException {
        try {
            String url = "https://keybase.io/" + path + URLEncoder.encode(query, "utf8");
            Log.d(Constants.TAG, "keybase query: " + url);

            URL realUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
            conn.setConnectTimeout(5000); // TODO: Reasonable values for keybase
            conn.setReadTimeout(25000);
            conn.connect();
            int response = conn.getResponseCode();
            if (response >= 200 && response < 300) {
                String text = readAll(conn.getInputStream(), conn.getContentEncoding());
                try {
                    JSONObject json = new JSONObject(text);
                    if (JWalk.getInt(json, "status", "code") != 0) {
                        throw new QueryFailedException("Keybase.io query failed: " + path + "?" +
                                query);
                    }
                    return json;
                } catch (JSONException e) {
                    throw new QueryFailedException("Keybase.io query returned broken JSON");
                }
            } else {
                String message = readAll(conn.getErrorStream(), conn.getContentEncoding());
                throw new QueryFailedException("Keybase.io query error (status=" + response +
                        "): " + message);
            }
        } catch (Exception e) {
            throw new QueryFailedException("Keybase.io query error");
        }
    }

    @Override
    public String get(String id) throws QueryFailedException {
        try {
            JSONObject user = getUser(id);
            return JWalk.getString(user, "them", "public_keys", "primary", "bundle");
        } catch (Exception e) {
            throw new QueryFailedException(e.getMessage());
        }
    }

    @Override
    public void add(String armoredKey) throws AddKeyException {
        throw new AddKeyException();
    }
}
