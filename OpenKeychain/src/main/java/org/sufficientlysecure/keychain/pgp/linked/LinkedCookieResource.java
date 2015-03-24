package org.sufficientlysecure.keychain.pgp.linked;

import android.content.Context;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.LinkedVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

public abstract class LinkedCookieResource extends LinkedResource {

    protected LinkedCookieResource(Set<String> flags, HashMap<String, String> params, URI uri) {
        super(flags, params, uri);
    }

    public URI toUri () {

        StringBuilder b = new StringBuilder();
        b.append("pgpid+cookie:");

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

    public URI getSubUri () {
        return mSubUri;
    }

    public static String generate (Context context, byte[] fingerprint) {
        return String.format("[Verifying my PGP key: openpgp4fpr:%s]",
                KeyFormattingUtils.convertFingerprintToHex(fingerprint));
    }

    public static String generatePreview () {
        return "[Verifying my PGP key: openpgp4fpr:0x…]";
    }

    public LinkedVerifyResult verify(byte[] fingerprint) {

        OperationLog log = new OperationLog();
        log.add(LogType.MSG_LV, 0);

        // Try to fetch resource. Logs for itself
        String res = fetchResource(log, 1);
        if (res == null) {
            // if this is null, an error was recorded in fetchResource above
            return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
        }

        Log.d(Constants.TAG, "Resource data: '" + res + "'");

        return verifyString(log, 1, res, fingerprint);

    }

    protected abstract String fetchResource (OperationLog log, int indent);

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

    public static String getResponseBody(HttpRequestBase request) throws IOException, HttpStatusException {
        StringBuilder sb = new StringBuilder();

        DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
        HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        String reason = response.getStatusLine().getReasonPhrase();

        if (statusCode != 200) {
            throw new HttpStatusException(statusCode, reason);
        }

        HttpEntity entity = response.getEntity();
        InputStream inputStream = entity.getContent();

        BufferedReader bReader = new BufferedReader(
                new InputStreamReader(inputStream, "UTF-8"), 8);
        String line;
        while ((line = bReader.readLine()) != null) {
            sb.append(line);
        }

        return sb.toString();
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
