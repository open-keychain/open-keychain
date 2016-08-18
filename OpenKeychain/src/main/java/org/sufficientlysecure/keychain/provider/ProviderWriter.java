package org.sufficientlysecure.keychain.provider;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.util.LongSparseArray;

import android.util.Pair;
import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.keyimport.EncryptedSecretKeyRing;
import org.sufficientlysecure.keychain.operations.results.ConsolidateResult.WriteKeyRingsResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing.SecretKeyRingType;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.PgpKeyOperation;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedPublicKey;
import org.sufficientlysecure.keychain.pgp.WrappedSignature;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor.EncryptDecryptException;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor.IncorrectPassphraseException;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UpdatedKeys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.KeyringPassphrases;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;
import org.sufficientlysecure.keychain.util.Utf8Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProviderWriter {
    private ProviderHelper mProviderHelper;
    private ContentResolver mContentResolver;
    private Context mContext;

    private ProviderWriter(ProviderHelper helper, ContentResolver resolver) {
        mProviderHelper = helper;
        mContentResolver = resolver;
        mContext = mProviderHelper.mContext;
    }

    public static ProviderWriter newInstance(ProviderHelper helper, ContentResolver resolver) {
        return new ProviderWriter(helper, resolver);
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
        selfCertsAreTrusted = selfCertsAreTrusted || mProviderHelper.mConsolidatingOwnPublic;

        // start with ok result
        int result = SaveKeyringResult.SAVED_PUBLIC;

        long masterKeyId = keyRing.getMasterKeyId();
        UncachedPublicKey masterKey = keyRing.getPublicKey();

        ArrayList<ContentProviderOperation> operations;
        try {

            mProviderHelper.log(LogType.MSG_IP_PREPARE);
            result += 1;

            // save all keys and userIds included in keyRing object in database
            operations = new ArrayList<>();

            mProviderHelper.log(LogType.MSG_IP_INSERT_KEYRING);
            { // insert keyring
                ContentValues values = new ContentValues();
                values.put(KeyRingData.MASTER_KEY_ID, masterKeyId);
                try {
                    values.put(KeyRingData.KEY_RING_DATA, keyRing.getEncoded());
                } catch (IOException e) {
                    mProviderHelper.log(LogType.MSG_IP_ENCODE_FAIL);
                    return SaveKeyringResult.RESULT_ERROR;
                }

                Uri uri = KeyRingData.buildPublicKeyRingUri(masterKeyId);
                operations.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
            }

            mProviderHelper.log(LogType.MSG_IP_INSERT_SUBKEYS);
            progress.setProgress(LogType.MSG_IP_INSERT_SUBKEYS.getMsgId(), 40, 100);
            result += 1;
            { // insert subkeys
                Uri uri = Keys.buildKeysUri(masterKeyId);
                int rank = 0;
                for (CanonicalizedPublicKey key : keyRing.publicKeyIterator()) {
                    long keyId = key.getKeyId();
                    mProviderHelper.log(keyId == masterKeyId ? LogType.MSG_IP_MASTER : LogType.MSG_IP_SUBKEY,
                            KeyFormattingUtils.convertKeyIdToHex(keyId)
                    );
                    result += 1;

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
                            mProviderHelper.log(LogType.MSG_IP_MASTER_FLAGS_UNSPECIFIED);
                        } else {
                            mProviderHelper.log(LOG_TYPES_FLAG_MASTER[(c ? 1 : 0) + (e ? 2 : 0) + (s ? 4 : 0) + (a ? 8 : 0)]);
                        }
                    } else {
                        if (key.getKeyUsage() == null) {
                            mProviderHelper.log(LogType.MSG_IP_SUBKEY_FLAGS_UNSPECIFIED);
                        } else {
                            mProviderHelper.log(LOG_TYPES_FLAG_SUBKEY[(c ? 1 : 0) + (e ? 2 : 0) + (s ? 4 : 0) + (a ? 8 : 0)]);
                        }
                    }

                    Date creation = key.getCreationTime();
                    values.put(Keys.CREATION, creation.getTime() / 1000);
                    Date expiryDate = key.getExpiryTime();
                    if (expiryDate != null) {
                        values.put(Keys.EXPIRY, expiryDate.getTime() / 1000);
                        if (key.isExpired()) {
                            mProviderHelper.log(keyId == masterKeyId ?
                                            LogType.MSG_IP_MASTER_EXPIRED : LogType.MSG_IP_SUBKEY_EXPIRED,
                                    expiryDate.toString());
                        } else {
                            mProviderHelper.log(keyId == masterKeyId ?
                                            LogType.MSG_IP_MASTER_EXPIRES : LogType.MSG_IP_SUBKEY_EXPIRES,
                                    expiryDate.toString());
                        }
                    }

                    operations.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
                    ++rank;
                    result -= 1;
                }
            }
            result -= 1;

            // get a list of owned secret keys, for verification filtering
            LongSparseArray<CanonicalizedPublicKey> trustedKeys = getTrustedMasterKeys();

            // classify and order user ids. primary are moved to the front, revoked to the back,
            // otherwise the order in the keyfile is preserved.
            List<UserPacketItem> uids = new ArrayList<>();

            if (trustedKeys.size() == 0) {
                mProviderHelper.log(LogType.MSG_IP_UID_CLASSIFYING_ZERO);
            } else {
                mProviderHelper.log(LogType.MSG_IP_UID_CLASSIFYING, trustedKeys.size());
            }
            result += 1;
            for (byte[] rawUserId : masterKey.getUnorderedRawUserIds()) {
                String userId = Utf8Util.fromUTF8ByteArrayReplaceBadEncoding(rawUserId);
                UserPacketItem item = new UserPacketItem();
                uids.add(item);
                OpenPgpUtils.UserId splitUserId =  KeyRing.splitUserId(userId);
                item.userId = userId;
                item.name = splitUserId.name;
                item.email = splitUserId.email;
                item.comment = splitUserId.comment;
                int unknownCerts = 0;

                mProviderHelper.log(LogType.MSG_IP_UID_PROCESSING, userId);
                result += 1;
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
                            mProviderHelper.log(LogType.MSG_IP_UID_REVOKED);
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
                            mProviderHelper.log(LogType.MSG_IP_UID_CERT_BAD);
                            continue;
                        }

                        mProviderHelper.log(cert.isRevocation()
                                        ? LogType.MSG_IP_UID_CERT_GOOD_REVOKE
                                        : LogType.MSG_IP_UID_CERT_GOOD,
                                KeyFormattingUtils.convertKeyIdToHexShort(trustedKey.getKeyId())
                        );

                        // check if there is a previous certificate
                        WrappedSignature prev = item.trustedCerts.get(cert.getKeyId());
                        if (prev != null) {
                            // if it's newer, skip this one
                            if (prev.getCreationTime().after(cert.getCreationTime())) {
                                mProviderHelper.log(LogType.MSG_IP_UID_CERT_OLD);
                                continue;
                            }
                            // if the previous one was a non-revokable certification, no need to look further
                            if (!prev.isRevocation() && !prev.isRevokable()) {
                                mProviderHelper.log(LogType.MSG_IP_UID_CERT_NONREVOKE);
                                continue;
                            }
                            mProviderHelper.log(LogType.MSG_IP_UID_CERT_NEW);
                        }
                        item.trustedCerts.put(cert.getKeyId(), cert);

                    } catch (PgpGeneralException e) {
                        mProviderHelper.log(LogType.MSG_IP_UID_CERT_ERROR,
                                KeyFormattingUtils.convertKeyIdToHex(cert.getKeyId()));
                    }

                }

                if (unknownCerts > 0) {
                    mProviderHelper.log(LogType.MSG_IP_UID_CERTS_UNKNOWN, unknownCerts);
                }
                result -= 1;

            }
            result -= 1;

            ArrayList<WrappedUserAttribute> userAttributes = masterKey.getUnorderedUserAttributes();
            // Don't spam the log if there aren't even any attributes
            if (!userAttributes.isEmpty()) {
                mProviderHelper.log(LogType.MSG_IP_UAT_CLASSIFYING);
            }

            result += 1;
            for (WrappedUserAttribute userAttribute : userAttributes) {

                UserPacketItem item = new UserPacketItem();
                uids.add(item);
                item.type = userAttribute.getType();
                item.attributeData = userAttribute.getEncoded();

                int unknownCerts = 0;

                switch (item.type) {
                    case WrappedUserAttribute.UAT_IMAGE:
                        mProviderHelper.log(LogType.MSG_IP_UAT_PROCESSING_IMAGE);
                        break;
                    default:
                        mProviderHelper.log(LogType.MSG_IP_UAT_PROCESSING_UNKNOWN);
                        break;
                }
                result += 1;
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
                            mProviderHelper.log(LogType.MSG_IP_UAT_REVOKED);
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
                            mProviderHelper.log(LogType.MSG_IP_UAT_CERT_BAD);
                            continue;
                        }

                        mProviderHelper.log(cert.isRevocation()
                                        ? LogType.MSG_IP_UAT_CERT_GOOD_REVOKE
                                        : LogType.MSG_IP_UAT_CERT_GOOD,
                                KeyFormattingUtils.convertKeyIdToHexShort(trustedKey.getKeyId())
                        );

                        // check if there is a previous certificate
                        WrappedSignature prev = item.trustedCerts.get(cert.getKeyId());
                        if (prev != null) {
                            // if it's newer, skip this one
                            if (prev.getCreationTime().after(cert.getCreationTime())) {
                                mProviderHelper.log(LogType.MSG_IP_UAT_CERT_OLD);
                                continue;
                            }
                            // if the previous one was a non-revokable certification, no need to look further
                            if (!prev.isRevocation() && !prev.isRevokable()) {
                                mProviderHelper.log(LogType.MSG_IP_UAT_CERT_NONREVOKE);
                                continue;
                            }
                            mProviderHelper.log(LogType.MSG_IP_UAT_CERT_NEW);
                        }
                        item.trustedCerts.put(cert.getKeyId(), cert);

                    } catch (PgpGeneralException e) {
                        mProviderHelper.log(LogType.MSG_IP_UAT_CERT_ERROR,
                                KeyFormattingUtils.convertKeyIdToHex(cert.getKeyId()));
                    }

                }

                if (unknownCerts > 0) {
                    mProviderHelper.log(LogType.MSG_IP_UAT_CERTS_UNKNOWN, unknownCerts);
                }
                result -= 1;

            }
            result -= 1;

            progress.setProgress(LogType.MSG_IP_UID_REORDER.getMsgId(), 65, 100);
            mProviderHelper.log(LogType.MSG_IP_UID_REORDER);
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
            mProviderHelper.log(LogType.MSG_IP_ERROR_IO_EXC);
            Log.e(Constants.TAG, "IOException during import", e);
            return SaveKeyringResult.RESULT_ERROR;
        } finally {
            result -= 1;
        }

        // before deleting key, retrieve it's last updated time
        final int INDEX_MASTER_KEY_ID = 0;
        final int INDEX_LAST_UPDATED = 1;
        Cursor lastUpdatedCursor = mContentResolver.query(
                UpdatedKeys.CONTENT_URI,
                new String[]{
                        UpdatedKeys.MASTER_KEY_ID,
                        UpdatedKeys.LAST_UPDATED
                },
                UpdatedKeys.MASTER_KEY_ID + " = ?",
                new String[]{"" + masterKeyId},
                null
        );
        if (lastUpdatedCursor.moveToNext()) {
            // there was an entry to re-insert
            // this operation must happen after the new key is inserted
            ContentValues lastUpdatedEntry = new ContentValues(2);
            lastUpdatedEntry.put(UpdatedKeys.MASTER_KEY_ID,
                    lastUpdatedCursor.getLong(INDEX_MASTER_KEY_ID));
            lastUpdatedEntry.put(UpdatedKeys.LAST_UPDATED,
                    lastUpdatedCursor.getLong(INDEX_LAST_UPDATED));
            operations.add(
                    ContentProviderOperation
                            .newInsert(UpdatedKeys.CONTENT_URI)
                            .withValues(lastUpdatedEntry)
                            .build()
            );
        }
        lastUpdatedCursor.close();

        try {
            // delete old version of this keyRing, which also deletes all keys and userIds on cascade
            int deleted = mContentResolver.delete(
                    KeyRingData.buildPublicKeyRingUri(masterKeyId), null, null);
            if (deleted > 0) {
                mProviderHelper.log(LogType.MSG_IP_DELETE_OLD_OK);
                result |= SaveKeyringResult.UPDATED;
            } else {
                mProviderHelper.log(LogType.MSG_IP_DELETE_OLD_FAIL);
            }

            mProviderHelper.log(LogType.MSG_IP_APPLY_BATCH);
            progress.setProgress(LogType.MSG_IP_APPLY_BATCH.getMsgId(), 75, 100);
            mContentResolver.applyBatch(KeychainContract.CONTENT_AUTHORITY, operations);

            mProviderHelper.log(LogType.MSG_IP_SUCCESS);
            progress.setProgress(LogType.MSG_IP_SUCCESS.getMsgId(), 90, 100);
            return result;

        } catch (RemoteException e) {
            mProviderHelper.log(LogType.MSG_IP_ERROR_REMOTE_EX);
            Log.e(Constants.TAG, "RemoteException during import", e);
            return SaveKeyringResult.RESULT_ERROR;
        } catch (OperationApplicationException e) {
            mProviderHelper.log(LogType.MSG_IP_ERROR_OP_EXC);
            Log.e(Constants.TAG, "OperationApplicationException during import", e);
            return SaveKeyringResult.RESULT_ERROR;
        }

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

    /**
     * Encrypts and saves an UncachedKeyRing of the secret variant into the db,
     * overriding any existing data for the keyring.
     * This method will fail if no corresponding public keyring is in the database!
     */
    private int saveCanonicalizedSecretKeyRing(CanonicalizedSecretKeyRing keyRing,
                                               KeyringPassphrases passphrases, boolean skipReEncryption) {

        long masterKeyId = keyRing.getMasterKeyId();
        mProviderHelper.log(LogType.MSG_IS, KeyFormattingUtils.convertKeyIdToHex(masterKeyId));
        mProviderHelper.mIndent += 1;

        try {

            // IF this is successful, it's a secret key
            int result = SaveKeyringResult.SAVED_SECRET;

            // decide what sort of keyring this is as well
            SecretKeyRingType keyRingType = SecretKeyRingType.PASSPHRASE;

            byte[] keyData;

            // skip re-encryption for tests
            if (skipReEncryption) {
                try {
                    keyData = keyRing.getEncoded();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "Failed to encode key!", e);
                    mProviderHelper.log(LogType.MSG_IS_ERROR_IO_EXC);
                    return SaveKeyringResult.RESULT_ERROR;
                }
            } else {
                // remove s2k encryption on individual keys
                PgpKeyOperation op = new PgpKeyOperation(null);
                PgpEditKeyResult editResult = op.removeEncryption(keyRing, passphrases);
                mProviderHelper.getLog().add(editResult, mProviderHelper.mIndent);
                if (!editResult.success()) {
                    mProviderHelper.log(LogType.MSG_IS_ERROR_REMOVING_PASSPHRASES);
                    return SaveKeyringResult.RESULT_ERROR;
                }
                keyRing = (CanonicalizedSecretKeyRing) editResult.getRing().canonicalize(mProviderHelper.getLog(), mProviderHelper.mIndent);

                // get passphrase for keyring block encryption, or use empty passphrase if no obvious one exists
                Passphrase passphrase = passphrases.mKeyringPassphrase;
                if (passphrase == null) {
                    // guess from subkeys' passphrases
                    for (CanonicalizedSecretKey key: keyRing.secretKeyIterator()) {
                        Passphrase current = passphrases.mSubkeyPassphrases.get(key.getKeyId());
                        if(current != null && !current.isEmpty()) {
                            passphrase = current;
                            break;
                        }
                    }
                    // use empty passphrase
                    passphrase = (passphrase == null) ? new Passphrase() : passphrase;
                }

                if (passphrase.isEmpty()) {
                    keyRingType = SecretKeyRingType.PASSPHRASE_EMPTY;
                }

                // encrypt secret keyring block
                try {
                    keyData = ByteArrayEncryptor.encryptByteArray(keyRing.getEncoded(), passphrase.getCharArray());
                } catch (EncryptDecryptException | IOException e) {
                    Log.e(Constants.TAG, "Encryption went wrong.", e);
                    mProviderHelper.log(LogType.MSG_IS_ERROR_IO_ENCRYPT);
                    return SaveKeyringResult.RESULT_ERROR;
                }
            }

            // insert secret keyring
            {
                ContentValues values = new ContentValues();
                values.put(KeyRingData.MASTER_KEY_ID, masterKeyId);
                values.put(KeyRingData.KEY_RING_DATA, keyData);
                values.put(KeyRingData.SECRET_RING_TYPE, keyRingType.getNum());
                values.put(KeyRingData.AWAITING_MERGE, 0);
                Uri uri = KeyRingData.buildSecretKeyRingUri(masterKeyId);
                // delete whatever lies there first
                mContentResolver.delete(uri, null, null);
                if (mContentResolver.insert(uri, values) == null) {
                    mProviderHelper.log(LogType.MSG_IS_DB_EXCEPTION);
                    return SaveKeyringResult.RESULT_ERROR;
                }
            }

            // save subkey data
            {
                Uri uri = Keys.buildKeysUri(masterKeyId);

                // first, mark all keys as not available
                ContentValues values = new ContentValues();
                values.put(Keys.HAS_SECRET, SecretKeyType.GNU_DUMMY.getNum());
                mContentResolver.update(uri, values, null, null);

                // then, mark exactly the keys we have available
                mProviderHelper.log(LogType.MSG_IS_IMPORTING_SUBKEYS);
                mProviderHelper.mIndent += 1;
                for (CanonicalizedSecretKey sub : keyRing.secretKeyIterator()) {
                    long id = sub.getKeyId();
                    SecretKeyType mode = sub.getSecretKeyTypeSuperExpensive();
                    values.put(Keys.HAS_SECRET, mode.getNum());
                    int upd = mContentResolver.update(uri, values, Keys.KEY_ID + " = ?",
                            new String[]{Long.toString(id)});
                    if (upd == 1) {
                        switch (mode) {
                            case PASSPHRASE:
                                mProviderHelper.log(LogType.MSG_IS_SUBKEY_OK,
                                        KeyFormattingUtils.convertKeyIdToHex(id)
                                );
                                break;
                            case PASSPHRASE_EMPTY:
                                mProviderHelper.log(LogType.MSG_IS_SUBKEY_EMPTY,
                                        KeyFormattingUtils.convertKeyIdToHex(id)
                                );
                                break;
                            case GNU_DUMMY:
                                mProviderHelper.log(LogType.MSG_IS_SUBKEY_STRIPPED,
                                        KeyFormattingUtils.convertKeyIdToHex(id)
                                );
                                break;
                            case DIVERT_TO_CARD:
                                mProviderHelper.log(LogType.MSG_IS_SUBKEY_DIVERT,
                                        KeyFormattingUtils.convertKeyIdToHex(id)
                                );
                                break;
                        }
                    } else {
                        mProviderHelper.log(LogType.MSG_IS_SUBKEY_NONEXISTENT,
                                KeyFormattingUtils.convertKeyIdToHex(id)
                        );
                    }
                }
                mProviderHelper.mIndent -= 1;

                // this implicitly leaves all keys which were not in the secret key ring
                // with has_secret = 1
            }

            mProviderHelper.log(LogType.MSG_IS_SUCCESS);
            return result;

        } finally {
            mProviderHelper.mIndent -= 1;
        }

    }

    public SaveKeyringResult savePublicKeyRing(UncachedKeyRing keyRing) {
        return savePublicKeyRing(keyRing, new ProgressScaler(), null);
    }

    /**
     * Save a public keyring into the database.
     * <p/>
     * This is a high level method, which takes care of merging all new information into the old and
     * keep public and secret keyrings in sync.
     */
    public SaveKeyringResult savePublicKeyRing(UncachedKeyRing publicRing, Progressable progress, String expectedFingerprint) {

        try {
            long masterKeyId = publicRing.getMasterKeyId();
            mProviderHelper.log(LogType.MSG_IP, KeyFormattingUtils.convertKeyIdToHex(masterKeyId));
            mProviderHelper.mIndent += 1;

            if (publicRing.isSecret()) {
                mProviderHelper.log(LogType.MSG_IP_BAD_TYPE_SECRET);
                return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
            }

            CanonicalizedPublicKeyRing canPublicRing;

            // If there is an old keyring, merge it
            try {
                UncachedKeyRing oldPublicRing = mProviderHelper.read().getCanonicalizedPublicKeyRing(masterKeyId).getUncachedKeyRing();

                // Merge data from new public ring into the old one
                mProviderHelper.log(LogType.MSG_IP_MERGE_PUBLIC);
                publicRing = oldPublicRing.merge(publicRing, mProviderHelper.getLog(), mProviderHelper.mIndent);

                // If this is null, there is an error in the log so we can just return
                if (publicRing == null) {
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
                }

                // Canonicalize this keyring, to assert a number of assumptions made about it.
                canPublicRing = (CanonicalizedPublicKeyRing) publicRing.canonicalize(mProviderHelper.getLog(), mProviderHelper.mIndent);
                if (canPublicRing == null) {
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
                }

                // Early breakout if nothing changed
                if (Arrays.hashCode(publicRing.getEncoded())
                        == Arrays.hashCode(oldPublicRing.getEncoded())) {
                    mProviderHelper.log(LogType.MSG_IP_SUCCESS_IDENTICAL);
                    return new SaveKeyringResult(SaveKeyringResult.UPDATED, mProviderHelper.getLog(), null);
                }
            } catch (ProviderReader.NotFoundException e) {
                // Not an issue, just means we are dealing with a new keyring.

                // Canonicalize this keyring, to assert a number of assumptions made about it.
                canPublicRing = (CanonicalizedPublicKeyRing) publicRing.canonicalize(mProviderHelper.getLog(), mProviderHelper.mIndent);
                if (canPublicRing == null) {
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
                }
            }

            // If we have an expected fingerprint, make sure it matches
            if (expectedFingerprint != null) {
                if (!canPublicRing.containsBoundSubkey(expectedFingerprint)) {
                    mProviderHelper.log(LogType.MSG_IP_FINGERPRINT_ERROR);
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
                } else {
                    mProviderHelper.log(LogType.MSG_IP_FINGERPRINT_OK);
                }
            }

            boolean hasSecretKeyRing = hasSecretKeyRing(publicRing.getMasterKeyId());

            if (!hasSecretKeyRing) {
                // No secret key available (this is what happens most of the time)
                // simply save the public one and return
                int result = saveCanonicalizedPublicKeyRing(canPublicRing, progress, false);
                return new SaveKeyringResult(result, mProviderHelper.getLog(), null);

            } else {
                // Get the secret key data & place it back later
                // if we have the passphrase to decode the secret key, merge new data
                // else, schedule a merge to occur when the secret key is next retrieved

                Passphrase passphrase = null;
                UncachedKeyRing secretRing = null;
                CanonicalizedSecretKeyRing canSecretRing = null;
                EncryptedSecretKeyRing secretKeyData = null;
                try {
                    passphrase = PassphraseCacheService.getCachedPassphrase(mContext,
                            publicRing.getMasterKeyId());
                    if (passphrase != null) {
                        secretRing = mProviderHelper.read().getCanonicalizedSecretKeyRing(publicRing.getMasterKeyId(), passphrase)
                                .getUncachedKeyRing();
                    }
                } catch (PassphraseCacheService.KeyNotFoundException | ProviderReader.NotFoundException ignored) {}

                if (secretRing != null) {
                    // we got our secret ring, merge data from new public ring into secret one
                    mProviderHelper.log(LogType.MSG_IP_MERGE_SECRET);
                    secretRing = secretRing.merge(publicRing, mProviderHelper.getLog(), mProviderHelper.mIndent);
                    if (secretRing == null) {
                        return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
                    }

                    // This has always been a secret key ring, this is a safe cast
                    canSecretRing = (CanonicalizedSecretKeyRing) secretRing.canonicalize(mProviderHelper.getLog(), mProviderHelper.mIndent);
                    if (canSecretRing == null) {
                        return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
                    }
                } else {
                    // set a flag so merge will occur the next time we retrieve secret keyring
                    secretKeyData = getSecretKeyRingData(masterKeyId);
                    secretKeyData.mAwaitingMerge = true;
                }

                // save the public key
                int result = saveCanonicalizedPublicKeyRing(canPublicRing, progress, true);
                progress.setProgress(LogType.MSG_IP_REINSERT_SECRET.getMsgId(), 90, 100);

                // the secret key data has been wiped from db after saving the public key
                // reinsert previous secret data or save merged secret key, whichever is appropriate
                int secretResult = (secretRing == null)
                        ? writeSecretKeyRingToDb(secretKeyData).getResult()
                        : saveCanonicalizedSecretKeyRing(canSecretRing,
                        new KeyringPassphrases(masterKeyId, passphrase), false);

                if ((secretResult & SaveKeyringResult.RESULT_ERROR) != SaveKeyringResult.RESULT_ERROR) {
                    result |= SaveKeyringResult.SAVED_SECRET;
                }
                return new SaveKeyringResult(result, mProviderHelper.getLog(), canSecretRing);
            }

        } catch (IOException e) {
            mProviderHelper.log(LogType.MSG_IP_ERROR_IO_EXC);
            return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
        } catch (EncryptDecryptException e) {
            mProviderHelper.log(LogType.MSG_IP_ERROR_IO_EXC);
            return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
        } catch (IncorrectPassphraseException e) {
            mProviderHelper.log(LogType.MSG_IP_ERROR_PASSPHRASE_CACHE);
            return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
        } finally {
            mProviderHelper.mIndent -= 1;
        }

    }

    /**
     * Returns secret keyring blob, as well as all other related fields
     */
    public EncryptedSecretKeyRing getSecretKeyRingData(long masterKeyId) {
        EncryptedSecretKeyRing data = null;
        final int INDEX_SECRET_KEY_BLOB = 0;
        final int INDEX_AWAITING_MERGE_INT = 1;
        final int INDEX_HAS_SECRET_RING = 2;
        Cursor secretKeyCursor = mContentResolver.query(
                KeyRingData.buildSecretKeyRingUri(masterKeyId),
                new String[]{ KeyRingData.KEY_RING_DATA,
                        KeyRingData.AWAITING_MERGE,
                        KeyRingData.SECRET_RING_TYPE},
                null, null, null);
        if (secretKeyCursor != null && secretKeyCursor.moveToNext()) {
            data = new EncryptedSecretKeyRing(
                    secretKeyCursor.getBlob(INDEX_SECRET_KEY_BLOB),
                    masterKeyId,
                    secretKeyCursor.getInt(INDEX_AWAITING_MERGE_INT) == 1,
                    secretKeyCursor.getInt(INDEX_HAS_SECRET_RING),
                    getSubKeyIdsAndType(masterKeyId)
            );
            secretKeyCursor.close();
        }
        return data;
    }

    private boolean hasSecretKeyRing(long masterKeyId) {

        Cursor cursor = mContentResolver.query(KeyRings.buildUnifiedKeyRingUri(masterKeyId),
                new String[]{ KeyRings.HAS_ANY_SECRET },
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            boolean hasSecret = cursor.getInt(0) > 0;
            cursor.close();
            return hasSecret;
        } else {
            return false;
        }
    }

    public boolean hasSecretKeys() {
        Uri uri = KeychainContract.KeyRingData.buildSecretKeyRingUri();
        Cursor cursor = mContentResolver.query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            cursor.close();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Only for import testing, where we lack passphrases for the test keyrings
     */
    public SaveKeyringResult saveSecretKeyRingForTest(UncachedKeyRing secretRing) {
        return saveSecretKeyRingHelper(secretRing, null, new ProgressScaler(), true);
    }

    public SaveKeyringResult saveSecretKeyRing(UncachedKeyRing secretRing, KeyringPassphrases keyringPassphrases,
                                               Progressable progress) {
        return saveSecretKeyRingHelper(secretRing, keyringPassphrases, progress, false);
    }

    private SaveKeyringResult saveSecretKeyRingHelper(UncachedKeyRing secretRing, KeyringPassphrases keyringPassphrases,
                                                      Progressable progress, boolean skipReEncryption) {
        try {
            long masterKeyId = secretRing.getMasterKeyId();
            mProviderHelper.log(LogType.MSG_IS, KeyFormattingUtils.convertKeyIdToHex(masterKeyId));
            mProviderHelper.mIndent += 1;

            if (!secretRing.isSecret()) {
                mProviderHelper.log(LogType.MSG_IS_BAD_TYPE_PUBLIC);
                return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
            }

            CanonicalizedSecretKeyRing canSecretRing = (CanonicalizedSecretKeyRing) secretRing.canonicalize(mProviderHelper.getLog(), mProviderHelper.mIndent);

            if (canSecretRing == null) {
                // Special case: If keyring canonicalization failed, try again after adding
                // all self-certificates from the public key.
                try {
                    mProviderHelper.log(LogType.MSG_IS_MERGE_SPECIAL);
                    UncachedKeyRing oldPublicRing = mProviderHelper.read().getCanonicalizedPublicKeyRing(masterKeyId).getUncachedKeyRing();
                    secretRing = secretRing.merge(oldPublicRing, mProviderHelper.getLog(), mProviderHelper.mIndent);
                    canSecretRing = (CanonicalizedSecretKeyRing) secretRing.canonicalize(mProviderHelper.getLog(), mProviderHelper.mIndent);
                } catch (ProviderReader.NotFoundException e2) {
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
                }
            }

            // Merge new data into public keyring, if there is any
            UncachedKeyRing publicRing;
            try {
                UncachedKeyRing oldPublicRing = mProviderHelper.read().getCanonicalizedPublicKeyRing(masterKeyId).getUncachedKeyRing();

                // Merge data from new secret ring into public one
                mProviderHelper.log(LogType.MSG_IS_MERGE_PUBLIC);
                publicRing = oldPublicRing.merge(secretRing, mProviderHelper.getLog(), mProviderHelper.mIndent);
                if (publicRing == null) {
                    return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
                }

            } catch (ProviderReader.NotFoundException e) {
                mProviderHelper.log(LogType.MSG_IS_PUBRING_GENERATE);
                publicRing = secretRing.extractPublicKeyRing();
            }

            CanonicalizedPublicKeyRing canPublicRing = (CanonicalizedPublicKeyRing) publicRing.canonicalize(mProviderHelper.getLog(),
                    mProviderHelper.mIndent);
            if (canPublicRing == null) {
                return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
            }

            int result;

            result = saveCanonicalizedPublicKeyRing(canPublicRing, progress, true);
            if ((result & SaveKeyringResult.RESULT_ERROR) == SaveKeyringResult.RESULT_ERROR) {
                return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
            }

            progress.setProgress(LogType.MSG_IP_REINSERT_SECRET.getMsgId(), 90, 100);
            result = saveCanonicalizedSecretKeyRing(canSecretRing, keyringPassphrases, skipReEncryption);

            return new SaveKeyringResult(result, mProviderHelper.getLog(), canSecretRing);

        } catch (IOException e) {
            mProviderHelper.log(LogType.MSG_IS_ERROR_IO_EXC);
            return new SaveKeyringResult(SaveKeyringResult.RESULT_ERROR, mProviderHelper.getLog(), null);
        } finally {
            mProviderHelper.mIndent -= 1;
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

    public String getKeyRingAsArmoredString(Uri uri)
            throws ProviderReader.NotFoundException, IOException, PgpGeneralException {
        byte[] data = (byte[]) mProviderHelper.read().getGenericData(
                uri, KeyRingData.KEY_RING_DATA, Cursor.FIELD_TYPE_BLOB);
        return getKeyRingAsArmoredString(data);
    }

    private String getKeyRingAsArmoredString(byte[] data) throws IOException, PgpGeneralException {
        UncachedKeyRing keyRing = UncachedKeyRing.decodeFromData(data);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        keyRing.encodeArmored(bos, null);
        String armoredKey = bos.toString("UTF-8");

        Log.d(Constants.TAG, "armoredKey:" + armoredKey);

        return armoredKey;
    }

    public Uri renewKeyLastUpdatedTime(long masterKeyId, long time, TimeUnit timeUnit) {
        ContentValues values = new ContentValues();
        values.put(UpdatedKeys.MASTER_KEY_ID, masterKeyId);
        values.put(UpdatedKeys.LAST_UPDATED, timeUnit.toSeconds(time));

        return mContentResolver.insert(UpdatedKeys.CONTENT_URI, values);
    }

    public WriteKeyRingsResult writeSecretKeyRingToDb(@NonNull EncryptedSecretKeyRing data) {
        ArrayList<EncryptedSecretKeyRing> container = new ArrayList<>();
        container.add(data);
        return writeSecretKeyRingsToDb(container.iterator(), container.size());
    }

    /**
     * Writes encrypted keyring block and related data directly into db, returning error on failure
     * EncryptedSecretKeyRings are trusted to be valid.
     */
    public WriteKeyRingsResult writeSecretKeyRingsToDb(Iterator<EncryptedSecretKeyRing> it, int numRings) {
        OperationLog log = new OperationLog();
        int indent = 0;
        ContentResolver contentResolver = mContext.getContentResolver();
        log.add(LogType.MSG_WRITE, indent, numRings);

        indent += 1;
        while(it.hasNext()) {
            EncryptedSecretKeyRing encryptedRing = it.next();
            long masterKeyId = encryptedRing.mMasterKeyId;
            log.add(LogType.MSG_WS, indent, KeyFormattingUtils.convertKeyIdToHex(masterKeyId));
            indent += 1;

            // 1. save secret keyring
            ContentValues values = new ContentValues();
            values.put(KeyRingData.MASTER_KEY_ID, masterKeyId);
            values.put(KeyRingData.KEY_RING_DATA, encryptedRing.mBytes);
            values.put(KeyRingData.AWAITING_MERGE, encryptedRing.mAwaitingMerge);
            values.put(KeyRingData.SECRET_RING_TYPE, encryptedRing.mKeyRingType);
            Uri uri = KeyRingData.buildSecretKeyRingUri(masterKeyId);
            if (contentResolver.insert(uri, values) == null) {
                log.add(LogType.MSG_WS_DB_EXCEPTION, indent);
                return new WriteKeyRingsResult(OperationResult.RESULT_ERROR, log);
            }

            // 2. insert subkey info
            uri = Keys.buildKeysUri(masterKeyId);

            // first, mark all keys as not available
            values = new ContentValues();
            values.put(Keys.HAS_SECRET, SecretKeyType.GNU_DUMMY.getNum());
            contentResolver.update(uri, values, null, null);

            // then, mark exactly the keys we have available
            log.add(LogType.MSG_WS_WRITING_SUBKEY_DATA, indent);
            indent += 1;
            for (Pair<Long, Integer> subKeyIdWithType : encryptedRing.mSubKeyIdsAndType) {
                long id = subKeyIdWithType.first;
                int subKeyType = subKeyIdWithType.second;
                values.put(Keys.HAS_SECRET, subKeyType);
                int upd = contentResolver.update(uri, values, Keys.KEY_ID + " = ?",
                        new String[]{Long.toString(id)});
                if (upd == 1) {
                    switch (SecretKeyType.values()[subKeyType]) {
                        case PASSPHRASE:
                            log.add(LogType.MSG_WS_SUBKEY_OK, indent,
                                    KeyFormattingUtils.convertKeyIdToHex(id)
                            );
                            break;
                        case PASSPHRASE_EMPTY:
                            log.add(LogType.MSG_WS_SUBKEY_EMPTY, indent,
                                    KeyFormattingUtils.convertKeyIdToHex(id)
                            );
                            break;
                        case PIN:
                            log.add(LogType.MSG_WS_SUBKEY_PIN, indent,
                                    KeyFormattingUtils.convertKeyIdToHex(id)
                            );
                            break;
                        case GNU_DUMMY:
                            log.add(LogType.MSG_WS_SUBKEY_STRIPPED, indent,
                                    KeyFormattingUtils.convertKeyIdToHex(id)
                            );
                            break;
                        case DIVERT_TO_CARD:
                            log.add(LogType.MSG_WS_SUBKEY_DIVERT, indent,
                                    KeyFormattingUtils.convertKeyIdToHex(id)
                            );
                            break;
                    }
                } else {
                    log.add(LogType.MSG_WS_SUBKEY_NONEXISTENT, indent,
                            KeyFormattingUtils.convertKeyIdToHex(id)
                    );
                }
            }
            indent -= 1;

            // this implicitly leaves all keys which were not in the secret key ring
            // with has_secret = 1
            log.add(LogType.MSG_WS_SUCCESS, indent);
        }
        indent -= 1;
        log.add(LogType.MSG_WRITE_SUCCESS, indent);
        return new WriteKeyRingsResult(OperationResult.RESULT_OK, log);
    }

    public ArrayList<Pair<Long, Integer>> getSubKeyIdsAndType(long masterKeyId) {
        ArrayList<Pair<Long, Integer>> idsAndType = new ArrayList<>();
        Cursor cursor = mContentResolver.query(KeychainContract.Keys.buildKeysUri(masterKeyId),
                new String[] {Keys.KEY_ID, Keys.HAS_SECRET,},
                Keys.HAS_SECRET + " !=0", null, null);
        if (cursor != null) {
            while(cursor.moveToNext()) {
                long subKeyId = cursor.getLong(0);
                int subKeyType = cursor.getInt(1);
                idsAndType.add(new Pair<>(subKeyId, subKeyType));
            }
            cursor.close();
        }

        return idsAndType;
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
            if ( (trustedCerts.size() == 0) != (o.trustedCerts.size() == 0) ) {
                return trustedCerts.size() > o.trustedCerts.size() ? -1 : 1;
            }
            // if one key is primary but the other isn't, the primary one always comes first
            if (isPrimary != o.isPrimary) {
                return isPrimary ? -1 : 1;
            }
            return 0;
        }
    }
}
