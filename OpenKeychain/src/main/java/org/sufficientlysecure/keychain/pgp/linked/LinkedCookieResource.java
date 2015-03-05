package org.sufficientlysecure.keychain.pgp.linked;

import android.content.Context;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.LinkedVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.linked.resources.GenericHttpsResource;
import org.sufficientlysecure.keychain.pgp.linked.resources.UnknownResource;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static String generate (Context context, byte[] fingerprint, int nonce) {
        return String.format("\"[Verifying my PGP key: openpgp4fpr:%s#%08x]\"",
                KeyFormattingUtils.convertFingerprintToHex(fingerprint), nonce);
    }

    public static String generatePreview () {
        return "[Verifying my PGP key: openpgp4fpr:0x…]";
    }

    public LinkedVerifyResult verify(byte[] fingerprint, int nonce) {

        OperationLog log = new OperationLog();
        log.add(LogType.MSG_LV, 0);

        // Try to fetch resource. Logs for itself
        String res = fetchResource(log, 1);
        if (res == null) {
            // if this is null, an error was recorded in fetchResource above
            return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
        }

        Log.d(Constants.TAG, "Resource data: '" + res + "'");

        return verifyString(log, 1, res, nonce, fingerprint);

    }

    protected abstract String fetchResource (OperationLog log, int indent);

    protected Matcher matchResource (OperationLog log, int indent, String res) {
        return magicPattern.matcher(res);
    }

    protected LinkedVerifyResult verifyString (OperationLog log, int indent,
                                               String res,
                                               int nonce, byte[] fingerprint) {

        log.add(LogType.MSG_LV_MATCH, indent);
        Matcher match = matchResource(log, indent+1, res);
        if (!match.find()) {
            log.add(LogType.MSG_LV_MATCH_ERROR, 2);
            return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
        }

        String candidateFp = match.group(1).toLowerCase();
        try {
            int nonceCandidate = Integer.parseInt(match.group(2).toLowerCase(), 16);

            if (nonce != nonceCandidate) {
                log.add(LogType.MSG_LV_NONCE_ERROR, indent);
                return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
            }
        } catch (NumberFormatException e) {
            log.add(LogType.MSG_LV_NONCE_ERROR, indent);
            return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
        }

        String fp = KeyFormattingUtils.convertFingerprintToHex(fingerprint);

        if (!fp.equals(candidateFp)) {
            log.add(LogType.MSG_LV_FP_ERROR, indent);
            return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
        }
        log.add(LogType.MSG_LV_FP_OK, indent);

        log.add(LogType.MSG_LV_NONCE_OK, indent);
        return new LinkedVerifyResult(LinkedVerifyResult.RESULT_OK, log);

    }

}
