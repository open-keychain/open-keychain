/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.RemoteException;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpConversionHelper;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.remote.AccountSettings;
import org.sufficientlysecure.keychain.remote.AppSettings;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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

    public static PGPPublicKey getPGPPublicKeyByKeyId(Context context, long keyId) {
        return getPGPPublicKeyRingWithKeyId(context, keyId).getPublicKey(keyId);
    }
    public static PGPPublicKeyRing getPGPPublicKeyRingWithKeyId(Context context, long keyId) {
        // todo do
        return null;
    }

    public static PGPSecretKey getPGPSecretKeyByKeyId(Context context, long keyId) {
        return getPGPSecretKeyRingWithKeyId(context, keyId).getSecretKey(keyId);
    }
    public static PGPSecretKeyRing getPGPSecretKeyRingWithKeyId(Context context, long keyId) {
        // todo do
        return null;
    }

    /**
     * Retrieves the actual PGPPublicKeyRing object from the database blob based on the masterKeyId
     */
    public static PGPPublicKeyRing getPGPPublicKeyRing(Context context,
                                                                    long masterKeyId) {
        Uri queryUri = KeyRings.buildPublicKeyRingUri(Long.toString(masterKeyId));
        return (PGPPublicKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Retrieves the actual PGPSecretKeyRing object from the database blob based on the maserKeyId
     */
    public static PGPSecretKeyRing getPGPSecretKeyRing(Context context,
                                                                    long masterKeyId) {
        Uri queryUri = KeyRings.buildSecretKeyRingUri(Long.toString(masterKeyId));
        return (PGPSecretKeyRing) getPGPKeyRing(context, queryUri);
    }

    /**
     * Saves PGPPublicKeyRing with its keys and userIds in DB
     */
    @SuppressWarnings("unchecked")
    public static void saveKeyRing(Context context, PGPPublicKeyRing keyRing) throws IOException {
        PGPPublicKey masterKey = keyRing.getPublicKey();
        long masterKeyId = masterKey.getKeyID();

        // IF there is a secret key, preserve it!
        // TODO This even necessary?
        // PGPSecretKeyRing secretRing = ProviderHelper.getPGPSecretKeyRing(context, masterKeyId);

        // delete old version of this keyRing, which also deletes all keys and userIds on cascade
        try {
            context.getContentResolver().delete(KeyRings.buildPublicKeyRingUri(Long.toString(masterKeyId)), null, null);
        } catch (UnsupportedOperationException e) {
            Log.e(Constants.TAG, "Key could not be deleted! Maybe we are creating a new one!", e);
        }

        ContentValues values = new ContentValues();
        // use exactly the same _ID again to replace key in-place.
        // NOTE: If we would not use the same _ID again,
        // getting back to the ViewKeyActivity would result in Nullpointer,
        // because the currently loaded key would be gone from the database
        values.put(KeyRings.MASTER_KEY_ID, masterKeyId);
        values.put(KeyRings.KEY_RING_DATA, keyRing.getEncoded());

        // insert new version of this keyRing
        Uri uri = KeyRings.buildPublicKeyRingUri(Long.toString(masterKeyId));
        Uri insertedUri = context.getContentResolver().insert(uri, values);

        // save all keys and userIds included in keyRing object in database
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

        int rank = 0;
        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
            operations.add(buildPublicKeyOperations(context, masterKeyId, key, rank));
            ++rank;
        }

        int userIdRank = 0;
        for (String userId : new IterableIterator<String>(masterKey.getUserIDs())) {
            operations.add(buildUserIdOperations(context, masterKeyId, userId, userIdRank));
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

        // Save the saved keyring (if any)
        // TODO this even necessary? see above...
        // if(secretRing != null)
            // saveKeyRing(context, secretRing);

    }

    /**
     * Saves PGPSecretKeyRing with its keys and userIds in DB
     */
    @SuppressWarnings("unchecked")
    public static void saveKeyRing(Context context, PGPSecretKeyRing keyRing) throws IOException {
        PGPSecretKey masterKey = keyRing.getSecretKey();
        long masterKeyId = masterKey.getKeyID();

        // TODO Make sure there is a public key for this secret key in the db (create one maybe)

        {
            ContentValues values = new ContentValues();
            values.put(KeyRings.MASTER_KEY_ID, masterKeyId);
            values.put(KeyRings.KEY_RING_DATA, keyRing.getEncoded());
            // insert new version of this keyRing
            Uri uri = KeyRings.buildSecretKeyRingUri(Long.toString(masterKeyId));
            context.getContentResolver().insert(uri, values);
        }

    }

    /**
     * Build ContentProviderOperation to add PGPPublicKey to database corresponding to a keyRing
     */
    private static ContentProviderOperation buildPublicKeyOperations(Context context,
                                                                     long masterKeyId, PGPPublicKey key, int rank) throws IOException {

        ContentValues values = new ContentValues();
        values.put(Keys.MASTER_KEY_ID, masterKeyId);
        values.put(Keys.RANK, rank);

        values.put(Keys.KEY_ID, key.getKeyID());
        values.put(Keys.KEY_SIZE, key.getBitStrength());
        values.put(Keys.ALGORITHM, key.getAlgorithm());
        values.put(Keys.FINGERPRINT, key.getFingerprint());

        values.put(Keys.CAN_CERTIFY, (PgpKeyHelper.isCertificationKey(key)));
        values.put(Keys.CAN_SIGN, (PgpKeyHelper.isSigningKey(key)));
        values.put(Keys.CAN_ENCRYPT, PgpKeyHelper.isEncryptionKey(key));
        values.put(Keys.IS_REVOKED, key.isRevoked());

        values.put(Keys.CREATION, PgpKeyHelper.getCreationDate(key).getTime() / 1000);
        Date expiryDate = PgpKeyHelper.getExpiryDate(key);
        if (expiryDate != null) {
            values.put(Keys.EXPIRY, expiryDate.getTime() / 1000);
        }

        Uri uri = Keys.buildKeysUri(Long.toString(masterKeyId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * Build ContentProviderOperation to add PGPSecretKey to database corresponding to a keyRing
     */
    private static ContentProviderOperation buildSecretKeyOperations(Context context,
                                                                     long masterKeyId, PGPSecretKey key, int rank) throws IOException {
        return buildPublicKeyOperations(context, masterKeyId, key.getPublicKey(), rank);
    }

    /**
     * Build ContentProviderOperation to add PublicUserIds to database corresponding to a keyRing
     */
    private static ContentProviderOperation buildUserIdOperations(Context context,
                                                                  long masterKeyId, String userId, int rank) {
        ContentValues values = new ContentValues();
        values.put(UserIds.MASTER_KEY_ID, masterKeyId);
        values.put(UserIds.USER_ID, userId);
        values.put(UserIds.RANK, rank);

        Uri uri = UserIds.buildUserIdsUri(Long.toString(masterKeyId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    public static void deletePublicKeyRing(Context context, long masterKeyId) {
        ContentResolver cr = context.getContentResolver();
        cr.delete(KeyRings.buildPublicKeyRingUri(Long.toString(masterKeyId)), null, null);
    }

    public static void deleteSecretKeyRing(Context context, long masterKeyId) {
        ContentResolver cr = context.getContentResolver();
        cr.delete(KeyRings.buildSecretKeyRingUri(Long.toString(masterKeyId)), null, null);
    }

    public static boolean hasSecretKeyByMasterKeyId(Context context, long masterKeyId) {
        Uri queryUri = KeyRings.buildSecretKeyRingUri(Long.toString(masterKeyId));
        // see if we can get our master key id back from the uri
        return getMasterKeyId(context, queryUri) == masterKeyId;
    }

    /**
     * Get master key id of key
     */
    public static long getMasterKeyId(Context context, Uri queryUri) {
        // try extracting from the uri first
        try {
            return Long.parseLong(queryUri.getPathSegments().get(1));
        } catch(NumberFormatException e) {
            // didn't work? oh well.
        }

        Cursor cursor = context.getContentResolver().query(queryUri, new String[] {
                KeyRings.MASTER_KEY_ID
        }, null, null, null);

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

            PGPPublicKey key = ProviderHelper.getPGPPublicKeyRing(context, masterKeyId).getPublicKey();
            // if it is no public key get it from your own keys...
            if (key == null) {
                PGPSecretKey secretKey = ProviderHelper.getPGPSecretKeyRing(context, masterKeyId).getSecretKey();
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
        return values;
    }

    private static ContentValues contentValueForApiAccounts(AccountSettings accSettings) {
        ContentValues values = new ContentValues();
        values.put(KeychainContract.ApiAccounts.ACCOUNT_NAME, accSettings.getAccountName());
        values.put(KeychainContract.ApiAccounts.KEY_ID, accSettings.getKeyId());
        values.put(KeychainContract.ApiAccounts.COMPRESSION, accSettings.getCompression());
        values.put(KeychainContract.ApiAccounts.ENCRYPTION_ALGORITHM, accSettings.getEncryptionAlgorithm());
        values.put(KeychainContract.ApiAccounts.HASH_ALORITHM, accSettings.getHashAlgorithm());
        return values;
    }

    public static void insertApiApp(Context context, AppSettings appSettings) {
        context.getContentResolver().insert(KeychainContract.ApiApps.CONTENT_URI,
                contentValueForApiApps(appSettings));
    }

    public static void insertApiAccount(Context context, Uri uri, AccountSettings accSettings) {
        context.getContentResolver().insert(uri, contentValueForApiAccounts(accSettings));
    }

    public static void updateApiApp(Context context, AppSettings appSettings, Uri uri) {
        if (context.getContentResolver().update(uri, contentValueForApiApps(appSettings), null,
                null) <= 0) {
            throw new RuntimeException();
        }
    }

    public static void updateApiAccount(Context context, AccountSettings accSettings, Uri uri) {
        if (context.getContentResolver().update(uri, contentValueForApiAccounts(accSettings), null,
                null) <= 0) {
            throw new RuntimeException();
        }
    }

    /**
     * Must be an uri pointing to an account
     *
     * @param context
     * @param uri
     * @return
     */
    public static AppSettings getApiAppSettings(Context context, Uri uri) {
        AppSettings settings = null;

        Cursor cur = context.getContentResolver().query(uri, null, null, null, null);
        if (cur != null && cur.moveToFirst()) {
            settings = new AppSettings();
            settings.setPackageName(cur.getString(
                    cur.getColumnIndex(KeychainContract.ApiApps.PACKAGE_NAME)));
            settings.setPackageSignature(cur.getBlob(
                    cur.getColumnIndex(KeychainContract.ApiApps.PACKAGE_SIGNATURE)));
        }

        return settings;
    }

    public static AccountSettings getApiAccountSettings(Context context, Uri accountUri) {
        AccountSettings settings = null;

        Cursor cur = context.getContentResolver().query(accountUri, null, null, null, null);
        if (cur != null && cur.moveToFirst()) {
            settings = new AccountSettings();

            settings.setAccountName(cur.getString(
                    cur.getColumnIndex(KeychainContract.ApiAccounts.ACCOUNT_NAME)));
            settings.setKeyId(cur.getLong(
                    cur.getColumnIndex(KeychainContract.ApiAccounts.KEY_ID)));
            settings.setCompression(cur.getInt(
                    cur.getColumnIndexOrThrow(KeychainContract.ApiAccounts.COMPRESSION)));
            settings.setHashAlgorithm(cur.getInt(
                    cur.getColumnIndexOrThrow(KeychainContract.ApiAccounts.HASH_ALORITHM)));
            settings.setEncryptionAlgorithm(cur.getInt(
                    cur.getColumnIndexOrThrow(KeychainContract.ApiAccounts.ENCRYPTION_ALGORITHM)));
        }

        return settings;
    }

    public static Set<Long> getAllKeyIdsForApp(Context context, Uri uri) {
        Set<Long> keyIds = new HashSet<Long>();

        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int keyIdColumn = cursor.getColumnIndex(KeychainContract.ApiAccounts.KEY_ID);
            while (cursor.moveToNext()) {
                keyIds.add(cursor.getLong(keyIdColumn));
            }
        }

        return keyIds;
    }

    public static byte[] getApiAppSignature(Context context, String packageName) {
        Uri queryUri = ApiApps.buildByPackageNameUri(packageName);

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
