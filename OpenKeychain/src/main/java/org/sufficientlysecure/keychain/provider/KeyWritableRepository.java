/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.LongSparseArray;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.operations.results.UpdateTrustResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.pgp.WrappedSignature;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAutocryptPeer;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeySignatures;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UpdatedKeys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.Utf8Util;
import timber.log.Timber;


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
public class KeyWritableRepository extends KeyRepository {
    private static final int MAX_CACHED_KEY_SIZE = 1024 * 50;

    private final Context mContext;

    public static KeyWritableRepository create(Context context) {
        LocalPublicKeyStorage localPublicKeyStorage = LocalPublicKeyStorage.getInstance(context);

        return new KeyWritableRepository(context, localPublicKeyStorage);
    }

    @VisibleForTesting
    KeyWritableRepository(Context context, LocalPublicKeyStorage localPublicKeyStorage) {
        this(context, localPublicKeyStorage, new OperationLog(), 0);
    }

    private KeyWritableRepository(
            Context context, LocalPublicKeyStorage localPublicKeyStorage, OperationLog log, int indent) {
        super(context.getContentResolver(), localPublicKeyStorage, log, indent);

        mContext = context;
    }

    private LongSparseArray<CanonicalizedPublicKey> getTrustedMasterKeys() {
        Cursor cursor = mContentResolver.query(KeyRings.buildUnifiedKeyRingsUri(), new String[] {
                KeyRings.MASTER_KEY_ID,
                // we pick from cache only information that is not easily available from keyrings
                KeyRings.HAS_ANY_SECRET, KeyRings.VERIFIED
        }, KeyRings.HAS_ANY_SECRET + " = 1", null, null);

        try {
            LongSparseArray<CanonicalizedPublicKey> result = new LongSparseArray<>();

            if (cursor == null) {
                return result;
            }

            while (cursor.moveToNext()) {
                try {
                    long masterKeyId = cursor.getLong(0);
                    int verified = cursor.getInt(2);
                    byte[] blob = loadPublicKeyRingData(masterKeyId);
                    if (blob != null) {
                        result.put(masterKeyId, new CanonicalizedPublicKeyRing(blob, verified).getPublicKey());
                    }
                } catch (NotFoundException e) {
                    throw new IllegalStateException("Error reading secret key data, this should not happen!", e);
                }
            }

            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    // bits, in order: CESA. make SURE these are correct, we will get bad log entries otherwise!!
    private static final LogType LOG_TYPES_FLAG_MASTER[] = new LogType[]{
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
    private static final LogType LOG_TYPES_FLAG_SUBKEY[] = new LogType[]{
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
    private int saveCanonicalizedPublicKeyRing(CanonicalizedPublicKeyRing keyRing, boolean selfCertsAreTrusted) {

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
            try {
                writePublicKeyRing(keyRing, masterKeyId, operations);
            } catch (IOException e) {
                log(LogType.MSG_IP_ENCODE_FAIL);
                return SaveKeyringResult.RESULT_ERROR;
            }

            log(LogType.MSG_IP_INSERT_SUBKEYS);
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
                    values.put(Keys.IS_SECURE, key.isSecure());

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

            List<Long> signerKeyIds = new ArrayList<>();

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
                OpenPgpUtils.UserId splitUserId = KeyRing.splitUserId(userId);
                item.userId = userId;
                item.name = splitUserId.name;
                item.email = splitUserId.email;
                item.comment = splitUserId.comment;
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
                        if (!signerKeyIds.contains(certId)) {
                            operations.add(ContentProviderOperation.newInsert(KeySignatures.CONTENT_URI)
                                    .withValue(KeySignatures.MASTER_KEY_ID, masterKeyId)
                                    .withValue(KeySignatures.SIGNER_KEY_ID, certId)
                                    .build());
                            signerKeyIds.add(certId);
                        }
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
            Timber.e(e, "IOException during import");
            return SaveKeyringResult.RESULT_ERROR;
        } finally {
            mIndent -= 1;
        }

        ContentProviderOperation lastUpdateReinsertOp = getLastUpdatedReinsertOperationByMasterKeyId(masterKeyId);
        if (lastUpdateReinsertOp != null) {
            operations.add(lastUpdateReinsertOp);
        }

        try {
            // delete old version of this keyRing (from database only!), which also deletes all keys and userIds on cascade
            int deleted = mContentResolver.delete(
                    KeyRingData.buildPublicKeyRingUri(masterKeyId), null, null);
            if (deleted > 0) {
                log(LogType.MSG_IP_DELETE_OLD_OK);
                result |= SaveKeyringResult.UPDATED;
            } else {
                log(LogType.MSG_IP_DELETE_OLD_FAIL);
            }

            log(LogType.MSG_IP_APPLY_BATCH);
            mContentResolver.applyBatch(KeychainContract.CONTENT_AUTHORITY, operations);

            log(LogType.MSG_IP_SUCCESS);
            return result;

        } catch (RemoteException e) {
            log(LogType.MSG_IP_ERROR_REMOTE_EX);
            Timber.e(e, "RemoteException during import");
            return SaveKeyringResult.RESULT_ERROR;
        } catch (OperationApplicationException e) {
            log(LogType.MSG_IP_ERROR_OP_EXC);
            Timber.e(e, "OperationApplicationException during import");
            return SaveKeyringResult.RESULT_ERROR;
        }

    }

    private ContentProviderOperation getLastUpdatedReinsertOperationByMasterKeyId(long masterKeyId) {
        Long lastUpdateTime = getLastUpdateTime(masterKeyId);
        if (lastUpdateTime == null) {
            return null;
        }

        ContentValues lastUpdatedEntry = new ContentValues(2);
        lastUpdatedEntry.put(UpdatedKeys.MASTER_KEY_ID, masterKeyId);
        lastUpdatedEntry.put(UpdatedKeys.LAST_UPDATED, lastUpdateTime);
        return ContentProviderOperation
                .newInsert(UpdatedKeys.CONTENT_URI)
                .withValues(lastUpdatedEntry)
                .build();
    }

    private void writePublicKeyRing(CanonicalizedPublicKeyRing keyRing, long masterKeyId,
            ArrayList<ContentProviderOperation> operations) throws IOException {
        byte[] encodedKey = keyRing.getEncoded();
        mLocalPublicKeyStorage.writePublicKey(masterKeyId, encodedKey);

        ContentValues values = new ContentValues();
        values.put(KeyRingData.MASTER_KEY_ID, masterKeyId);
        if (encodedKey.length < MAX_CACHED_KEY_SIZE) {
            values.put(KeyRingData.KEY_RING_DATA, encodedKey);
        } else {
            values.put(KeyRingData.KEY_RING_DATA, (byte[]) null);
        }

        Uri uri = KeyRingData.buildPublicKeyRingUri(masterKeyId);
        operations.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
    }

    private Uri writeSecretKeyRing(CanonicalizedSecretKeyRing keyRing, long masterKeyId) throws IOException {
        byte[] encodedKey = keyRing.getEncoded();

        ContentValues values = new ContentValues();
        values.put(KeyRingData.MASTER_KEY_ID, masterKeyId);
        values.put(KeyRingData.KEY_RING_DATA, encodedKey);

        // insert new version of this keyRing
        Uri uri = KeyRingData.buildSecretKeyRingUri(masterKeyId);
        return mContentResolver.insert(uri, values);
    }

    public boolean deleteKeyRing(long masterKeyId) {
        try {
            mLocalPublicKeyStorage.deletePublicKey(masterKeyId);
        } catch (IOException e) {
            Timber.e(e, "Could not delete file!");
            return false;
        }
        mContentResolver.delete(ApiAutocryptPeer.buildByMasterKeyId(masterKeyId),null, null);
        int deletedRows = mContentResolver.delete(KeyRingData.buildPublicKeyRingUri(masterKeyId), null, null);
        return deletedRows > 0;
    }

    private static class UserPacketItem implements Comparable<UserPacketItem> {
        Integer type;
        String userId;
        String name;
        String email;
        String comment;
        byte[] attributeData;
        boolean isPrimary = false;
        WrappedSignature selfCert;
        WrappedSignature selfRevocation;
        LongSparseArray<WrappedSignature> trustedCerts = new LongSparseArray<>();

        @Override
        public int compareTo(@NonNull UserPacketItem o) {
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
            // if one is *trusted* but the other isn't, that one comes first
            // this overrides the primary attribute, even!
            if ((trustedCerts.size() == 0) != (o.trustedCerts.size() == 0)) {
                return trustedCerts.size() > o.trustedCerts.size() ? -1 : 1;
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
                Uri insertedUri = writeSecretKeyRing(keyRing, masterKeyId);
                if (insertedUri == null) {
                    log(LogType.MSG_IS_DB_EXCEPTION);
                    return SaveKeyringResult.RESULT_ERROR;
                }
            } catch (IOException e) {
                Timber.e(e, "Failed to encode key!");
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
                    SecretKeyType mode = sub.getSecretKeyTypeSuperExpensive();
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

    /**
     * Save a public keyring into the database.
     * <p>
     * This is a high level method, which takes care of merging all new information into the old and
     * keep public and secret keyrings in sync.
     * <p>
     * If you want to merge keys in-memory only and not save in database set skipSave=true.
     */
    public SaveKeyringResult savePublicKeyRing(UncachedKeyRing publicRing,
            byte[] expectedFingerprint,
            ArrayList<CanonicalizedKeyRing> canKeyRings,
            boolean forceRefresh,
            boolean skipSave) {

        try {
            long masterKeyId = publicRing.getMasterKeyId();
            log(LogType.MSG_IP, KeyFormattingUtils.convertKeyIdToHex(masterKeyId));
            mIndent += 1;

            if (publicRing.isSecret()) {
                log(LogType.MSG_IP_BAD_TYPE_SECRET);
                return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
            }

            CanonicalizedPublicKeyRing canPublicRing;
            boolean alreadyExists = false;

            // If there is an old keyring, merge it
            try {
                UncachedKeyRing oldPublicRing = getCanonicalizedPublicKeyRing(masterKeyId).getUncachedKeyRing();
                alreadyExists = true;

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
                if (canKeyRings != null) canKeyRings.add(canPublicRing);

                // Early breakout if nothing changed
                if (!forceRefresh && Arrays.hashCode(publicRing.getEncoded())
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
                if (canKeyRings != null) canKeyRings.add(canPublicRing);
            }

            // If there is a secret key, merge new data (if any) and save the key for later
            CanonicalizedSecretKeyRing canSecretRing;
            try {
                UncachedKeyRing secretRing = getCanonicalizedSecretKeyRing(publicRing.getMasterKeyId())
                        .getUncachedKeyRing();

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


            // If we have an expected fingerprint, make sure it matches
            if (expectedFingerprint != null) {
                if (!canPublicRing.containsBoundSubkey(expectedFingerprint)) {
                    log(LogType.MSG_IP_FINGERPRINT_ERROR);
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
                } else {
                    log(LogType.MSG_IP_FINGERPRINT_OK);
                }
            }

            int result;
            if (skipSave) {
                // skip save method, set fixed result
                result = SaveKeyringResult.SAVED_PUBLIC
                        | (alreadyExists ? SaveKeyringResult.UPDATED : 0);
            } else {
                result = saveCanonicalizedPublicKeyRing(canPublicRing, canSecretRing != null);
            }

            // Save the saved keyring (if any)
            if (canSecretRing != null) {
                int secretResult;
                if (skipSave) {
                    // skip save method, set fixed result
                    secretResult = SaveKeyringResult.SAVED_SECRET;
                } else {
                    secretResult = saveCanonicalizedSecretKeyRing(canSecretRing);
                }

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

    public SaveKeyringResult savePublicKeyRing(UncachedKeyRing publicRing, byte[] expectedFingerprint) {
        return savePublicKeyRing(publicRing, expectedFingerprint, null, false, false);
    }

    public SaveKeyringResult savePublicKeyRing(UncachedKeyRing publicRing, byte[] expectedFingerprint,
            boolean forceRefresh) {
        return savePublicKeyRing(publicRing, expectedFingerprint, null, forceRefresh, false);
    }

    public SaveKeyringResult savePublicKeyRing(UncachedKeyRing keyRing) {
        return savePublicKeyRing(keyRing, null, false);
    }

    public SaveKeyringResult savePublicKeyRing(UncachedKeyRing keyRing, boolean forceRefresh) {
        return savePublicKeyRing(keyRing, null, forceRefresh);
    }

    public SaveKeyringResult saveSecretKeyRing(UncachedKeyRing secretRing,
                                               ArrayList<CanonicalizedKeyRing> canKeyRings,
                                               boolean skipSave) {

        try {
            long masterKeyId = secretRing.getMasterKeyId();
            log(LogType.MSG_IS, KeyFormattingUtils.convertKeyIdToHex(masterKeyId));
            mIndent += 1;

            if (!secretRing.isSecret()) {
                log(LogType.MSG_IS_BAD_TYPE_PUBLIC);
                return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
            }

            CanonicalizedSecretKeyRing canSecretRing;
            boolean alreadyExists = false;

            // If there is an old secret key, merge it.
            try {
                UncachedKeyRing oldSecretRing = getCanonicalizedSecretKeyRing(masterKeyId).getUncachedKeyRing();
                alreadyExists = true;

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
                if (canKeyRings != null) canKeyRings.add(canSecretRing);

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
                if (canKeyRings != null) canKeyRings.add(canSecretRing);
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

            CanonicalizedPublicKeyRing canPublicRing = (CanonicalizedPublicKeyRing) publicRing.canonicalize(mLog,
                    mIndent);
            if (canPublicRing == null) {
                return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
            }

            int publicResult;
            if (skipSave) {
                // skip save method, set fixed result
                publicResult = SaveKeyringResult.SAVED_PUBLIC;
            } else {
                publicResult = saveCanonicalizedPublicKeyRing(canPublicRing, true);
            }

            if ((publicResult & SaveKeyringResult.RESULT_ERROR) == SaveKeyringResult.RESULT_ERROR) {
                return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
            }

            int result;
            if (skipSave) {
                // skip save method, set fixed result
                result = SaveKeyringResult.SAVED_SECRET
                        | (alreadyExists ? SaveKeyringResult.UPDATED : 0);
            } else {
                result = saveCanonicalizedSecretKeyRing(canSecretRing);
            }

            return new SaveKeyringResult(result, mLog, canSecretRing);
        } catch (IOException e) {
            log(LogType.MSG_IS_ERROR_IO_EXC);
            return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mLog, null);
        } finally {
            mIndent -= 1;
        }
    }

    public SaveKeyringResult saveSecretKeyRing(UncachedKeyRing secretRing) {
        return saveSecretKeyRing(secretRing, null, false);
    }

    @NonNull
    public UpdateTrustResult updateTrustDb(List<Long> signerMasterKeyIds, Progressable progress) {
        OperationLog log = new OperationLog();

        log.add(LogType.MSG_TRUST, 0);

        Cursor cursor;
        Preferences preferences = Preferences.getPreferences(mContext);
        boolean isTrustDbInitialized = preferences.isKeySignaturesTableInitialized();
        if (!isTrustDbInitialized) {
            log.add(LogType.MSG_TRUST_INITIALIZE, 1);
            cursor = mContentResolver.query(KeyRings.buildUnifiedKeyRingsUri(),
                    new String[] { KeyRings.MASTER_KEY_ID }, null, null, null);
        } else {
            String[] signerMasterKeyIdStrings = new String[signerMasterKeyIds.size()];
            int i = 0;
            for (Long masterKeyId : signerMasterKeyIds) {
                log.add(LogType.MSG_TRUST_KEY, 1, KeyFormattingUtils.beautifyKeyId(masterKeyId));
                signerMasterKeyIdStrings[i++] = Long.toString(masterKeyId);
            }

            cursor = mContentResolver.query(KeyRings.buildUnifiedKeyRingsFilterBySigner(),
                    new String[] { KeyRings.MASTER_KEY_ID }, null, signerMasterKeyIdStrings, null);
        }

        if (cursor == null) {
            throw new IllegalStateException();
        }

        int totalKeys = cursor.getCount();
        int processedKeys = 0;

        if (totalKeys == 0) {
            log.add(LogType.MSG_TRUST_COUNT_NONE, 1);
        } else {
            progress.setProgress(R.string.progress_update_trust, 0, totalKeys);
            log.add(LogType.MSG_TRUST_COUNT, 1, totalKeys);
        }

        try {
            while (cursor.moveToNext()) {
                try {
                    long masterKeyId = cursor.getLong(0);

                    byte[] pubKeyData = loadPublicKeyRingData(masterKeyId);
                    UncachedKeyRing uncachedKeyRing = UncachedKeyRing.decodeFromData(pubKeyData);

                    clearLog();
                    SaveKeyringResult result = savePublicKeyRing(uncachedKeyRing, true);

                    log.add(result, 1);
                    progress.setProgress(processedKeys++, totalKeys);
                } catch (NotFoundException | PgpGeneralException | IOException e) {
                    Timber.e(e, "Error updating trust database");
                    return new UpdateTrustResult(UpdateTrustResult.RESULT_ERROR, log);
                }
            }

            if (!isTrustDbInitialized) {
                preferences.setKeySignaturesTableInitialized();
            }

            log.add(LogType.MSG_TRUST_OK, 1);
            return new UpdateTrustResult(UpdateTrustResult.RESULT_OK, log);
        } finally {
            cursor.close();
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
        values.put(UserPackets.NAME, item.name);
        values.put(UserPackets.EMAIL, item.email);
        values.put(UserPackets.COMMENT, item.comment);
        values.put(UserPackets.ATTRIBUTE_DATA, item.attributeData);
        values.put(UserPackets.IS_PRIMARY, item.isPrimary);
        values.put(UserPackets.IS_REVOKED, item.selfRevocation != null);
        values.put(UserPackets.RANK, rank);

        Uri uri = UserPackets.buildUserIdsUri(masterKeyId);

        return ContentProviderOperation.newInsert(uri).withValues(values).build();
    }

    public Uri renewKeyLastUpdatedTime(long masterKeyId, boolean seenOnKeyservers) {
        boolean isFirstKeyserverStatusCheck = getSeenOnKeyservers(masterKeyId) == null;

        ContentValues values = new ContentValues();
        values.put(UpdatedKeys.MASTER_KEY_ID, masterKeyId);
        values.put(UpdatedKeys.LAST_UPDATED, GregorianCalendar.getInstance().getTimeInMillis() / 1000);
        if (seenOnKeyservers || isFirstKeyserverStatusCheck) {
            values.put(UpdatedKeys.SEEN_ON_KEYSERVERS, seenOnKeyservers);
        }

        // this will actually update/replace, doing the right thing™ for seenOnKeyservers value
        // see `KeychainProvider.insert()`
        return mContentResolver.insert(UpdatedKeys.CONTENT_URI, values);
    }

    public void resetAllLastUpdatedTimes() {
        ContentValues values = new ContentValues();
        values.putNull(UpdatedKeys.LAST_UPDATED);
        values.putNull(UpdatedKeys.SEEN_ON_KEYSERVERS);
        mContentResolver.update(UpdatedKeys.CONTENT_URI, values, null, null);
    }

}
