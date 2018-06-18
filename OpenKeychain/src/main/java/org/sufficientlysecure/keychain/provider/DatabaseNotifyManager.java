package org.sufficientlysecure.keychain.provider;


import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;


public class DatabaseNotifyManager {
    private ContentResolver contentResolver;

    public static DatabaseNotifyManager create(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        return new DatabaseNotifyManager(contentResolver);
    }

    private DatabaseNotifyManager(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public void notifyKeyChange(long masterKeyId) {
        Uri uri = KeyRings.buildGenericKeyRingUri(masterKeyId);
        contentResolver.notifyChange(uri, null);
    }

    public void notifyAutocryptDelete(String autocryptId, Long masterKeyId) {
        Uri uri = KeyRings.buildGenericKeyRingUri(masterKeyId);
        contentResolver.notifyChange(uri, null);
    }

    public void notifyAutocryptUpdate(String autocryptId, long masterKeyId) {
        Uri uri = KeyRings.buildGenericKeyRingUri(masterKeyId);
        contentResolver.notifyChange(uri, null);
    }

    public void notifyKeyMetadataChange(long masterKeyId) {
        Uri uri = KeyRings.buildGenericKeyRingUri(masterKeyId);
        contentResolver.notifyChange(uri, null);
    }
}
