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
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.WorkerThread;

import com.squareup.sqldelight.SqlDelightQuery;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.sufficientlysecure.keychain.KeychainDatabase;
import org.sufficientlysecure.keychain.model.Certification;
import org.sufficientlysecure.keychain.model.KeyRingPublic;
import org.sufficientlysecure.keychain.model.KeySignature;
import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.model.UserPacket;
import org.sufficientlysecure.keychain.model.UserPacket.UserId;
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

    OperationLog mLog;
    int mIndent;

    public static KeyRepository create(Context context) {
        LocalPublicKeyStorage localPublicKeyStorage = LocalPublicKeyStorage.getInstance(context);
        LocalSecretKeyStorage localSecretKeyStorage = LocalSecretKeyStorage.getInstance(context);
        KeychainDatabase database = KeychainDatabase.getInstance(context);
        DatabaseNotifyManager databaseNotifyManager = DatabaseNotifyManager.create(context);

        return new KeyRepository(database, databaseNotifyManager, localPublicKeyStorage, localSecretKeyStorage);
    }

    private KeyRepository(KeychainDatabase database,
            DatabaseNotifyManager databaseNotifyManager,
            LocalPublicKeyStorage localPublicKeyStorage,
            LocalSecretKeyStorage localSecretKeyStorage) {
        this(database, databaseNotifyManager, localPublicKeyStorage, localSecretKeyStorage, new OperationLog(), 0);
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

    public CanonicalizedPublicKeyRing getCanonicalizedPublicKeyRing(long masterKeyId) throws NotFoundException {
        UnifiedKeyInfo unifiedKeyInfo = getUnifiedKeyInfo(masterKeyId);
        if (unifiedKeyInfo == null) {
            throw new NotFoundException();
        }

        byte[] publicKeyData = loadPublicKeyRingData(masterKeyId);
        return new CanonicalizedPublicKeyRing(publicKeyData, unifiedKeyInfo.verified());
    }

    public CanonicalizedSecretKeyRing getCanonicalizedSecretKeyRing(long masterKeyId) throws NotFoundException {
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
        SqlDelightQuery query = KeyRingPublic.FACTORY.selectAllMasterKeyIds();
        return mapAllRows(query, KeySignature.FACTORY.selectMasterKeyIdsBySignerMapper());
    }

    public List<Long> getMasterKeyIdsBySigner(List<Long> signerMasterKeyIds) {
        long[] signerKeyIds = getLongListAsArray(signerMasterKeyIds);
        SqlDelightQuery query = KeySignature.FACTORY.selectMasterKeyIdsBySigner(signerKeyIds);
        return mapAllRows(query, KeySignature.FACTORY.selectMasterKeyIdsBySignerMapper());
    }

    public Long getMasterKeyIdBySubkeyId(long subKeyId) {
        SqlDelightQuery query = SubKey.FACTORY.selectMasterKeyIdBySubkey(subKeyId);
        return mapSingleRow(query, SubKey.FACTORY.selectMasterKeyIdBySubkeyMapper());
    }

    public Long getMasterKeyIdByAuthSubkeyId(long subKeyId) throws NotFoundException {
        SqlDelightQuery query = SubKey.FACTORY.selectMasterKeyIdByAuthSubkey(subKeyId);
        return mapSingleRowOrThrow(query, SubKey.FACTORY.selectMasterKeyIdByAuthSubkeyMapper());
    }

    public UnifiedKeyInfo getUnifiedKeyInfo(long masterKeyId) {
        SqlDelightQuery query = SubKey.FACTORY.selectUnifiedKeyInfoByMasterKeyId(masterKeyId);
        return mapSingleRow(query, SubKey.UNIFIED_KEY_INFO_MAPPER);
    }

    public List<UnifiedKeyInfo> getUnifiedKeyInfo(long... masterKeyIds) {
        SqlDelightQuery query = SubKey.FACTORY.selectUnifiedKeyInfoByMasterKeyIds(masterKeyIds);
        return mapAllRows(query, SubKey.UNIFIED_KEY_INFO_MAPPER);
    }

    public List<UnifiedKeyInfo> getUnifiedKeyInfosByMailAddress(String mailAddress) {
        SqlDelightQuery query = SubKey.FACTORY.selectUnifiedKeyInfoSearchMailAddress('%' + mailAddress + '%');
        return mapAllRows(query, SubKey.UNIFIED_KEY_INFO_MAPPER);
    }

    public List<UnifiedKeyInfo> getAllUnifiedKeyInfo() {
        SqlDelightQuery query = SubKey.FACTORY.selectAllUnifiedKeyInfo();
        return mapAllRows(query, SubKey.UNIFIED_KEY_INFO_MAPPER);
    }

    public List<UnifiedKeyInfo> getAllUnifiedKeyInfoWithSecret() {
        SqlDelightQuery query = SubKey.FACTORY.selectAllUnifiedKeyInfoWithSecret();
        return mapAllRows(query, SubKey.UNIFIED_KEY_INFO_MAPPER);
    }

    public List<UserId> getUserIds(long... masterKeyIds) {
        SqlDelightQuery query = UserPacket.FACTORY.selectUserIdsByMasterKeyId(masterKeyIds);
        return mapAllRows(query, UserPacket.USER_ID_MAPPER);
    }

    public List<String> getConfirmedUserIds(long masterKeyId) {
        SqlDelightQuery query = UserPacket.FACTORY.selectUserIdsByMasterKeyIdAndVerification(
                Certification.FACTORY, masterKeyId, VerificationStatus.VERIFIED_SECRET);
        return mapAllRows(query, cursor -> UserPacket.USER_ID_MAPPER.map(cursor).user_id());
    }

    public List<SubKey> getSubKeysByMasterKeyId(long masterKeyId) {
        SqlDelightQuery query = SubKey.FACTORY.selectSubkeysByMasterKeyId(masterKeyId);
        return mapAllRows(query, SubKey.SUBKEY_MAPPER);
    }

    public List<SubKey> getAuthSubKeysByMasterKeyId(long masterKeyId) {
        SqlDelightQuery query = SubKey.FACTORY.selectAuthSubkeysByMasterKeyId(masterKeyId);
        return mapAllRows(query, SubKey.SUBKEY_MAPPER);
    }

    public SecretKeyType getSecretKeyType(long keyId) throws NotFoundException {
        SqlDelightQuery query = SubKey.FACTORY.selectSecretKeyType(keyId);
        return mapSingleRowOrThrow(query, SubKey.SKT_MAPPER);
    }

    public byte[] getFingerprintByKeyId(long keyId) throws NotFoundException {
        SqlDelightQuery query = SubKey.FACTORY.selectFingerprintByKeyId(keyId);
        return mapSingleRowOrThrow(query, SubKey.FACTORY.selectFingerprintByKeyIdMapper());
    }

    private byte[] getKeyRingAsArmoredData(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArmoredOutputStream aos = new ArmoredOutputStream(bos);

        aos.write(data);
        aos.close();

        return bos.toByteArray();
    }

    public String getPublicKeyRingAsArmoredString(long masterKeyId) throws NotFoundException, IOException {
        byte[] data = loadPublicKeyRingData(masterKeyId);
        byte[] armoredData = getKeyRingAsArmoredData(data);
        return new String(armoredData);
    }

    public byte[] getSecretKeyRingAsArmoredData(long masterKeyId) throws NotFoundException, IOException {
        byte[] data = loadSecretKeyRingData(masterKeyId);
        return getKeyRingAsArmoredData(data);
    }

    public final byte[] loadPublicKeyRingData(long masterKeyId) throws NotFoundException {
        SqlDelightQuery query = KeyRingPublic.FACTORY.selectByMasterKeyId(masterKeyId);
        try (Cursor cursor = getReadableDb().query(query)) {
            if (cursor.moveToFirst()) {
                KeyRingPublic keyRingPublic = KeyRingPublic.MAPPER.map(cursor);
                byte[] keyRingData = keyRingPublic.key_ring_data();
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
        SqlDelightQuery query = SubKey.FACTORY.selectEffectiveSignKeyIdByMasterKeyId(masterKeyId);
        return mapSingleRowOrThrow(query, SubKey.FACTORY.selectEffectiveSignKeyIdByMasterKeyIdMapper());
    }

    public long getSecretAuthenticationId(long masterKeyId) throws NotFoundException {
        SqlDelightQuery query = SubKey.FACTORY.selectEffectiveAuthKeyIdByMasterKeyId(masterKeyId);
        return mapSingleRowOrThrow(query, SubKey.FACTORY.selectEffectiveAuthKeyIdByMasterKeyIdMapper());
    }

    public List<Long> getPublicEncryptionIds(long masterKeyId) {
        SqlDelightQuery query = SubKey.FACTORY.selectEffectiveEncryptionKeyIdsByMasterKeyId(masterKeyId);
        return mapAllRows(query, SubKey.FACTORY.selectEffectiveEncryptionKeyIdsByMasterKeyIdMapper());
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
}
