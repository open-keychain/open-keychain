package org.thialfihar.android.apg.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.helper.PGPConversionHelper;
import org.thialfihar.android.apg.helper.PGPHelper;
import org.thialfihar.android.apg.provider.ApgContract.PublicKeyRings;
import org.thialfihar.android.apg.provider.ApgContract.PublicKeys;
import org.thialfihar.android.apg.provider.ApgContract.PublicUserIds;
import org.thialfihar.android.apg.provider.ApgContract.SecretKeyRings;
import org.thialfihar.android.apg.provider.ApgContract.SecretKeys;
import org.thialfihar.android.apg.provider.ApgContract.SecretUserIds;
import org.thialfihar.android.apg.util.IterableIterator;
import org.thialfihar.android.apg.util.Log;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

public class ProviderHelper {

    //
    // public static HashMap<String, String> sKeyRingsProjection;
    // public static HashMap<String, String> sKeysProjection;
    // public static HashMap<String, String> sUserIdsProjection;
    //
    // private SQLiteDatabase mDb = null;
    // private int mStatus = 0;
    //
    // static {
    // sKeyRingsProjection = new HashMap<String, String>();
    // sKeyRingsProjection.put(KeyRings._ID, KeyRings._ID);
    // sKeyRingsProjection.put(KeyRings.MASTER_KEY_ID, KeyRings.MASTER_KEY_ID);
    // sKeyRingsProjection.put(KeyRings.TYPE, KeyRings.TYPE);
    // sKeyRingsProjection.put(KeyRings.WHO_ID, KeyRings.WHO_ID);
    // sKeyRingsProjection.put(KeyRings.KEY_RING_DATA, KeyRings.KEY_RING_DATA);
    //
    // sKeysProjection = new HashMap<String, String>();
    // sKeysProjection.put(Keys._ID, Keys._ID);
    // sKeysProjection.put(Keys.KEY_ID, Keys.KEY_ID);
    // sKeysProjection.put(Keys.TYPE, Keys.TYPE);
    // sKeysProjection.put(Keys.IS_MASTER_KEY, Keys.IS_MASTER_KEY);
    // sKeysProjection.put(Keys.ALGORITHM, Keys.ALGORITHM);
    // sKeysProjection.put(Keys.KEY_SIZE, Keys.KEY_SIZE);
    // sKeysProjection.put(Keys.CAN_SIGN, Keys.CAN_SIGN);
    // sKeysProjection.put(Keys.CAN_ENCRYPT, Keys.CAN_ENCRYPT);
    // sKeysProjection.put(Keys.IS_REVOKED, Keys.IS_REVOKED);
    // sKeysProjection.put(Keys.CREATION, Keys.CREATION);
    // sKeysProjection.put(Keys.EXPIRY, Keys.EXPIRY);
    // sKeysProjection.put(Keys.KEY_DATA, Keys.KEY_DATA);
    // sKeysProjection.put(Keys.RANK, Keys.RANK);
    //
    // sUserIdsProjection = new HashMap<String, String>();
    // sUserIdsProjection.put(UserIds._ID, UserIds._ID);
    // sUserIdsProjection.put(UserIds.KEY_ID, UserIds.KEY_ID);
    // sUserIdsProjection.put(UserIds.USER_ID, UserIds.USER_ID);
    // sUserIdsProjection.put(UserIds.RANK, UserIds.RANK);
    // }

    /**
     * Retrieves the actual PGPPublicKeyRing object from the database blob associated with the
     * maserKeyId
     * 
     * @param context
     * @param masterKeyId
     * @return
     */
    public static PGPPublicKeyRing getPGPPublicKeyRingByMasterKeyId(Context context,
            long masterKeyId) {
        Uri queryUri = PublicKeyRings.buildPublicKeyRingsByMasterKeyIdUri(Long
                .toString(masterKeyId));
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
     * Retrieves the actual PGPSecretKeyRing object from the database blob associated with the
     * maserKeyId
     * 
     * @param context
     * @param masterKeyId
     * @return
     */
    public static PGPSecretKeyRing getPGPSecretKeyRingByMasterKeyId(Context context,
            long masterKeyId) {
        Uri queryUri = SecretKeyRings.buildSecretKeyRingsByMasterKeyIdUri(Long
                .toString(masterKeyId));
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
        PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(context, keyId);
        if (keyRing == null) {
            return null;
        }
        return keyRing.getSecretKey(keyId);
    }

    public static PGPPublicKey getPGPPublicKey(Context context, long keyId) {
        PGPPublicKeyRing keyRing = ProviderHelper.getPGPPublicKeyRingByMasterKeyId(context, keyId);
        if (keyRing == null) {
            return null;
        }

        return keyRing.getPublicKey(keyId);
    }

    /**
     * Saves PGPPublicKeyRing with its keys and userIds in DB
     * 
     * @param context
     * @param keyRing
     * @return
     * @throws IOException
     * @throws GeneralException
     */
    public static void saveKeyRing(Context context, PGPPublicKeyRing keyRing) throws IOException {
        PGPPublicKey masterKey = keyRing.getPublicKey();
        long masterKeyId = masterKey.getKeyID();

        // delete old version of this keyRing, which also deletes all keys and userIds on cascade
        Uri deleteUri = PublicKeyRings.buildPublicKeyRingsByMasterKeyIdUri(Long
                .toString(masterKeyId));
        context.getContentResolver().delete(deleteUri, null, null);

        ContentValues values = new ContentValues();
        values.put(PublicKeyRings.MASTER_KEY_ID, masterKeyId);
        values.put(PublicKeyRings.KEY_RING_DATA, keyRing.getEncoded());

        // insert new version of this keyRing
        Uri uri = PublicKeyRings.buildPublicKeyRingsByMasterKeyIdUri(values
                .getAsString(PublicKeyRings.MASTER_KEY_ID));
        Uri insertedUri = context.getContentResolver().insert(uri, values);
        long keyRingRowId = Long.getLong(insertedUri.getLastPathSegment());

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

        try {
            context.getContentResolver().applyBatch(ApgContract.CONTENT_AUTHORITY, operations);
        } catch (RemoteException e) {
            Log.e(Constants.TAG, "applyBatch failed!", e);
        } catch (OperationApplicationException e) {
            Log.e(Constants.TAG, "applyBatch failed!", e);
        }
    }

    /**
     * Saves PGPSecretKeyRing with its keys and userIds in DB
     * 
     * @param context
     * @param keyRing
     * @return
     * @throws IOException
     * @throws GeneralException
     */
    public static void saveKeyRing(Context context, PGPSecretKeyRing keyRing) throws IOException {
        PGPSecretKey masterKey = keyRing.getSecretKey();
        long masterKeyId = masterKey.getKeyID();

        // delete old version of this keyRing, which also deletes all keys and userIds on cascade
        Uri deleteUri = SecretKeyRings.buildSecretKeyRingsByMasterKeyIdUri(Long
                .toString(masterKeyId));
        context.getContentResolver().delete(deleteUri, null, null);

        ContentValues values = new ContentValues();
        values.put(SecretKeyRings.MASTER_KEY_ID, masterKeyId);
        values.put(SecretKeyRings.KEY_RING_DATA, keyRing.getEncoded());

        // insert new version of this keyRing
        Uri uri = SecretKeyRings.buildSecretKeyRingsByMasterKeyIdUri(values
                .getAsString(SecretKeyRings.MASTER_KEY_ID));
        Uri insertedUri = context.getContentResolver().insert(uri, values);
        long keyRingRowId = Long.getLong(insertedUri.getLastPathSegment());

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
            context.getContentResolver().applyBatch(ApgContract.CONTENT_AUTHORITY, operations);
        } catch (RemoteException e) {
            Log.e(Constants.TAG, "applyBatch failed!", e);
        } catch (OperationApplicationException e) {
            Log.e(Constants.TAG, "applyBatch failed!", e);
        }
    }

    /**
     * Build ContentProviderOperation to add PGPPublicKey to database corresponding to a keyRing
     * 
     * @param context
     * @param keyRingRowId
     * @param key
     * @param rank
     * @return
     * @throws IOException
     */
    private static ContentProviderOperation buildPublicKeyOperations(Context context,
            long keyRingRowId, PGPPublicKey key, int rank) throws IOException {
        ContentValues values = new ContentValues();
        values.put(PublicKeys.KEY_ID, key.getKeyID());
        values.put(PublicKeys.IS_MASTER_KEY, key.isMasterKey());
        values.put(PublicKeys.ALGORITHM, key.getAlgorithm());
        values.put(PublicKeys.KEY_SIZE, key.getBitStrength());
        values.put(PublicKeys.CAN_SIGN, PGPHelper.isSigningKey(key));
        values.put(PublicKeys.CAN_ENCRYPT, PGPHelper.isEncryptionKey(key));
        values.put(PublicKeys.IS_REVOKED, key.isRevoked());
        values.put(PublicKeys.CREATION, PGPHelper.getCreationDate(key).getTime() / 1000);
        Date expiryDate = PGPHelper.getExpiryDate(key);
        if (expiryDate != null) {
            values.put(PublicKeys.EXPIRY, expiryDate.getTime() / 1000);
        }
        values.put(PublicKeys.KEY_RING_ROW_ID, keyRingRowId);
        values.put(PublicKeys.KEY_DATA, key.getEncoded());
        values.put(PublicKeys.RANK, rank);

        Uri uri = PublicKeys.buildPublicKeysUri(Long.toString(keyRingRowId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * Build ContentProviderOperation to add PublicUserIds to database corresponding to a keyRing
     * 
     * @param context
     * @param keyRingRowId
     * @param key
     * @param rank
     * @return
     * @throws IOException
     */
    private static ContentProviderOperation buildPublicUserIdOperations(Context context,
            long keyRingRowId, String userId, int rank) {
        ContentValues values = new ContentValues();
        values.put(PublicUserIds.KEY_RING_ROW_ID, keyRingRowId);
        values.put(PublicUserIds.USER_ID, userId);
        values.put(PublicUserIds.RANK, rank);

        Uri uri = PublicUserIds.buildPublicUserIdsUri(Long.toString(keyRingRowId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * Build ContentProviderOperation to add PGPSecretKey to database corresponding to a keyRing
     * 
     * @param context
     * @param keyRingRowId
     * @param key
     * @param rank
     * @return
     * @throws IOException
     */
    private static ContentProviderOperation buildSecretKeyOperations(Context context,
            long keyRingRowId, PGPSecretKey key, int rank) throws IOException {
        ContentValues values = new ContentValues();
        values.put(SecretKeys.KEY_ID, key.getKeyID());
        values.put(SecretKeys.IS_MASTER_KEY, key.isMasterKey());
        values.put(SecretKeys.ALGORITHM, key.getPublicKey().getAlgorithm());
        values.put(SecretKeys.KEY_SIZE, key.getPublicKey().getBitStrength());
        values.put(SecretKeys.CAN_SIGN, PGPHelper.isSigningKey(key));
        values.put(SecretKeys.CAN_ENCRYPT, PGPHelper.isEncryptionKey(key));
        values.put(SecretKeys.IS_REVOKED, key.getPublicKey().isRevoked());
        values.put(SecretKeys.CREATION, PGPHelper.getCreationDate(key).getTime() / 1000);
        Date expiryDate = PGPHelper.getExpiryDate(key);
        if (expiryDate != null) {
            values.put(SecretKeys.EXPIRY, expiryDate.getTime() / 1000);
        }
        values.put(SecretKeys.KEY_RING_ROW_ID, keyRingRowId);
        values.put(SecretKeys.KEY_DATA, key.getEncoded());
        values.put(SecretKeys.RANK, rank);

        Uri uri = SecretKeys.buildSecretKeysUri(Long.toString(keyRingRowId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * Build ContentProviderOperation to add SecretUserIds to database corresponding to a keyRing
     * 
     * @param context
     * @param keyRingRowId
     * @param key
     * @param rank
     * @return
     * @throws IOException
     */
    private static ContentProviderOperation buildSecretUserIdOperations(Context context,
            long keyRingRowId, String userId, int rank) {
        ContentValues values = new ContentValues();
        values.put(SecretUserIds.KEY_RING_ROW_ID, keyRingRowId);
        values.put(SecretUserIds.USER_ID, userId);
        values.put(SecretUserIds.RANK, rank);

        Uri uri = SecretUserIds.buildSecretUserIdsUri(Long.toString(keyRingRowId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

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
