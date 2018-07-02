package org.sufficientlysecure.keychain.provider;


import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;


public class DatabaseNotifyManager {
    private static final Uri BASE_URI = Uri.parse("content://" + Constants.PROVIDER_AUTHORITY);

    private ContentResolver contentResolver;

    public static DatabaseNotifyManager create(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        return new DatabaseNotifyManager(contentResolver);
    }

    private DatabaseNotifyManager(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public void notifyKeyChange(long masterKeyId) {
        Uri uri = getNotifyUriMasterKeyId(masterKeyId);
        contentResolver.notifyChange(uri, null);
    }

    public void notifyAutocryptDelete(String autocryptId, Long masterKeyId) {
        Uri uri = getNotifyUriMasterKeyId(masterKeyId);
        contentResolver.notifyChange(uri, null);
    }

    public void notifyAutocryptUpdate(String autocryptId, long masterKeyId) {
        Uri uri = getNotifyUriMasterKeyId(masterKeyId);
        contentResolver.notifyChange(uri, null);
    }

    public void notifyKeyMetadataChange(long masterKeyId) {
        Uri uri = getNotifyUriMasterKeyId(masterKeyId);
        contentResolver.notifyChange(uri, null);
    }

    public static Uri getNotifyUriAllKeys() {
        return BASE_URI;
    }

    public static Uri getNotifyUriMasterKeyId(long masterKeyId) {
        return BASE_URI.buildUpon().appendPath(Long.toString(masterKeyId)).build();
    }
}
