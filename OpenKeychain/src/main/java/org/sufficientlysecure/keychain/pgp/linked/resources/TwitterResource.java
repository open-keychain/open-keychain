package org.sufficientlysecure.keychain.pgp.linked.resources;

import android.content.Context;
import android.util.Base64;

import com.textuality.keybase.lib.JWalk;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.linked.LinkedCookieResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Set;

public class TwitterResource extends LinkedCookieResource {

    TwitterResource(Set<String> flags, HashMap<String,String> params, URI uri) {
        super(flags, params, uri);
    }

    public static String generateText (Context context, byte[] fingerprint, String nonce) {
        // nothing special here for now, might change this later
        return LinkedCookieResource.generate(context, fingerprint, nonce);
    }

    private String getTwitterStream(String screenName) {
        String results = null;

        // Step 1: Encode consumer key and secret
        try {
            // URL encode the consumer key and secret
            String urlApiKey = URLEncoder.encode("6IhPnWbYxASAoAzH2QaUtHD0J", "UTF-8");
            String urlApiSecret = URLEncoder.encode("L0GnuiOnapWbSBbQtLIqtpeS5BTtvh06dmoMoKQfHQS8UwHuWm", "UTF-8");

            // Concatenate the encoded consumer key, a colon character, and the
            // encoded consumer secret
            String combined = urlApiKey + ":" + urlApiSecret;

            // Base64 encode the string
            String base64Encoded = Base64.encodeToString(combined.getBytes(), Base64.NO_WRAP);

            // Step 2: Obtain a bearer token
            HttpPost httpPost = new HttpPost("https://api.twitter.com/oauth2/token");
            httpPost.setHeader("Authorization", "Basic " + base64Encoded);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            httpPost.setEntity(new StringEntity("grant_type=client_credentials"));
            JSONObject rawAuthorization = new JSONObject(getResponseBody(httpPost));
            String auth = JWalk.getString(rawAuthorization, "access_token");

            // Applications should verify that the value associated with the
            // token_type key of the returned object is bearer
            if (auth != null && JWalk.getString(rawAuthorization, "token_type").equals("bearer")) {

                // Step 3: Authenticate API requests with bearer token
                HttpGet httpGet =
                        new HttpGet("https://api.twitter.com/1.1/statuses/user_timeline.json?screen_name=" + screenName);

                // construct a normal HTTPS request and include an Authorization
                // header with the value of Bearer <>
                httpGet.setHeader("Authorization", "Bearer " + auth);
                httpGet.setHeader("Content-Type", "application/json");
                // update the results with the body of the response
                results = getResponseBody(httpGet);
            }
        } catch (UnsupportedEncodingException ex) {
        } catch (JSONException ex) {
        } catch (IllegalStateException ex1) {
        }
        return results;
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
                String line = null;
                while ((line = bReader.readLine()) != null) {
                    sb.append(line);
                }
            } else {
                sb.append(reason);
            }
        } catch (UnsupportedEncodingException ex) {
        } catch (ClientProtocolException ex1) {
        } catch (IOException ex2) {
        }
        return sb.toString();
    }

    @Override
    protected String fetchResource(OperationLog log, int indent) {
        return getTwitterStream("Valodim");
    }

}
