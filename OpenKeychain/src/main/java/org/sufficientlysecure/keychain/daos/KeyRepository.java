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

package org.sufficientlysecure.keychain.daos;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import android.content.Context;

import androidx.annotation.WorkerThread;
import com.squareup.sqldelight.Query;
import com.squareup.sqldelight.db.SqlCursor;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.sufficientlysecure.keychain.KeyRingsPublicQueries;
import org.sufficientlysecure.keychain.KeySignaturesQueries;
import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.Keyrings_public;
import org.sufficientlysecure.keychain.Keys;
import org.sufficientlysecure.keychain.KeysQueries;
import org.sufficientlysecure.keychain.model.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.model.UserId;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import timber.log.Timber;


@WorkerThread
public class KeyRepository extends AbstractDao {
    final LocalPublicKeyStorage mLocalPublicKeyStorage;
    final LocalSecretKeyStorage localSecretKeyStorage;
    private final KeysQueries keysQueries = getDatabase().getKeysQueries();

    OperationLog mLog;
    int mIndent;
    private final KeyRingsPublicQueries keyRingsPublicQueries = getDatabase().getKeyRingsPublicQueries();
    private final KeySignaturesQueries keySignaturesQueries = getDatabase().getKeySignaturesQueries();

    public static KeyRepository create(Context context) {
        LocalPublicKeyStorage localPublicKeyStorage = LocalPublicKeyStorage.getInstance(context);
        LocalSecretKeyStorage localSecretKeyStorage = LocalSecretKeyStorage.getInstance(context);
        KeychainDatabase database = KeychainDatabase.getInstance(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new KeyRepository(database, databaseNotifyManager, localPublicKeyStorage,
                localSecretKeyStorage);
    }

    private KeyRepository(KeychainDatabase database,
            DatabaseNotifyManager databaseNotifyManager,
            LocalPublicKeyStorage localPublicKeyStorage,
            LocalSecretKeyStorage localSecretKeyStorage) {
        this(database, databaseNotifyManager, localPublicKeyStorage, localSecretKeyStorage,
                new OperationLog(), 0);
    }

    KeyRepository(KeychainDatabase database,
            DatabaseNotifyManager databaseNotifyManager,
            LocalPublicKeyStorage localPublicKeyStorage,
            LocalSecretKeyStorage localSecretKeyStorage,
            OperationLog log, int indent) {
        super(database, databaseNotifyManager);
        mLocalPublicKeyStorage = localPublicKeyStorage;
        this.localSecretKeyStorage = localSecretKeyStorage;
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

    public CanonicalizedPublicKeyRing getCanonicalizedPublicKeyRing(long masterKeyId)
            throws NotFoundException {
        UnifiedKeyInfo unifiedKeyInfo = getUnifiedKeyInfo(masterKeyId);
        if (unifiedKeyInfo == null) {
            throw new NotFoundException();
        }

        byte[] publicKeyData = loadPublicKeyRingData(masterKeyId);
        return new CanonicalizedPublicKeyRing(publicKeyData, unifiedKeyInfo.verified());
    }

    public CanonicalizedSecretKeyRing getCanonicalizedSecretKeyRing(long masterKeyId)
            throws NotFoundException {
        UnifiedKeyInfo unifiedKeyInfo = getUnifiedKeyInfo(masterKeyId);
        if (unifiedKeyInfo == null || !unifiedKeyInfo.has_any_secret()) {
            throw new NotFoundException();
        }
        byte[] secretKeyData = loadSecretKeyRingData(masterKeyId);
        if (secretKeyData == null) {
            throw new IllegalStateException("Missing expected secret key data!");
        }
        return new CanonicalizedSecretKeyRing(secretKeyData, unifiedKeyInfo.verified());
    }

    public List<Long> getAllMasterKeyIds() {
        return keyRingsPublicQueries.selectAllMasterKeyIds().executeAsList();
    }

    public List<Long> getMasterKeyIdsBySigner(List<Long> signerMasterKeyIds) {
        return keySignaturesQueries.selectMasterKeyIdsBySigner(signerMasterKeyIds).executeAsList();
    }

    public Long getMasterKeyIdBySubkeyId(long subKeyId) {
        return keysQueries.selectMasterKeyIdBySubkey(subKeyId).executeAsOneOrNull();
    }

    public UnifiedKeyInfo getUnifiedKeyInfo(long masterKeyId) {
        return keysQueries.selectUnifiedKeyInfoByMasterKeyId(masterKeyId, UnifiedKeyInfo::create).executeAsOneOrNull();
    }

    public List<UnifiedKeyInfo> getUnifiedKeyInfo(long... masterKeyIds) {
        return keysQueries.selectUnifiedKeyInfoByMasterKeyIds(getLongArrayAsList(masterKeyIds), UnifiedKeyInfo::create)
                .executeAsList();
    }

    public List<UnifiedKeyInfo> getUnifiedKeyInfosByMailAddress(String mailAddress) {
        return keysQueries.selectUnifiedKeyInfoSearchMailAddress('%' + mailAddress + '%', UnifiedKeyInfo::create)
                .executeAsList();
    }

    public List<UnifiedKeyInfo> getAllUnifiedKeyInfo() {
        return keysQueries.selectAllUnifiedKeyInfo(UnifiedKeyInfo::create).executeAsList();
    }

    public List<UnifiedKeyInfo> getAllUnifiedKeyInfoWithSecret() {
        return keysQueries.selectAllUnifiedKeyInfoWithSecret(UnifiedKeyInfo::create).executeAsList();
    }

    public List<UnifiedKeyInfo> getAllUnifiedKeyInfoWithAuthKeySecret() {
        return keysQueries.selectAllUnifiedKeyInfoWithAuthKeySecret(UnifiedKeyInfo::create).executeAsList();
    }

    public List<UserId> getUserIds(long... masterKeyIds) {
        return getDatabase().getUserPacketsQueries()
                .selectUserIdsByMasterKeyId(getLongArrayAsList(masterKeyIds), UserId::create).executeAsList();
    }



    public List<String> getConfirmedUserIds(long masterKeyId) {
        return getDatabase().getUserPacketsQueries()
                .selectUserIdsByMasterKeyIdAndVerification(masterKeyId,
                        VerificationStatus.VERIFIED_SECRET, (
                                master_key_id,
                                rank,
                                user_id,
                                name,
                                email,
                                comment,
                                is_primary,
                                is_revoked,
                                verified_int

                        ) -> user_id).executeAsList();
    }

    public List<Keys> getSubKeysByMasterKeyId(long masterKeyId) {
        return keysQueries.selectSubkeysByMasterKeyId(masterKeyId).executeAsList();
    }

    public SecretKeyType getSecretKeyType(long keyId) throws NotFoundException {
        try {
            return keysQueries.selectSecretKeyType(keyId).executeAsOne();
        } catch (NullPointerException npe) {
            throw new NotFoundException();
        }
    }

    public byte[] getFingerprintByKeyId(long keyId) throws NotFoundException {
        try {
            return keysQueries.selectFingerprintByKeyId(keyId).executeAsOne();
        } catch (NullPointerException npe) {
            throw new NotFoundException();
        }
    }

    private byte[] getKeyRingAsArmoredData(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArmoredOutputStream aos = new ArmoredOutputStream(bos);

        aos.write(data);
        aos.close();

        return bos.toByteArray();
    }

    public String getPublicKeyRingAsArmoredString(long masterKeyId)
            throws NotFoundException, IOException {
        byte[] data = loadPublicKeyRingData(masterKeyId);
        byte[] armoredData = getKeyRingAsArmoredData(data);
        return new String(armoredData);
    }

    public byte[] getSecretKeyRingAsArmoredData(long masterKeyId)
            throws NotFoundException, IOException {
        byte[] data = loadSecretKeyRingData(masterKeyId);
        return getKeyRingAsArmoredData(data);
    }

    public final byte[] loadPublicKeyRingData(long masterKeyId) throws NotFoundException {
        Query<Keyrings_public> keyringsPublicQuery =
                keyRingsPublicQueries.selectByMasterKeyId(masterKeyId);
        try (SqlCursor cursor = keyringsPublicQuery.execute()) {
            if (cursor.next()) {
                Keyrings_public keyRingPublic = keyringsPublicQuery.getMapper().invoke(cursor);
                byte[] keyRingData = keyRingPublic.getKey_ring_data();
                if (keyRingData == null) {
                    keyRingData = mLocalPublicKeyStorage.readPublicKey(masterKeyId);
                }
                return keyRingData;
            }
        } catch (IOException e) {
            Timber.e(e, "Error reading public key from storage!");
        }
        throw new NotFoundException();
    }

    public final byte[] loadSecretKeyRingData(long masterKeyId) throws NotFoundException {
        try {
            return localSecretKeyStorage.readSecretKey(masterKeyId);
        } catch (IOException e) {
            Timber.e(e, "Error reading secret key from storage!");
            throw new NotFoundException();
        }
    }

    public long getSecretSignId(long masterKeyId) throws NotFoundException {
        return keysQueries.selectEffectiveSignKeyIdByMasterKeyId(masterKeyId).executeAsOneOrNull();
    }

    public long getEffectiveAuthenticationKeyId(long masterKeyId) throws NotFoundException {
        return keysQueries.selectEffectiveAuthKeyIdByMasterKeyId(masterKeyId).executeAsOneOrNull();
    }

    public List<Long> getPublicEncryptionIds(long masterKeyId) {
        return keysQueries.selectEffectiveEncryptionKeyIdsByMasterKeyId(masterKeyId)
                .executeAsList();
    }

    public static class NotFoundException extends Exception {
        public NotFoundException() {
        }

        public NotFoundException(String name) {
            super(name);
        }
    }

    private long[] getLongListAsArray(List<Long> longList) {
        long[] longs = new long[longList.size()];
        int i = 0;
        for (Long aLong : longList) {
            longs[i++] = aLong;
        }
        return longs;
    }

    private List<Long> getLongArrayAsList(long[] longList) {
        Long[] longs = new Long[longList.length];
        int i = 0;
        for (Long aLong : longList) {
            longs[i++] = aLong;
        }
        return Arrays.asList(longs);
    }
}
