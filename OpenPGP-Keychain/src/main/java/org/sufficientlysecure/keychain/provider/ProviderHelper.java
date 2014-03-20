/*
 * Copyright (C) 2012-2013 Dominik SchÃ¼rmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.provider;

import android.content.*;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.RemoteException;
import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.*;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpConversionHelper;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.service.remote.AppSettings;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class ProviderHelper {

    /**
     * Private helper method to get PGPKeyRing from database
     */
    public static PGPKeyRing getPGPKeyRing(Context context, Uri queryUri) {
        Cursor cursor = context.getContentResolver().query(queryUri,
                new String[]{KeyRings._ID, KeyRings.KEY_RING_DATA}, null, null, null);

        PGPKeyRing keyRing = null;
        if (cursor != null && cursor.moveToFirst()) {
            int keyRingDataCol = cursor.getColumnIndex(KeyRings.KEY_RING_DATA);

            byte[] data = cursor.getBlob(keyRingDataCol);
            if (data != null) {
                keyRing = PgpConversionHelper.BytesToPGPKeyRing(data);
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return keyRing;
    }

    /**
     * Retrieves the actual PGPPublicKeyRing object from the database blob based on the rowId
     */
    public static PGPPublicKeyRing getPGPPublicKeyRingByRowId(Context context, long rowId) {
        Uri queryUri = KeyRings.buildPublicKeyRingsUri(Long.toString(rowId));
        return (PGPPublicKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Retrieves the actual PGPPublicKeyRing object from the database blob based on the masterKeyId
     */
    public static PGPPublicKeyRing getPGPPublicKeyRingByMasterKeyId(Context context,
                                                                    long masterKeyId) {
        Uri queryUri = KeyRings.buildPublicKeyRingsByMasterKeyIdUri(Long.toString(masterKeyId));
        return (PGPPublicKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Retrieves the actual PGPPublicKeyRing object from the database blob associated with a key
     * with this keyId
     */
    public static PGPPublicKeyRing getPGPPublicKeyRingByKeyId(Context context, long keyId) {
        Uri queryUri = KeyRings.buildPublicKeyRingsByKeyIdUri(Long.toString(keyId));
        return (PGPPublicKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Retrieves the actual PGPPublicKey object from the database blob associated with a key with
     * this keyId
     */
    public static PGPPublicKey getPGPPublicKeyByKeyId(Context context, long keyId) {
        PGPPublicKeyRing keyRing = getPGPPublicKeyRingByKeyId(context, keyId);

        return (keyRing == null) ? null : keyRing.getPublicKey(keyId);
    }

    /**
     * Retrieves the actual PGPSecretKeyRing object from the database blob based on the rowId
     */
    public static PGPSecretKeyRing getPGPSecretKeyRingByRowId(Context context, long rowId) {
        Uri queryUri = KeyRings.buildSecretKeyRingsUri(Long.toString(rowId));
        return (PGPSecretKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Retrieves the actual PGPSecretKeyRing object from the database blob based on the maserKeyId
     */
    public static PGPSecretKeyRing getPGPSecretKeyRingByMasterKeyId(Context context,
                                                                    long masterKeyId) {
        Uri queryUri = KeyRings.buildSecretKeyRingsByMasterKeyIdUri(Long.toString(masterKeyId));
        return (PGPSecretKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Retrieves the actual PGPSecretKeyRing object from the database blob associated with a key
     * with this keyId
     */
    public static PGPSecretKeyRing getPGPSecretKeyRingByKeyId(Context context, long keyId) {
        Uri queryUri = KeyRings.buildSecretKeyRingsByKeyIdUri(Long.toString(keyId));
        return (PGPSecretKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Retrieves the actual PGPSecretKey object from the database blob associated with a key with
     * this keyId
     */
    public static PGPSecretKey getPGPSecretKeyByKeyId(Context context, long keyId) {
        PGPSecretKeyRing keyRing = getPGPSecretKeyRingByKeyId(context, keyId);

        return (keyRing == null) ? null : keyRing.getSecretKey(keyId);
    }

    /**
     * Saves PGPPublicKeyRing with its keys and userIds in DB
     */
    @SuppressWarnings("unchecked")
    public static void saveKeyRing(Context context, PGPPublicKeyRing keyRing) throws IOException {
        PGPPublicKey masterKey = keyRing.getPublicKey();
        long masterKeyId = masterKey.getKeyID();

        Uri deleteUri = KeyRings.buildPublicKeyRingsByMasterKeyIdUri(Long.toString(masterKeyId));

        // get current _ID of key
        long currentRowId = -1;
        Cursor oldQuery = context.getContentResolver()
                .query(deleteUri, new String[]{KeyRings._ID}, null, null, null);
        if (oldQuery != null && oldQuery.moveToFirst()) {
            currentRowId = oldQuery.getLong(0);
        } else {
            Log.e(Constants.TAG, "Key could not be found! Something wrong is happening!");
        }

        // delete old version of this keyRing, which also deletes all keys and userIds on cascade
        try {
            context.getContentResolver().delete(deleteUri, null, null);
        } catch (UnsupportedOperationException e) {
            Log.e(Constants.TAG, "Key could not be deleted! Maybe we are creating a new one!", e);
        }

        ContentValues values = new ContentValues();
        // use exactly the same _ID again to replace key in-place.
        // NOTE: If we would not use the same _ID again,
        // getting back to the ViewKeyActivity would result in Nullpointer,
        // because the currently loaded key would be gone from the database
        if (currentRowId != -1) {
            values.put(KeyRings._ID, currentRowId);
        }
        values.put(KeyRings.MASTER_KEY_ID, masterKeyId);
        values.put(KeyRings.KEY_RING_DATA, keyRing.getEncoded());

        // insert new version of this keyRing
        Uri uri = KeyRings.buildPublicKeyRingsUri();
        Uri insertedUri = context.getContentResolver().insert(uri, values);
        long keyRingRowId = Long.valueOf(insertedUri.getLastPathSegment());

        // save all keys and userIds included in keyRing object in database
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

        int rank = 0;
        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
            operations.add(buildPublicKeyOperations(context, keyRingRowId, key, rank));
            ++rank;
        }

        int userIdRank = 0;
        for (String userId : new IterableIterator<String>(masterKey.getUserIDs())) {
            operations.add(buildPublicUserIdOperations(context, keyRingRowId, userId, userIdRank));
            ++userIdRank;
        }

        for (PGPSignature certification :
                new IterableIterator<PGPSignature>(
                        masterKey.getSignaturesOfType(PGPSignature.POSITIVE_CERTIFICATION))) {
            // TODO: how to do this??
            // we need to verify the signatures again and again when they are displayed...
//            if (certification.verify
//            operations.add(buildPublicKeyOperations(context, keyRingRowId, key, rank));
        }


        try {
            context.getContentResolver().applyBatch(KeychainContract.CONTENT_AUTHORITY, operations);
        } catch (RemoteException e) {
            Log.e(Constants.TAG, "applyBatch failed!", e);
        } catch (OperationApplicationException e) {
            Log.e(Constants.TAG, "applyBatch failed!", e);
        }
    }

    /**
     * Saves PGPSecretKeyRing with its keys and userIds in DB
     */
    @SuppressWarnings("unchecked")
    public static void saveKeyRing(Context context, PGPSecretKeyRing keyRing) throws IOException {
        PGPSecretKey masterKey = keyRing.getSecretKey();
        long masterKeyId = masterKey.getKeyID();

        Uri deleteUri = KeyRings.buildSecretKeyRingsByMasterKeyIdUri(Long.toString(masterKeyId));

        // get current _ID of key
        long currentRowId = -1;
        Cursor oldQuery = context.getContentResolver()
                .query(deleteUri, new String[]{KeyRings._ID}, null, null, null);
        if (oldQuery != null && oldQuery.moveToFirst()) {
            currentRowId = oldQuery.getLong(0);
        } else {
            Log.e(Constants.TAG, "Key could not be found! Something wrong is happening!");
        }

        // delete old version of this keyRing, which also deletes all keys and userIds on cascade
        try {
            context.getContentResolver().delete(deleteUri, null, null);
        } catch (UnsupportedOperationException e) {
            Log.e(Constants.TAG, "Key could not be deleted! Maybe we are creating a new one!", e);
        }

        ContentValues values = new ContentValues();
        // use exactly the same _ID again to replace key in-place.
        // NOTE: If we would not use the same _ID again,
        // getting back to the ViewKeyActivity would result in Nullpointer,
        // because the currently loaded key would be gone from the database
        if (currentRowId != -1) {
            values.put(KeyRings._ID, currentRowId);
        }
        values.put(KeyRings.MASTER_KEY_ID, masterKeyId);
        values.put(KeyRings.KEY_RING_DATA, keyRing.getEncoded());

        // insert new version of this keyRing
        Uri uri = KeyRings.buildSecretKeyRingsUri();
        Uri insertedUri = context.getContentResolver().insert(uri, values);
        long keyRingRowId = Long.valueOf(insertedUri.getLastPathSegment());

        // save all keys and userIds included in keyRing object in database
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

        int rank = 0;
        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
            operations.add(buildSecretKeyOperations(context, keyRingRowId, key, rank));
            ++rank;
        }

        int userIdRank = 0;
        for (String userId : new IterableIterator<String>(masterKey.getUserIDs())) {
            operations.add(buildSecretUserIdOperations(context, keyRingRowId, userId, userIdRank));
            ++userIdRank;
        }

        try {
            context.getContentResolver().applyBatch(KeychainContract.CONTENT_AUTHORITY, operations);
        } catch (RemoteException e) {
            Log.e(Constants.TAG, "applyBatch failed!", e);
        } catch (OperationApplicationException e) {
            Log.e(Constants.TAG, "applyBatch failed!", e);
        }
    }

    /**
     * Build ContentProviderOperation to add PGPPublicKey to database corresponding to a keyRing
     */
    private static ContentProviderOperation buildPublicKeyOperations(Context context,
                                                                     long keyRingRowId, PGPPublicKey key, int rank) throws IOException {
        ContentValues values = new ContentValues();
        values.put(Keys.KEY_ID, key.getKeyID());
        values.put(Keys.IS_MASTER_KEY, key.isMasterKey());
        values.put(Keys.ALGORITHM, key.getAlgorithm());
        values.put(Keys.KEY_SIZE, key.getBitStrength());
        values.put(Keys.CAN_SIGN, PgpKeyHelper.isSigningKey(key));
        values.put(Keys.CAN_ENCRYPT, PgpKeyHelper.isEncryptionKey(key));
        values.put(Keys.IS_REVOKED, key.isRevoked());
        values.put(Keys.CREATION, PgpKeyHelper.getCreationDate(key).getTime() / 1000);
        Date expiryDate = PgpKeyHelper.getExpiryDate(key);
        if (expiryDate != null) {
            values.put(Keys.EXPIRY, expiryDate.getTime() / 1000);
        }
        values.put(Keys.KEY_RING_ROW_ID, keyRingRowId);
        values.put(Keys.KEY_DATA, key.getEncoded());
        values.put(Keys.RANK, rank);
        values.put(Keys.FINGERPRINT, key.getFingerprint());

        Uri uri = Keys.buildPublicKeysUri(Long.toString(keyRingRowId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * Build ContentProviderOperation to add PublicUserIds to database corresponding to a keyRing
     */
    private static ContentProviderOperation buildPublicUserIdOperations(Context context,
                                                                        long keyRingRowId, String userId, int rank) {
        ContentValues values = new ContentValues();
        values.put(UserIds.KEY_RING_ROW_ID, keyRingRowId);
        values.put(UserIds.USER_ID, userId);
        values.put(UserIds.RANK, rank);

        Uri uri = UserIds.buildPublicUserIdsUri(Long.toString(keyRingRowId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * Build ContentProviderOperation to add PGPSecretKey to database corresponding to a keyRing
     */
    private static ContentProviderOperation buildSecretKeyOperations(Context context,
                                                                     long keyRingRowId, PGPSecretKey key, int rank) throws IOException {
        ContentValues values = new ContentValues();

        boolean hasPrivate = true;
        if (key.isMasterKey()) {
            if (key.isPrivateKeyEmpty()) {
                hasPrivate = false;
            }
        }

        values.put(Keys.KEY_ID, key.getKeyID());
        values.put(Keys.IS_MASTER_KEY, key.isMasterKey());
        values.put(Keys.ALGORITHM, key.getPublicKey().getAlgorithm());
        values.put(Keys.KEY_SIZE, key.getPublicKey().getBitStrength());
        values.put(Keys.CAN_CERTIFY, (PgpKeyHelper.isCertificationKey(key) && hasPrivate));
        values.put(Keys.CAN_SIGN, (PgpKeyHelper.isSigningKey(key) && hasPrivate));
        values.put(Keys.CAN_ENCRYPT, PgpKeyHelper.isEncryptionKey(key));
        values.put(Keys.IS_REVOKED, key.getPublicKey().isRevoked());
        values.put(Keys.CREATION, PgpKeyHelper.getCreationDate(key).getTime() / 1000);
        Date expiryDate = PgpKeyHelper.getExpiryDate(key);
        if (expiryDate != null) {
            values.put(Keys.EXPIRY, expiryDate.getTime() / 1000);
        }
        values.put(Keys.KEY_RING_ROW_ID, keyRingRowId);
        values.put(Keys.KEY_DATA, key.getEncoded());
        values.put(Keys.RANK, rank);
        values.put(Keys.FINGERPRINT, key.getPublicKey().getFingerprint());

        Uri uri = Keys.buildSecretKeysUri(Long.toString(keyRingRowId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * Build ContentProviderOperation to add SecretUserIds to database corresponding to a keyRing
     */
    private static ContentProviderOperation buildSecretUserIdOperations(Context context,
                                                                        long keyRingRowId, String userId, int rank) {
        ContentValues values = new ContentValues();
        values.put(UserIds.KEY_RING_ROW_ID, keyRingRowId);
        values.put(UserIds.USER_ID, userId);
        values.put(UserIds.RANK, rank);

        Uri uri = UserIds.buildSecretUserIdsUri(Long.toString(keyRingRowId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * Private helper method
     */
    private static ArrayList<Long> getKeyRingsMasterKeyIds(Context context, Uri queryUri) {
        Cursor cursor = context.getContentResolver().query(queryUri,
                new String[]{KeyRings.MASTER_KEY_ID}, null, null, null);

        ArrayList<Long> masterKeyIds = new ArrayList<Long>();
        if (cursor != null) {
            int masterKeyIdCol = cursor.getColumnIndex(KeyRings.MASTER_KEY_ID);
            if (cursor.moveToFirst()) {
                do {
                    masterKeyIds.add(cursor.getLong(masterKeyIdCol));
                } while (cursor.moveToNext());
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return masterKeyIds;
    }

    /**
     * Private helper method
     */
    private static ArrayList<Long> getKeyRingsRowIds(Context context, Uri queryUri) {
        Cursor cursor = context.getContentResolver().query(queryUri,
                new String[]{KeyRings._ID}, null, null, null);

        ArrayList<Long> rowIds = new ArrayList<Long>();
        if (cursor != null) {
            int idCol = cursor.getColumnIndex(KeyRings._ID);
            if (cursor.moveToFirst()) {
                do {
                    rowIds.add(cursor.getLong(idCol));
                } while (cursor.moveToNext());
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return rowIds;
    }

    /**
     * Retrieves ids of all SecretKeyRings
     */
    public static ArrayList<Long> getSecretKeyRingsMasterKeyIds(Context context) {
        Uri queryUri = KeyRings.buildSecretKeyRingsUri();
        return getKeyRingsMasterKeyIds(context, queryUri);
    }

    /**
     * Retrieves ids of all PublicKeyRings
     */
    public static ArrayList<Long> getPublicKeyRingsMasterKeyIds(Context context) {
        Uri queryUri = KeyRings.buildPublicKeyRingsUri();
        return getKeyRingsMasterKeyIds(context, queryUri);
    }

    /**
     * Retrieves ids of all SecretKeyRings
     */
    public static ArrayList<Long> getSecretKeyRingsRowIds(Context context) {
        Uri queryUri = KeyRings.buildSecretKeyRingsUri();
        return getKeyRingsRowIds(context, queryUri);
    }

    /**
     * Retrieves ids of all PublicKeyRings
     */
    public static ArrayList<Long> getPublicKeyRingsRowIds(Context context) {
        Uri queryUri = KeyRings.buildPublicKeyRingsUri();
        return getKeyRingsRowIds(context, queryUri);
    }

    public static void deletePublicKeyRing(Context context, long rowId) {
        ContentResolver cr = context.getContentResolver();
        cr.delete(KeyRings.buildPublicKeyRingsUri(Long.toString(rowId)), null, null);
    }

    public static void deleteSecretKeyRing(Context context, long rowId) {
        ContentResolver cr = context.getContentResolver();
        cr.delete(KeyRings.buildSecretKeyRingsUri(Long.toString(rowId)), null, null);
    }

    public static void deleteUnifiedKeyRing(Context context,String masterKeyId,boolean isSecretKey){
        ContentResolver cr= context.getContentResolver();
        cr.delete(KeyRings.buildPublicKeyRingsByMasterKeyIdUri(masterKeyId),null,null);
        if(isSecretKey){
            cr.delete(KeyRings.buildSecretKeyRingsByMasterKeyIdUri(masterKeyId),null,null);
        }

    }

    /**
     * Get master key id of keyring by its row id
     */
    public static long getPublicMasterKeyId(Context context, long keyRingRowId) {
        Uri queryUri = KeyRings.buildPublicKeyRingsUri(String.valueOf(keyRingRowId));
        return getMasterKeyId(context, queryUri);
    }

    /**
     * Get empty status of master key of keyring by its row id
     */
    public static boolean getSecretMasterKeyCanSign(Context context, long keyRingRowId) {
        Uri queryUri = KeyRings.buildSecretKeyRingsUri(String.valueOf(keyRingRowId));
        return getMasterKeyCanSign(context, queryUri);
    }

    /**
     * Private helper method to get master key private empty status of keyring by its row id
     */
    public static boolean getMasterKeyCanSign(Context context, Uri queryUri) {
        String[] projection = new String[]{
                KeyRings.MASTER_KEY_ID,
                "(SELECT COUNT(sign_keys." + Keys._ID + ") FROM " + Tables.KEYS
                        + " AS sign_keys WHERE sign_keys." + Keys.KEY_RING_ROW_ID + " = "
                        + KeychainDatabase.Tables.KEY_RINGS + "." + KeyRings._ID
                        + " AND sign_keys." + Keys.CAN_SIGN + " = '1' AND " + Keys.IS_MASTER_KEY
                        + " = 1) AS sign", };

        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(queryUri, projection, null, null, null);

        long masterKeyId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            int masterKeyIdCol = cursor.getColumnIndex("sign");

            masterKeyId = cursor.getLong(masterKeyIdCol);
        }

        if (cursor != null) {
            cursor.close();
        }

        return (masterKeyId > 0);
    }

    public static boolean hasSecretKeyByMasterKeyId(Context context, long masterKeyId) {
        Uri queryUri = KeyRings.buildSecretKeyRingsByMasterKeyIdUri(Long.toString(masterKeyId));
        // see if we can get our master key id back from the uri
        return getMasterKeyId(context, queryUri) == masterKeyId;
    }

    /**
     * Get master key id of keyring by its row id
     */
    public static long getSecretMasterKeyId(Context context, long keyRingRowId) {
        Uri queryUri = KeyRings.buildSecretKeyRingsUri(String.valueOf(keyRingRowId));
        return getMasterKeyId(context, queryUri);
    }

    /**
     * Get master key id of key
     */
    public static long getMasterKeyId(Context context, Uri queryUri) {
        String[] projection = new String[]{KeyRings.MASTER_KEY_ID};
        Cursor cursor = context.getContentResolver().query(queryUri, projection, null, null, null);

        long masterKeyId = 0;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int masterKeyIdCol = cursor.getColumnIndexOrThrow(KeyRings.MASTER_KEY_ID);

                masterKeyId = cursor.getLong(masterKeyIdCol);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return masterKeyId;
    }

    public static long getRowId(Context context, Uri queryUri) {
        String[] projection = new String[]{KeyRings._ID};
        Cursor cursor = context.getContentResolver().query(queryUri, projection, null, null, null);

        long rowId = 0;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow(KeyRings._ID);

                rowId = cursor.getLong(idCol);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return rowId;
    }

    /**
     * Get fingerprint of key
     */
    public static byte[] getFingerprint(Context context, Uri queryUri) {
        String[] projection = new String[]{Keys.FINGERPRINT};
        Cursor cursor = context.getContentResolver().query(queryUri, projection, null, null, null);

        byte[] fingerprint = null;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndexOrThrow(Keys.FINGERPRINT);

                fingerprint = cursor.getBlob(col);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // FALLBACK: If fingerprint is not in database, get it from key blob!
        // this could happen if the key was saved by a previous version of Keychain!
        if (fingerprint == null) {
            Log.d(Constants.TAG, "FALLBACK: fingerprint is not in database, get it from key blob!");

            // get master key id
            projection = new String[]{KeyRings.MASTER_KEY_ID};
            cursor = context.getContentResolver().query(queryUri, projection, null, null, null);
            long masterKeyId = 0;
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int col = cursor.getColumnIndexOrThrow(KeyRings.MASTER_KEY_ID);

                    masterKeyId = cursor.getLong(col);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            PGPPublicKey key = ProviderHelper.getPGPPublicKeyByKeyId(context, masterKeyId);
            // if it is no public key get it from your own keys...
            if (key == null) {
                PGPSecretKey secretKey = ProviderHelper.getPGPSecretKeyByKeyId(context, masterKeyId);
                if (secretKey == null) {
                    Log.e(Constants.TAG, "Key could not be found!");
                    return null;
                }
                key = secretKey.getPublicKey();
            }

            fingerprint = key.getFingerprint();
        }

        return fingerprint;
    }

    public static String getUserId(Context context, Uri queryUri) {
        String[] projection = new String[]{UserIds.USER_ID};
        Cursor cursor = context.getContentResolver().query(queryUri, projection, null, null, null);

        String userId = null;
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndexOrThrow(UserIds.USER_ID);

                userId = cursor.getString(col);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return userId;
    }

    public static ArrayList<String> getKeyRingsAsArmoredString(Context context, Uri uri,
                                                               long[] masterKeyIds) {
        ArrayList<String> output = new ArrayList<String>();

        if (masterKeyIds != null && masterKeyIds.length > 0) {

            Cursor cursor = getCursorWithSelectedKeyringMasterKeyIds(context, uri, masterKeyIds);

            if (cursor != null) {
                int masterIdCol = cursor.getColumnIndex(KeyRings.MASTER_KEY_ID);
                int dataCol = cursor.getColumnIndex(KeyRings.KEY_RING_DATA);
                if (cursor.moveToFirst()) {
                    do {
                        Log.d(Constants.TAG, "masterKeyId: " + cursor.getLong(masterIdCol));

                        // get actual keyring data blob and write it to ByteArrayOutputStream
                        try {
                            Object keyRing = null;
                            byte[] data = cursor.getBlob(dataCol);
                            if (data != null) {
                                keyRing = PgpConversionHelper.BytesToPGPKeyRing(data);
                            }

                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            ArmoredOutputStream aos = new ArmoredOutputStream(bos);
                            aos.setHeader("Version", PgpHelper.getFullVersion(context));

                            if (keyRing instanceof PGPSecretKeyRing) {
                                aos.write(((PGPSecretKeyRing) keyRing).getEncoded());
                            } else if (keyRing instanceof PGPPublicKeyRing) {
                                aos.write(((PGPPublicKeyRing) keyRing).getEncoded());
                            }
                            aos.close();

                            String armoredKey = bos.toString("UTF-8");

                            Log.d(Constants.TAG, "armoredKey:" + armoredKey);

                            output.add(armoredKey);
                        } catch (IOException e) {
                            Log.e(Constants.TAG, "IOException", e);
                        }
                    } while (cursor.moveToNext());
                }
            }

            if (cursor != null) {
                cursor.close();
            }

        } else {
            Log.e(Constants.TAG, "No master keys given!");
        }

        if (output.size() > 0) {
            return output;
        } else {
            return null;
        }
    }

    public static byte[] getKeyRingsAsByteArray(Context context, Uri uri, long[] masterKeyIds) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        if (masterKeyIds != null && masterKeyIds.length > 0) {

            Cursor cursor = getCursorWithSelectedKeyringMasterKeyIds(context, uri, masterKeyIds);

            if (cursor != null) {
                int masterIdCol = cursor.getColumnIndex(KeyRings.MASTER_KEY_ID);
                int dataCol = cursor.getColumnIndex(KeyRings.KEY_RING_DATA);
                if (cursor.moveToFirst()) {
                    do {
                        Log.d(Constants.TAG, "masterKeyId: " + cursor.getLong(masterIdCol));

                        // get actual keyring data blob and write it to ByteArrayOutputStream
                        try {
                            bos.write(cursor.getBlob(dataCol));
                        } catch (IOException e) {
                            Log.e(Constants.TAG, "IOException", e);
                        }
                    } while (cursor.moveToNext());
                }
            }

            if (cursor != null) {
                cursor.close();
            }

        } else {
            Log.e(Constants.TAG, "No master keys given!");
        }

        return bos.toByteArray();
    }

    private static Cursor getCursorWithSelectedKeyringMasterKeyIds(Context context, Uri baseUri,
                                                                   long[] masterKeyIds) {
        Cursor cursor = null;
        if (masterKeyIds != null && masterKeyIds.length > 0) {

            String inMasterKeyList = KeyRings.MASTER_KEY_ID + " IN (";
            for (int i = 0; i < masterKeyIds.length; ++i) {
                if (i != 0) {
                    inMasterKeyList += ", ";
                }
                inMasterKeyList += DatabaseUtils.sqlEscapeString("" + masterKeyIds[i]);
            }
            inMasterKeyList += ")";

            cursor = context.getContentResolver().query(baseUri,
                    new String[]{KeyRings._ID, KeyRings.MASTER_KEY_ID, KeyRings.KEY_RING_DATA},
                    inMasterKeyList, null, null);
        }

        return cursor;
    }

    public static ArrayList<String> getRegisteredApiApps(Context context) {
        Cursor cursor = context.getContentResolver().query(ApiApps.CONTENT_URI, null, null, null,
                null);

        ArrayList<String> packageNames = new ArrayList<String>();
        if (cursor != null) {
            int packageNameCol = cursor.getColumnIndex(ApiApps.PACKAGE_NAME);
            if (cursor.moveToFirst()) {
                do {
                    packageNames.add(cursor.getString(packageNameCol));
                } while (cursor.moveToNext());
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return packageNames;
    }

    private static ContentValues contentValueForApiApps(AppSettings appSettings) {
        ContentValues values = new ContentValues();
        values.put(ApiApps.PACKAGE_NAME, appSettings.getPackageName());
        values.put(ApiApps.PACKAGE_SIGNATURE, appSettings.getPackageSignature());
        values.put(ApiApps.KEY_ID, appSettings.getKeyId());
        values.put(ApiApps.COMPRESSION, appSettings.getCompression());
        values.put(ApiApps.ENCRYPTION_ALGORITHM, appSettings.getEncryptionAlgorithm());
        values.put(ApiApps.HASH_ALORITHM, appSettings.getHashAlgorithm());

        return values;
    }

    public static void insertApiApp(Context context, AppSettings appSettings) {
        context.getContentResolver().insert(ApiApps.CONTENT_URI,
                contentValueForApiApps(appSettings));
    }

    public static void updateApiApp(Context context, AppSettings appSettings, Uri uri) {
        if (context.getContentResolver().update(uri, contentValueForApiApps(appSettings), null,
                null) <= 0) {
            throw new RuntimeException();
        }
    }

    public static AppSettings getApiAppSettings(Context context, Uri uri) {
        AppSettings settings = null;

        Cursor cur = context.getContentResolver().query(uri, null, null, null, null);
        if (cur != null && cur.moveToFirst()) {
            settings = new AppSettings();
            settings.setPackageName(cur.getString(cur
                    .getColumnIndex(KeychainContract.ApiApps.PACKAGE_NAME)));
            settings.setPackageSignature(cur.getBlob(cur
                    .getColumnIndex(KeychainContract.ApiApps.PACKAGE_SIGNATURE)));
            settings.setKeyId(cur.getLong(cur.getColumnIndex(KeychainContract.ApiApps.KEY_ID)));
            settings.setCompression(cur.getInt(cur
                    .getColumnIndexOrThrow(KeychainContract.ApiApps.COMPRESSION)));
            settings.setHashAlgorithm(cur.getInt(cur
                    .getColumnIndexOrThrow(KeychainContract.ApiApps.HASH_ALORITHM)));
            settings.setEncryptionAlgorithm(cur.getInt(cur
                    .getColumnIndexOrThrow(KeychainContract.ApiApps.ENCRYPTION_ALGORITHM)));
        }

        return settings;
    }

    public static byte[] getApiAppSignature(Context context, String packageName) {
        Uri queryUri = KeychainContract.ApiApps.buildByPackageNameUri(packageName);

        String[] projection = new String[]{ApiApps.PACKAGE_SIGNATURE};

        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(queryUri, projection, null, null, null);

        byte[] signature = null;
        if (cursor != null && cursor.moveToFirst()) {
            int signatureCol = 0;

            signature = cursor.getBlob(signatureCol);
        }

        if (cursor != null) {
            cursor.close();
        }

        return signature;
    }
}