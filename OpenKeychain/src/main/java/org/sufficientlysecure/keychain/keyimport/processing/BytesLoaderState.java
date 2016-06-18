package org.sufficientlysecure.keychain.keyimport.processing;

import android.net.Uri;

public class BytesLoaderState implements LoaderState {

    public byte[] mKeyBytes;
    public Uri mDataUri;

    public BytesLoaderState(byte[] keyBytes, Uri dataUri) {
        mKeyBytes = keyBytes;
        mDataUri = dataUri;
    }

}
