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

package org.sufficientlysecure.keychain.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Minimal hierarchy selector
 */
public class JWalk {

    public static int getInt(JSONObject json, String... path) throws JSONException {
        json = walk(json, path);
        return json.getInt(path[path.length - 1]);
    }

    public static long getLong(JSONObject json, String... path) throws JSONException {
        json = walk(json, path);
        return json.getLong(path[path.length - 1]);
    }

    public static String getString(JSONObject json, String... path) throws JSONException {
        json = walk(json, path);
        return json.getString(path[path.length - 1]);
    }

    public static JSONArray getArray(JSONObject json, String... path) throws JSONException {
        json = walk(json, path);
        return json.getJSONArray(path[path.length - 1]);
    }

    public static JSONObject optObject(JSONObject json, String... path) throws JSONException {
        json = walk(json, path);
        return json.optJSONObject(path[path.length - 1]);
    }

    private static JSONObject walk(JSONObject json, String... path) throws JSONException {
        int len = path.length - 1;
        int pathIndex = 0;
        try {
            while (pathIndex < len) {
                json = json.getJSONObject(path[pathIndex]);
                pathIndex++;
            }
        } catch (JSONException e) {
            // try to give ’em a nice-looking error
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                sb.append(path[i]).append('.');
            }
            sb.append(path[len]);
            throw new JSONException("JWalk error at step " + pathIndex + " of " + sb);
        }
        return json;
    }
}
