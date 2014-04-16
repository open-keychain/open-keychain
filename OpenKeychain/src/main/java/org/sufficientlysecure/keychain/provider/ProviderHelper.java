/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import org.spongycastle.bcpg.S2K;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpConversionHelper;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.remote.AccountSettings;
import org.sufficientlysecure.keychain.remote.AppSettings;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProviderHelper {
    private Context mContext;
    private ContentResolver mContentResolver;

    public ProviderHelper(Context context) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
    }

    public static class NotFoundException extends Exception {
        public NotFoundException() {
        }

        public NotFoundException(String name) {
            super(name);
        }
    }

    // If we ever switch to api level 11, we can ditch this whole mess!
    public static final int FIELD_TYPE_NULL = 1;
    // this is called integer to stay coherent with the constants in Cursor (api level 11)
    public static final int FIELD_TYPE_INTEGER = 2;
    public static final int FIELD_TYPE_FLOAT = 3;
    public static final int FIELD_TYPE_STRING = 4;
    public static final int FIELD_TYPE_BLOB = 5;

    public Object getGenericData(Uri uri, String column, int type) {
        return getGenericData(uri, new String[]{column}, new int[]{type}).get(column);
    }

    public HashMap<String, Object> getGenericData(Uri uri, String[] proj, int[] types) {
        Cursor cursor = mContentResolver.query(uri, proj, null, null, null);

        HashMap<String, Object> result = new HashMap<String, Object>(proj.length);
        if (cursor != null && cursor.moveToFirst()) {
            int pos = 0;
            for (String p : proj) {
                switch (types[pos]) {
                    case FIELD_TYPE_NULL:
                        result.put(p, cursor.isNull(pos));
                        break;
                    case FIELD_TYPE_INTEGER:
                        result.put(p, cursor.getLong(pos));
                        break;
                    case FIELD_TYPE_FLOAT:
                        result.put(p, cursor.getFloat(pos));
                        break;
                    case FIELD_TYPE_STRING:
                        result.put(p, cursor.getString(pos));
                        break;
                    case FIELD_TYPE_BLOB:
                        result.put(p, cursor.getBlob(pos));
                        break;
                }
                pos += 1;
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;
    }

    public Object getUnifiedData(long masterKeyId, String column, int type) {
        return getUnifiedData(masterKeyId, new String[]{column}, new int[]{type}).get(column);
    }

    public HashMap<String, Object> getUnifiedData(long masterKeyId, String[] proj, int[] types) {
        return getGenericData(KeyRings.buildUnifiedKeyRingUri(Long.toString(masterKeyId)), proj, types);
    }

    /**
     * Find the master key id related to a given query. The id will either be extracted from the
     * query, which should work for all specific /key_rings/ queries, or will be queried if it can't.
     */
    public long extractOrGetMasterKeyId(Uri queryUri)
            throws NotFoundException {
        // try extracting from the uri first
        String firstSegment = queryUri.getPathSegments().get(1);
        if (!firstSegment.equals("find")) try {
            return Long.parseLong(firstSegment);
        } catch (NumberFormatException e) {
            // didn't work? oh well.
            Log.d(Constants.TAG, "Couldn't get masterKeyId from URI, querying...");
        }
        return getMasterKeyId(queryUri);
    }

    public long getMasterKeyId(Uri queryUri) throws NotFoundException {
        Object data = getGenericData(queryUri, KeyRings.MASTER_KEY_ID, FIELD_TYPE_INTEGER);
        if (data != null) {
            return (Long) data;
        } else {
            throw new NotFoundException();
        }
    }

    public Map<Long, PGPKeyRing> getPGPKeyRings(Uri queryUri) {
        Cursor cursor = mContentResolver.query(queryUri,
                new String[]{KeyRingData.MASTER_KEY_ID, KeyRingData.KEY_RING_DATA},
                null, null, null);

        Map<Long, PGPKeyRing> result = new HashMap<Long, PGPKeyRing>(cursor.getCount());
        if (cursor != null && cursor.moveToFirst()) do {
            long masterKeyId = cursor.getLong(0);
            byte[] data = cursor.getBlob(1);
            if (data != null) {
                result.put(masterKeyId, PgpConversionHelper.BytesToPGPKeyRing(data));
            }
        } while (cursor.moveToNext());

        if (cursor != null) {
            cursor.close();
        }

        return result;
    }

    public PGPKeyRing getPGPKeyRing(Uri queryUri) throws NotFoundException {
        Map<Long, PGPKeyRing> result = getPGPKeyRings(queryUri);
        if (result.isEmpty()) {
            throw new NotFoundException("PGPKeyRing object not found!");
        } else {
            return result.values().iterator().next();
        }
    }

    public PGPPublicKeyRing getPGPPublicKeyRingWithKeyId(long keyId)
            throws NotFoundException {
        Uri uri = KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(Long.toString(keyId));
        long masterKeyId = getMasterKeyId(uri);
        return getPGPPublicKeyRing(masterKeyId);
    }

    public PGPSecretKeyRing getPGPSecretKeyRingWithKeyId(long keyId)
            throws NotFoundException {
        Uri uri = KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(Long.toString(keyId));
        long masterKeyId = getMasterKeyId(uri);
        return getPGPSecretKeyRing(masterKeyId);
    }

    /**
     * Retrieves the actual PGPPublicKeyRing object from the database blob based on the masterKeyId
     */
    public PGPPublicKeyRing getPGPPublicKeyRing(long masterKeyId) throws NotFoundException {
        Uri queryUri = KeyRingData.buildPublicKeyRingUri(Long.toString(masterKeyId));
        return (PGPPublicKeyRing) getPGPKeyRing(queryUri);
    }

    /**
     * Retrieves the actual PGPSecretKeyRing object from the database blob based on the maserKeyId
     */
    public PGPSecretKeyRing getPGPSecretKeyRing(long masterKeyId) throws NotFoundException {
        Uri queryUri = KeyRingData.buildSecretKeyRingUri(Long.toString(masterKeyId));
        return (PGPSecretKeyRing) getPGPKeyRing(queryUri);
    }

    /**
     * Saves PGPPublicKeyRing with its keys and userIds in DB
     */
    @SuppressWarnings("unchecked")
    public void saveKeyRing(PGPPublicKeyRing keyRing) throws IOException {
        PGPPublicKey masterKey = keyRing.getPublicKey();
        long masterKeyId = masterKey.getKeyID();

        // IF there is a secret key, preserve it!
        PGPSecretKeyRing secretRing = null;
        try {
            secretRing = getPGPSecretKeyRing(masterKeyId);
        } catch (NotFoundException e) {
            Log.e(Constants.TAG, "key not found!");
        }

        // delete old version of this keyRing, which also deletes all keys and userIds on cascade
        try {
            mContentResolver.delete(KeyRingData.buildPublicKeyRingUri(Long.toString(masterKeyId)), null, null);
        } catch (UnsupportedOperationException e) {
            Log.e(Constants.TAG, "Key could not be deleted! Maybe we are creating a new one!", e);
        }

        // insert new version of this keyRing
        ContentValues values = new ContentValues();
        values.put(KeyRingData.MASTER_KEY_ID, masterKeyId);
        values.put(KeyRingData.KEY_RING_DATA, keyRing.getEncoded());
        Uri uri = KeyRingData.buildPublicKeyRingUri(Long.toString(masterKeyId));
        mContentResolver.insert(uri, values);

        // save all keys and userIds included in keyRing object in database
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

        int rank = 0;
        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
            operations.add(buildPublicKeyOperations(masterKeyId, key, rank));
            ++rank;
        }

        // get a list of owned secret keys, for verification filtering
        Map<Long, PGPKeyRing> allKeyRings = getPGPKeyRings(KeyRingData.buildSecretKeyRingUri());
        // special case: available secret keys verify themselves!
        if (secretRing != null)
            allKeyRings.put(secretRing.getSecretKey().getKeyID(), secretRing);

        // classify and order user ids. primary are moved to the front, revoked to the back,
        // otherwise the order in the keyfile is preserved.
        List<UserIdItem> uids = new ArrayList<UserIdItem>();

        for (String userId : new IterableIterator<String>(masterKey.getUserIDs())) {
            UserIdItem item = new UserIdItem();
            uids.add(item);
            item.userId = userId;

            // look through signatures for this specific key
            for (PGPSignature cert : new IterableIterator<PGPSignature>(
                    masterKey.getSignaturesForID(userId))) {
                long certId = cert.getKeyID();
                try {
                    // self signature
                    if (certId == masterKeyId) {
                        cert.init(new JcaPGPContentVerifierBuilderProvider().setProvider(
                                Constants.BOUNCY_CASTLE_PROVIDER_NAME), masterKey);
                        if (!cert.verifyCertification(userId, masterKey)) {
                            // not verified?! dang! TODO notify user? this is kinda serious...
                            Log.e(Constants.TAG, "Could not verify self signature for " + userId + "!");
                            continue;
                        }
                        // is this the first, or a more recent certificate?
                        if (item.selfCert == null ||
                                item.selfCert.getCreationTime().before(cert.getCreationTime())) {
                            item.selfCert = cert;
                            item.isPrimary = cert.getHashedSubPackets().isPrimaryUserID();
                            item.isRevoked =
                                    cert.getSignatureType() == PGPSignature.CERTIFICATION_REVOCATION;
                        }
                    }
                    // verify signatures from known private keys
                    if (allKeyRings.containsKey(certId)) {
                        // mark them as verified
                        cert.init(new JcaPGPContentVerifierBuilderProvider().setProvider(
                                Constants.BOUNCY_CASTLE_PROVIDER_NAME),
                                allKeyRings.get(certId).getPublicKey());
                        if (cert.verifyCertification(userId, masterKey)) {
                            item.trustedCerts.add(cert);
                        }
                    }
                } catch (SignatureException e) {
                    Log.e(Constants.TAG, "Signature verification failed! "
                            + PgpKeyHelper.convertKeyIdToHex(masterKey.getKeyID())
                            + " from "
                            + PgpKeyHelper.convertKeyIdToHex(cert.getKeyID()), e);
                } catch (PGPException e) {
                    Log.e(Constants.TAG, "Signature verification failed! "
                            + PgpKeyHelper.convertKeyIdToHex(masterKey.getKeyID())
                            + " from "
                            + PgpKeyHelper.convertKeyIdToHex(cert.getKeyID()), e);
                }
            }
        }

        // primary before regular before revoked (see UserIdItem.compareTo)
        // this is a stable sort, so the order of keys is otherwise preserved.
        Collections.sort(uids);
        // iterate and put into db
        for (int userIdRank = 0; userIdRank < uids.size(); userIdRank++) {
            UserIdItem item = uids.get(userIdRank);
            operations.add(buildUserIdOperations(masterKeyId, item, userIdRank));
            // no self cert is bad, but allowed by the rfc...
            if (item.selfCert != null) {
                operations.add(buildCertOperations(
                        masterKeyId, userIdRank, item.selfCert, Certs.VERIFIED_SELF));
            }
            // don't bother with trusted certs if the uid is revoked, anyways
            if (item.isRevoked) {
                continue;
            }
            for (int i = 0; i < item.trustedCerts.size(); i++) {
                operations.add(buildCertOperations(
                        masterKeyId, userIdRank, item.trustedCerts.get(i), Certs.VERIFIED_SECRET));
            }
        }

        try {
            mContentResolver.applyBatch(KeychainContract.CONTENT_AUTHORITY, operations);
        } catch (RemoteException e) {
            Log.e(Constants.TAG, "applyBatch failed!", e);
        } catch (OperationApplicationException e) {
            Log.e(Constants.TAG, "applyBatch failed!", e);
        }

        // Save the saved keyring (if any)
        if (secretRing != null) {
            saveKeyRing(secretRing);
        }

    }

    private static class UserIdItem implements Comparable<UserIdItem> {
        String userId;
        boolean isPrimary = false;
        boolean isRevoked = false;
        PGPSignature selfCert;
        List<PGPSignature> trustedCerts = new ArrayList<PGPSignature>();

        @Override
        public int compareTo(UserIdItem o) {
            // if one key is primary but the other isn't, the primary one always comes first
            if (isPrimary != o.isPrimary) {
                return isPrimary ? -1 : 1;
            }
            // revoked keys always come last!
            if (isRevoked != o.isRevoked) {
                return isRevoked ? 1 : -1;
            }
            return 0;
        }
    }

    /**
     * Saves a PGPSecretKeyRing in the DB. This will only work if a corresponding public keyring
     * is already in the database!
     */
    public void saveKeyRing(PGPSecretKeyRing keyRing) throws IOException {
        long masterKeyId = keyRing.getPublicKey().getKeyID();

        {
            Uri uri = Keys.buildKeysUri(Long.toString(masterKeyId));

            // first, mark all keys as not available
            ContentValues values = new ContentValues();
            values.put(Keys.HAS_SECRET, 0);
            mContentResolver.update(uri, values, null, null);

            values.put(Keys.HAS_SECRET, 1);
            // then, mark exactly the keys we have available
            for (PGPSecretKey sub : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
                // Set to 1, except if the encryption type is GNU_DUMMY_S2K
                if(sub.getS2K().getType() != S2K.GNU_DUMMY_S2K) {
                    mContentResolver.update(uri, values, Keys.KEY_ID + " = ?", new String[]{
                            Long.toString(sub.getKeyID())
                    });
                }
            }
            // this implicitly leaves all keys which were not in the secret key ring
            // with has_secret = 0
        }

        // save secret keyring
        {
            ContentValues values = new ContentValues();
            values.put(KeyRingData.MASTER_KEY_ID, masterKeyId);
            values.put(KeyRingData.KEY_RING_DATA, keyRing.getEncoded());
            // insert new version of this keyRing
            Uri uri = KeyRingData.buildSecretKeyRingUri(Long.toString(masterKeyId));
            mContentResolver.insert(uri, values);
        }

    }

    /**
     * Saves (or updates) a pair of public and secret KeyRings in the database
     */
    public void saveKeyRing(PGPPublicKeyRing pubRing, PGPSecretKeyRing privRing) throws IOException {
        long masterKeyId = pubRing.getPublicKey().getKeyID();

        // delete secret keyring (so it isn't unnecessarily saved by public-saveKeyRing below)
        mContentResolver.delete(KeyRingData.buildSecretKeyRingUri(Long.toString(masterKeyId)), null, null);

        // save public keyring
        saveKeyRing(pubRing);
        saveKeyRing(privRing);
    }

    /**
     * Build ContentProviderOperation to add PGPPublicKey to database corresponding to a keyRing
     */
    private ContentProviderOperation
    buildPublicKeyOperations(long masterKeyId, PGPPublicKey key, int rank) throws IOException {

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
     * Build ContentProviderOperation to add PGPPublicKey to database corresponding to a keyRing
     */
    private ContentProviderOperation
    buildCertOperations(long masterKeyId, int rank, PGPSignature cert, int verified) throws IOException {
        ContentValues values = new ContentValues();
        values.put(Certs.MASTER_KEY_ID, masterKeyId);
        values.put(Certs.RANK, rank);
        values.put(Certs.KEY_ID_CERTIFIER, cert.getKeyID());
        values.put(Certs.TYPE, cert.getSignatureType());
        values.put(Certs.CREATION, cert.getCreationTime().getTime() / 1000);
        values.put(Certs.VERIFIED, verified);
        values.put(Certs.DATA, cert.getEncoded());

        Uri uri = Certs.buildCertsUri(Long.toString(masterKeyId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * Build ContentProviderOperation to add PublicUserIds to database corresponding to a keyRing
     */
    private ContentProviderOperation
    buildUserIdOperations(long masterKeyId, UserIdItem item, int rank) {
        ContentValues values = new ContentValues();
        values.put(UserIds.MASTER_KEY_ID, masterKeyId);
        values.put(UserIds.USER_ID, item.userId);
        values.put(UserIds.IS_PRIMARY, item.isPrimary);
        values.put(UserIds.IS_REVOKED, item.isRevoked);
        values.put(UserIds.RANK, rank);

        Uri uri = UserIds.buildUserIdsUri(Long.toString(masterKeyId));

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    private String getKeyRingAsArmoredString(byte[] data) throws IOException {
        Object keyRing = null;
        if (data != null) {
            keyRing = PgpConversionHelper.BytesToPGPKeyRing(data);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArmoredOutputStream aos = new ArmoredOutputStream(bos);
        aos.setHeader("Version", PgpHelper.getFullVersion(mContext));

        if (keyRing instanceof PGPSecretKeyRing) {
            aos.write(((PGPSecretKeyRing) keyRing).getEncoded());
        } else if (keyRing instanceof PGPPublicKeyRing) {
            aos.write(((PGPPublicKeyRing) keyRing).getEncoded());
        }
        aos.close();

        String armoredKey = bos.toString("UTF-8");

        Log.d(Constants.TAG, "armoredKey:" + armoredKey);

        return armoredKey;
    }

    public String getKeyRingAsArmoredString(Uri uri)
            throws NotFoundException, IOException {
        byte[] data = (byte[]) getGenericData(
                uri, KeyRingData.KEY_RING_DATA, ProviderHelper.FIELD_TYPE_BLOB);
        return getKeyRingAsArmoredString(data);
    }

    /**
     * TODO: currently not used, but will be needed to upload many keys at once!
     *
     * @param masterKeyIds
     * @return
     * @throws IOException
     */
    public ArrayList<String> getKeyRingsAsArmoredString(long[] masterKeyIds)
            throws IOException {
        ArrayList<String> output = new ArrayList<String>();

        if (masterKeyIds == null || masterKeyIds.length == 0) {
            Log.e(Constants.TAG, "No master keys given!");
            return output;
        }

        // Build a cursor for the selected masterKeyIds
        Cursor cursor;
        {
            String inMasterKeyList = KeyRingData.MASTER_KEY_ID + " IN (";
            for (int i = 0; i < masterKeyIds.length; ++i) {
                if (i != 0) {
                    inMasterKeyList += ", ";
                }
                inMasterKeyList += DatabaseUtils.sqlEscapeString("" + masterKeyIds[i]);
            }
            inMasterKeyList += ")";

            cursor = mContentResolver.query(KeyRingData.buildPublicKeyRingUri(), new String[]{
                    KeyRingData._ID, KeyRingData.MASTER_KEY_ID, KeyRingData.KEY_RING_DATA
            }, inMasterKeyList, null, null);
        }

        if (cursor != null) {
            int masterIdCol = cursor.getColumnIndex(KeyRingData.MASTER_KEY_ID);
            int dataCol = cursor.getColumnIndex(KeyRingData.KEY_RING_DATA);
            if (cursor.moveToFirst()) {
                do {
                    Log.d(Constants.TAG, "masterKeyId: " + cursor.getLong(masterIdCol));

                    byte[] data = cursor.getBlob(dataCol);

                    // get actual keyring data blob and write it to ByteArrayOutputStream
                    try {
                        output.add(getKeyRingAsArmoredString(data));
                    } catch (IOException e) {
                        Log.e(Constants.TAG, "IOException", e);
                    }
                } while (cursor.moveToNext());
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        if (output.size() > 0) {
            return output;
        } else {
            return null;
        }
    }

    public ArrayList<String> getRegisteredApiApps() {
        Cursor cursor = mContentResolver.query(ApiApps.CONTENT_URI, null, null, null, null);

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

    private ContentValues contentValueForApiApps(AppSettings appSettings) {
        ContentValues values = new ContentValues();
        values.put(ApiApps.PACKAGE_NAME, appSettings.getPackageName());
        values.put(ApiApps.PACKAGE_SIGNATURE, appSettings.getPackageSignature());
        return values;
    }

    private ContentValues contentValueForApiAccounts(AccountSettings accSettings) {
        ContentValues values = new ContentValues();
        values.put(KeychainContract.ApiAccounts.ACCOUNT_NAME, accSettings.getAccountName());
        values.put(KeychainContract.ApiAccounts.KEY_ID, accSettings.getKeyId());
        values.put(KeychainContract.ApiAccounts.COMPRESSION, accSettings.getCompression());
        values.put(KeychainContract.ApiAccounts.ENCRYPTION_ALGORITHM, accSettings.getEncryptionAlgorithm());
        values.put(KeychainContract.ApiAccounts.HASH_ALORITHM, accSettings.getHashAlgorithm());
        return values;
    }

    public void insertApiApp(AppSettings appSettings) {
        mContentResolver.insert(KeychainContract.ApiApps.CONTENT_URI,
                contentValueForApiApps(appSettings));
    }

    public void insertApiAccount(Uri uri, AccountSettings accSettings) {
        mContentResolver.insert(uri, contentValueForApiAccounts(accSettings));
    }

    public void updateApiAccount(AccountSettings accSettings, Uri uri) {
        if (mContentResolver.update(uri, contentValueForApiAccounts(accSettings), null,
                null) <= 0) {
            throw new RuntimeException();
        }
    }

    /**
     * Must be an uri pointing to an account
     *
     * @param uri
     * @return
     */
    public AppSettings getApiAppSettings(Uri uri) {
        AppSettings settings = null;

        Cursor cur = mContentResolver.query(uri, null, null, null, null);
        if (cur != null && cur.moveToFirst()) {
            settings = new AppSettings();
            settings.setPackageName(cur.getString(
                    cur.getColumnIndex(KeychainContract.ApiApps.PACKAGE_NAME)));
            settings.setPackageSignature(cur.getBlob(
                    cur.getColumnIndex(KeychainContract.ApiApps.PACKAGE_SIGNATURE)));
        }

        return settings;
    }

    public AccountSettings getApiAccountSettings(Uri accountUri) {
        AccountSettings settings = null;

        Cursor cur = mContentResolver.query(accountUri, null, null, null, null);
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

    public Set<Long> getAllKeyIdsForApp(Uri uri) {
        Set<Long> keyIds = new HashSet<Long>();

        Cursor cursor = mContentResolver.query(uri, null, null, null, null);
        if (cursor != null) {
            int keyIdColumn = cursor.getColumnIndex(KeychainContract.ApiAccounts.KEY_ID);
            while (cursor.moveToNext()) {
                keyIds.add(cursor.getLong(keyIdColumn));
            }
        }

        return keyIds;
    }

    public byte[] getApiAppSignature(String packageName) {
        Uri queryUri = ApiApps.buildByPackageNameUri(packageName);

        String[] projection = new String[]{ApiApps.PACKAGE_SIGNATURE};

        Cursor cursor = mContentResolver.query(queryUri, projection, null, null, null);

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
