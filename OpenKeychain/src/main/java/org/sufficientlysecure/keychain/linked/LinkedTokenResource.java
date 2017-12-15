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

package org.sufficientlysecure.keychain.linked;

import android.content.Context;

import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.linked.resources.GenericHttpsResource;
import org.sufficientlysecure.keychain.linked.resources.GithubResource;
import org.sufficientlysecure.keychain.linked.resources.TwitterResource;
import org.sufficientlysecure.keychain.operations.results.LinkedVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.network.OkHttpClientFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class LinkedTokenResource extends LinkedResource {

    protected final URI mSubUri;
    protected final Set<String> mFlags;
    protected final HashMap<String,String> mParams;

    public static Pattern magicPattern =
            Pattern.compile("\\[Verifying my (?:Open|)?PGP key(?::|) openpgp4fpr:([a-zA-Z0-9]+)]");

    protected LinkedTokenResource(Set<String> flags, HashMap<String, String> params, URI uri) {
        mFlags = flags;
        mParams = params;
        mSubUri = uri;
    }

    @SuppressWarnings("unused")
    public URI getSubUri () {
        return mSubUri;
    }

    public Set<String> getFlags () {
        return new HashSet<>(mFlags);
    }

    public HashMap<String,String> getParams () {
        return new HashMap<>(mParams);
    }

    public static String generate (byte[] fingerprint) {
        return String.format("[Verifying my OpenPGP key: openpgp4fpr:%s]",
                KeyFormattingUtils.convertFingerprintToHex(fingerprint));
    }

    protected static LinkedTokenResource fromUri (URI uri) {

        if (!"openpgpid+token".equals(uri.getScheme())
                && !"openpgpid+cookie".equals(uri.getScheme())) {
            Log.e(Constants.TAG, "unknown uri scheme in (suspected) linked id packet");
            return null;
        }

        if (!uri.isOpaque()) {
            Log.e(Constants.TAG, "non-opaque uri in (suspected) linked id packet");
            return null;
        }

        String specific = uri.getSchemeSpecificPart();
        if (!specific.contains("@")) {
            Log.e(Constants.TAG, "unknown uri scheme in linked id packet");
            return null;
        }

        String[] pieces = specific.split("@", 2);
        URI subUri = URI.create(pieces[1]);

        Set<String> flags = new HashSet<>();
        HashMap<String,String> params = new HashMap<>();
        if (!pieces[0].isEmpty()) {
            String[] rawParams = pieces[0].split(";");
            for (String param : rawParams) {
                String[] p = param.split("=", 2);
                if (p.length == 1) {
                    flags.add(param);
                } else {
                    params.put(p[0], p[1]);
                }
            }
        }

        return findResourceType(flags, params, subUri);

    }

    protected static LinkedTokenResource findResourceType (Set<String> flags,
            HashMap<String,String> params, URI  subUri) {

        LinkedTokenResource res;

        res = GenericHttpsResource.create(flags, params, subUri);
        if (res != null) {
            return res;
        }
        // res = DnsResource.create(flags, params, subUri);
        // if (res != null) {
            // return res;
        // }
        res = TwitterResource.create(flags, params, subUri);
        if (res != null) {
            return res;
        }
        res = GithubResource.create(flags, params, subUri);
        if (res != null) {
            return res;
        }

        return null;

    }

    public URI toUri () {

        StringBuilder b = new StringBuilder();
        b.append("openpgpid+token:");

        // add flags
        if (mFlags != null) {
            boolean first = true;
            for (String flag : mFlags) {
                if (!first) {
                    b.append(";");
                }
                first = false;
                b.append(flag);
            }
        }

        // add parameters
        if (mParams != null) {
            boolean first = true;
            for (Entry<String, String> stringStringEntry : mParams.entrySet()) {
                if (!first) {
                    b.append(";");
                }
                first = false;
                b.append(stringStringEntry.getKey()).append("=").append(stringStringEntry.getValue());
            }
        }

        b.append("@");
        b.append(mSubUri);

        return URI.create(b.toString());

    }

    public LinkedVerifyResult verify(Context context, byte[] fingerprint) {

        OperationLog log = new OperationLog();
        log.add(LogType.MSG_LV, 0);

        // Try to fetch resource. Logs for itself
        String res = null;
        try {
            res = fetchResource(context, log, 1);
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

        if (res == null) {
            // if this is null, an error was recorded in fetchResource above
            return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
        }

        Log.d(Constants.TAG, "Resource data: '" + res + "'");

        return verifyString(log, 1, res, fingerprint);

    }

    protected abstract String fetchResource (Context context, OperationLog log, int indent)
            throws HttpStatusException, IOException, JSONException;

    protected Matcher matchResource (OperationLog log, int indent, String res) {
        return magicPattern.matcher(res);
    }

    protected LinkedVerifyResult verifyString (OperationLog log, int indent,
                                               String res,
                                               byte[] fingerprint) {

        log.add(LogType.MSG_LV_MATCH, indent);
        Matcher match = matchResource(log, indent+1, res);
        if (!match.find()) {
            log.add(LogType.MSG_LV_MATCH_ERROR, 2);
            return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
        }

        String candidateFp = match.group(1).toLowerCase();
        String fp = KeyFormattingUtils.convertFingerprintToHex(fingerprint);
        if (!fp.equals(candidateFp)) {
            log.add(LogType.MSG_LV_FP_ERROR, indent);
            return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
        }
        log.add(LogType.MSG_LV_FP_OK, indent);

        return new LinkedVerifyResult(LinkedVerifyResult.RESULT_OK, log);

    }


    private static CertificatePinner getCertificatePinner(String hostname, String[] pins){
        CertificatePinner.Builder builder = new CertificatePinner.Builder();
        for(String pin : pins){
            builder.add(hostname,pin);
        }
        return builder.build();
    }


    public static String getResponseBody(Request request, String... pins)
            throws IOException, HttpStatusException {

        Log.d("Connection to: " + request.url().url().getHost(), "");
        OkHttpClient client;
        if (pins != null) {
            client = OkHttpClientFactory.getSimpleClientPinned(getCertificatePinner(request.url().url().getHost(), pins));
        } else {
            client = OkHttpClientFactory.getSimpleClient();
        }

        Response response = client.newCall(request).execute();


        int statusCode = response.code();
        String reason = response.message();

        if (statusCode != 200) {
            throw new HttpStatusException(statusCode, reason);
        }

        return response.body().string();
    }

    public static class HttpStatusException extends Throwable {

        private final int mStatusCode;
        private final String mReason;

        HttpStatusException(int statusCode, String reason) {
            super("http status " + statusCode + ": " + reason);
            mStatusCode = statusCode;
            mReason = reason;
        }

        public int getStatus() {
            return mStatusCode;
        }

        public String getReason() {
            return mReason;
        }

    }

}
