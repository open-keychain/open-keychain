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
