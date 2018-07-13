package org.sufficientlysecure.keychain.daos;


import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.sufficientlysecure.keychain.Constants;


public class DatabaseNotifyManager {
    private static final Uri URI_KEYS = Uri.parse("content://" + Constants.PROVIDER_AUTHORITY + "/keys");
    private static final Uri URI_APPS = Uri.parse("content://" + Constants.PROVIDER_AUTHORITY + "/apps");

    private ContentResolver contentResolver;

    public static DatabaseNotifyManager create(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        return new DatabaseNotifyManager(contentResolver);
    }

    private DatabaseNotifyManager(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public void notifyAllKeysChange() {
        Uri uri = getNotifyUriAllKeys();
        contentResolver.notifyChange(uri, null);
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

    public void notifyApiAppChange(String apiApp) {
        Uri uri = getNotifyUriPackageName(apiApp);
        contentResolver.notifyChange(uri, null);
    }

    public static Uri getNotifyUriAllKeys() {
        return URI_KEYS;
    }

    public static Uri getNotifyUriMasterKeyId(long masterKeyId) {
        return URI_KEYS.buildUpon().appendPath(Long.toString(masterKeyId)).build();
    }

    public static Uri getNotifyUriAllApps() {
        return URI_APPS;
    }

    public static Uri getNotifyUriPackageName(String packageName) {
        return URI_APPS.buildUpon().appendPath(packageName).build();
    }
}
