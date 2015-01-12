package org.sufficientlysecure.keychain.pgp.affirmation;

import org.spongycastle.bcpg.UserAttributeSubpacket;
import org.spongycastle.util.Strings;
import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class LinkedIdentity implements Serializable {

    protected byte[] mData;
    public final String mNonce;
    public final URI mSubUri;
    final Set<String> mFlags;
    final HashMap<String,String> mParams;

    protected LinkedIdentity(byte[] data, String nonce, Set<String> flags,
                             HashMap<String, String> params, URI subUri) {
        if ( ! nonce.matches("[0-9a-zA-Z]+")) {
            throw new AssertionError("bug: nonce must be hexstring!");
        }

        mData = data;
        mNonce = nonce;
        mFlags = flags;
        mParams = params;
        mSubUri = subUri;
    }

    LinkedIdentity(String nonce, Set<String> flags,
                   HashMap<String, String> params, URI subUri) {
        this(null, nonce, flags, params, subUri);
    }

    public byte[] encode() {
        if (mData != null) {
            return mData;
        }

        StringBuilder b = new StringBuilder();
        b.append("pgpid:");

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
            Iterator<Entry<String, String>> it = mParams.entrySet().iterator();
            while (it.hasNext()) {
                if (!first) {
                    b.append(";");
                }
                first = false;
                Entry<String, String> entry = it.next();
                b.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }

        b.append("@");
        b.append(mSubUri);

        byte[] nonceBytes = Hex.decode(mNonce);
        byte[] data = Strings.toUTF8ByteArray(b.toString());

        byte[] result = new byte[data.length+12];
        System.arraycopy(nonceBytes, 0, result, 0, 12);
        System.arraycopy(data, 0, result, 12, result.length);

        return result;
    }

    /** This method parses an affirmation from a UserAttributeSubpacket, or returns null if the
     * subpacket can not be parsed as a valid affirmation.
     */
    public static LinkedIdentity parseAffirmation(UserAttributeSubpacket subpacket) {
        if (subpacket.getType() != 100) {
            return null;
        }

        byte[] data = subpacket.getData();
        String nonce = Hex.toHexString(data, 0, 12);

        try {
            return parseUri(nonce, Strings.fromUTF8ByteArray(Arrays.copyOfRange(data, 12, data.length)));

        } catch (IllegalArgumentException e) {
            Log.e(Constants.TAG, "error parsing uri in (suspected) affirmation packet");
            return null;
        }
    }

    protected static LinkedIdentity parseUri (String nonce, String uriString) {
        URI uri = URI.create(uriString);

        if ("pgpid".equals(uri.getScheme())) {
            Log.e(Constants.TAG, "unknown uri scheme in (suspected) affirmation packet");
            return null;
        }

        if (!uri.isOpaque()) {
            Log.e(Constants.TAG, "non-opaque uri in (suspected) affirmation packet");
            return null;
        }

        String specific = uri.getSchemeSpecificPart();
        if (!specific.contains("@")) {
            Log.e(Constants.TAG, "unknown uri scheme in affirmation packet");
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

        return new LinkedIdentity(nonce, flags, params, subUri);

    }

    public static String generateNonce() {
        // TODO make this actually random
        // byte[] data = new byte[96];
        // new SecureRandom().nextBytes(data);
        // return Hex.toHexString(data);

        // debug for now
        return "0123456789ABCDEF01234567";
    }

}
