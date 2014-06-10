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
import android.net.Uri;
import android.os.RemoteException;
import android.support.v4.util.LongSparseArray;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.service.OperationResultParcel;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogType;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogLevel;
import org.sufficientlysecure.keychain.service.OperationResultParcel.OperationLog;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.pgp.WrappedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.WrappedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.WrappedSignature;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** This class contains high level methods for database access. Despite its
 * name, it is not only a helper but actually the main interface for all
 * synchronous database operations.
 *
 * Operations in this class write logs (TODO). These can be obtained from the
 * OperationResultParcel return values directly, but are also accumulated over
 * the lifetime of the executing ProviderHelper object unless the resetLog()
 * method is called to start a new one specifically.
 *
 */
public class ProviderHelper {
    private final Context mContext;
    private final ContentResolver mContentResolver;
    private OperationLog mLog;
    private int mIndent;

    public ProviderHelper(Context context) {
        this(context, new OperationLog(), 0);
    }

    public ProviderHelper(Context context, OperationLog log, int indent) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mLog = log;
        mIndent = indent;
    }

    public void resetLog() {
        if(mLog != null) {
            // Start a new log (leaving the old one intact)
            mLog = new OperationLog();
            mIndent = 0;
        }
    }

    public OperationLog getLog() {
        return mLog;
    }

    public static class NotFoundException extends Exception {
        public NotFoundException() {
        }

        public NotFoundException(String name) {
            super(name);
        }
    }

    public void log(LogLevel level, LogType type) {
        if(mLog != null) {
            mLog.add(level, type, null, mIndent);
        }
    }
    public void log(LogLevel level, LogType type, String[] parameters) {
        if(mLog != null) {
            mLog.add(level, type, parameters, mIndent);
        }
    }

    // If we ever switch to api level 11, we can ditch this whole mess!
    public static final int FIELD_TYPE_NULL = 1;
    // this is called integer to stay coherent with the constants in Cursor (api level 11)
    public static final int FIELD_TYPE_INTEGER = 2;
    public static final int FIELD_TYPE_FLOAT = 3;
    public static final int FIELD_TYPE_STRING = 4;
    public static final int FIELD_TYPE_BLOB = 5;

    public Object getGenericData(Uri uri, String column, int type) throws NotFoundException {
        return getGenericData(uri, new String[]{column}, new int[]{type}).get(column);
    }

    public HashMap<String, Object> getGenericData(Uri uri, String[] proj, int[] types)
            throws NotFoundException {
        Cursor cursor = mContentResolver.query(uri, proj, null, null, null);

        try {
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

            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public Object getUnifiedData(long masterKeyId, String column, int type)
            throws NotFoundException {
        return getUnifiedData(masterKeyId, new String[]{column}, new int[]{type}).get(column);
    }

    public HashMap<String, Object> getUnifiedData(long masterKeyId, String[] proj, int[] types)
            throws NotFoundException {
        return getGenericData(KeyRings.buildUnifiedKeyRingUri(masterKeyId), proj, types);
    }

    private LongSparseArray<UncachedPublicKey> getUncachedMasterKeys(Uri queryUri) {
        Cursor cursor = mContentResolver.query(queryUri,
                new String[]{KeyRingData.MASTER_KEY_ID, KeyRingData.KEY_RING_DATA},
                null, null, null);

        LongSparseArray<UncachedPublicKey> result =
                new LongSparseArray<UncachedPublicKey>(cursor.getCount());
        try {
            if (cursor != null && cursor.moveToFirst()) do {
                long masterKeyId = cursor.getLong(0);
                byte[] data = cursor.getBlob(1);
                if (data != null) {
                    try {
                        result.put(masterKeyId,
                                UncachedKeyRing.decodeFromData(data).getPublicKey());
                    } catch(PgpGeneralException e) {
                        Log.e(Constants.TAG, "Error parsing keyring, skipping " + masterKeyId, e);
                    } catch(IOException e) {
                        Log.e(Constants.TAG, "IO error, skipping keyring" + masterKeyId, e);
                    }
                }
            } while (cursor.moveToNext());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }

    public CachedPublicKeyRing getCachedPublicKeyRing(Uri queryUri) {
        return new CachedPublicKeyRing(this, queryUri);
    }

    public WrappedPublicKeyRing getWrappedPublicKeyRing(long id) throws NotFoundException {
        return (WrappedPublicKeyRing) getWrappedKeyRing(KeyRings.buildUnifiedKeyRingUri(id), false);
    }

    public WrappedPublicKeyRing getWrappedPublicKeyRing(Uri queryUri) throws NotFoundException {
        return (WrappedPublicKeyRing) getWrappedKeyRing(queryUri, false);
    }

    public WrappedSecretKeyRing getWrappedSecretKeyRing(long id) throws NotFoundException {
        return (WrappedSecretKeyRing) getWrappedKeyRing(KeyRings.buildUnifiedKeyRingUri(id), true);
    }

    public WrappedSecretKeyRing getWrappedSecretKeyRing(Uri queryUri) throws NotFoundException {
        return (WrappedSecretKeyRing) getWrappedKeyRing(queryUri, true);
    }

    private KeyRing getWrappedKeyRing(Uri queryUri, boolean secret) throws NotFoundException {
        Cursor cursor = mContentResolver.query(queryUri,
                new String[]{
                        // we pick from cache only information that is not easily available from keyrings
                        KeyRings.HAS_ANY_SECRET, KeyRings.VERIFIED,
                        // and of course, ring data
                        secret ? KeyRings.PRIVKEY_DATA : KeyRings.PUBKEY_DATA
                }, null, null, null
        );
        try {
            if (cursor != null && cursor.moveToFirst()) {

                boolean hasAnySecret = cursor.getInt(0) > 0;
                int verified = cursor.getInt(1);
                byte[] blob = cursor.getBlob(2);
                if(secret &! hasAnySecret) {
                    throw new NotFoundException("Secret key not available!");
                }
                return secret
                        ? new WrappedSecretKeyRing(blob, true, verified)
                        : new WrappedPublicKeyRing(blob, hasAnySecret, verified);
            } else {
                throw new NotFoundException("Key not found!");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Saves PGPPublicKeyRing with its keys and userIds in DB
     */
    @SuppressWarnings("unchecked")
    public OperationResultParcel savePublicKeyRing(UncachedKeyRing keyRing) {
        if (keyRing.isSecret()) {
            log(LogLevel.ERROR, LogType.MSG_IP_BAD_TYPE_SECRET);
            return new OperationResultParcel(1, mLog);
        }

        long masterKeyId = keyRing.getMasterKeyId();
        log(LogLevel.START, LogType.MSG_IP,
                new String[]{ PgpKeyHelper.convertKeyIdToHex(masterKeyId) });
        mIndent += 1;

        // Canonicalize this key, to assert a number of assumptions made about it.
        keyRing = keyRing.canonicalize(mLog);

        UncachedPublicKey masterKey = keyRing.getPublicKey();

        // IF there is a secret key, preserve it!
        UncachedKeyRing secretRing;
        try {
            secretRing = getWrappedSecretKeyRing(masterKeyId).getUncached();
            log(LogLevel.DEBUG, LogType.MSG_IP_PRESERVING_SECRET);
        } catch (NotFoundException e) {
            secretRing = null;
        }

        // delete old version of this keyRing, which also deletes all keys and userIds on cascade
        try {
            mContentResolver.delete(KeyRingData.buildPublicKeyRingUri(Long.toString(masterKeyId)), null, null);
            log(LogLevel.DEBUG, LogType.MSG_IP_DELETE_OLD_OK);
        } catch (UnsupportedOperationException e) {
            Log.e(Constants.TAG, "Key could not be deleted! Maybe we are creating a new one!", e);
            log(LogLevel.DEBUG, LogType.MSG_IP_DELETE_OLD_FAIL);
        }

        try {

            // save all keys and userIds included in keyRing object in database
            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

            log(LogLevel.INFO, LogType.MSG_IP_INSERT_KEYRING);
            { // insert keyring
                // insert new version of this keyRing
                ContentValues values = new ContentValues();
                values.put(KeyRingData.MASTER_KEY_ID, masterKeyId);
                try {
                    values.put(KeyRingData.KEY_RING_DATA, keyRing.getEncoded());
                } catch (IOException e) {
                    log(LogLevel.ERROR, LogType.MSG_IP_ENCODE_FAIL);
                    return new OperationResultParcel(1, mLog);
                }

                Uri uri = KeyRingData.buildPublicKeyRingUri(Long.toString(masterKeyId));
                operations.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
            }

            log(LogLevel.INFO, LogType.MSG_IP_INSERT_SUBKEYS);
            mIndent += 1;
            { // insert subkeys
                Uri uri = Keys.buildKeysUri(Long.toString(masterKeyId));
                int rank = 0;
                for (UncachedPublicKey key : new IterableIterator<UncachedPublicKey>(keyRing.getPublicKeys())) {
                    log(LogLevel.DEBUG, LogType.MSG_IP_SUBKEY, new String[]{
                            PgpKeyHelper.convertKeyIdToHex(key.getKeyId())
                    });
                    mIndent += 1;

                    ContentValues values = new ContentValues();
                    values.put(Keys.MASTER_KEY_ID, masterKeyId);
                    values.put(Keys.RANK, rank);

                    values.put(Keys.KEY_ID, key.getKeyId());
                    values.put(Keys.KEY_SIZE, key.getBitStrength());
                    values.put(Keys.ALGORITHM, key.getAlgorithm());
                    values.put(Keys.FINGERPRINT, key.getFingerprint());

                    boolean c = key.canCertify(), e = key.canEncrypt(), s = key.canSign();
                    values.put(Keys.CAN_CERTIFY, c);
                    values.put(Keys.CAN_ENCRYPT, e);
                    values.put(Keys.CAN_SIGN, s);
                    values.put(Keys.IS_REVOKED, key.isRevoked());
                    if (c) {
                        if (e) {
                            log(LogLevel.DEBUG,s ? LogType.MSG_IP_SUBKEY_FLAGS_CES
                                                 : LogType.MSG_IP_SUBKEY_FLAGS_CEX, null);
                        } else {
                            log(LogLevel.DEBUG, s ? LogType.MSG_IP_SUBKEY_FLAGS_CXS
                                                  : LogType.MSG_IP_SUBKEY_FLAGS_CXX, null);
                        }
                    } else {
                        if (e) {
                            log(LogLevel.DEBUG, s ? LogType.MSG_IP_SUBKEY_FLAGS_XES
                                                  : LogType.MSG_IP_SUBKEY_FLAGS_XEX, null);
                        } else {
                            log(LogLevel.DEBUG, s ? LogType.MSG_IP_SUBKEY_FLAGS_XXS
                                                  : LogType.MSG_IP_SUBKEY_FLAGS_XXX, null);
                        }
                    }

                    Date creation = key.getCreationTime();
                    values.put(Keys.CREATION, creation.getTime() / 1000);
                    if (creation.after(new Date())) {
                        log(LogLevel.ERROR, LogType.MSG_IP_SUBKEY_FUTURE, new String[] {
                                creation.toString()
                        });
                        return new OperationResultParcel(1, mLog);
                    }
                    Date expiryDate = key.getExpiryTime();
                    if (expiryDate != null) {
                        values.put(Keys.EXPIRY, expiryDate.getTime() / 1000);
                        if (key.isExpired()) {
                            log(LogLevel.INFO, LogType.MSG_IP_SUBKEY_EXPIRED, new String[] {
                                    expiryDate.toString()
                            });
                        } else {
                            log(LogLevel.DEBUG, LogType.MSG_IP_SUBKEY_EXPIRES, new String[] {
                                    expiryDate.toString()
                            });
                        }
                    }

                    operations.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
                    ++rank;
                    mIndent -= 1;
                }
            }
            mIndent -= 1;

            log(LogLevel.DEBUG, LogType.MSG_IP_TRUST_RETRIEVE);
            // get a list of owned secret keys, for verification filtering
            LongSparseArray<UncachedPublicKey> trustedKeys =
                    getUncachedMasterKeys(KeyRingData.buildSecretKeyRingUri());
            // special case: available secret keys verify themselves!
            if (secretRing != null) {
                trustedKeys.put(secretRing.getMasterKeyId(), secretRing.getPublicKey());
                log(LogLevel.INFO, LogType.MSG_IP_TRUST_USING_SEC, new String[]{
                        Integer.toString(trustedKeys.size())
                });
            } else {
                log(LogLevel.INFO, LogType.MSG_IP_TRUST_USING, new String[] {
                    Integer.toString(trustedKeys.size())
                });
            }

            // classify and order user ids. primary are moved to the front, revoked to the back,
            // otherwise the order in the keyfile is preserved.
            log(LogLevel.DEBUG, LogType.MSG_IP_UID_CLASSIFYING);
            mIndent += 1;
            List<UserIdItem> uids = new ArrayList<UserIdItem>();
            for (String userId : new IterableIterator<String>(
                    masterKey.getUnorderedUserIds().iterator())) {
                UserIdItem item = new UserIdItem();
                uids.add(item);
                item.userId = userId;

                int unknownCerts = 0;

                log(LogLevel.INFO, LogType.MSG_IP_UID_PROCESSING, new String[] { userId });
                mIndent += 1;
                // look through signatures for this specific key
                for (WrappedSignature cert : new IterableIterator<WrappedSignature>(
                        masterKey.getSignaturesForId(userId))) {
                    long certId = cert.getKeyId();
                    try {
                        // self signature
                        if (certId == masterKeyId) {
                            cert.init(masterKey);
                            if (!cert.verifySignature(masterKey, userId)) {
                                // Bad self certification? That's kinda bad...
                                log(LogLevel.ERROR, LogType.MSG_IP_UID_SELF_BAD);
                                return new OperationResultParcel(1, mLog);
                            }

                            // if we already have a cert..
                            if (item.selfCert != null) {
                                // ..is this perchance a more recent one?
                                if (item.selfCert.getCreationTime().before(cert.getCreationTime())) {
                                    log(LogLevel.DEBUG, LogType.MSG_IP_UID_SELF_NEWER);
                                } else {
                                    log(LogLevel.DEBUG, LogType.MSG_IP_UID_SELF_IGNORING_OLD);
                                    continue;
                                }
                            } else {
                                log(LogLevel.DEBUG, LogType.MSG_IP_UID_SELF_GOOD);
                            }

                            // save certificate as primary self-cert
                            item.selfCert = cert;
                            item.isPrimary = cert.isPrimaryUserId();
                            if (cert.isRevocation()) {
                                item.isRevoked = true;
                                log(LogLevel.INFO, LogType.MSG_IP_UID_REVOKED);
                            } else {
                                item.isRevoked = false;
                            }

                        }

                        // verify signatures from known private keys
                        if (trustedKeys.indexOfKey(certId) >= 0) {
                            UncachedPublicKey trustedKey = trustedKeys.get(certId);
                            cert.init(trustedKey);
                            if (cert.verifySignature(masterKey, userId)) {
                                item.trustedCerts.add(cert);
                                log(LogLevel.INFO, LogType.MSG_IP_UID_CERT_GOOD, new String[] {
                                    PgpKeyHelper.convertKeyIdToHex(trustedKey.getKeyId())
                                });
                            } else {
                                log(LogLevel.WARN, LogType.MSG_IP_UID_CERT_BAD);
                            }
                        }

                        unknownCerts += 1;

                    } catch (PgpGeneralException e) {
                        log(LogLevel.WARN, LogType.MSG_IP_UID_CERT_ERROR, new String[]{
                                PgpKeyHelper.convertKeyIdToHex(cert.getKeyId())
                        });
                    }
                }
                mIndent -= 1;

                if (unknownCerts > 0) {
                    log(LogLevel.DEBUG, LogType.MSG_IP_UID_CERTS_UNKNOWN, new String[] {
                            Integer.toString(unknownCerts)
                    });
                }

            }
            mIndent -= 1;

            log(LogLevel.INFO, LogType.MSG_IP_UID_INSERT);
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

            log(LogLevel.DEBUG, LogType.MSG_IP_APPLY_BATCH);
            mContentResolver.applyBatch(KeychainContract.CONTENT_AUTHORITY, operations);
        } catch (IOException e) {
            log(LogLevel.ERROR, LogType.MSG_IP_FAIL_IO_EXC);
            Log.e(Constants.TAG, "IOException during import", e);
            mIndent -= 1;
            return new OperationResultParcel(1, mLog);
        } catch (RemoteException e) {
            log(LogLevel.ERROR, LogType.MSG_IP_FAIL_REMOTE_EX);
            Log.e(Constants.TAG, "RemoteException during import", e);
            mIndent -= 1;
            return new OperationResultParcel(1, mLog);
        } catch (OperationApplicationException e) {
            log(LogLevel.ERROR, LogType.MSG_IP_FAIL_OP_EX);
            Log.e(Constants.TAG, "OperationApplicationException during import", e);
            mIndent -= 1;
            return new OperationResultParcel(1, mLog);
        }

        // Save the saved keyring (if any)
        if (secretRing != null) {
            log(LogLevel.DEBUG, LogType.MSG_IP_REINSERT_SECRET);
            mIndent += 1;
            saveSecretKeyRing(secretRing);
            mIndent -= 1;
        }

        log(LogLevel.OK, LogType.MSG_IP_SUCCESS);
        mIndent -= 1;
        return new OperationResultParcel(0, mLog);

    }

    private static class UserIdItem implements Comparable<UserIdItem> {
        String userId;
        boolean isPrimary = false;
        boolean isRevoked = false;
        WrappedSignature selfCert;
        List<WrappedSignature> trustedCerts = new ArrayList<WrappedSignature>();

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
    public OperationResultParcel saveSecretKeyRing(UncachedKeyRing keyRing) {
        if (!keyRing.isSecret()) {
            log(LogLevel.ERROR, LogType.MSG_IS_BAD_TYPE_PUBLIC);
            return new OperationResultParcel(1, mLog);
        }

        long masterKeyId = keyRing.getMasterKeyId();
        log(LogLevel.START, LogType.MSG_IS,
                new String[]{PgpKeyHelper.convertKeyIdToHex(masterKeyId)});

        // save secret keyring
        try {
            ContentValues values = new ContentValues();
            values.put(KeyRingData.MASTER_KEY_ID, masterKeyId);
            values.put(KeyRingData.KEY_RING_DATA, keyRing.getEncoded());
            // insert new version of this keyRing
            Uri uri = KeyRingData.buildSecretKeyRingUri(Long.toString(masterKeyId));
            mContentResolver.insert(uri, values);
        } catch (IOException e) {
            Log.e(Constants.TAG, "Failed to encode key!", e);
            log(LogLevel.ERROR, LogType.MSG_IS_IO_EXCPTION);
            return new OperationResultParcel(1, mLog);
        }

        {
            Uri uri = Keys.buildKeysUri(Long.toString(masterKeyId));

            // first, mark all keys as not available
            ContentValues values = new ContentValues();
            values.put(Keys.HAS_SECRET, 0);
            mContentResolver.update(uri, values, null, null);

            values.put(Keys.HAS_SECRET, 1);
            // then, mark exactly the keys we have available
            log(LogLevel.INFO, LogType.MSG_IS_IMPORTING_SUBKEYS);
            mIndent += 1;
            Set<Long> available = keyRing.getAvailableSubkeys();
            for (UncachedPublicKey sub :
                    new IterableIterator<UncachedPublicKey>(keyRing.getPublicKeys())) {
                long id = sub.getKeyId();
                if(available.contains(id)) {
                    int upd = mContentResolver.update(uri, values, Keys.KEY_ID + " = ?",
                            new String[] { Long.toString(id) });
                    if (upd == 1) {
                        log(LogLevel.DEBUG, LogType.MSG_IS_SUBKEY_OK, new String[]{
                                PgpKeyHelper.convertKeyIdToHex(id)
                        });
                    } else {
                        log(LogLevel.WARN, LogType.MSG_IS_SUBKEY_NONEXISTENT, new String[]{
                                PgpKeyHelper.convertKeyIdToHex(id)
                        });
                    }
                } else {
                    log(LogLevel.INFO, LogType.MSG_IS_SUBKEY_STRIPPED, new String[]{
                            PgpKeyHelper.convertKeyIdToHex(id)
                    });
                }
            }
            mIndent -= 1;

            // this implicitly leaves all keys which were not in the secret key ring
            // with has_secret = 0
        }

        log(LogLevel.OK, LogType.MSG_IS_SUCCESS);
        return new OperationResultParcel(0, mLog);

    }

    /**
     * Saves (or updates) a pair of public and secret KeyRings in the database
     */
    public void saveKeyRing(UncachedKeyRing pubRing, UncachedKeyRing secRing) throws IOException {
        long masterKeyId = pubRing.getPublicKey().getKeyId();

        // delete secret keyring (so it isn't unnecessarily saved by public-savePublicKeyRing below)
        mContentResolver.delete(KeyRingData.buildSecretKeyRingUri(Long.toString(masterKeyId)), null, null);

        // save public keyring
        savePublicKeyRing(pubRing);
        saveSecretKeyRing(secRing);
    }

    /**
     * Build ContentProviderOperation to add PGPPublicKey to database corresponding to a keyRing
     */
    private ContentProviderOperation
    buildCertOperations(long masterKeyId, int rank, WrappedSignature cert, int verified)
            throws IOException {
        ContentValues values = new ContentValues();
        values.put(Certs.MASTER_KEY_ID, masterKeyId);
        values.put(Certs.RANK, rank);
        values.put(Certs.KEY_ID_CERTIFIER, cert.getKeyId());
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

    private String getKeyRingAsArmoredString(byte[] data) throws IOException, PgpGeneralException {
        UncachedKeyRing keyRing = UncachedKeyRing.decodeFromData(data);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        keyRing.encodeArmored(bos, PgpHelper.getFullVersion(mContext));
        String armoredKey = bos.toString("UTF-8");

        Log.d(Constants.TAG, "armoredKey:" + armoredKey);

        return armoredKey;
    }

    public String getKeyRingAsArmoredString(Uri uri)
            throws NotFoundException, IOException, PgpGeneralException {
        byte[] data = (byte[]) getGenericData(
                uri, KeyRingData.KEY_RING_DATA, ProviderHelper.FIELD_TYPE_BLOB);
        return getKeyRingAsArmoredString(data);
    }

    public ArrayList<String> getRegisteredApiApps() {
        Cursor cursor = mContentResolver.query(ApiApps.CONTENT_URI, null, null, null, null);

        ArrayList<String> packageNames = new ArrayList<String>();
        try {
            if (cursor != null) {
                int packageNameCol = cursor.getColumnIndex(ApiApps.PACKAGE_NAME);
                if (cursor.moveToFirst()) {
                    do {
                        packageNames.add(cursor.getString(packageNameCol));
                    } while (cursor.moveToNext());
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
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

        Cursor cursor = mContentResolver.query(uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                settings = new AppSettings();
                settings.setPackageName(cursor.getString(
                        cursor.getColumnIndex(KeychainContract.ApiApps.PACKAGE_NAME)));
                settings.setPackageSignature(cursor.getBlob(
                        cursor.getColumnIndex(KeychainContract.ApiApps.PACKAGE_SIGNATURE)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return settings;
    }

    public AccountSettings getApiAccountSettings(Uri accountUri) {
        AccountSettings settings = null;

        Cursor cursor = mContentResolver.query(accountUri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                settings = new AccountSettings();

                settings.setAccountName(cursor.getString(
                        cursor.getColumnIndex(KeychainContract.ApiAccounts.ACCOUNT_NAME)));
                settings.setKeyId(cursor.getLong(
                        cursor.getColumnIndex(KeychainContract.ApiAccounts.KEY_ID)));
                settings.setCompression(cursor.getInt(
                        cursor.getColumnIndexOrThrow(KeychainContract.ApiAccounts.COMPRESSION)));
                settings.setHashAlgorithm(cursor.getInt(
                        cursor.getColumnIndexOrThrow(KeychainContract.ApiAccounts.HASH_ALORITHM)));
                settings.setEncryptionAlgorithm(cursor.getInt(
                        cursor.getColumnIndexOrThrow(KeychainContract.ApiAccounts.ENCRYPTION_ALGORITHM)));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return settings;
    }

    public Set<Long> getAllKeyIdsForApp(Uri uri) {
        Set<Long> keyIds = new HashSet<Long>();

        Cursor cursor = mContentResolver.query(uri, null, null, null, null);
        try {
            if (cursor != null) {
                int keyIdColumn = cursor.getColumnIndex(KeychainContract.ApiAccounts.KEY_ID);
                while (cursor.moveToNext()) {
                    keyIds.add(cursor.getLong(keyIdColumn));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return keyIds;
    }

    public byte[] getApiAppSignature(String packageName) {
        Uri queryUri = ApiApps.buildByPackageNameUri(packageName);

        String[] projection = new String[]{ApiApps.PACKAGE_SIGNATURE};

        Cursor cursor = mContentResolver.query(queryUri, projection, null, null, null);
        try {
            byte[] signature = null;
            if (cursor != null && cursor.moveToFirst()) {
                int signatureCol = 0;

                signature = cursor.getBlob(signatureCol);
            }
            return signature;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
