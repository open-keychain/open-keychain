package org.sufficientlysecure.keychain.pgp.linked;

import org.spongycastle.bcpg.UserAttributeSubpacket;
import org.spongycastle.util.Strings;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.util.Log;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/** The RawLinkedIdentity contains raw parsed data from a Linked Identity subpacket. */
public class RawLinkedIdentity {

    public final int mNonce;
    public final URI mUri;

    protected RawLinkedIdentity(int nonce, URI uri) {
        mNonce = nonce;
        mUri = uri;
    }

    public byte[] getEncoded() {
        byte[] uriData = Strings.toUTF8ByteArray(mUri.toASCIIString());

        ByteBuffer buf = ByteBuffer.allocate(4 + uriData.length);

        buf.putInt(mNonce);
        buf.put(uriData);

        return buf.array();
    }

    /** This method parses a linked id from a UserAttributeSubpacket, or returns null if the
     * subpacket can not be parsed as a valid linked id.
     */
    static RawLinkedIdentity fromAttributeSubpacket(UserAttributeSubpacket subpacket) {
        if (subpacket.getType() != 100) {
            return null;
        }

        byte[] data = subpacket.getData();

        return fromSubpacketData(data);

    }

    public static RawLinkedIdentity fromSubpacketData(byte[] data) {

        try {
            int nonce = ByteBuffer.wrap(data).getInt();
            String uri = Strings.fromUTF8ByteArray(Arrays.copyOfRange(data, 4, data.length));

            return new RawLinkedIdentity(nonce, URI.create(uri));

        } catch (IllegalArgumentException e) {
            Log.e(Constants.TAG, "error parsing uri in (suspected) linked id packet");
            return null;
        }
    }

    public static RawLinkedIdentity fromResource (LinkedCookieResource res, int nonce) {
        return new RawLinkedIdentity(nonce, res.toUri());
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
