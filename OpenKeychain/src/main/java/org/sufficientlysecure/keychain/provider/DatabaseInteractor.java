package org.sufficientlysecure.keychain.provider;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.util.Log;


public class DatabaseInteractor {
    // If we ever switch to api level 11, we can ditch this whole mess!
    public static final int FIELD_TYPE_NULL = 1;
    // this is called integer to stay coherent with the constants in Cursor (api level 11)
    public static final int FIELD_TYPE_INTEGER = 2;
    public static final int FIELD_TYPE_FLOAT = 3;
    public static final int FIELD_TYPE_STRING = 4;
    public static final int FIELD_TYPE_BLOB = 5;

    final ContentResolver mContentResolver;
    OperationLog mLog;
    int mIndent;

    public DatabaseInteractor(ContentResolver contentResolver) {
        this(contentResolver, new OperationLog(), 0);
    }

    public DatabaseInteractor(ContentResolver contentResolver, OperationLog log, int indent) {
        mContentResolver = contentResolver;
        mIndent = indent;
        mLog = log;
    }

    public OperationLog getLog() {
        return mLog;
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

    public long getMasterKeyId(long subKeyId) throws NotFoundException {
        return (Long) getGenericData(KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(subKeyId),
                KeyRings.MASTER_KEY_ID, FIELD_TYPE_INTEGER);
    }

    public CachedPublicKeyRing getCachedPublicKeyRing(Uri queryUri) throws PgpKeyNotFoundException {
        long masterKeyId = new CachedPublicKeyRing(this, queryUri).extractOrGetMasterKeyId();
        return getCachedPublicKeyRing(masterKeyId);
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

    public ArrayList<String> getConfirmedUserIds(long masterKeyId) throws NotFoundException {
        Cursor cursor = mContentResolver.query(UserPackets.buildUserIdsUri(masterKeyId),
                new String[]{UserPackets.USER_ID}, UserPackets.VERIFIED + " = " + Certs.VERIFIED_SECRET, null, null
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
                uri, KeyRingData.KEY_RING_DATA, FIELD_TYPE_BLOB);
        return getKeyRingAsArmoredString(data);
    }

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    public static class NotFoundException extends Exception {
        public NotFoundException() {
        }

        public NotFoundException(String name) {
            super(name);
        }
    }
}
