package org.thialfihar.android.apg.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.helper.PGPConversionHelper;
import org.thialfihar.android.apg.helper.PGPHelper;
import org.thialfihar.android.apg.provider.ApgContract.KeyRings;
import org.thialfihar.android.apg.provider.ApgContract.UserIds;
import org.thialfihar.android.apg.provider.ApgContract.Keys;
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

    /**
     * Private helper method to get PGPKeyRing from database
     * 
     * @param context
     * @param queryUri
     * @return
     */
    private static PGPKeyRing getPGPKeyRing(Context context, Uri queryUri) {
        Cursor cursor = context.getContentResolver().query(queryUri,
                new String[] { KeyRings._ID, KeyRings.KEY_RING_DATA }, null, null, null);

        PGPKeyRing keyRing = null;
        if (cursor != null && cursor.moveToFirst()) {
            int keyRingDataCol = cursor.getColumnIndex(KeyRings.KEY_RING_DATA);

            byte[] data = cursor.getBlob(keyRingDataCol);
            if (data != null) {
                keyRing = PGPConversionHelper.BytesToPGPKeyRing(data);
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return keyRing;
    }

    /**
     * Retrieves the actual PGPPublicKeyRing object from the database blob based on the rowId
     * 
     * @param context
     * @param rowId
     * @return
     */
    public static PGPPublicKeyRing getPGPPublicKeyRingByRowId(Context context, long rowId) {
        Uri queryUri = KeyRings.buildPublicKeyRingsUri(Long.toString(rowId));
        return (PGPPublicKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Retrieves the actual PGPPublicKeyRing object from the database blob based on the maserKeyId
     * 
     * @param context
     * @param masterKeyId
     * @return
     */
    public static PGPPublicKeyRing getPGPPublicKeyRingByMasterKeyId(Context context,
            long masterKeyId) {
        Uri queryUri = KeyRings.buildPublicKeyRingsByMasterKeyIdUri(Long.toString(masterKeyId));
        return (PGPPublicKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Retrieves the actual PGPPublicKeyRing object from the database blob associated with a key
     * with this keyId
     * 
     * @param context
     * @param keyId
     * @return
     */
    public static PGPPublicKeyRing getPGPPublicKeyRingByKeyId(Context context, long keyId) {
        Uri queryUri = KeyRings.buildPublicKeyRingsByKeyIdUri(Long.toString(keyId));
        return (PGPPublicKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Retrieves the actual PGPPublicKey object from the database blob associated with a key with
     * this keyId
     * 
     * @param context
     * @param keyId
     * @return
     */
    public static PGPPublicKey getPGPPublicKeyByKeyId(Context context, long keyId) {
        PGPPublicKeyRing keyRing = getPGPPublicKeyRingByKeyId(context, keyId);
        if (keyRing == null) {
            return null;
        }

        return keyRing.getPublicKey(keyId);
    }

    /**
     * Retrieves the actual PGPSecretKeyRing object from the database blob based on the rowId
     * 
     * @param context
     * @param rowId
     * @return
     */
    public static PGPSecretKeyRing getPGPSecretKeyRingByRowId(Context context, long rowId) {
        Uri queryUri = KeyRings.buildSecretKeyRingsUri(Long.toString(rowId));
        return (PGPSecretKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Retrieves the actual PGPSecretKeyRing object from the database blob based on the maserKeyId
     * 
     * @param context
     * @param masterKeyId
     * @return
     */
    public static PGPSecretKeyRing getPGPSecretKeyRingByMasterKeyId(Context context,
            long masterKeyId) {
        Uri queryUri = KeyRings.buildSecretKeyRingsByMasterKeyIdUri(Long.toString(masterKeyId));
        return (PGPSecretKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Retrieves the actual PGPSecretKeyRing object from the database blob associated with a key
     * with this keyId
     * 
     * @param context
     * @param keyId
     * @return
     */
    public static PGPSecretKeyRing getPGPSecretKeyRingByKeyId(Context context, long keyId) {
        Uri queryUri = KeyRings.buildSecretKeyRingsByKeyIdUri(Long.toString(keyId));
        return (PGPSecretKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Retrieves the actual PGPSecretKey object from the database blob associated with a key with
     * this keyId
     * 
     * @param context
     * @param keyId
     * @return
     */
    public static PGPSecretKey getPGPSecretKeyByKeyId(Context context, long keyId) {
        PGPSecretKeyRing keyRing = getPGPSecretKeyRingByKeyId(context, keyId);
        if (keyRing == null) {
            return null;
        }

        return keyRing.getSecretKey(keyId);
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
        Uri deleteUri = KeyRings.buildPublicKeyRingsByMasterKeyIdUri(Long.toString(masterKeyId));

        try {
            context.getContentResolver().delete(deleteUri, null, null);
        } catch (UnsupportedOperationException e) {
            Log.e(Constants.TAG, "Key could not be deleted! Maybe we are creating a new one!", e);
        }

        ContentValues values = new ContentValues();
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
        Uri deleteUri = KeyRings.buildSecretKeyRingsByMasterKeyIdUri(Long.toString(masterKeyId));

        try {
            context.getContentResolver().delete(deleteUri, null, null);
        } catch (UnsupportedOperationException e) {
            Log.e(Constants.TAG, "Key could not be deleted! Maybe we are creating a new one!", e);
        }

        ContentValues values = new ContentValues();
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
        values.put(Keys.KEY_ID, key.getKeyID());
        values.put(Keys.IS_MASTER_KEY, key.isMasterKey());
        values.put(Keys.ALGORITHM, key.getAlgorithm());
        values.put(Keys.KEY_SIZE, key.getBitStrength());
        values.put(Keys.CAN_SIGN, PGPHelper.isSigningKey(key));
        values.put(Keys.CAN_ENCRYPT, PGPHelper.isEncryptionKey(key));
        values.put(Keys.IS_REVOKED, key.isRevoked());
        values.put(Keys.CREATION, PGPHelper.getCreationDate(key).getTime() / 1000);
        Date expiryDate = PGPHelper.getExpiryDate(key);
        if (expiryDate != null) {
            values.put(Keys.EXPIRY, expiryDate.getTime() / 1000);
        }
        values.put(Keys.KEY_RING_ROW_ID, keyRingRowId);
        values.put(Keys.KEY_DATA, key.getEncoded());
        values.put(Keys.RANK, rank);

        Uri uri = Keys.buildPublicKeysUri(Long.toString(keyRingRowId));

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
        values.put(UserIds.KEY_RING_ROW_ID, keyRingRowId);
        values.put(UserIds.USER_ID, userId);
        values.put(UserIds.RANK, rank);

        Uri uri = UserIds.buildPublicUserIdsUri(Long.toString(keyRingRowId));

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
        values.put(Keys.KEY_ID, key.getKeyID());
        values.put(Keys.IS_MASTER_KEY, key.isMasterKey());
        values.put(Keys.ALGORITHM, key.getPublicKey().getAlgorithm());
        values.put(Keys.KEY_SIZE, key.getPublicKey().getBitStrength());
        values.put(Keys.CAN_SIGN, PGPHelper.isSigningKey(key));
        values.put(Keys.CAN_ENCRYPT, PGPHelper.isEncryptionKey(key));
        values.put(Keys.IS_REVOKED, key.getPublicKey().isRevoked());
        values.put(Keys.CREATION, PGPHelper.getCreationDate(key).getTime() / 1000);
        Date expiryDate = PGPHelper.getExpiryDate(key);
        if (expiryDate != null) {
            values.put(Keys.EXPIRY, expiryDate.getTime() / 1000);
        }
        values.put(Keys.KEY_RING_ROW_ID, keyRingRowId);
        values.put(Keys.KEY_DATA, key.getEncoded());
        values.put(Keys.RANK, rank);

        Uri uri = Keys.buildSecretKeysUri(Long.toString(keyRingRowId));

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
        values.put(UserIds.KEY_RING_ROW_ID, keyRingRowId);
        values.put(UserIds.USER_ID, userId);
        values.put(UserIds.RANK, rank);

        Uri uri = UserIds.buildSecretUserIdsUri(Long.toString(keyRingRowId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * Private helper method
     * 
     * @param context
     * @param queryUri
     * @return
     */
    private static Vector<Integer> getKeyRingsRowIds(Context context, Uri queryUri) {
        Cursor cursor = context.getContentResolver().query(queryUri, new String[] { KeyRings._ID },
                null, null, null);

        Vector<Integer> keyIds = new Vector<Integer>();
        if (cursor != null) {
            int idCol = cursor.getColumnIndex(KeyRings._ID);
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
     * Retrieves ids of all SecretKeyRings
     * 
     * @param context
     * @return
     */
    public static Vector<Integer> getSecretKeyRingsRowIds(Context context) {
        Uri queryUri = KeyRings.buildSecretKeyRingsUri();
        return getKeyRingsRowIds(context, queryUri);
    }

    /**
     * Retrieves ids of all PublicKeyRings
     * 
     * @param context
     * @return
     */
    public static Vector<Integer> getPublicKeyRingsRowIds(Context context) {
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

    public static long getPublicMasterKeyId(Context context, long keyRingRowId) {
        Uri queryUri = KeyRings.buildPublicKeyRingsUri(String.valueOf(keyRingRowId));
        return getMasterKeyId(context, queryUri, keyRingRowId);
    }

    public static long getSecretMasterKeyId(Context context, long keyRingRowId) {
        Uri queryUri = KeyRings.buildSecretKeyRingsUri(String.valueOf(keyRingRowId));
        return getMasterKeyId(context, queryUri, keyRingRowId);
    }

    private static long getMasterKeyId(Context context, Uri queryUri, long keyRingRowId) {
        String[] projection = new String[] { KeyRings.MASTER_KEY_ID };

        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(queryUri, projection, null, null, null);

        long masterKeyId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            int masterKeyIdCol = cursor.getColumnIndex(KeyRings.MASTER_KEY_ID);

            masterKeyId = cursor.getLong(masterKeyIdCol);
        }

        if (cursor != null) {
            cursor.close();
        }

        return masterKeyId;
    }

}
