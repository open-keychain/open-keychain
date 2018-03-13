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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UpdatedKeys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import timber.log.Timber;


public class KeyRepository {
    // If we ever switch to api level 11, we can ditch this whole mess!
    public static final int FIELD_TYPE_NULL = 1;
    // this is called integer to stay coherent with the constants in Cursor (api level 11)
    public static final int FIELD_TYPE_INTEGER = 2;
    public static final int FIELD_TYPE_FLOAT = 3;
    public static final int FIELD_TYPE_STRING = 4;
    public static final int FIELD_TYPE_BLOB = 5;

    final ContentResolver mContentResolver;
    final LocalPublicKeyStorage mLocalPublicKeyStorage;
    OperationLog mLog;
    int mIndent;

    public static KeyRepository create(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        LocalPublicKeyStorage localPublicKeyStorage = LocalPublicKeyStorage.getInstance(context);

        return new KeyRepository(contentResolver, localPublicKeyStorage);
    }

    private KeyRepository(ContentResolver contentResolver, LocalPublicKeyStorage localPublicKeyStorage) {
        this(contentResolver, localPublicKeyStorage, new OperationLog(), 0);
    }

    KeyRepository(ContentResolver contentResolver, LocalPublicKeyStorage localPublicKeyStorage,
            OperationLog log, int indent) {
        mContentResolver = contentResolver;
        mLocalPublicKeyStorage = localPublicKeyStorage;
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

    Object getGenericData(Uri uri, String column, int type) throws NotFoundException {
        Object result = getGenericData(uri, new String[]{column}, new int[]{type}, null).get(column);
        if (result == null) {
            throw new NotFoundException();
        }
        return result;
    }

    Object getGenericDataOrNull(Uri uri, String column, int type) throws NotFoundException {
        return getGenericData(uri, new String[]{column}, new int[]{type}, null).get(column);
    }

    Object getGenericData(Uri uri, String column, int type, String selection)
            throws NotFoundException {
        return getGenericData(uri, new String[]{column}, new int[]{type}, selection).get(column);
    }

    private HashMap<String, Object> getGenericData(Uri uri, String[] proj, int[] types)
            throws NotFoundException {
        return getGenericData(uri, proj, types, null);
    }

    private HashMap<String, Object> getGenericData(Uri uri, String[] proj, int[] types, String selection)
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
        return getCanonicalizedPublicKeyRing(KeyRings.buildUnifiedKeyRingUri(id));
    }

    public CanonicalizedPublicKeyRing getCanonicalizedPublicKeyRing(Uri queryUri) throws NotFoundException {
        Cursor cursor = mContentResolver.query(queryUri,
                new String[] { KeyRings.MASTER_KEY_ID, KeyRings.VERIFIED }, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                long masterKeyId = cursor.getLong(0);
                int verified = cursor.getInt(1);

                byte[] publicKeyData = loadPublicKeyRingData(masterKeyId);
                return new CanonicalizedPublicKeyRing(publicKeyData, verified);
            } else {
                throw new NotFoundException("Key not found!");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public CanonicalizedSecretKeyRing getCanonicalizedSecretKeyRing(long id) throws NotFoundException {
        return getCanonicalizedSecretKeyRing(KeyRings.buildUnifiedKeyRingUri(id));
    }

    public CanonicalizedSecretKeyRing getCanonicalizedSecretKeyRing(Uri queryUri) throws NotFoundException {
        Cursor cursor = mContentResolver.query(queryUri,
                new String[] { KeyRings.MASTER_KEY_ID, KeyRings.VERIFIED, KeyRings.HAS_ANY_SECRET }, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                long masterKeyId = cursor.getLong(0);
                int verified = cursor.getInt(1);
                int hasAnySecret = cursor.getInt(2);
                if (hasAnySecret == 0) {
                    throw new NotFoundException("No secret key available or unknown public key!");
                }

                byte[] secretKeyData = loadSecretKeyRingData(masterKeyId);
                return new CanonicalizedSecretKeyRing(secretKeyData, verified);
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

    private byte[] getKeyRingAsArmoredData(byte[] data) throws IOException, PgpGeneralException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArmoredOutputStream aos = new ArmoredOutputStream(bos);

        aos.write(data);
        aos.close();

        return bos.toByteArray();
    }

    public String getPublicKeyRingAsArmoredString(long masterKeyId)
            throws NotFoundException, IOException, PgpGeneralException {
        byte[] data = loadPublicKeyRingData(masterKeyId);
        byte[] armoredData = getKeyRingAsArmoredData(data);
        return new String(armoredData);
    }

    public byte[] getSecretKeyRingAsArmoredData(long masterKeyId)
            throws NotFoundException, IOException, PgpGeneralException {
        byte[] data = loadSecretKeyRingData(masterKeyId);
        return getKeyRingAsArmoredData(data);
    }

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    @Nullable
    Long getLastUpdateTime(long masterKeyId) {
        Cursor lastUpdatedCursor = mContentResolver.query(
                UpdatedKeys.CONTENT_URI,
                new String[] { UpdatedKeys.LAST_UPDATED },
                UpdatedKeys.MASTER_KEY_ID + " = ?",
                new String[] { "" + masterKeyId },
                null
        );
        if (lastUpdatedCursor == null) {
            return null;
        }

        Long lastUpdateTime;
        try {
            if (!lastUpdatedCursor.moveToNext()) {
                return null;
            }
            lastUpdateTime = lastUpdatedCursor.getLong(0);
        } finally {
            lastUpdatedCursor.close();
        }
        return lastUpdateTime;
    }

    public final byte[] loadPublicKeyRingData(long masterKeyId) throws NotFoundException {
        byte[] data = (byte[]) getGenericDataOrNull(KeyRingData.buildPublicKeyRingUri(masterKeyId),
                KeyRingData.KEY_RING_DATA, FIELD_TYPE_BLOB);

        if (data == null) {
            try {
                data = mLocalPublicKeyStorage.readPublicKey(masterKeyId);
            } catch (IOException e) {
                Timber.e(e, "Error reading public key from storage!");
                throw new NotFoundException();
            }
        }

        if (data == null) {
            throw new NotFoundException();
        }

        return data;
    }

    public final byte[] loadSecretKeyRingData(long masterKeyId) throws NotFoundException {
        byte[] data = (byte[]) getGenericDataOrNull(KeychainContract.KeyRingData.buildSecretKeyRingUri(masterKeyId),
                KeyRingData.KEY_RING_DATA, FIELD_TYPE_BLOB);

        if (data == null) {
            throw new NotFoundException();
        }

        return data;
    }

    public static class NotFoundException extends Exception {
        public NotFoundException() {
        }

        public NotFoundException(String name) {
            super(name);
        }
    }
}
