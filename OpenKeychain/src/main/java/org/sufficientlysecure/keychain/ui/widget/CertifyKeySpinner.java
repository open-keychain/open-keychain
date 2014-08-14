package org.sufficientlysecure.keychain.ui.widget;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.AttributeSet;
import org.sufficientlysecure.keychain.provider.KeychainContract;

public class CertifyKeySpinner extends KeySpinner {
    public CertifyKeySpinner(Context context) {
        super(context);
    }

    public CertifyKeySpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CertifyKeySpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public Loader<Cursor> onCreateLoader() {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingsUri();

        // These are the rows that we will retrieve.
        String[] projection = new String[]{
                KeychainContract.KeyRings._ID,
                KeychainContract.KeyRings.MASTER_KEY_ID,
                KeychainContract.KeyRings.KEY_ID,
                KeychainContract.KeyRings.USER_ID,
                KeychainContract.KeyRings.IS_EXPIRED,
                KeychainContract.KeyRings.HAS_CERTIFY,
                KeychainContract.KeyRings.HAS_ANY_SECRET
        };

        String where = KeychainContract.KeyRings.HAS_ANY_SECRET + " = 1 AND " + KeychainContract.KeyRings.HAS_CERTIFY + " NOT NULL AND "
                + KeychainContract.KeyRings.IS_REVOKED + " = 0 AND " + KeychainContract.KeyRings.IS_EXPIRED + " = 0";

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getContext(), baseUri, projection, where, null, null);
    }
}
