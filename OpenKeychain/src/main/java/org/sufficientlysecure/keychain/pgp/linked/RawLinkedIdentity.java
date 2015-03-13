package org.sufficientlysecure.keychain.pgp.linked;

import org.spongycastle.util.Strings;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;

import java.net.URI;

import android.content.Context;
import android.support.annotation.DrawableRes;

/** The RawLinkedIdentity contains raw parsed data from a Linked Identity subpacket. */
public class RawLinkedIdentity {

    public final URI mUri;

    protected RawLinkedIdentity(URI uri) {
        mUri = uri;
    }

    public byte[] getEncoded() {
        return Strings.toUTF8ByteArray(mUri.toASCIIString());
    }

    public WrappedUserAttribute toUserAttribute () {
        return WrappedUserAttribute.fromSubpacket(WrappedUserAttribute.UAT_LINKED_ID, getEncoded());
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
