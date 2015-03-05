package org.sufficientlysecure.keychain.pgp.linked;

import org.spongycastle.util.Strings;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;

import java.net.URI;
import java.nio.ByteBuffer;

import android.content.Context;
import android.support.annotation.DrawableRes;

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

    public WrappedUserAttribute toUserAttribute () {
        return WrappedUserAttribute.fromSubpacket(WrappedUserAttribute.UAT_LINKED_ID, getEncoded());
    }

    public static int generateNonce() {
        // TODO make this actually random
        // byte[] data = new byte[4];
        // new SecureRandom().nextBytes(data);
        // return Hex.toHexString(data);

        // debug for now
        return 1234567;
    }

    public @DrawableRes int getDisplayIcon() {
        return R.drawable.ic_warning_grey_24dp;
    }

    public String getDisplayTitle(Context context) {
        return "unknown";
    }

    public String getDisplayComment(Context context) {
        return null;
    }

}
