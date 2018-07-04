package org.sufficientlysecure.keychain.model;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
