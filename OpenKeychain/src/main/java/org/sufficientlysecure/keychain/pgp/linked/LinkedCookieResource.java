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

public abstract class LinkedCookieResource {

    protected final URI mSubUri;
    protected final Set<String> mFlags;
    protected final HashMap<String,String> mParams;

    static Pattern magicPattern =
            Pattern.compile("\\[Verifying my PGP key: openpgp4fpr:([a-zA-Z0-9]+)#([a-zA-Z0-9]+)\\]");

    protected LinkedCookieResource(Set<String> flags, HashMap<String, String> params, URI uri) {
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

    public static String generate (Context context, byte[] fingerprint, String nonce) {

        return "[Verifying my PGP key: openpgp4fpr:"
                + KeyFormattingUtils.convertFingerprintToHex(fingerprint) + "#" + nonce + "]";

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
        int nonceCandidate = Integer.parseInt(match.group(2).toLowerCase(), 16);

        String fp = KeyFormattingUtils.convertFingerprintToHex(fingerprint);

        if (!fp.equals(candidateFp)) {
            log.add(LogType.MSG_LV_FP_ERROR, indent);
            return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
        }
        log.add(LogType.MSG_LV_FP_OK, indent);

        if (nonce != nonceCandidate) {
            log.add(LogType.MSG_LV_NONCE_ERROR, indent);
            return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
        }

        log.add(LogType.MSG_LV_NONCE_OK, indent);
        return new LinkedVerifyResult(LinkedVerifyResult.RESULT_OK, log);

    }

    protected static LinkedCookieResource fromRawLinkedId (RawLinkedIdentity id) {
        return fromUri(id.mNonce, id.mUri);
    }

    protected static LinkedCookieResource fromUri (int nonce, URI uri) {

        if ("pgpid".equals(uri.getScheme())) {
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

        Set<String> flags = new HashSet<String>();
        HashMap<String,String> params = new HashMap<String,String>();
        {
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

        return findResourceType(nonce, flags, params, subUri);

    }

    protected static LinkedCookieResource findResourceType (int nonce, Set<String> flags,
                                                            HashMap<String,String> params,
                                                            URI  subUri) {

        LinkedCookieResource res;

        res = GenericHttpsResource.create(flags, params, subUri);
        if (res != null) {
            return res;
        }

        return new UnknownResource(flags, params, subUri);

    }

}
