package org.thialfihar.android.apg.provider;

import java.io.IOException;
import java.util.Vector;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.helper.PGPConversionHelper;
import org.thialfihar.android.apg.helper.PGPHelper;
import org.thialfihar.android.apg.helper.PGPMain.ApgGeneralException;
import org.thialfihar.android.apg.provider.ApgContract.PublicKeyRings;
import org.thialfihar.android.apg.provider.ApgContract.PublicKeys;
import org.thialfihar.android.apg.provider.ApgContract.SecretKeyRings;
import org.thialfihar.android.apg.provider.ApgContract.SecretKeys;
import org.thialfihar.android.apg.util.Log;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class ProviderHelper {

    /**
     * Retrieves the actual PGPPublicKeyRing object from the database blob associated with the rowId
     * 
     * @param context
     * @param rowId
     * @return
     */
    public static PGPPublicKeyRing getPGPPublicKeyRing(Context context, long rowId) {
        Uri queryUri = PublicKeyRings.buildPublicKeyRingsUri(Long.toString(rowId));
        Cursor cursor = context.getContentResolver()
                .query(queryUri, new String[] { PublicKeyRings._ID, PublicKeyRings.KEY_RING_DATA },
                        null, null, null);

        PGPPublicKeyRing keyRing = null;
        if (cursor != null && cursor.moveToFirst()) {
            int keyRingDataCol = cursor.getColumnIndex(PublicKeyRings.KEY_RING_DATA);

            byte[] data = cursor.getBlob(keyRingDataCol);
            if (data != null) {
                keyRing = PGPConversionHelper.BytesToPGPPublicKeyRing(data);
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return keyRing;
    }

    /**
     * Retrieves the actual PGPSecretKeyRing object from the database blob associated with the rowId
     * 
     * @param context
     * @param rowId
     * @return
     */
    public static PGPSecretKeyRing getPGPSecretKeyRing(Context context, long rowId) {
        Uri queryUri = SecretKeyRings.buildSecretKeyRingsUri(Long.toString(rowId));
        Cursor cursor = context.getContentResolver()
                .query(queryUri, new String[] { SecretKeyRings._ID, SecretKeyRings.KEY_RING_DATA },
                        null, null, null);

        PGPSecretKeyRing keyRing = null;
        if (cursor != null && cursor.moveToFirst()) {
            int keyRingDataCol = cursor.getColumnIndex(SecretKeyRings.KEY_RING_DATA);

            byte[] data = cursor.getBlob(keyRingDataCol);
            if (data != null) {
                keyRing = PGPConversionHelper.BytesToPGPSecretKeyRing(data);
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return keyRing;
    }

    public static PGPSecretKey getPGPSecretKey(Context context, long keyId) {
        PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRing(context, keyId);
        if (keyRing == null) {
            return null;
        }
        return keyRing.getSecretKey(keyId);
    }

    public static PGPPublicKey getPGPPublicKey(Context context, long keyId) {
        PGPPublicKeyRing keyRing = ProviderHelper.getPGPPublicKeyRing(context, keyId);
        if (keyRing == null) {
            return null;
        }

        return keyRing.getPublicKey(keyId);
    }

    // public static String getMainUserId(long keyRowId, int type) {
    // Uri queryUri = SecretKeyRings.buildSecretKeyRingsUri(Long.toString(rowId));
    // Cursor cursor = context.getContentResolver()
    // .query(queryUri, new String[] { SecretKeyRings._ID, SecretKeyRings.KEY_RING_DATA },
    // null, null, null);

    // SQLiteDatabase db = mDatabase.db();
    // Cursor c = db.query(Keys.TABLE_NAME + " INNER JOIN " + KeyRings.TABLE_NAME + " ON ("
    // + KeyRings.TABLE_NAME + "." + KeyRings._ID + " = " + Keys.TABLE_NAME + "."
    // + Keys.KEY_RING_ID + ") " + " INNER JOIN " + Keys.TABLE_NAME + " AS masterKey ON ("
    // + KeyRings.TABLE_NAME + "." + KeyRings._ID + " = " + "masterKey."
    // + Keys.KEY_RING_ID + " AND " + "masterKey." + Keys.IS_MASTER_KEY + " = '1') "
    // + " INNER JOIN " + UserIds.TABLE_NAME + " ON (" + UserIds.TABLE_NAME + "."
    // + UserIds.KEY_ID + " = " + "masterKey." + Keys._ID + " AND " + UserIds.TABLE_NAME
    // + "." + UserIds.RANK + " = '0')", new String[] { UserIds.USER_ID }, Keys.TABLE_NAME
    // + "." + Keys.KEY_ID + " = ? AND " + KeyRings.TABLE_NAME + "." + KeyRings.TYPE
    // + " = ?", new String[] { "" + keyRowId, "" + type, }, null, null, null);
    // String userId = "";
    // if (c != null && c.moveToFirst()) {
    // do {
    // userId = c.getString(0);
    // } while (c.moveToNext());
    // }
    //
    // if (c != null) {
    // c.close();
    // }
    //
    // return userId;
    // }

    /**
     * Retrieves ids of all SecretKeyRings
     * 
     * @param context
     * @return
     */
    public static Vector<Integer> getSecretKeyRingsRowIds(Context context) {
        Uri queryUri = SecretKeyRings.buildSecretKeyRingsUri();
        Cursor cursor = context.getContentResolver().query(queryUri,
                new String[] { SecretKeyRings._ID }, null, null, null);

        Vector<Integer> keyIds = new Vector<Integer>();
        if (cursor != null) {
            int idCol = cursor.getColumnIndex(SecretKeyRings._ID);
            if (cursor.moveToFirst()) {
                do {
                    keyIds.add(cursor.getInt(idCol));
                } while (cursor.moveToNext());
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return keyIds;
    }

    /**
     * Retrieves ids of all PublicKeyRings
     * 
     * @param context
     * @return
     */
    public static Vector<Integer> getPublicKeyRingsRowIds(Context context) {
        Uri queryUri = PublicKeyRings.buildPublicKeyRingsUri();
        Cursor cursor = context.getContentResolver().query(queryUri,
                new String[] { PublicKeyRings._ID }, null, null, null);

        Vector<Integer> keyIds = new Vector<Integer>();
        if (cursor != null) {
            int idCol = cursor.getColumnIndex(PublicKeyRings._ID);
            if (cursor.moveToFirst()) {
                do {
                    keyIds.add(cursor.getInt(idCol));
                } while (cursor.moveToNext());
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return keyIds;
    }

    public static void deletePublicKeyRing(Context context, long rowId) {
        ContentResolver cr = context.getContentResolver();
        cr.delete(PublicKeyRings.buildPublicKeyRingsUri(Long.toString(rowId)), null, null);
    }

    public static void deleteSecretKeyRing(Context context, long rowId) {
        ContentResolver cr = context.getContentResolver();
        cr.delete(SecretKeyRings.buildSecretKeyRingsUri(Long.toString(rowId)), null, null);
    }

}
