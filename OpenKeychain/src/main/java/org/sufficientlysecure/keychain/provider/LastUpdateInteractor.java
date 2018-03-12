package org.sufficientlysecure.keychain.provider;


import java.util.GregorianCalendar;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import org.sufficientlysecure.keychain.provider.KeychainContract.UpdatedKeys;


public class LastUpdateInteractor {
    private final ContentResolver contentResolver;


    public static LastUpdateInteractor create(Context context) {
        return new LastUpdateInteractor(context.getContentResolver());
    }

    private LastUpdateInteractor(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    @Nullable
    public Boolean getSeenOnKeyservers(long masterKeyId) {
        Cursor cursor = contentResolver.query(
                UpdatedKeys.CONTENT_URI,
                new String[] { UpdatedKeys.SEEN_ON_KEYSERVERS },
                UpdatedKeys.MASTER_KEY_ID + " = ?",
                new String[] { "" + masterKeyId },
                null
        );
        if (cursor == null) {
            return null;
        }

        Boolean seenOnKeyservers;
        try {
            if (!cursor.moveToNext()) {
                return null;
            }
            seenOnKeyservers = cursor.isNull(0) ? null : cursor.getInt(0) != 0;
        } finally {
            cursor.close();
        }
        return seenOnKeyservers;
    }

    public void resetAllLastUpdatedTimes() {
        ContentValues values = new ContentValues();
        values.putNull(UpdatedKeys.LAST_UPDATED);
        values.putNull(UpdatedKeys.SEEN_ON_KEYSERVERS);
        contentResolver.update(UpdatedKeys.CONTENT_URI, values, null, null);
    }

    public Uri renewKeyLastUpdatedTime(long masterKeyId, boolean seenOnKeyservers) {
        boolean isFirstKeyserverStatusCheck = getSeenOnKeyservers(masterKeyId) == null;

        ContentValues values = new ContentValues();
        values.put(UpdatedKeys.MASTER_KEY_ID, masterKeyId);
        values.put(UpdatedKeys.LAST_UPDATED, GregorianCalendar.getInstance().getTimeInMillis() / 1000);
        if (seenOnKeyservers || isFirstKeyserverStatusCheck) {
            values.put(UpdatedKeys.SEEN_ON_KEYSERVERS, seenOnKeyservers);
        }

        // this will actually update/replace, doing the right thing™ for seenOnKeyservers value
        // see `KeychainProvider.insert()`
        return contentResolver.insert(UpdatedKeys.CONTENT_URI, values);
    }
}
