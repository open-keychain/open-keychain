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
import org.sufficientlysecure.keychain.util.JWalk;
import org.sufficientlysecure.keychain.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.WeakHashMap;

public class KeybaseKeyServer extends KeyServer {

    private WeakHashMap<String, String> mKeyCache = new WeakHashMap<String, String>();

    @Override
    public ArrayList<ImportKeysListEntry> search(String query) throws QueryException, TooManyResponses,
            InsufficientQuery {
        ArrayList<ImportKeysListEntry> results = new ArrayList<ImportKeysListEntry>();

        JSONObject fromQuery = getFromKeybase("_/api/1.0/user/autocomplete.json?q=", query);
        try {

            JSONArray matches = JWalk.getArray(fromQuery, "completions");
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);

                // only list them if they have a key
                if (JWalk.optObject(match, "components", "key_fingerprint") != null) {
                    results.add(makeEntry(match));
                }
            }
        } catch (Exception e) {
            throw new QueryException("Unexpected structure in keybase search result: " + e.getMessage());
        }

        return results;
    }

    private JSONObject getUser(String keybaseID) throws QueryException {
        try {
            return getFromKeybase("_/api/1.0/user/lookup.json?username=", keybaseID);
        } catch (Exception e) {
            String detail = "";
            if (keybaseID != null) {
                detail = ". Query was for user '" + keybaseID + "'";
            }
            throw new QueryException(e.getMessage() + detail);
        }
    }

    private ImportKeysListEntry makeEntry(JSONObject match) throws QueryException, JSONException {

        String keybaseID = JWalk.getString(match, "components", "username", "val");
        String key_fingerprint = JWalk.getString(match, "components", "key_fingerprint", "val");
        key_fingerprint = key_fingerprint.replace(" ", "").toUpperCase();
        match = getUser(keybaseID);

        final ImportKeysListEntry entry = new ImportKeysListEntry();

        // TODO: Fix; have suggested keybase provide this value to avoid search-time crypto calls
        entry.setBitStrength(4096);
        entry.setAlgorithm("RSA");
        entry.setKeyIdHex("0x" + key_fingerprint);
        entry.setRevoked(false);

        // ctime
        final long creationDate = JWalk.getLong(match, "them", "public_keys", "primary", "ctime");
        final GregorianCalendar tmpGreg = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        tmpGreg.setTimeInMillis(creationDate * 1000);
        entry.setDate(tmpGreg.getTime());

        // key bits
        // we have to fetch the user object to construct the search-result list, so we might as
        //  well (weakly) remember the key, in case they try to import it
        mKeyCache.put(keybaseID, JWalk.getString(match,"them", "public_keys", "primary", "bundle"));

        // String displayName = JWalk.getString(match, "them", "profile", "full_name");
        ArrayList<String> userIds = new ArrayList<String>();
        String name = "keybase.io/" + keybaseID + " <" + keybaseID + "@keybase.io>";
        userIds.add(name);
        userIds.add(keybaseID);
        entry.setUserIds(userIds);
        entry.setPrimaryUserId(name);
        return entry;
    }

    private JSONObject getFromKeybase(String path, String query) throws QueryException {
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
                        throw new QueryException("Keybase autocomplete search failed");
                    }
                    return json;
                } catch (JSONException e) {
                    throw new QueryException("Keybase.io query returned broken JSON");
                }
            } else {
                String message = readAll(conn.getErrorStream(), conn.getContentEncoding());
                throw new QueryException("Keybase.io query error (status=" + response +
                        "): " + message);
            }
        } catch (Exception e) {
            throw new QueryException("Keybase.io query error");
        }
    }

    @Override
    public String get(String id) throws QueryException {
        String key = mKeyCache.get(id);
        if (key == null) {
            try {
                JSONObject user = getUser(id);
                key = JWalk.getString(user, "them", "public_keys", "primary", "bundle");
            } catch (Exception e) {
                throw new QueryException(e.getMessage());
            }
        }
        return key;
    }

    @Override
    public void add(String armoredKey) throws AddKeyException {
        throw new AddKeyException();
    }
}