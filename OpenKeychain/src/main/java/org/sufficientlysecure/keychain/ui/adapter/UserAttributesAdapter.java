package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.View;

import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;

public abstract class UserAttributesAdapter extends CursorAdapter {
    public static final String[] USER_IDS_PROJECTION = new String[]{
            UserPackets._ID,
            UserPackets.TYPE,
            UserPackets.USER_ID,
            UserPackets.RANK,
            UserPackets.VERIFIED,
            UserPackets.IS_PRIMARY,
            UserPackets.IS_REVOKED
    };
    protected static final int INDEX_ID = 0;
    protected static final int INDEX_TYPE = 1;
    protected static final int INDEX_USER_ID = 2;
    protected static final int INDEX_RANK = 3;
    protected static final int INDEX_VERIFIED = 4;
    protected static final int INDEX_IS_PRIMARY = 5;
    protected static final int INDEX_IS_REVOKED = 6;

    public UserAttributesAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public abstract void bindView(View view, Context context, Cursor cursor);

    public String getUserId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getString(INDEX_USER_ID);
    }

    public boolean getIsRevoked(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getInt(INDEX_IS_REVOKED) > 0;
    }

    public int getIsVerified(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getInt(INDEX_VERIFIED);
    }
}
