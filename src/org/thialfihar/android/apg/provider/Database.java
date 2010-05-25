package org.thialfihar.android.apg.provider;

import java.io.IOException;
import java.util.HashMap;

import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPPublicKey;
import org.bouncycastle2.openpgp.PGPPublicKeyRing;
import org.bouncycastle2.openpgp.PGPSecretKey;
import org.bouncycastle2.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.Apg;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.utils.IterableIterator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class Database extends SQLiteOpenHelper {
    public static class GeneralException extends Exception {
        static final long serialVersionUID = 0xf812773343L;

        public GeneralException(String message) {
            super(message);
        }
    }

    private static final String DATABASE_NAME = "apg";
    private static final int DATABASE_VERSION = 2;

    public static final String AUTHORITY = "org.thialfihar.android.apg.database";

    private static HashMap<String, String> sKeyRingsProjection;
    private static HashMap<String, String> sKeysProjection;
    private static HashMap<String, String> sUserIdsProjection;

    private SQLiteDatabase mCurrentDb = null;

    static {
        sKeyRingsProjection = new HashMap<String, String>();
        sKeyRingsProjection.put(KeyRings._ID, KeyRings._ID);
        sKeyRingsProjection.put(KeyRings.MASTER_KEY_ID, KeyRings.MASTER_KEY_ID);
        sKeyRingsProjection.put(KeyRings.TYPE, KeyRings.TYPE);
        sKeyRingsProjection.put(KeyRings.WHO_ID, KeyRings.WHO_ID);
        sKeyRingsProjection.put(KeyRings.KEY_RING_DATA, KeyRings.KEY_RING_DATA);

        sKeysProjection = new HashMap<String, String>();
        sKeysProjection.put(Keys._ID, Keys._ID);
        sKeysProjection.put(Keys.KEY_ID, Keys.KEY_ID);
        sKeysProjection.put(Keys.TYPE, Keys.TYPE);
        sKeysProjection.put(Keys.IS_MASTER_KEY, Keys.IS_MASTER_KEY);
        sKeysProjection.put(Keys.ALGORITHM, Keys.ALGORITHM);
        sKeysProjection.put(Keys.KEY_SIZE, Keys.KEY_SIZE);
        sKeysProjection.put(Keys.CAN_SIGN, Keys.CAN_SIGN);
        sKeysProjection.put(Keys.CAN_ENCRYPT, Keys.CAN_ENCRYPT);
        sKeysProjection.put(Keys.KEY_DATA, Keys.KEY_DATA);
        sKeysProjection.put(Keys.RANK, Keys.RANK);

        sUserIdsProjection = new HashMap<String, String>();
        sUserIdsProjection.put(UserIds._ID, UserIds._ID);
        sUserIdsProjection.put(UserIds.KEY_ID, UserIds.KEY_ID);
        sUserIdsProjection.put(UserIds.USER_ID, UserIds.USER_ID);
        sUserIdsProjection.put(UserIds.RANK, UserIds.RANK);
    }

    Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        //getWritableDatabase();
        // force upgrade to test things
        onUpgrade(getWritableDatabase(), 1, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + KeyRings.TABLE_NAME + " (" +
                   KeyRings._ID + " " + KeyRings._ID_type + "," +
                   KeyRings.MASTER_KEY_ID + " " + KeyRings.MASTER_KEY_ID_type + ", " +
                   KeyRings.TYPE + " " + KeyRings.TYPE_type + ", " +
                   KeyRings.WHO_ID + " " + KeyRings.WHO_ID_type + ", " +
                   KeyRings.KEY_RING_DATA + " " + KeyRings.KEY_RING_DATA_type + ");");

        db.execSQL("CREATE TABLE " + Keys.TABLE_NAME + " (" +
                   Keys._ID + " " + Keys._ID_type + "," +
                   Keys.KEY_ID + " " + Keys.KEY_ID_type + ", " +
                   Keys.TYPE + " " + Keys.TYPE_type + ", " +
                   Keys.IS_MASTER_KEY + " " + Keys.IS_MASTER_KEY_type + ", " +
                   Keys.ALGORITHM + " " + Keys.ALGORITHM_type + ", " +
                   Keys.KEY_SIZE + " " + Keys.KEY_SIZE_type + ", " +
                   Keys.CAN_SIGN + " " + Keys.CAN_SIGN_type + ", " +
                   Keys.CAN_ENCRYPT + " " + Keys.CAN_ENCRYPT_type + ", " +
                   Keys.KEY_RING_ID + " " + Keys.KEY_RING_ID_type + ", " +
                   Keys.KEY_DATA + " " + Keys.KEY_DATA_type +
                   Keys.RANK + " " + Keys.RANK_type + ");");

        db.execSQL("CREATE TABLE " + UserIds.TABLE_NAME + " (" +
                   UserIds._ID + " " + UserIds._ID_type + "," +
                   UserIds.KEY_ID + " " + UserIds.KEY_ID_type + "," +
                   UserIds.USER_ID + " " + UserIds.USER_ID_type + "," +
                   UserIds.RANK + " " + UserIds.RANK_type + ");");

        db.execSQL("CREATE TABLE " + Accounts.TABLE_NAME + " (" +
                   Accounts._ID + " " + Accounts._ID_type + "," +
                   Accounts.NAME + " " + Accounts.NAME_type + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        mCurrentDb = db;
        for (int version = oldVersion; version < newVersion; ++version) {
            switch (version) {
                case 1: { // upgrade 1 to 2
                    db.execSQL("DROP TABLE IF EXISTS " + KeyRings.TABLE_NAME + ";");
                    db.execSQL("DROP TABLE IF EXISTS " + Keys.TABLE_NAME + ";");
                    db.execSQL("DROP TABLE IF EXISTS " + UserIds.TABLE_NAME + ";");

                    db.execSQL("CREATE TABLE " + KeyRings.TABLE_NAME + " (" +
                               KeyRings._ID + " " + KeyRings._ID_type + "," +
                               KeyRings.MASTER_KEY_ID + " " + KeyRings.MASTER_KEY_ID_type + ", " +
                               KeyRings.TYPE + " " + KeyRings.TYPE_type + ", " +
                               KeyRings.WHO_ID + " " + KeyRings.WHO_ID_type + ", " +
                               KeyRings.KEY_RING_DATA + " " + KeyRings.KEY_RING_DATA_type + ");");

                    db.execSQL("CREATE TABLE " + Keys.TABLE_NAME + " (" +
                               Keys._ID + " " + Keys._ID_type + "," +
                               Keys.KEY_ID + " " + Keys.KEY_ID_type + ", " +
                               Keys.TYPE + " " + Keys.TYPE_type + ", " +
                               Keys.IS_MASTER_KEY + " " + Keys.IS_MASTER_KEY_type + ", " +
                               Keys.ALGORITHM + " " + Keys.ALGORITHM_type + ", " +
                               Keys.KEY_SIZE + " " + Keys.KEY_SIZE_type + ", " +
                               Keys.CAN_SIGN + " " + Keys.CAN_SIGN_type + ", " +
                               Keys.CAN_ENCRYPT + " " + Keys.CAN_ENCRYPT_type + ", " +
                               Keys.KEY_RING_ID + " " + Keys.KEY_RING_ID_type + ", " +
                               Keys.KEY_DATA + " " + Keys.KEY_DATA_type +
                               Keys.RANK + " " + Keys.RANK_type + ");");

                    db.execSQL("CREATE TABLE " + UserIds.TABLE_NAME + " (" +
                               UserIds._ID + " " + UserIds._ID_type + "," +
                               UserIds.KEY_ID + " " + UserIds.KEY_ID_type + "," +
                               UserIds.USER_ID + " " + UserIds.USER_ID_type + "," +
                               UserIds.RANK + " " + UserIds.RANK_type + ");");

                    Cursor cursor = db.query(PublicKeys.TABLE_NAME,
                                             new String[]{
                                                 PublicKeys.KEY_DATA,
                                             }, null, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            byte[] data = cursor.getBlob(cursor.getColumnIndex(PublicKeys.KEY_DATA));
                            try {
                                PGPPublicKeyRing keyRing = new PGPPublicKeyRing(data);
                                saveKeyRing(keyRing);
                                Log.e("good", "imported " + keyRing);
                            } catch (IOException e) {
                                Log.e("apg.db.upgrade", "key import failed: " + e);
                            } catch (GeneralException e) {
                                Log.e("apg.db.upgrade", "key import failed: " + e);
                            }
                        } while (cursor.moveToNext());
                    }

                    cursor = db.query(SecretKeys.TABLE_NAME,
                                      new String[]{
                                          SecretKeys.KEY_DATA,
                                      }, null, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            byte[] data = cursor.getBlob(cursor.getColumnIndex(SecretKeys.KEY_DATA));
                            try {
                                PGPSecretKeyRing keyRing = new PGPSecretKeyRing(data);
                                saveKeyRing(keyRing);
                                Log.e("good", "imported " + keyRing);
                            } catch (IOException e) {
                                Log.e("apg.db.upgrade", "key import failed: " + e);
                            } catch (PGPException e) {
                                Log.e("apg.db.upgrade", "key import failed: " + e);
                            } catch (GeneralException e) {
                                Log.e("apg.db.upgrade", "key import failed: " + e);
                            }
                        } while (cursor.moveToNext());
                    }

                    break;
                }

                default: {
                    break;
                }
            }
        }
        mCurrentDb = null;
    }

    public void saveKeyRing(PGPPublicKeyRing keyRing) throws IOException, GeneralException {
        ContentValues values = new ContentValues();
        PGPPublicKey masterKey = keyRing.getPublicKey();
        long masterKeyId = masterKey.getKeyID();

        values.put(KeyRings.MASTER_KEY_ID, masterKeyId);
        values.put(KeyRings.TYPE, Id.database.type_public);
        values.put(KeyRings.KEY_RING_DATA, keyRing.getEncoded());

        long rowId = insertOrUpdateKeyRing(values);

        if (rowId == -1) {
            throw new GeneralException("saving public key ring " + masterKeyId + " failed");
        }

        int rank = 0;
        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
            saveKey(rowId, key, rank);
            ++rank;
        }
    }

    public void saveKeyRing(PGPSecretKeyRing keyRing) throws IOException, GeneralException {
        ContentValues values = new ContentValues();
        PGPSecretKey masterKey = keyRing.getSecretKey();
        long masterKeyId = masterKey.getKeyID();

        values.put(KeyRings.MASTER_KEY_ID, masterKeyId);
        values.put(KeyRings.TYPE, Id.database.type_secret);
        values.put(KeyRings.KEY_RING_DATA, keyRing.getEncoded());

        long rowId = insertOrUpdateKeyRing(values);

        if (rowId == -1) {
            throw new GeneralException("saving secret key ring " + masterKeyId + " failed");
        }

        // TODO: delete every related key not saved now
        int rank = 0;
        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
            saveKey(rowId, key, rank);
            ++rank;
        }
    }

    private void saveKey(long keyRingId, PGPPublicKey key, int rank)
            throws IOException, GeneralException {
        ContentValues values = new ContentValues();

        values.put(Keys.KEY_ID, key.getKeyID());
        values.put(Keys.TYPE, Id.database.type_public);
        values.put(Keys.IS_MASTER_KEY, key.isMasterKey());
        values.put(Keys.ALGORITHM, key.getAlgorithm());
        values.put(Keys.KEY_SIZE, key.getBitStrength());
        values.put(Keys.CAN_SIGN, Apg.isSigningKey(key));
        values.put(Keys.CAN_ENCRYPT, Apg.isEncryptionKey(key));
        values.put(Keys.KEY_RING_ID, keyRingId);
        values.put(Keys.KEY_DATA, key.getEncoded());
        values.put(Keys.RANK, rank);

        long rowId = insertOrUpdateKey(values);

        if (rowId == -1) {
            throw new GeneralException("saving public key " + key.getKeyID() + " failed");
        }

        int userIdRank = 0;
        for (String userId : new IterableIterator<String>(key.getUserIDs())) {
            saveUserId(rowId, userId, userIdRank);
            ++userIdRank;
        }
    }

    private void saveKey(long keyRingId, PGPSecretKey key, int rank)
            throws IOException, GeneralException {
        ContentValues values = new ContentValues();

        values.put(Keys.KEY_ID, key.getPublicKey().getKeyID());
        values.put(Keys.TYPE, Id.database.type_secret);
        values.put(Keys.IS_MASTER_KEY, key.isMasterKey());
        values.put(Keys.ALGORITHM, key.getPublicKey().getAlgorithm());
        values.put(Keys.KEY_SIZE, key.getPublicKey().getBitStrength());
        values.put(Keys.CAN_SIGN, Apg.isSigningKey(key));
        values.put(Keys.CAN_ENCRYPT, Apg.isEncryptionKey(key));
        values.put(Keys.KEY_RING_ID, keyRingId);
        values.put(Keys.KEY_DATA, key.getEncoded());
        values.put(Keys.RANK, rank);

        long rowId = insertOrUpdateKey(values);

        if (rowId == -1) {
            throw new GeneralException("saving secret key " + key.getPublicKey().getKeyID() + " failed");
        }

        int userIdRank = 0;
        for (String userId : new IterableIterator<String>(key.getUserIDs())) {
            saveUserId(rowId, userId, userIdRank);
            ++userIdRank;
        }
    }

    private void saveUserId(long keyId, String userId, int rank) throws GeneralException {
        ContentValues values = new ContentValues();

        values.put(UserIds.KEY_ID, keyId);
        values.put(UserIds.USER_ID, userId);
        values.put(UserIds.RANK, rank);

        long rowId = insertOrUpdateUserId(values);

        if (rowId == -1) {
            throw new GeneralException("saving user id " + userId + " failed");
        }
    }

    private long insertOrUpdateKeyRing(ContentValues values) {
        SQLiteDatabase db = mCurrentDb != null ? mCurrentDb : getWritableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        qb.setTables(KeyRings.TABLE_NAME);
        qb.setProjectionMap(sKeyRingsProjection);

        Cursor c = qb.query(db, new String[] { KeyRings._ID },
                            KeyRings.MASTER_KEY_ID + " = ? AND " + KeyRings.TYPE + " = ?",
                            new String[] {
                                values.getAsString(KeyRings.MASTER_KEY_ID),
                                values.getAsString(KeyRings.TYPE),
                            },
                            null, null, null);
        long rowId = -1;
        if (c != null && c.moveToFirst()) {
            rowId = c.getLong(0);
            db.update(KeyRings.TABLE_NAME, values,
                      KeyRings._ID + " = ?", new String[] { "" + rowId });
        } else {
            rowId = db.insert(KeyRings.TABLE_NAME, KeyRings.WHO_ID, values);
        }

        return rowId;
    }

    private long insertOrUpdateKey(ContentValues values) {
        SQLiteDatabase db = mCurrentDb != null ? mCurrentDb : getWritableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        qb.setTables(Keys.TABLE_NAME);
        qb.setProjectionMap(sKeysProjection);

        Cursor c = qb.query(db, new String[] { Keys._ID },
                            Keys.KEY_ID + " = ? AND " + Keys.TYPE + " = ?",
                            new String[] {
                                values.getAsString(Keys.KEY_ID),
                                values.getAsString(Keys.TYPE),
                            },
                            null, null, null);
        long rowId = -1;
        if (c != null && c.moveToFirst()) {
            rowId = c.getLong(0);
            db.update(Keys.TABLE_NAME, values,
                      Keys._ID + " = ?", new String[] { "" + rowId });
        } else {
            rowId = db.insert(Keys.TABLE_NAME, Keys.KEY_DATA, values);
        }

        return rowId;
    }

    private long insertOrUpdateUserId(ContentValues values) {
        SQLiteDatabase db = mCurrentDb != null ? mCurrentDb : getWritableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        qb.setTables(UserIds.TABLE_NAME);
        qb.setProjectionMap(sUserIdsProjection);

        Cursor c = qb.query(db, new String[] { UserIds._ID },
                            UserIds.KEY_ID + " = ? AND " + UserIds.USER_ID + " = ?",
                            new String[] {
                                values.getAsString(UserIds.KEY_ID),
                                values.getAsString(UserIds.USER_ID),
                            },
                            null, null, null);
        long rowId = -1;
        if (c != null && c.moveToFirst()) {
            rowId = c.getLong(0);
            db.update(UserIds.TABLE_NAME, values,
                      UserIds._ID + " = ?", new String[] { "" + rowId });
        } else {
            rowId = db.insert(UserIds.TABLE_NAME, UserIds.USER_ID, values);
        }

        return rowId;
    }
}
