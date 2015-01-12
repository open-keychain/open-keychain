package org.sufficientlysecure.keychain.pgp.affirmation;

import org.spongycastle.bcpg.UserAttributeSubpacket;
import org.spongycastle.util.Strings;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class Affirmation {

    protected byte[] mData;
    public final long mNonce;
    public final URI mSubUri;
    final Set<String> mFlags;
    final HashMap<String,String> mParams;

    protected Affirmation(byte[] data, long nonce, Set<String> flags,
                          HashMap<String,String> params, URI subUri) {
        mData = data;
        mNonce = nonce;
        mFlags = flags;
        mParams = params;
        mSubUri = subUri;
    }

    Affirmation(long nonce, Set<String> flags,
                          HashMap<String,String> params, URI subUri) {
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

        byte[] data = Strings.toUTF8ByteArray(b.toString());

        byte[] result = new byte[data.length+4];
        result[0] = (byte) (mNonce >> 24 & 255);
        result[1] = (byte) (mNonce >> 16 & 255);
        result[2] = (byte) (mNonce >> 8 & 255);
        result[3] = (byte) (mNonce & 255);

        System.arraycopy(data, 0, result, 4, result.length);

        return result;
    }

    /** This method parses an affirmation from a UserAttributeSubpacket, or returns null if the
     * subpacket can not be parsed as a valid affirmation.
     */
    public static Affirmation parseAffirmation(UserAttributeSubpacket subpacket) {
        if (subpacket.getType() != 100) {
            return null;
        }

        byte[] data = subpacket.getData();

        long nonce = (data[0] << 24) | (data[1] << 16) |  (data[2] << 8)  | data[3];

        try {
            return parseUri(nonce, Strings.fromUTF8ByteArray(Arrays.copyOfRange(data, 4, data.length)));

        } catch (IllegalArgumentException e) {
            Log.e(Constants.TAG, "error parsing uri in (suspected) affirmation packet");
            return null;
        }
    }

    public static Affirmation generateForUri(String uri) {
        return parseUri(generateNonce(), uri);
    }

    protected static Affirmation parseUri (long nonce, String uriString) {
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

        return new Affirmation(null, nonce, flags, params, subUri);

    }

    public static long generateNonce() {
        return 1234567890L; // new SecureRandom().nextLong();
    }

}
