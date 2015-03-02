package org.sufficientlysecure.keychain.pgp.linked;

import org.spongycastle.bcpg.UserAttributeSubpacket;
import org.spongycastle.util.Strings;
import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.util.Log;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class LinkedIdentity {

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

    public byte[] getEncoded() {
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
        if (nonceBytes.length != 4) {
            throw new AssertionError("nonce must be 4 bytes");
        }
        byte[] data = Strings.toUTF8ByteArray(b.toString());

        byte[] result = new byte[data.length+4];
        System.arraycopy(nonceBytes, 0, result, 0, 4);
        System.arraycopy(data, 0, result, 4, data.length);

        return result;
    }

    /** This method parses a linked id from a UserAttributeSubpacket, or returns null if the
     * subpacket can not be parsed as a valid linked id.
     */
    static LinkedIdentity parseAttributeSubpacket(UserAttributeSubpacket subpacket) {
        if (subpacket.getType() != 100) {
            return null;
        }

        byte[] data = subpacket.getData();
        String nonce = Hex.toHexString(data, 0, 4);

        try {
            return parseUri(nonce, Strings.fromUTF8ByteArray(Arrays.copyOfRange(data, 4, data.length)));

        } catch (IllegalArgumentException e) {
            Log.e(Constants.TAG, "error parsing uri in (suspected) linked id packet");
            return null;
        }
    }

    protected static LinkedIdentity parseUri (String nonce, String uriString) {
        URI uri = URI.create(uriString);

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

        return new LinkedIdentity(nonce, flags, params, subUri);

    }

    public static LinkedIdentity fromResource (LinkedResource res, String nonce) {
        return new LinkedIdentity(nonce, res.getFlags(), res.getParams(), res.getSubUri());
    }

    public WrappedUserAttribute toUserAttribute () {
        return WrappedUserAttribute.fromSubpacket(WrappedUserAttribute.UAT_LINKED_ID, getEncoded());
    }

    public static String generateNonce() {
        // TODO make this actually random
        // byte[] data = new byte[4];
        // new SecureRandom().nextBytes(data);
        // return Hex.toHexString(data);

        // debug for now
        return "01234567";
    }

}
