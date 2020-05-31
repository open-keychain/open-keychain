package org.sufficientlysecure.keychain.model;


import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.auto.value.AutoValue;
import com.squareup.sqldelight.RowMapper;
import org.sufficientlysecure.keychain.KeysModel;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;


@AutoValue
public abstract class SubKey implements KeysModel {
    public static final Factory<SubKey> FACTORY =
            new Factory<>(AutoValue_SubKey::new, CustomColumnAdapters.SECRET_KEY_TYPE_ADAPTER);
    public static final UnifiedKeyViewMapper<UnifiedKeyInfo, Certification> UNIFIED_KEY_INFO_MAPPER =
            FACTORY.selectAllUnifiedKeyInfoMapper(
                    AutoValue_SubKey_UnifiedKeyInfo::new, Certification.FACTORY);
    public static Mapper<SubKey> SUBKEY_MAPPER = new Mapper<>(FACTORY);
    public static RowMapper<SecretKeyType> SKT_MAPPER = FACTORY.selectSecretKeyTypeMapper();

    public boolean expires() {
        return expiry() != null;
    }

    public static SubKey create(long masterKeyId, long rank, long keyId, Integer keySize, String keyCurveOid,
            int algorithm, byte[] fingerprint, boolean canCertify, boolean canSign, boolean canEncrypt, boolean canAuth,
            boolean isRevoked, SecretKeyType hasSecret, boolean isSecure, Date creation, Date expiry,
            Date validFrom) {
        long creationUnixTime = creation.getTime() / 1000;
        Long expiryUnixTime = expiry != null ? expiry.getTime() / 1000 : null;
        long validFromTime = validFrom.getTime() / 1000;
        return new AutoValue_SubKey(masterKeyId, rank, keyId, keySize, keyCurveOid, algorithm, fingerprint, canCertify,
                canSign, canEncrypt, canAuth, isRevoked, hasSecret, isSecure, creationUnixTime, expiryUnixTime, validFromTime);
    }

    public static InsertKey createInsertStatement(SupportSQLiteDatabase db) {
        return new InsertKey(db, FACTORY);
    }

    public static UpdateHasSecretByMasterKeyId createUpdateHasSecretByMasterKeyIdStatement(SupportSQLiteDatabase db) {
        return new UpdateHasSecretByMasterKeyId(db, FACTORY);
    }

    public static UpdateHasSecretByKeyId createUpdateHasSecretByKeyId(SupportSQLiteDatabase db) {
        return new UpdateHasSecretByKeyId(db, FACTORY);
    }

    public void bindTo(InsertKey statement) {
        statement.bind(master_key_id(), rank(), key_id(), key_size(), key_curve_oid(), algorithm(), fingerprint(),
                can_certify(), can_sign(), can_encrypt(), can_authenticate(), is_revoked(), has_secret(), is_secure(),
                creation(), expiry(), validFrom());
    }

    @AutoValue
    public static abstract class UnifiedKeyInfo implements KeysModel.UnifiedKeyViewModel {
        private List<String> autocryptPackageNames;
        private String cachedUidSearchString;

        public boolean is_expired() {
            Long expiry = expiry();
            return expiry != null && expiry * 1000 < System.currentTimeMillis();
        }

        public boolean has_any_secret() {
            return has_any_secret_int() != 0;
        }

        public boolean is_verified() {
            VerificationStatus verified = verified();
            return verified != null && verified == VerificationStatus.VERIFIED_SECRET;
        }

        public boolean has_duplicate() {
            return has_duplicate_int() != 0;
        }

        public List<String> autocrypt_package_names() {
            if (autocryptPackageNames == null) {
                String csv = autocrypt_package_names_csv();
                autocryptPackageNames = csv == null ? Collections.emptyList() :
                        Arrays.asList(csv.split(","));
            }
            return autocryptPackageNames;
        }

        public boolean has_auth_key() {
            return has_auth_key_int() != 0;
        }

        public boolean has_encrypt_key() {
            return has_encrypt_key_int() != 0;
        }

        public boolean has_sign_key() {
            return has_sign_key_int() != 0;
        }

        public String uidSearchString() {
            if (cachedUidSearchString == null) {
                cachedUidSearchString = user_id_list();
                if (cachedUidSearchString == null) {
                    cachedUidSearchString = "";
                }
                cachedUidSearchString = cachedUidSearchString.toLowerCase();
            }
            return cachedUidSearchString;
        }
    }
}
