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

package org.sufficientlysecure.keychain.linked.resources;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.Log;

import com.textuality.keybase.lib.JWalk;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.linked.LinkedTokenResource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitterResource extends LinkedTokenResource {

    public static final String[] CERT_PINS = null; /*(new String[] {
        // Symantec Class 3 Secure Server CA - G4
        "513fb9743870b73440418d30930699ff"
    };*/

    final String mHandle;
    final String mTweetId;

    TwitterResource(Set<String> flags, HashMap<String,String> params,
            URI uri, String handle, String tweetId) {
        super(flags, params, uri);

        mHandle = handle;
        mTweetId = tweetId;
    }

    public static TwitterResource create(URI uri) {
        return create(new HashSet<String>(), new HashMap<String,String>(), uri);
    }

    public static TwitterResource create(Set<String> flags, HashMap<String,String> params, URI uri) {

        // no params or flags
        if (!flags.isEmpty() || !params.isEmpty()) {
            return null;
        }

        Pattern p = Pattern.compile("https://twitter\\.com/([a-zA-Z0-9_]+)/status/([0-9]+)");
        Matcher match = p.matcher(uri.toString());
        if (!match.matches()) {
            return null;
        }
        String handle = match.group(1);
        String tweetId = match.group(2);

        return new TwitterResource(flags, params, uri, handle, tweetId);

    }

    @SuppressWarnings("deprecation")
    @Override
    protected String fetchResource(Context context, OperationLog log, int indent)
            throws IOException, HttpStatusException, JSONException {

        String authToken;
        try {
            authToken = getAuthToken(context);
        } catch (IOException | HttpStatusException | JSONException e) {
            log.add(LogType.MSG_LV_ERROR_TWITTER_AUTH, indent);
            return null;
        }

        // construct a normal HTTPS request and include an Authorization
        // header with the value of Bearer <>
        Request request = new Request.Builder()
                .url("https://api.twitter.com/1.1/statuses/show.json"
                        + "?id=" + mTweetId
                        + "&include_entities=false")
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "OpenKeychain")
                .build();

        try {
            String response = getResponseBody(request, CERT_PINS);
            JSONObject obj = new JSONObject(response);
            JSONObject user = obj.getJSONObject("user");
            if (!mHandle.equalsIgnoreCase(user.getString("screen_name"))) {
                log.add(LogType.MSG_LV_ERROR_TWITTER_HANDLE, indent);
                return null;
            }

            // update the results with the body of the response
            return obj.getString("text");
        } catch (JSONException e) {
            log.add(LogType.MSG_LV_ERROR_TWITTER_RESPONSE, indent);
            return null;
        }

    }

    @Override
    public @DrawableRes int getDisplayIcon() {
        return R.drawable.linked_twitter;
    }

    @Override
    public @StringRes
    int getVerifiedText(boolean isSecret) {
        return isSecret ? R.string.linked_verified_secret_twitter : R.string.linked_verified_twitter;
    }

    @Override
    public String getDisplayTitle(Context context) {
        return context.getString(R.string.linked_title_twitter);
    }

    @Override
    public String getDisplayComment(Context context) {
        return "@" + mHandle;
    }

    @Override
    public boolean isViewable() {
        return true;
    }

    @Override
    public Intent getViewIntent() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(mSubUri.toString()));
        return intent;
    }

    @SuppressWarnings("deprecation")
    public static TwitterResource searchInTwitterStream(
            Context context, String screenName, String needle, OperationLog log) {

        String authToken;
        try {
            authToken = getAuthToken(context);
        } catch (IOException | HttpStatusException | JSONException e) {
            log.add(LogType.MSG_LV_ERROR_TWITTER_AUTH, 1);
            return null;
        }

        Request request = new Request.Builder()
                .url("https://api.twitter.com/1.1/statuses/user_timeline.json"
                        + "?screen_name=" + screenName
                        + "&count=15"
                        + "&include_rts=false"
                        + "&trim_user=true"
                        + "&exclude_replies=true")
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "OpenKeychain")
                .build();

        try {
            String response = getResponseBody(request, CERT_PINS);
            JSONArray array = new JSONArray(response);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String tweet = obj.getString("text");
                if (tweet.contains(needle)) {
                    String id = obj.getString("id_str");
                    URI uri = URI.create("https://twitter.com/" + screenName + "/status/" + id);
                    return create(uri);
                }
            }

            // update the results with the body of the response
            log.add(LogType.MSG_LV_FETCH_ERROR_NOTHING, 1);
            return null;

        } catch (HttpStatusException e) {
            // log verbose output to logcat
            Log.e(Constants.TAG, "http error (" + e.getStatus() + "): " + e.getReason());
            log.add(LogType.MSG_LV_FETCH_ERROR, 1, Integer.toString(e.getStatus()));
        } catch (MalformedURLException e) {
            log.add(LogType.MSG_LV_FETCH_ERROR_URL, 1);
        } catch (IOException e) {
            Log.e(Constants.TAG, "io error", e);
            log.add(LogType.MSG_LV_FETCH_ERROR_IO, 1);
        } catch (JSONException e) {
            Log.e(Constants.TAG, "json error", e);
            log.add(LogType.MSG_LV_FETCH_ERROR_FORMAT, 1);
        }

        return null;
    }

    private static String cachedAuthToken;

    @SuppressWarnings("deprecation")
    private static String getAuthToken(Context context)
            throws IOException, HttpStatusException, JSONException {
        if (cachedAuthToken != null) {
            return cachedAuthToken;
        }
        String base64Encoded = rot13("D293FQqanH0jH29KIaWJER5DomqSGRE2Ewc1LJACn3cbD1c"
                    + "Fq1bmqSAQAz5MI2cIHKOuo3cPoRAQI1OyqmIVFJS6LHMXq2g6MRLkIj") + "==";

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/x-www-form-urlencoded;charset=UTF-8"),
                "grant_type=client_credentials");

        // Step 2: Obtain a bearer token
        Request request = new Request.Builder()
                .url("https://api.twitter.com/oauth2/token")
                .addHeader("Authorization", "Basic " + base64Encoded)
                .addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .addHeader("User-Agent", "OpenKeychain")
                .post(requestBody)
                .build();

        JSONObject rawAuthorization = new JSONObject(getResponseBody(request, CERT_PINS));

        // Applications should verify that the value associated with the
        // token_type key of the returned object is bearer
        if (!"bearer".equals(JWalk.getString(rawAuthorization, "token_type"))) {
            throw new JSONException("Expected bearer token in response!");
        }

        cachedAuthToken = rawAuthorization.getString("access_token");
        return cachedAuthToken;

    }

    public static String rot13(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if       (c >= 'a' && c <= 'm') c += 13;
            else if  (c >= 'A' && c <= 'M') c += 13;
            else if  (c >= 'n' && c <= 'z') c -= 13;
            else if  (c >= 'N' && c <= 'Z') c -= 13;
            sb.append(c);
        }
        return sb.toString();
    }

}
