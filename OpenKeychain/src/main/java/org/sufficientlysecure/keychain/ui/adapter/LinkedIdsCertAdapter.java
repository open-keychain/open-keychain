package org.sufficientlysecure.keychain.ui.adapter;


import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;


public class LinkedIdsCertAdapter extends CursorAdapter {

    public static final String[] USER_CERTS_PROJECTION = new String[]{
            UserPackets._ID,
            UserPackets.TYPE,
            UserPackets.USER_ID,
            UserPackets.ATTRIBUTE_DATA,
            UserPackets.RANK,
            UserPackets.VERIFIED,
            UserPackets.IS_PRIMARY,
            UserPackets.IS_REVOKED
    };
    protected static final int INDEX_ID = 0;
    protected static final int INDEX_TYPE = 1;
    protected static final int INDEX_USER_ID = 2;
    protected static final int INDEX_ATTRIBUTE_DATA = 3;
    protected static final int INDEX_RANK = 4;
    protected static final int INDEX_VERIFIED = 5;
    protected static final int INDEX_IS_PRIMARY = 6;
    protected static final int INDEX_IS_REVOKED = 7;

    public LinkedIdsCertAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return null;
    }

    public static CursorLoader createLoader(Activity activity, Uri dataUri) {
        Uri baseUri = UserPackets.buildLinkedIdsUri(dataUri);
        return new CursorLoader(activity, baseUri,
                UserIdsAdapter.USER_PACKETS_PROJECTION, null, null, null);
    }

}
