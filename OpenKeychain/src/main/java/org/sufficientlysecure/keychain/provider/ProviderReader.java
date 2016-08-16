package org.sufficientlysecure.keychain.provider;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor.EncryptDecryptException;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor.IncorrectPassphraseException;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.util.KeyringPassphrases;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.util.ArrayList;
import java.util.HashMap;

public class ProviderReader {
    private ProviderHelper mProviderHelper;
    private ContentResolver mContentResolver;

    protected ProviderReader(ProviderHelper helper, ContentResolver resolver) {
        mProviderHelper = helper;
        mContentResolver = resolver;

    }

    public static ProviderReader newInstance(ProviderHelper helper, ContentResolver resolver) {
        return new ProviderReader(helper, resolver);
    }

    public Object getGenericData(Uri uri, String column, int type) throws ProviderReader.NotFoundException {
        Object result = getGenericData(uri, new String[]{column}, new int[]{type}, null).get(column);
        if (result == null) {
            throw new ProviderReader.NotFoundException();
        }
        return result;
    }

    public Object getGenericData(Uri uri, String column, int type, String selection)
            throws ProviderReader.NotFoundException {
        return getGenericData(uri, new String[]{column}, new int[]{type}, selection).get(column);
    }

    public HashMap<String, Object> getGenericData(Uri uri, String[] proj, int[] types)
            throws ProviderReader.NotFoundException {
        return getGenericData(uri, proj, types, null);
    }

    public HashMap<String, Object> getGenericData(Uri uri, String[] proj, int[] types, String selection)
            throws ProviderReader.NotFoundException {
        Cursor cursor = mContentResolver.query(uri, proj, selection, null, null);

        try {
            HashMap<String, Object> result = new HashMap<>(proj.length);
            if (cursor != null && cursor.moveToFirst()) {
                int pos = 0;
                for (String p : proj) {
                    switch (types[pos]) {
                        case Cursor.FIELD_TYPE_NULL:
                            result.put(p, cursor.isNull(pos));
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            result.put(p, cursor.getLong(pos));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            result.put(p, cursor.getFloat(pos));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            result.put(p, cursor.getString(pos));
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            result.put(p, cursor.getBlob(pos));
                            break;
                    }
                    pos += 1;
                }
            } else {
                // If no data was found, throw an appropriate exception
                throw new ProviderReader.NotFoundException();
            }

            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public HashMap<String, Object> getUnifiedData(long masterKeyId, String[] proj, int[] types)
            throws ProviderReader.NotFoundException {
        return getGenericData(KeyRings.buildUnifiedKeyRingUri(masterKeyId), proj, types);
    }

    public CachedPublicKeyRing getCachedPublicKeyRing(Uri queryUri) throws PgpKeyNotFoundException {
        long masterKeyId = new CachedPublicKeyRing(mProviderHelper, queryUri).extractOrGetMasterKeyId();
        return getCachedPublicKeyRing(masterKeyId);
    }

    public CachedPublicKeyRing getCachedPublicKeyRing(long id) {
        return new CachedPublicKeyRing(mProviderHelper, KeyRings.buildUnifiedKeyRingUri(id));
    }

    public CanonicalizedPublicKeyRing getCanonicalizedPublicKeyRing(long id) throws NotFoundException {
        return getCanonicalizedPublicKeyRing(KeyRings.buildUnifiedKeyRingUri(id));
    }

    public CanonicalizedPublicKeyRing getCanonicalizedPublicKeyRing(Uri queryUri) throws NotFoundException {
        Cursor cursor = mContentResolver.query(queryUri,
                new String[]{
                        // we pick from cache only information that is not easily available from keyrings
                        KeyRings.VERIFIED,
                        // and of course, ring data
                        KeyRings.PUBKEY_DATA
                }, null, null, null
        );
        try {
            if (cursor != null && cursor.moveToFirst()) {

                int verified = cursor.getInt(0);
                byte[] blob = cursor.getBlob(1);
                return new CanonicalizedPublicKeyRing(blob, verified);
            } else {
                throw new NotFoundException("Key not found!");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public ArrayList<String> getConfirmedUserIds(long masterKeyId) throws NotFoundException {
        Cursor cursor = mContentResolver.query(UserPackets.buildUserIdsUri(masterKeyId),
                new String[]{ UserPackets.USER_ID }, UserPackets.VERIFIED + " = " + Certs.VERIFIED_SECRET, null, null
        );
        if (cursor == null) {
            throw new NotFoundException("Key id for requested user ids not found");
        }

        try {
            ArrayList<String> userIds = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                String userId = cursor.getString(0);
                userIds.add(userId);
            }

            return userIds;
        } finally {
            cursor.close();
        }
    }

    public CanonicalizedSecretKeyRing getCanonicalizedSecretKeyRingForTest(long id)
            throws NotFoundException, EncryptDecryptException, IncorrectPassphraseException, FailedMergeException {
        return getCanonicalizedSecretKeyRingHelper(KeyRings.buildUnifiedKeyRingUri(id), null, false, false);
    }

    /**
     * Retrieves and merges a canonicalized secret keyring with its updated public counterpart if flagged for a merge
     */
    public CanonicalizedSecretKeyRing getCanonicalizedSecretKeyRingWithMerge(long id, Passphrase passphrase)
            throws NotFoundException, EncryptDecryptException, IncorrectPassphraseException, FailedMergeException {
        return getCanonicalizedSecretKeyRingHelper(KeyRings.buildUnifiedKeyRingUri(id), passphrase, true, false);
    }

    public CanonicalizedSecretKeyRing getCanonicalizedSecretKeyRing(Uri uri, Passphrase passphrase)
            throws NotFoundException, EncryptDecryptException, IncorrectPassphraseException, FailedMergeException {
        return getCanonicalizedSecretKeyRingHelper(uri, passphrase, true, true);
    }

    public CanonicalizedSecretKeyRing getCanonicalizedSecretKeyRing(long id, Passphrase passphrase)
            throws NotFoundException, EncryptDecryptException, IncorrectPassphraseException {
        try {
            return getCanonicalizedSecretKeyRingHelper(KeyRings.buildUnifiedKeyRingUri(id), passphrase, true, true);
        } catch (FailedMergeException ignored) {
            return null;
        }
    }

    private CanonicalizedSecretKeyRing getCanonicalizedSecretKeyRingHelper(Uri uri, Passphrase passphrase,
                                                                           boolean isEncrypted, boolean skipMerge)
            throws ProviderReader.NotFoundException, EncryptDecryptException, IncorrectPassphraseException, ProviderReader.FailedMergeException {
        if (passphrase == null && isEncrypted) {
            throw new IllegalArgumentException("passphrase is null");
        }
        Cursor cursor = mContentResolver.query(uri,
                new String[]{
                        // we pick from cache only information that is not easily available from keyrings
                        KeyRings.HAS_ANY_SECRET, KeyRings.VERIFIED,
                        // and of course, ring data and data for merging
                        KeyRings.PRIVKEY_DATA, KeyRings.AWAITING_MERGE,
                        // TODO: move pub data collection into merge block to improve performance?
                        KeyRings.PUBKEY_DATA
                }, null, null, null
        );
        try {
            if (cursor != null && cursor.moveToFirst()) {

                boolean hasAnySecret = cursor.getInt(0) > 0;
                int verified = cursor.getInt(1);
                byte[] secBlob = cursor.getBlob(2);
                boolean awaitingMerge = cursor.getInt(3) == 1;
                if (!hasAnySecret) {
                    throw new ProviderReader.NotFoundException("Secret key not available!");
                }
                if (isEncrypted) {
                    secBlob = ByteArrayEncryptor.decryptByteArray(secBlob, passphrase.getCharArray());
                }

                CanonicalizedSecretKeyRing canSecretKey = new CanonicalizedSecretKeyRing(secBlob, false, verified);

                if (skipMerge || !awaitingMerge) {
                    return canSecretKey;
                } else {
                    byte[] pubBlob = cursor.getBlob(4);
                    long masterKeyId = canSecretKey.getMasterKeyId();
                    KeyringPassphrases passphrases = new KeyringPassphrases(masterKeyId, passphrase);

                    try {
                        // merge public into secret
                        UncachedKeyRing secretKey = canSecretKey.getUncachedKeyRing();
                        UncachedKeyRing publicKey = new CanonicalizedPublicKeyRing(pubBlob, verified).getUncachedKeyRing();
                        secretKey = secretKey.merge(publicKey, new OperationLog(), 0);
                        if (secretKey == null) {
                            throw new ProviderReader.FailedMergeException();
                        }

                        SaveKeyringResult saveResult = mProviderHelper.write().saveSecretKeyRing(secretKey,
                                passphrases, new ProgressScaler());

                        if (saveResult.success()) {
                            return getCanonicalizedSecretKeyRing(uri, passphrase);
                        } else {
                            throw new ProviderReader.FailedMergeException();
                        }

                    } catch (ProviderReader.FailedMergeException e) {
                        // wipe all existing keyring data from db & reload data using secret keyring
                        // should succeed as the secret keyring was saved before

                        mContentResolver.delete(KeyRingData.buildPublicKeyRingUri(masterKeyId), null, null);
                        SaveKeyringResult saveResult = mProviderHelper.write().saveSecretKeyRing(canSecretKey.getUncachedKeyRing(),
                                passphrases, new ProgressScaler());
                        if (!saveResult.success()) {
                            throw new RuntimeException("Unrecoverable error, io/bad secret key");
                        } else {
                            throw new ProviderReader.FailedMergeException("Merge failed but key was successfully restored");
                        }
                    }
                }
            } else {
                throw new ProviderReader.NotFoundException("Key not found!");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static class NotFoundException extends Exception {
        public NotFoundException() {
        }

        public NotFoundException(String name) {
            super(name);
        }
    }

    public static class FailedMergeException extends Exception {
        public FailedMergeException() {
        }

        public FailedMergeException(String name) {
            super(name);
        }
    }
}
