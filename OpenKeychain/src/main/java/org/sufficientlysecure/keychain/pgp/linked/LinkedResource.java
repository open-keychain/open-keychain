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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class LinkedResource {

    protected final URI mSubUri;
    protected final Set<String> mFlags;
    protected final HashMap<String,String> mParams;

    static Pattern magicPattern =
            Pattern.compile("\\[Verifying my PGP key: pgpid\\+cookie:([a-zA-Z0-9]+)#([a-zA-Z0-9]+)\\]");

    protected LinkedResource(Set<String> flags, HashMap<String, String> params, URI uri) {
        mFlags = flags;
        mParams = params;
        mSubUri = uri;
    }

    public Set<String> getFlags () {
        return new HashSet<String>(mFlags);
    }

    public HashMap<String,String> getParams () {
        return new HashMap<String,String>(mParams);
    }

    public URI getSubUri () {
        return mSubUri;
    }

    public static String generate (Context context, byte[] fingerprint, String nonce) {

        return "[Verifying my PGP key: pgpid+cookie:"
                + KeyFormattingUtils.convertFingerprintToHex(fingerprint) + "#" + nonce + "]";

    }

    public LinkedVerifyResult verify(byte[] fingerprint, String nonce) {

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
                                               String nonce, byte[] fingerprint) {

        log.add(LogType.MSG_LV_MATCH, indent);
        Matcher match = matchResource(log, indent+1, res);
        if (!match.find()) {
            log.add(LogType.MSG_LV_MATCH_ERROR, 2);
            return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
        }

        String candidateFp = match.group(1).toLowerCase();
        String nonceCandidate = match.group(2).toLowerCase();

        String fp = KeyFormattingUtils.convertFingerprintToHex(fingerprint);

        if (!fp.equals(candidateFp)) {
            log.add(LogType.MSG_LV_FP_ERROR, indent);
            return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
        }
        log.add(LogType.MSG_LV_FP_OK, indent);

        if (!nonce.equals(nonceCandidate)) {
            log.add(LogType.MSG_LV_NONCE_ERROR, indent);
            return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
        }

        log.add(LogType.MSG_LV_NONCE_OK, indent);
        return new LinkedVerifyResult(LinkedVerifyResult.RESULT_OK, log);

    }

    public static LinkedResource findResourceType
            (Set<String> flags, HashMap<String,String> params, URI uri) {

        LinkedResource res;

        res = GenericHttpsResource.create(flags, params, uri);
        if (res != null) {
            return res;
        }

        return new UnknownResource(flags, params, uri);

    }

}
