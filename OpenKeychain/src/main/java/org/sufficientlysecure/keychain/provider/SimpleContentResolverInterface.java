package org.sufficientlysecure.keychain.provider;


import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/** This interface contains the principal methods for database access
 * from {#android.content.ContentResolver}. It is used to allow substitution
 * of a ContentResolver in DAOs.
 *
 * @see ApiDataAccessObject
 */
public interface SimpleContentResolverInterface {
    Cursor query(Uri contentUri, String[] projection, String selection, String[] selectionArgs, String sortOrder);

    Uri insert(Uri contentUri, ContentValues values);

    int update(Uri contentUri, ContentValues values, String where, String[] selectionArgs);

    int delete(Uri contentUri, String where, String[] selectionArgs);
}
