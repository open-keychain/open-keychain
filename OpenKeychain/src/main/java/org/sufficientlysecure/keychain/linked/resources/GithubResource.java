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

import okhttp3.Request;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.linked.LinkedTokenResource;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GithubResource extends LinkedTokenResource {

    final String mHandle;
    final String mGistId;

    GithubResource(Set<String> flags, HashMap<String,String> params, URI uri,
            String handle, String gistId) {
        super(flags, params, uri);

        mHandle = handle;
        mGistId = gistId;
    }

    public static String generate(Context context, byte[] fingerprint) {
        String token = LinkedTokenResource.generate(fingerprint);

        return String.format(context.getResources().getString(R.string.linked_id_github_text), token);
    }


    @Override
    protected String fetchResource (Context context, OperationLog log, int indent)
            throws HttpStatusException, IOException, JSONException {

        log.add(LogType.MSG_LV_FETCH, indent, mSubUri.toString());
        indent += 1;

        Request request = new Request.Builder()
                .url("https://api.github.com/gists/" + mGistId)
                .addHeader("User-Agent", "OpenKeychain")
                .build();
        String response = getResponseBody(request);

        JSONObject obj = new JSONObject(response);

        JSONObject owner = obj.getJSONObject("owner");
        if (!mHandle.equals(owner.getString("login"))) {
            log.add(LogType.MSG_LV_ERROR_GITHUB_HANDLE, indent);
            return null;
        }

        JSONObject files = obj.getJSONObject("files");
        Iterator<String> it = files.keys();
        if (it.hasNext()) {
            // TODO can there be multiple candidates?
            JSONObject file = files.getJSONObject(it.next());
            return file.getString("content");
        }

        log.add(LogType.MSG_LV_ERROR_GITHUB_NOT_FOUND, indent);
        return null;

    }


    @SuppressWarnings({ "deprecation", "unused" })
    public static GithubResource searchInGithubStream(
            Context context, String screenName, String needle, OperationLog log) {

        // narrow the needle down to important part
        Matcher matcher = magicPattern.matcher(needle);
        if (!matcher.find()) {
            throw new AssertionError("Needle must contain token pattern! This is a programming error, please report.");
        }
        needle = matcher.group();

        try {

            JSONArray array; {
                Request request = new Request.Builder()
                        .url("https://api.github.com/users/" + screenName + "/gists")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("User-Agent", "OpenKeychain")
                        .build();
                String response = getResponseBody(request);
                array = new JSONArray(response);
            }

            for (int i = 0, j = Math.min(array.length(), 5); i < j; i++) {
                JSONObject obj = array.getJSONObject(i);

                JSONObject files = obj.getJSONObject("files");
                Iterator<String> it = files.keys();
                if (it.hasNext()) {

                    JSONObject file = files.getJSONObject(it.next());
                    String type = file.getString("type");
                    if (!"text/plain".equals(type)) {
                        continue;
                    }
                    String id = obj.getString("id");

                    Request request = new Request.Builder()
                            .url("https://api.github.com/gists/" + id)
                            .addHeader("User-Agent", "OpenKeychain")
                            .build();

                    JSONObject gistObj = new JSONObject(getResponseBody(request));
                    JSONObject gistFiles = gistObj.getJSONObject("files");
                    Iterator<String> gistIt = gistFiles.keys();
                    if (!gistIt.hasNext()) {
                        continue;
                    }
                    // TODO can there be multiple candidates?
                    JSONObject gistFile = gistFiles.getJSONObject(gistIt.next());
                    String content = gistFile.getString("content");
                    if (!content.contains(needle)) {
                        continue;
                    }

                    URI uri = URI.create("https://gist.github.com/" + screenName + "/" + id);
                    return create(uri);
                }
            }

            // update the results with the body of the response
            log.add(LogType.MSG_LV_FETCH_ERROR_NOTHING, 2);
            return null;

        } catch (HttpStatusException e) {
            // log verbose output to logcat
            Log.e(Constants.TAG, "http error (" + e.getStatus() + "): " + e.getReason());
            log.add(LogType.MSG_LV_FETCH_ERROR, 2, Integer.toString(e.getStatus()));
        } catch (MalformedURLException e) {
            log.add(LogType.MSG_LV_FETCH_ERROR_URL, 2);
        } catch (IOException e) {
            Log.e(Constants.TAG, "io error", e);
            log.add(LogType.MSG_LV_FETCH_ERROR_IO, 2);
        } catch (JSONException e) {
            Log.e(Constants.TAG, "json error", e);
            log.add(LogType.MSG_LV_FETCH_ERROR_FORMAT, 2);
        }

        return null;
    }

    public static GithubResource create(URI uri) {
        return create(new HashSet<String>(), new HashMap<String,String>(), uri);
    }

    public static GithubResource create(Set<String> flags, HashMap<String,String> params, URI uri) {

        // no params or flags
        if (!flags.isEmpty() || !params.isEmpty()) {
            return null;
        }

        Pattern p = Pattern.compile("https://gist\\.github\\.com/([a-zA-Z0-9_-]+)/([0-9a-f]+)");
        Matcher match = p.matcher(uri.toString());
        if (!match.matches()) {
            return null;
        }
        String handle = match.group(1);
        String gistId = match.group(2);

        return new GithubResource(flags, params, uri, handle, gistId);

    }


    @Override
    public @DrawableRes
    int getDisplayIcon() {
        return R.drawable.linked_github;
    }

    @Override
    public @StringRes
    int getVerifiedText(boolean isSecret) {
        return isSecret ? R.string.linked_verified_secret_github : R.string.linked_verified_github;
    }

    @Override
    public String getDisplayTitle(Context context) {
        return context.getString(R.string.linked_title_github);
    }

    @Override
    public String getDisplayComment(Context context) {
        return mHandle;
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
}
