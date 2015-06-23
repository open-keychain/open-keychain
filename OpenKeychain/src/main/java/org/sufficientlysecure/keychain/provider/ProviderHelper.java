/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.ImportExportOperation;
import org.sufficientlysecure.keychain.operations.results.ConsolidateResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.PgpConstants;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.pgp.WrappedSignature;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAllowedKeys;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.remote.AccountSettings;
import org.sufficientlysecure.keychain.remote.AppSettings;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;
import org.sufficientlysecure.keychain.util.ParcelableFileCache.IteratorWithSize;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.ProgressFixedScaler;
import org.sufficientlysecure.keychain.util.ProgressScaler;
import org.sufficientlysecure.keychain.util.Utf8Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This class contains high level methods for database access. Despite its
 * name, it is not only a helper but actually the main interface for all
 * synchronous database operations.
 * <p/>
 * Operations in this class write logs. These can be obtained from the
 * OperationResultParcel return values directly, but are also accumulated over
 * the lifetime of the executing ProviderHelper object unless the resetLog()
 * method is called to start a new one specifically.
 */
public class ProviderHelper {
    private final Context mContext;
    private final ContentResolver mContentResolver;
    private OperationLog mLog;
    private int mIndent;

    public ProviderHelper(Context context) {
        this(context, new OperationLog(), 0);
    }

    public ProviderHelper(Context context, OperationLog log) {
        this(context, log, 0);
    }

    public ProviderHelper(Context context, OperationLog log, int indent) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mLog = log;
        mIndent = indent;
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

    public void log(LogType type) {
        if (mLog != null) {
            mLog.add(type, mIndent);
        }
    }

    public void log(LogType type, Object... parameters) {
        if (mLog != null) {
            mLog.add(type, mIndent, parameters);
        }
    }

    public void clearLog() {
        mLog = new OperationLog();
    }

    // If we ever switch to api level 11, we can ditch this whole mess!
    public static final int FIELD_TYPE_NULL = 1;
    // this is called integer to stay coherent with the constants in Cursor (api level 11)
    public static final int FIELD_TYPE_INTEGER = 2;
    public static final int FIELD_TYPE_FLOAT = 3;
    public static final int FIELD_TYPE_STRING = 4;
    public static final int FIELD_TYPE_BLOB = 5;

    public Object getGenericData(Uri uri, String column, int type) throws NotFoundException {
        Object result = getGenericData(uri, new String[]{column}, new int[]{type}, null).get(column);
        if (result == null) {
            throw new NotFoundException();
        }
        return result;
    }

    public Object getGenericData(Uri uri, String column, int type, String selection)
            throws NotFoundException {
        return getGenericData(uri, new String[]{column}, new int[]{type}, selection).get(column);
    }

    public HashMap<String, Object> getGenericData(Uri uri, String[] proj, int[] types)
            throws NotFoundException {
        return getGenericData(uri, proj, types, null);
    }

    public HashMap<String, Object> getGenericData(Uri uri, String[] proj, int[] types, String selection)
            throws NotFoundException {
        Cursor cursor = mContentResolver.query(uri, proj, selection, null, null);

        try {
            HashMap<String, Object> result = new HashMap<>(proj.length);
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
            } else {
                // If no data was found, throw an appropriate exception
                throw new NotFoundException();
            }

            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public HashMap<String, Object> getUnifiedData(long masterKeyId, String[] proj, int[] types)
            throws NotFoundException {
        return getGenericData(KeyRings.buildUnifiedKeyRingUri(masterKeyId), proj, types);
    }

    private LongSparseArray<CanonicalizedPublicKey> getTrustedMasterKeys() {
        Cursor cursor = mContentResolver.query(KeyRings.buildUnifiedKeyRingsUri(), new String[]{
                KeyRings.MASTER_KEY_ID,
                // we pick from cache only information that is not easily available from keyrings
                KeyRings.HAS_ANY_SECRET, KeyRings.VERIFIED,
                // and of course, ring data
                KeyRings.PUBKEY_DATA
        }, KeyRings.HAS_ANY_SECRET + " = 1", null, null);

        try {
            LongSparseArray<CanonicalizedPublicKey> result = new LongSparseArray<>();

            if (cursor != null && cursor.moveToFirst()) do {
                long masterKeyId = cursor.getLong(0);
                int verified = cursor.getInt(2);
                byte[] blob = cursor.getBlob(3);
                if (blob != null) {
                    result.put(masterKeyId,
                            new CanonicalizedPublicKeyRing(blob, verified).getPublicKey());
                }
            } while (cursor.moveToNext());

            return result;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    public long getMasterKeyId(long subKeyId) throws NotFoundException {
        return (Long) getGenericData(KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(subKeyId),
                KeyRings.MASTER_KEY_ID, FIELD_TYPE_INTEGER);
    }

    public CachedPublicKeyRing getCachedPublicKeyRing(Uri queryUri) {
        return new CachedPublicKeyRing(this, queryUri);
    }

    public CachedPublicKeyRing getCachedPublicKeyRing(long id) {
        return new CachedPublicKeyRing(this, KeyRings.buildUnifiedKeyRingUri(id));
    }

    public CanonicalizedPublicKeyRing getCanonicalizedPublicKeyRing(long id) throws NotFoundException {
        return (CanonicalizedPublicKeyRing) getCanonicalizedKeyRing(KeyRings.buildUnifiedKeyRingUri(id), false);
    }

    public CanonicalizedPublicKeyRing getCanonicalizedPublicKeyRing(Uri queryUri) throws NotFoundException {
        return (CanonicalizedPublicKeyRing) getCanonicalizedKeyRing(queryUri, false);
    }

    public CanonicalizedSecretKeyRing getCanonicalizedSecretKeyRing(long id) throws NotFoundException {
        return (CanonicalizedSecretKeyRing) getCanonicalizedKeyRing(KeyRings.buildUnifiedKeyRingUri(id), true);
    }

    public CanonicalizedSecretKeyRing getCanonicalizedSecretKeyRing(Uri queryUri) throws NotFoundException {
        return (CanonicalizedSecretKeyRing) getCanonicalizedKeyRing(queryUri, true);
    }

    private KeyRing getCanonicalizedKeyRing(Uri queryUri, boolean secret) throws NotFoundException {
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
                if (secret & !hasAnySecret) {
                    throw new NotFoundException("Secret key not available!");
                }
                return secret
                        ? new CanonicalizedSecretKeyRing(blob, true, verified)
                        : new CanonicalizedPublicKeyRing(blob, verified);
            } else {
                throw new NotFoundException("Key not found!");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // bits, in order: CESA. make SURE these are correct, we will get bad log entries otherwise!!
    static final LogType LOG_TYPES_FLAG_MASTER[] = new LogType[]{
            LogType.MSG_IP_MASTER_FLAGS_XXXX, LogType.MSG_IP_MASTER_FLAGS_CXXX,
            LogType.MSG_IP_MASTER_FLAGS_XEXX, LogType.MSG_IP_MASTER_FLAGS_CEXX,
            LogType.MSG_IP_MASTER_FLAGS_XXSX, LogType.MSG_IP_MASTER_FLAGS_CXSX,
            LogType.MSG_IP_MASTER_FLAGS_XESX, LogType.MSG_IP_MASTER_FLAGS_CESX,
            LogType.MSG_IP_MASTER_FLAGS_XXXA, LogType.MSG_IP_MASTER_FLAGS_CXXA,
            LogType.MSG_IP_MASTER_FLAGS_XEXA, LogType.MSG_IP_MASTER_FLAGS_CEXA,
            LogType.MSG_IP_MASTER_FLAGS_XXSA, LogType.MSG_IP_MASTER_FLAGS_CXSA,
            LogType.MSG_IP_MASTER_FLAGS_XESA, LogType.MSG_IP_MASTER_FLAGS_CESA
    };

    // same as above, but for subkeys
    static final LogType LOG_TYPES_FLAG_SUBKEY[] = new LogType[]{
            LogType.MSG_IP_SUBKEY_FLAGS_XXXX, LogType.MSG_IP_SUBKEY_FLAGS_CXXX,
            LogType.MSG_IP_SUBKEY_FLAGS_XEXX, LogType.MSG_IP_SUBKEY_FLAGS_CEXX,
            LogType.MSG_IP_SUBKEY_FLAGS_XXSX, LogType.MSG_IP_SUBKEY_FLAGS_CXSX,
            LogType.MSG_IP_SUBKEY_FLAGS_XESX, LogType.MSG_IP_SUBKEY_FLAGS_CESX,
            LogType.MSG_IP_SUBKEY_FLAGS_XXXA, LogType.MSG_IP_SUBKEY_FLAGS_CXXA,
            LogType.MSG_IP_SUBKEY_FLAGS_XEXA, LogType.MSG_IP_SUBKEY_FLAGS_CEXA,
            LogType.MSG_IP_SUBKEY_FLAGS_XXSA, LogType.MSG_IP_SUBKEY_FLAGS_CXSA,
            LogType.MSG_IP_SUBKEY_FLAGS_XESA, LogType.MSG_IP_SUBKEY_FLAGS_CESA
    };

    /**
     * Saves an UncachedKeyRing of the public variant into the db.
     * <p/>
     * This method will not delete all previous data for this masterKeyId from the database prior
     * to inserting. All public data is effectively re-inserted, secret keyrings are left deleted
     * and need to be saved externally to be preserved past the operation.
     */
    @SuppressWarnings("unchecked")
    private int saveCanonicalizedPublicKeyRing(CanonicalizedPublicKeyRing keyRing,
                                               Progressable progress, boolean selfCertsAreTrusted) {

        // start with ok result
        int result = SaveKeyringResult.SAVED_PUBLIC;

        long masterKeyId = keyRing.getMasterKeyId();
        UncachedPublicKey masterKey = keyRing.getPublicKey();

        ArrayList<ContentProviderOperation> operations;
        try {

            log(LogType.MSG_IP_PREPARE);
            mIndent += 1;

            // save all keys and userIds included in keyRing object in database
            operations = new ArrayList<>();

            log(LogType.MSG_IP_INSERT_KEYRING);
            { // insert keyring
                ContentValues values = new ContentValues();
                values.put(KeyRingData.MASTER_KEY_ID, masterKeyId);
                try {
                    values.put(KeyRingData.KEY_RING_DATA, keyRing.getEncoded());
                } catch (IOException e) {
                    log(LogType.MSG_IP_ENCODE_FAIL);
                    return SaveKeyringResult.RESULT_ERROR;
                }

                Uri uri = KeyRingData.buildPublicKeyRingUri(masterKeyId);
                operations.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
            }

            log(LogType.MSG_IP_INSERT_SUBKEYS);
            progress.setProgress(LogType.MSG_IP_INSERT_SUBKEYS.getMsgId(), 40, 100);
            mIndent += 1;
            { // insert subkeys
                Uri uri = Keys.buildKeysUri(masterKeyId);
                int rank = 0;
                for (CanonicalizedPublicKey key : keyRing.publicKeyIterator()) {
                    long keyId = key.getKeyId();
                    log(keyId == masterKeyId ? LogType.MSG_IP_MASTER : LogType.MSG_IP_SUBKEY,
                            KeyFormattingUtils.convertKeyIdToHex(keyId)
                    );
                    mIndent += 1;

                    ContentValues values = new ContentValues();
                    values.put(Keys.MASTER_KEY_ID, masterKeyId);
                    values.put(Keys.RANK, rank);

                    values.put(Keys.KEY_ID, key.getKeyId());
                    values.put(Keys.KEY_SIZE, key.getBitStrength());
                    values.put(Keys.KEY_CURVE_OID, key.getCurveOid());
                    values.put(Keys.ALGORITHM, key.getAlgorithm());
                    values.put(Keys.FINGERPRINT, key.getFingerprint());

                    boolean c = key.canCertify(), e = key.canEncrypt(), s = key.canSign(), a = key.canAuthenticate();
                    values.put(Keys.CAN_CERTIFY, c);
                    values.put(Keys.CAN_ENCRYPT, e);
                    values.put(Keys.CAN_SIGN, s);
                    values.put(Keys.CAN_AUTHENTICATE, a);
                    values.put(Keys.IS_REVOKED, key.isRevoked());

                    // see above
                    if (masterKeyId == keyId) {
                        if (key.getKeyUsage() == null) {
                            log(LogType.MSG_IP_MASTER_FLAGS_UNSPECIFIED);
                        } else {
                            log(LOG_TYPES_FLAG_MASTER[(c ? 1 : 0) + (e ? 2 : 0) + (s ? 4 : 0) + (a ? 8 : 0)]);
                        }
                    } else {
                        if (key.getKeyUsage() == null) {
                            log(LogType.MSG_IP_SUBKEY_FLAGS_UNSPECIFIED);
                        } else {
                            log(LOG_TYPES_FLAG_SUBKEY[(c ? 1 : 0) + (e ? 2 : 0) + (s ? 4 : 0) + (a ? 8 : 0)]);
                        }
                    }

                    Date creation = key.getCreationTime();
                    values.put(Keys.CREATION, creation.getTime() / 1000);
                    Date expiryDate = key.getExpiryTime();
                    if (expiryDate != null) {
                        values.put(Keys.EXPIRY, expiryDate.getTime() / 1000);
                        if (key.isExpired()) {
                            log(keyId == masterKeyId ?
                                            LogType.MSG_IP_MASTER_EXPIRED : LogType.MSG_IP_SUBKEY_EXPIRED,
                                    expiryDate.toString());
                        } else {
                            log(keyId == masterKeyId ?
                                            LogType.MSG_IP_MASTER_EXPIRES : LogType.MSG_IP_SUBKEY_EXPIRES,
                                    expiryDate.toString());
                        }
                    }

                    operations.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
                    ++rank;
                    mIndent -= 1;
                }
            }
            mIndent -= 1;

            // get a list of owned secret keys, for verification filtering
            LongSparseArray<CanonicalizedPublicKey> trustedKeys = getTrustedMasterKeys();

            // classify and order user ids. primary are moved to the front, revoked to the back,
            // otherwise the order in the keyfile is preserved.
            List<UserPacketItem> uids = new ArrayList<>();

            if (trustedKeys.size() == 0) {
                log(LogType.MSG_IP_UID_CLASSIFYING_ZERO);
            } else {
                log(LogType.MSG_IP_UID_CLASSIFYING, trustedKeys.size());
            }
            mIndent += 1;
            for (byte[] rawUserId : masterKey.getUnorderedRawUserIds()) {
                String userId = Utf8Util.fromUTF8ByteArrayReplaceBadEncoding(rawUserId);

                UserPacketItem item = new UserPacketItem();
                uids.add(item);
                item.userId = userId;

                int unknownCerts = 0;

                log(LogType.MSG_IP_UID_PROCESSING, userId);
                mIndent += 1;
                // look through signatures for this specific key
                for (WrappedSignature cert : new IterableIterator<>(
                        masterKey.getSignaturesForRawId(rawUserId))) {
                    long certId = cert.getKeyId();
                    // self signature
                    if (certId == masterKeyId) {

                        // NOTE self-certificates are already verified during canonicalization,
                        // AND we know there is at most one cert plus at most one revocation
                        if (!cert.isRevocation()) {
                            item.selfCert = cert;
                            item.isPrimary = cert.isPrimaryUserId();
                        } else {
                            item.selfRevocation = cert;
                            log(LogType.MSG_IP_UID_REVOKED);
                        }
                        continue;

                    }

                    // do we have a trusted key for this?
                    if (trustedKeys.indexOfKey(certId) < 0) {
                        unknownCerts += 1;
                        continue;
                    }

                    // verify signatures from known private keys
                    CanonicalizedPublicKey trustedKey = trustedKeys.get(certId);

                    try {
                        cert.init(trustedKey);
                        // if it doesn't certify, leave a note and skip
                        if (!cert.verifySignature(masterKey, rawUserId)) {
                            log(LogType.MSG_IP_UID_CERT_BAD);
                            continue;
                        }

                        log(cert.isRevocation()
                                        ? LogType.MSG_IP_UID_CERT_GOOD_REVOKE
                                        : LogType.MSG_IP_UID_CERT_GOOD,
                                KeyFormattingUtils.convertKeyIdToHexShort(trustedKey.getKeyId())
                        );

                        // check if there is a previous certificate
                        WrappedSignature prev = item.trustedCerts.get(cert.getKeyId());
                        if (prev != null) {
                            // if it's newer, skip this one
                            if (prev.getCreationTime().after(cert.getCreationTime())) {
                                log(LogType.MSG_IP_UID_CERT_OLD);
                                continue;
                            }
                            // if the previous one was a non-revokable certification, no need to look further
                            if (!prev.isRevocation() && !prev.isRevokable()) {
                                log(LogType.MSG_IP_UID_CERT_NONREVOKE);
                                continue;
                            }
                            log(LogType.MSG_IP_UID_CERT_NEW);
                        }
                        item.trustedCerts.put(cert.getKeyId(), cert);

                    } catch (PgpGeneralException e) {
                        log(LogType.MSG_IP_UID_CERT_ERROR,
                                KeyFormattingUtils.convertKeyIdToHex(cert.getKeyId()));
                    }

                }

                if (unknownCerts > 0) {
                    log(LogType.MSG_IP_UID_CERTS_UNKNOWN, unknownCerts);
                }
                mIndent -= 1;

            }
            mIndent -= 1;

            ArrayList<WrappedUserAttribute> userAttributes = masterKey.getUnorderedUserAttributes();
            // Don't spam the log if there aren't even any attributes
            if (!userAttributes.isEmpty()) {
                log(LogType.MSG_IP_UAT_CLASSIFYING);
            }

            mIndent += 1;
            for (WrappedUserAttribute userAttribute : userAttributes) {

                UserPacketItem item = new UserPacketItem();
                uids.add(item);
                item.type = userAttribute.getType();
                item.attributeData = userAttribute.getEncoded();

                int unknownCerts = 0;

                switch (item.type) {
                    case WrappedUserAttribute.UAT_IMAGE:
                        log(LogType.MSG_IP_UAT_PROCESSING_IMAGE);
                        break;
                    default:
                        log(LogType.MSG_IP_UAT_PROCESSING_UNKNOWN);
                        break;
                }
                mIndent += 1;
                // look through signatures for this specific key
                for (WrappedSignature cert : new IterableIterator<>(
                        masterKey.getSignaturesForUserAttribute(userAttribute))) {
                    long certId = cert.getKeyId();
                    // self signature
                    if (certId == masterKeyId) {

                        // NOTE self-certificates are already verified during canonicalization,
                        // AND we know there is at most one cert plus at most one revocation
                        // AND the revocation only exists if there is no newer certification
                        if (!cert.isRevocation()) {
                            item.selfCert = cert;
                        } else {
                            item.selfRevocation = cert;
                            log(LogType.MSG_IP_UAT_REVOKED);
                        }
                        continue;

                    }

                    // do we have a trusted key for this?
                    if (trustedKeys.indexOfKey(certId) < 0) {
                        unknownCerts += 1;
                        continue;
                    }

                    // verify signatures from known private keys
                    CanonicalizedPublicKey trustedKey = trustedKeys.get(certId);

                    try {
                        cert.init(trustedKey);
                        // if it doesn't certify, leave a note and skip
                        if (!cert.verifySignature(masterKey, userAttribute)) {
                            log(LogType.MSG_IP_UAT_CERT_BAD);
                            continue;
                        }

                        log(cert.isRevocation()
                                        ? LogType.MSG_IP_UAT_CERT_GOOD_REVOKE
                                        : LogType.MSG_IP_UAT_CERT_GOOD,
                                KeyFormattingUtils.convertKeyIdToHexShort(trustedKey.getKeyId())
                        );

                        // check if there is a previous certificate
                        WrappedSignature prev = item.trustedCerts.get(cert.getKeyId());
                        if (prev != null) {
                            // if it's newer, skip this one
                            if (prev.getCreationTime().after(cert.getCreationTime())) {
                                log(LogType.MSG_IP_UAT_CERT_OLD);
                                continue;
                            }
                            // if the previous one was a non-revokable certification, no need to look further
                            if (!prev.isRevocation() && !prev.isRevokable()) {
                                log(LogType.MSG_IP_UAT_CERT_NONREVOKE);
                                continue;
                            }
                            log(LogType.MSG_IP_UAT_CERT_NEW);
                        }
                        item.trustedCerts.put(cert.getKeyId(), cert);

                    } catch (PgpGeneralException e) {
                        log(LogType.MSG_IP_UAT_CERT_ERROR,
                                KeyFormattingUtils.convertKeyIdToHex(cert.getKeyId()));
                    }

                }

                if (unknownCerts > 0) {
                    log(LogType.MSG_IP_UAT_CERTS_UNKNOWN, unknownCerts);
                }
                mIndent -= 1;

            }
            mIndent -= 1;

            progress.setProgress(LogType.MSG_IP_UID_REORDER.getMsgId(), 65, 100);
            log(LogType.MSG_IP_UID_REORDER);
            // primary before regular before revoked (see UserIdItem.compareTo)
            // this is a stable sort, so the order of keys is otherwise preserved.
            Collections.sort(uids);
            // iterate and put into db
            for (int userIdRank = 0; userIdRank < uids.size(); userIdRank++) {
                UserPacketItem item = uids.get(userIdRank);
                operations.add(buildUserIdOperations(masterKeyId, item, userIdRank));

                if (item.selfRevocation != null) {
                    operations.add(buildCertOperations(masterKeyId, userIdRank, item.selfRevocation,
                            Certs.VERIFIED_SELF));
                    // don't bother with trusted certs if the uid is revoked, anyways
                    continue;
                }

                if (item.selfCert == null) {
                    throw new AssertionError("User ids MUST be self-certified at this point!!");
                }

                operations.add(buildCertOperations(masterKeyId, userIdRank, item.selfCert,
                        selfCertsAreTrusted ? Certs.VERIFIED_SECRET : Certs.VERIFIED_SELF));

                // iterate over signatures
                for (int i = 0; i < item.trustedCerts.size(); i++) {
                    WrappedSignature sig = item.trustedCerts.valueAt(i);
                    // if it's a revocation
                    if (sig.isRevocation()) {
                        // don't further process it
                        continue;
                    }
                    // otherwise, build database operation
                    operations.add(buildCertOperations(
                            masterKeyId, userIdRank, sig, Certs.VERIFIED_SECRET));
                }
            }

        } catch (IOException e) {
            log(LogType.MSG_IP_ERROR_IO_EXC);
            Log.e(Constants.TAG, "IOException during import", e);
            return SaveKeyringResult.RESULT_ERROR;
        } finally {
            mIndent -= 1;
        }

        try {
            // delete old version of this keyRing, which also deletes all keys and userIds on cascade
            int deleted = mContentResolver.delete(
                    KeyRingData.buildPublicKeyRingUri(masterKeyId), null, null);
            if (deleted > 0) {
                log(LogType.MSG_IP_DELETE_OLD_OK);
                result |= SaveKeyringResult.UPDATED;
            } else {
                log(LogType.MSG_IP_DELETE_OLD_FAIL);
            }

            log(LogType.MSG_IP_APPLY_BATCH);
            progress.setProgress(LogType.MSG_IP_APPLY_BATCH.getMsgId(), 75, 100);
            mContentResolver.applyBatch(KeychainContract.CONTENT_AUTHORITY, operations);

            log(LogType.MSG_IP_SUCCESS);
            progress.setProgress(LogType.MSG_IP_SUCCESS.getMsgId(), 90, 100);
            return result;

        } catch (RemoteException e) {
            log(LogType.MSG_IP_ERROR_REMOTE_EX);
            Log.e(Constants.TAG, "RemoteException during import", e);
            return SaveKeyringResult.RESULT_ERROR;
        } catch (OperationApplicationException e) {
            log(LogType.MSG_IP_ERROR_OP_EXC);
            Log.e(Constants.TAG, "OperationApplicationException during import", e);
            return SaveKeyringResult.RESULT_ERROR;
        }

    }

    private static class UserPacketItem implements Comparable<UserPacketItem> {
        Integer type;
        String userId;
        byte[] attributeData;
        boolean isPrimary = false;
        WrappedSignature selfCert;
        WrappedSignature selfRevocation;
        LongSparseArray<WrappedSignature> trustedCerts = new LongSparseArray<>();

        @Override
        public int compareTo(UserPacketItem o) {
            // revoked keys always come last!
            //noinspection DoubleNegation
            if ((selfRevocation != null) != (o.selfRevocation != null)) {
                return selfRevocation != null ? 1 : -1;
            }
            // if one is a user id, but the other isn't, the user id always comes first.
            // we compare for null values here, so != is the correct operator!
            // noinspection NumberEquality
            if (type != o.type) {
                return type == null ? -1 : 1;
            }
            // if one key is primary but the other isn't, the primary one always comes first
            if (isPrimary != o.isPrimary) {
                return isPrimary ? -1 : 1;
            }
            return 0;
        }
    }

    /**
     * Saves an UncachedKeyRing of the secret variant into the db.
     * This method will fail if no corresponding public keyring is in the database!
     */
    private int saveCanonicalizedSecretKeyRing(CanonicalizedSecretKeyRing keyRing) {

        long masterKeyId = keyRing.getMasterKeyId();
        log(LogType.MSG_IS, KeyFormattingUtils.convertKeyIdToHex(masterKeyId));
        mIndent += 1;

        try {

            // IF this is successful, it's a secret key
            int result = SaveKeyringResult.SAVED_SECRET;

            // save secret keyring
            try {
                ContentValues values = new ContentValues();
                values.put(KeyRingData.MASTER_KEY_ID, masterKeyId);
                values.put(KeyRingData.KEY_RING_DATA, keyRing.getEncoded());
                // insert new version of this keyRing
                Uri uri = KeyRingData.buildSecretKeyRingUri(masterKeyId);
                if (mContentResolver.insert(uri, values) == null) {
                    log(LogType.MSG_IS_DB_EXCEPTION);
                    return SaveKeyringResult.RESULT_ERROR;
                }
            } catch (IOException e) {
                Log.e(Constants.TAG, "Failed to encode key!", e);
                log(LogType.MSG_IS_ERROR_IO_EXC);
                return SaveKeyringResult.RESULT_ERROR;
            }

            {
                Uri uri = Keys.buildKeysUri(masterKeyId);

                // first, mark all keys as not available
                ContentValues values = new ContentValues();
                values.put(Keys.HAS_SECRET, SecretKeyType.GNU_DUMMY.getNum());
                mContentResolver.update(uri, values, null, null);

                // then, mark exactly the keys we have available
                log(LogType.MSG_IS_IMPORTING_SUBKEYS);
                mIndent += 1;
                for (CanonicalizedSecretKey sub : keyRing.secretKeyIterator()) {
                    long id = sub.getKeyId();
                    SecretKeyType mode = sub.getSecretKeyType();
                    values.put(Keys.HAS_SECRET, mode.getNum());
                    int upd = mContentResolver.update(uri, values, Keys.KEY_ID + " = ?",
                            new String[]{Long.toString(id)});
                    if (upd == 1) {
                        switch (mode) {
                            case PASSPHRASE:
                                log(LogType.MSG_IS_SUBKEY_OK,
                                        KeyFormattingUtils.convertKeyIdToHex(id)
                                );
                                break;
                            case PASSPHRASE_EMPTY:
                                log(LogType.MSG_IS_SUBKEY_EMPTY,
                                        KeyFormattingUtils.convertKeyIdToHex(id)
                                );
                                break;
                            case PIN:
                                log(LogType.MSG_IS_SUBKEY_PIN,
                                        KeyFormattingUtils.convertKeyIdToHex(id)
                                );
                                break;
                            case GNU_DUMMY:
                                log(LogType.MSG_IS_SUBKEY_STRIPPED,
                                        KeyFormattingUtils.convertKeyIdToHex(id)
                                );
                                break;
                            case DIVERT_TO_CARD:
                                log(LogType.MSG_IS_SUBKEY_DIVERT,
                                        KeyFormattingUtils.convertKeyIdToHex(id)
                                );
                                break;
                        }
                    } else {
                        log(LogType.MSG_IS_SUBKEY_NONEXISTENT,
                                KeyFormattingUtils.convertKeyIdToHex(id)
                        );
                    }
                }
                mIndent -= 1;

                // this implicitly leaves all keys which were not in the secret key ring
                // with has_secret = 1
            }

            log(LogType.MSG_IS_SUCCESS);
            return result;

        } finally {
            mIndent -= 1;
        }

    }

    public SaveKeyringResult savePublicKeyRing(UncachedKeyRing keyRing) {
        return savePublicKeyRing(keyRing, new ProgressScaler());
    }

    /**
     * Save a public keyring into the database.
     * <p/>
     * This is a high level method, which takes care of merging all new information into the old and
     * keep public and secret keyrings in sync.
     */
    public SaveKeyringResult savePublicKeyRing(UncachedKeyRing publicRing, Progressable progress) {

        try {
            long masterKeyId = publicRing.getMasterKeyId();
            log(LogType.MSG_IP, KeyFormattingUtils.convertKeyIdToHex(masterKeyId));
            mIndent += 1;

            if (publicRing.isSecret()) {
                log(LogType.MSG_IP_BAD_TYPE_SECRET);
                return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
            }

            CanonicalizedPublicKeyRing canPublicRing;

            // If there is an old keyring, merge it
            try {
                UncachedKeyRing oldPublicRing = getCanonicalizedPublicKeyRing(masterKeyId).getUncachedKeyRing();

                // Merge data from new public ring into the old one
                log(LogType.MSG_IP_MERGE_PUBLIC);
                publicRing = oldPublicRing.merge(publicRing, mLog, mIndent);

                // If this is null, there is an error in the log so we can just return
                if (publicRing == null) {
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
                }

                // Canonicalize this keyring, to assert a number of assumptions made about it.
                canPublicRing = (CanonicalizedPublicKeyRing) publicRing.canonicalize(mLog, mIndent);
                if (canPublicRing == null) {
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
                }

                // Early breakout if nothing changed
                if (Arrays.hashCode(publicRing.getEncoded())
                        == Arrays.hashCode(oldPublicRing.getEncoded())) {
                    log(LogType.MSG_IP_SUCCESS_IDENTICAL);
                    return new SaveKeyringResult(SaveKeyringResult.UPDATED, mLog, null);
                }
            } catch (NotFoundException e) {
                // Not an issue, just means we are dealing with a new keyring.

                // Canonicalize this keyring, to assert a number of assumptions made about it.
                canPublicRing = (CanonicalizedPublicKeyRing) publicRing.canonicalize(mLog, mIndent);
                if (canPublicRing == null) {
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
                }

            }

            // If there is a secret key, merge new data (if any) and save the key for later
            CanonicalizedSecretKeyRing canSecretRing;
            try {
                UncachedKeyRing secretRing = getCanonicalizedSecretKeyRing(publicRing.getMasterKeyId()).getUncachedKeyRing();

                // Merge data from new public ring into secret one
                log(LogType.MSG_IP_MERGE_SECRET);
                secretRing = secretRing.merge(publicRing, mLog, mIndent);
                if (secretRing == null) {
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
                }
                // This has always been a secret key ring, this is a safe cast
                canSecretRing = (CanonicalizedSecretKeyRing) secretRing.canonicalize(mLog, mIndent);
                if (canSecretRing == null) {
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
                }

            } catch (NotFoundException e) {
                // No secret key available (this is what happens most of the time)
                canSecretRing = null;
            }

            int result = saveCanonicalizedPublicKeyRing(canPublicRing, progress, canSecretRing != null);

            // Save the saved keyring (if any)
            if (canSecretRing != null) {
                progress.setProgress(LogType.MSG_IP_REINSERT_SECRET.getMsgId(), 90, 100);
                int secretResult = saveCanonicalizedSecretKeyRing(canSecretRing);
                if ((secretResult & SaveKeyringResult.RESULT_ERROR) != SaveKeyringResult.RESULT_ERROR) {
                    result |= SaveKeyringResult.SAVED_SECRET;
                }
            }

            return new SaveKeyringResult(result, mLog, canSecretRing);

        } catch (IOException e) {
            log(LogType.MSG_IP_ERROR_IO_EXC);
            return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
        } finally {
            mIndent -= 1;
        }

    }

    public SaveKeyringResult saveSecretKeyRing(UncachedKeyRing secretRing, Progressable progress) {

        try {
            long masterKeyId = secretRing.getMasterKeyId();
            log(LogType.MSG_IS, KeyFormattingUtils.convertKeyIdToHex(masterKeyId));
            mIndent += 1;

            if (!secretRing.isSecret()) {
                log(LogType.MSG_IS_BAD_TYPE_PUBLIC);
                return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
            }

            CanonicalizedSecretKeyRing canSecretRing;

            // If there is an old secret key, merge it.
            try {
                UncachedKeyRing oldSecretRing = getCanonicalizedSecretKeyRing(masterKeyId).getUncachedKeyRing();

                // Merge data from new secret ring into old one
                log(LogType.MSG_IS_MERGE_SECRET);
                secretRing = secretRing.merge(oldSecretRing, mLog, mIndent);

                // If this is null, there is an error in the log so we can just return
                if (secretRing == null) {
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
                }

                // Canonicalize this keyring, to assert a number of assumptions made about it.
                // This is a safe cast, because we made sure this is a secret ring above
                canSecretRing = (CanonicalizedSecretKeyRing) secretRing.canonicalize(mLog, mIndent);
                if (canSecretRing == null) {
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
                }

                // Early breakout if nothing changed
                if (Arrays.hashCode(secretRing.getEncoded())
                        == Arrays.hashCode(oldSecretRing.getEncoded())) {
                    log(LogType.MSG_IS_SUCCESS_IDENTICAL,
                            KeyFormattingUtils.convertKeyIdToHex(masterKeyId));
                    return new SaveKeyringResult(SaveKeyringResult.UPDATED, mLog, null);
                }
            } catch (NotFoundException e) {
                // Not an issue, just means we are dealing with a new keyring

                // Canonicalize this keyring, to assert a number of assumptions made about it.
                // This is a safe cast, because we made sure this is a secret ring above
                canSecretRing = (CanonicalizedSecretKeyRing) secretRing.canonicalize(mLog, mIndent);
                if (canSecretRing == null) {

                    // Special case: If keyring canonicalization failed, try again after adding
                    // all self-certificates from the public key.
                    try {
                        log(LogType.MSG_IS_MERGE_SPECIAL);
                        UncachedKeyRing oldPublicRing = getCanonicalizedPublicKeyRing(masterKeyId).getUncachedKeyRing();
                        secretRing = secretRing.merge(oldPublicRing, mLog, mIndent);
                        canSecretRing = (CanonicalizedSecretKeyRing) secretRing.canonicalize(mLog, mIndent);
                    } catch (NotFoundException e2) {
                        // nothing, this is handled right in the next line
                    }

                    if (canSecretRing == null) {
                        return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
                    }
                }

            }

            // Merge new data into public keyring as well, if there is any
            UncachedKeyRing publicRing;
            try {
                UncachedKeyRing oldPublicRing = getCanonicalizedPublicKeyRing(masterKeyId).getUncachedKeyRing();

                // Merge data from new secret ring into public one
                log(LogType.MSG_IS_MERGE_PUBLIC);
                publicRing = oldPublicRing.merge(secretRing, mLog, mIndent);
                if (publicRing == null) {
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
                }

            } catch (NotFoundException e) {
                log(LogType.MSG_IS_PUBRING_GENERATE);
                publicRing = secretRing.extractPublicKeyRing();
            }

            CanonicalizedPublicKeyRing canPublicRing = (CanonicalizedPublicKeyRing) publicRing.canonicalize(mLog, mIndent);
            if (canPublicRing == null) {
                return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
            }

            int result;

            result = saveCanonicalizedPublicKeyRing(canPublicRing, progress, true);
            if ((result & SaveKeyringResult.RESULT_ERROR) == SaveKeyringResult.RESULT_ERROR) {
                return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
            }

            progress.setProgress(LogType.MSG_IP_REINSERT_SECRET.getMsgId(), 90, 100);
            result = saveCanonicalizedSecretKeyRing(canSecretRing);

            return new SaveKeyringResult(result, mLog, canSecretRing);

        } catch (IOException e) {
            log(LogType.MSG_IS_ERROR_IO_EXC);
            return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
        } finally {
            mIndent -= 1;
        }

    }

    public ConsolidateResult consolidateDatabaseStep1(Progressable progress) {

        OperationLog log = new OperationLog();
        int indent = 0;

        // 1a. fetch all secret keyrings into a cache file
        log.add(LogType.MSG_CON, indent);
        indent += 1;

        if (mConsolidateCritical) {
            log.add(LogType.MSG_CON_RECURSIVE, indent);
            return new ConsolidateResult(ConsolidateResult.RESULT_OK, log);
        }

        progress.setProgress(R.string.progress_con_saving, 0, 100);

        // The consolidate operation can never be cancelled!
        progress.setPreventCancel();

        try {

            log.add(LogType.MSG_CON_SAVE_SECRET, indent);
            indent += 1;

            final Cursor cursor = mContentResolver.query(KeyRingData.buildSecretKeyRingUri(),
                    new String[]{ KeyRingData.KEY_RING_DATA }, null, null, null);

            if (cursor == null) {
                log.add(LogType.MSG_CON_ERROR_DB, indent);
                return new ConsolidateResult(ConsolidateResult.RESULT_ERROR, log);
            }

            // No keys existing might be a legitimate option, we write an empty file in that case
            cursor.moveToFirst();
            ParcelableFileCache<ParcelableKeyRing> cache =
                    new ParcelableFileCache<>(mContext, "consolidate_secret.pcl");
            cache.writeCache(cursor.getCount(), new Iterator<ParcelableKeyRing>() {
                ParcelableKeyRing ring;

                @Override
                public boolean hasNext() {
                    if (ring != null) {
                        return true;
                    }
                    if (cursor.isAfterLast()) {
                        return false;
                    }
                    ring = new ParcelableKeyRing(cursor.getBlob(0));
                    cursor.moveToNext();
                    return true;
                }

                @Override
                public ParcelableKeyRing next() {
                    try {
                        return ring;
                    } finally {
                        ring = null;
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

            });

        } catch (IOException e) {
            Log.e(Constants.TAG, "error saving secret", e);
            log.add(LogType.MSG_CON_ERROR_IO_SECRET, indent);
            return new ConsolidateResult(ConsolidateResult.RESULT_ERROR, log);
        } finally {
            indent -= 1;
        }

        progress.setProgress(R.string.progress_con_saving, 3, 100);

        // 1b. fetch all public keyrings into a cache file
        try {

            log.add(LogType.MSG_CON_SAVE_PUBLIC, indent);
            indent += 1;

            final Cursor cursor = mContentResolver.query(
                    KeyRingData.buildPublicKeyRingUri(),
                    new String[]{ KeyRingData.KEY_RING_DATA }, null, null, null);

            if (cursor == null) {
                log.add(LogType.MSG_CON_ERROR_DB, indent);
                return new ConsolidateResult(ConsolidateResult.RESULT_ERROR, log);
            }

            // No keys existing might be a legitimate option, we write an empty file in that case
            cursor.moveToFirst();
            ParcelableFileCache<ParcelableKeyRing> cache =
                    new ParcelableFileCache<>(mContext, "consolidate_public.pcl");
            cache.writeCache(cursor.getCount(), new Iterator<ParcelableKeyRing>() {
                ParcelableKeyRing ring;

                @Override
                public boolean hasNext() {
                    if (ring != null) {
                        return true;
                    }
                    if (cursor.isAfterLast()) {
                        return false;
                    }
                    ring = new ParcelableKeyRing(cursor.getBlob(0));
                    cursor.moveToNext();
                    return true;
                }

                @Override
                public ParcelableKeyRing next() {
                    try {
                        return ring;
                    } finally {
                        ring = null;
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

            });

        } catch (IOException e) {
            Log.e(Constants.TAG, "error saving public", e);
            log.add(LogType.MSG_CON_ERROR_IO_PUBLIC, indent);
            return new ConsolidateResult(ConsolidateResult.RESULT_ERROR, log);
        } finally {
            indent -= 1;
        }

        log.add(LogType.MSG_CON_CRITICAL_IN, indent);
        Preferences.getPreferences(mContext).setCachedConsolidate(true);

        return consolidateDatabaseStep2(log, indent, progress, false);
    }

    public ConsolidateResult consolidateDatabaseStep2(Progressable progress) {
        return consolidateDatabaseStep2(new OperationLog(), 0, progress, true);
    }

    private static boolean mConsolidateCritical = false;

    private ConsolidateResult consolidateDatabaseStep2(
            OperationLog log, int indent, Progressable progress, boolean recovery) {

        synchronized (ProviderHelper.class) {
            if (mConsolidateCritical) {
                log.add(LogType.MSG_CON_ERROR_CONCURRENT, indent);
                return new ConsolidateResult(ConsolidateResult.RESULT_ERROR, log);
            }
            mConsolidateCritical = true;
        }

        try {
            Preferences prefs = Preferences.getPreferences(mContext);

            if (recovery) {
                log.add(LogType.MSG_CON_RECOVER, indent);
                indent += 1;
            }

            if (!prefs.getCachedConsolidate()) {
                log.add(LogType.MSG_CON_ERROR_BAD_STATE, indent);
                return new ConsolidateResult(ConsolidateResult.RESULT_ERROR, log);
            }

            // 2. wipe database (IT'S DANGEROUS)
            log.add(LogType.MSG_CON_DB_CLEAR, indent);
            mContentResolver.delete(KeyRings.buildUnifiedKeyRingsUri(), null, null);

            ParcelableFileCache<ParcelableKeyRing> cacheSecret, cachePublic;

            // Set flag that we have a cached consolidation here
            try {
                cacheSecret = new ParcelableFileCache<>(mContext, "consolidate_secret.pcl");
                IteratorWithSize<ParcelableKeyRing> itSecrets = cacheSecret.readCache(false);
                int numSecrets = itSecrets.getSize();

                log.add(LogType.MSG_CON_REIMPORT_SECRET, indent, numSecrets);
                indent += 1;

                // 3. Re-Import secret keyrings from cache
                if (numSecrets > 0) {

                    ImportKeyResult result = new ImportExportOperation(mContext, this,
                            new ProgressFixedScaler(progress, 10, 25, 100, R.string.progress_con_reimport))
                            .serialKeyRingImport(itSecrets, numSecrets, null);
                    log.add(result, indent);
                } else {
                    log.add(LogType.MSG_CON_REIMPORT_SECRET_SKIP, indent);
                }

            } catch (IOException e) {
                Log.e(Constants.TAG, "error importing secret", e);
                log.add(LogType.MSG_CON_ERROR_SECRET, indent);
                return new ConsolidateResult(ConsolidateResult.RESULT_ERROR, log);
            } finally {
                indent -= 1;
            }

            try {

                cachePublic = new ParcelableFileCache<>(mContext, "consolidate_public.pcl");
                IteratorWithSize<ParcelableKeyRing> itPublics = cachePublic.readCache();
                int numPublics = itPublics.getSize();

                log.add(LogType.MSG_CON_REIMPORT_PUBLIC, indent, numPublics);
                indent += 1;

                // 4. Re-Import public keyrings from cache
                if (numPublics > 0) {

                    ImportKeyResult result = new ImportExportOperation(mContext, this,
                            new ProgressFixedScaler(progress, 25, 99, 100, R.string.progress_con_reimport))
                            .serialKeyRingImport(itPublics, numPublics, null);
                    log.add(result, indent);
                } else {
                    log.add(LogType.MSG_CON_REIMPORT_PUBLIC_SKIP, indent);
                }

            } catch (IOException e) {
                Log.e(Constants.TAG, "error importing public", e);
                log.add(LogType.MSG_CON_ERROR_PUBLIC, indent);
                return new ConsolidateResult(ConsolidateResult.RESULT_ERROR, log);
            } finally {
                indent -= 1;
            }

            log.add(LogType.MSG_CON_CRITICAL_OUT, indent);
            Preferences.getPreferences(mContext).setCachedConsolidate(false);

            // 5. Delete caches
            try {
                log.add(LogType.MSG_CON_DELETE_SECRET, indent);
                indent += 1;
                cacheSecret.delete();
            } catch (IOException e) {
                // doesn't /really/ matter
                Log.e(Constants.TAG, "IOException during delete of secret cache", e);
                log.add(LogType.MSG_CON_WARN_DELETE_SECRET, indent);
            } finally {
                indent -= 1;
            }

            try {
                log.add(LogType.MSG_CON_DELETE_PUBLIC, indent);
                indent += 1;
                cachePublic.delete();
            } catch (IOException e) {
                // doesn't /really/ matter
                Log.e(Constants.TAG, "IOException during deletion of public cache", e);
                log.add(LogType.MSG_CON_WARN_DELETE_PUBLIC, indent);
            } finally {
                indent -= 1;
            }

            progress.setProgress(100, 100);
            log.add(LogType.MSG_CON_SUCCESS, indent);

            return new ConsolidateResult(ConsolidateResult.RESULT_OK, log);

        } finally {
            mConsolidateCritical = false;
        }

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

        Uri uri = Certs.buildCertsUri(masterKeyId);

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    /**
     * Build ContentProviderOperation to add PublicUserIds to database corresponding to a keyRing
     */
    private ContentProviderOperation
    buildUserIdOperations(long masterKeyId, UserPacketItem item, int rank) {
        ContentValues values = new ContentValues();
        values.put(UserPackets.MASTER_KEY_ID, masterKeyId);
        values.put(UserPackets.TYPE, item.type);
        values.put(UserPackets.USER_ID, item.userId);
        values.put(UserPackets.ATTRIBUTE_DATA, item.attributeData);
        values.put(UserPackets.IS_PRIMARY, item.isPrimary);
        values.put(UserPackets.IS_REVOKED, item.selfRevocation != null);
        values.put(UserPackets.RANK, rank);

        Uri uri = UserPackets.buildUserIdsUri(masterKeyId);

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    private String getKeyRingAsArmoredString(byte[] data) throws IOException, PgpGeneralException {
        UncachedKeyRing keyRing = UncachedKeyRing.decodeFromData(data);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        keyRing.encodeArmored(bos, null);
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

        ArrayList<String> packageNames = new ArrayList<>();
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
        values.put(ApiApps.PACKAGE_CERTIFICATE, appSettings.getPackageSignature());
        return values;
    }

    private ContentValues contentValueForApiAccounts(AccountSettings accSettings) {
        ContentValues values = new ContentValues();
        values.put(KeychainContract.ApiAccounts.ACCOUNT_NAME, accSettings.getAccountName());
        values.put(KeychainContract.ApiAccounts.KEY_ID, accSettings.getKeyId());

        // DEPRECATED and thus hardcoded
        values.put(KeychainContract.ApiAccounts.COMPRESSION, CompressionAlgorithmTags.ZLIB);
        values.put(KeychainContract.ApiAccounts.ENCRYPTION_ALGORITHM,
                PgpConstants.OpenKeychainSymmetricKeyAlgorithmTags.USE_PREFERRED);
        values.put(KeychainContract.ApiAccounts.HASH_ALORITHM,
                PgpConstants.OpenKeychainHashAlgorithmTags.USE_PREFERRED);
        return values;
    }

    public void insertApiApp(AppSettings appSettings) {
        mContentResolver.insert(KeychainContract.ApiApps.CONTENT_URI,
                contentValueForApiApps(appSettings));
    }

    public void insertApiAccount(Uri uri, AccountSettings accSettings) {
        mContentResolver.insert(uri, contentValueForApiAccounts(accSettings));
    }

    public void updateApiAccount(Uri uri, AccountSettings accSettings) {
        if (mContentResolver.update(uri, contentValueForApiAccounts(accSettings), null,
                null) <= 0) {
            throw new RuntimeException();
        }
    }

    /**
     * Must be an uri pointing to an account
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
                        cursor.getColumnIndex(KeychainContract.ApiApps.PACKAGE_CERTIFICATE)));
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
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return settings;
    }

    public Set<Long> getAllKeyIdsForApp(Uri uri) {
        Set<Long> keyIds = new HashSet<>();

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

    public HashSet<Long> getAllowedKeyIdsForApp(Uri uri) {
        HashSet<Long> keyIds = new HashSet<>();

        Cursor cursor = mContentResolver.query(uri, null, null, null, null);
        try {
            if (cursor != null) {
                int keyIdColumn = cursor.getColumnIndex(KeychainContract.ApiAllowedKeys.KEY_ID);
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

    public void saveAllowedKeyIdsForApp(Uri uri, Set<Long> allowedKeyIds)
            throws RemoteException, OperationApplicationException {
        // wipe whole table of allowed keys for this account
        mContentResolver.delete(uri, null, null);

        // re-insert allowed key ids
        for (Long keyId : allowedKeyIds) {
            ContentValues values = new ContentValues();
            values.put(ApiAllowedKeys.KEY_ID, keyId);
            mContentResolver.insert(uri, values);
        }
    }

    public void addAllowedKeyIdForApp(Uri uri, long allowedKeyId) {
        ContentValues values = new ContentValues();
        values.put(ApiAllowedKeys.KEY_ID, allowedKeyId);
        mContentResolver.insert(uri, values);
    }

    public byte[] getApiAppCertificate(String packageName) {
        Uri queryUri = ApiApps.buildByPackageNameUri(packageName);

        String[] projection = new String[]{ApiApps.PACKAGE_CERTIFICATE};

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

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }
}
