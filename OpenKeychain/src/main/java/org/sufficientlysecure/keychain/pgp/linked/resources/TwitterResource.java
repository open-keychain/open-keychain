package org.sufficientlysecure.keychain.pgp.linked.resources;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.util.Log;

import com.textuality.keybase.lib.JWalk;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.linked.LinkedCookieResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TwitterResource extends LinkedCookieResource {

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

        Pattern p = Pattern.compile("https://twitter.com/([a-zA-Z0-9_]+)/status/([0-9]+)");
        Matcher match = p.matcher(uri.toString());
        if (!match.matches()) {
            return null;
        }
        String handle = match.group(1);
        String tweetId = match.group(2);

        return new TwitterResource(flags, params, uri, handle, tweetId);

    }

    public static String generateText (Context context, byte[] fingerprint, int nonce) {
        // nothing special here for now, might change this later
        return LinkedCookieResource.generate(context, fingerprint, nonce);
    }

    @Override
    protected String fetchResource(OperationLog log, int indent) {

        String authToken = getAuthToken();

        if (authToken == null) {
            return null;
        }

        HttpGet httpGet =
                new HttpGet("https://api.twitter.com/1.1/statuses/show.json"
                        + "?id=" + mTweetId
                        + "&include_entities=false");

        // construct a normal HTTPS request and include an Authorization
        // header with the value of Bearer <>
        httpGet.setHeader("Authorization", "Bearer " + authToken);
        httpGet.setHeader("Content-Type", "application/json");

        try {
            String response = getResponseBody(httpGet);
            JSONObject obj = new JSONObject(response);

            if (!obj.has("text")) {
                return null;
            }

            JSONObject user = obj.getJSONObject("user");
            if (!mHandle.equalsIgnoreCase(user.getString("screen_name"))) {
                return null;
            }

            // update the results with the body of the response
            return obj.getString("text");
        } catch (JSONException e) {
            Log.e(Constants.TAG, "json error parsing stream", e);
            return null;
        }

    }

    @Override
    public @DrawableRes int getDisplayIcon() {
        return R.drawable.twitter;
    }

    @Override
    public String getDisplayTitle(Context context) {
        return "Twitter";
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

    public static TwitterResource searchInTwitterStream(String screenName, String needle) {

        String authToken = getAuthToken();

        if (authToken == null) {
            return null;
        }

        HttpGet httpGet =
                new HttpGet("https://api.twitter.com/1.1/statuses/user_timeline.json"
                        + "?screen_name=" + screenName
                        + "&count=15"
                        + "&include_rts=false"
                        + "&trim_user=true"
                        + "&exclude_replies=true");

        // construct a normal HTTPS request and include an Authorization
        // header with the value of Bearer <>
        httpGet.setHeader("Authorization", "Bearer " + authToken);
        httpGet.setHeader("Content-Type", "application/json");

        try {
            String response = getResponseBody(httpGet);
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
            return null;
        } catch (JSONException e) {
            Log.e(Constants.TAG, "json error parsing stream", e);
            return null;
        }
    }

    private static String authToken;

    private static String getAuthToken() {
        if (authToken != null) {
            return authToken;
        }
        try {
            String base64Encoded =
                    "NkloUG5XYll4QVNBb0F6SDJRYVV0SEQwSjpMMEdudWlPbmFwV2JTQ"
                        + "mJRdExJcXRwZVM1QlR0dmgwNmRtb01vS1FmSFFTOFV3SHVXbQ==";

            // Step 2: Obtain a bearer token
            HttpPost httpPost = new HttpPost("https://api.twitter.com/oauth2/token");
            httpPost.setHeader("Authorization", "Basic " + base64Encoded);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            httpPost.setEntity(new StringEntity("grant_type=client_credentials"));
            JSONObject rawAuthorization = new JSONObject(getResponseBody(httpPost));

            // Applications should verify that the value associated with the
            // token_type key of the returned object is bearer
            if (!"bearer".equals(JWalk.getString(rawAuthorization, "token_type"))) {
                return null;
            }

            authToken = JWalk.getString(rawAuthorization, "access_token");
            return authToken;

        } catch (UnsupportedEncodingException | JSONException | IllegalStateException ex) {
            Log.e(Constants.TAG, "auth token fetching error", ex);
            return null;
        }

    }

    private static String getResponseBody(HttpRequestBase request) {
        StringBuilder sb = new StringBuilder();
        try {

            DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String reason = response.getStatusLine().getReasonPhrase();

            if (statusCode == 200) {

                HttpEntity entity = response.getEntity();
                InputStream inputStream = entity.getContent();

                BufferedReader bReader = new BufferedReader(
                        new InputStreamReader(inputStream, "UTF-8"), 8);
                String line;
                while ((line = bReader.readLine()) != null) {
                    sb.append(line);
                }
            } else {
                sb.append(reason);
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, "http request error", e);
        }
        return sb.toString();
    }

}
