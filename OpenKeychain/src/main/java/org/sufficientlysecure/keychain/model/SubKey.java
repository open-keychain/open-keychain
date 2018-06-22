package org.sufficientlysecure.keychain.model;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.auto.value.AutoValue;
import com.squareup.sqldelight.RowMapper;
import org.sufficientlysecure.keychain.KeysModel;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;


@AutoValue
public abstract class SubKey implements KeysModel {
    public static final Factory<SubKey> FACTORY =
            new Factory<>(AutoValue_SubKey::new, CustomColumnAdapters.SECRET_KEY_TYPE_ADAPTER);
    public static final SelectAllUnifiedKeyInfoMapper<UnifiedKeyInfo> UNIFIED_KEY_INFO_MAPPER =
            FACTORY.selectAllUnifiedKeyInfoMapper(AutoValue_SubKey_UnifiedKeyInfo::new);
    public static Mapper<SubKey> SUBKEY_MAPPER = new Mapper<>(FACTORY);
    public static RowMapper<SecretKeyType> SKT_MAPPER = FACTORY.selectSecretKeyTypeMapper();

    public boolean expires() {
        return expiry() != null;
    }

    @AutoValue
    public static abstract class UnifiedKeyInfo implements SelectAllUnifiedKeyInfoModel {
        private List<String> autocryptPackageNames;

        public boolean is_expired() {
            Long expiry = expiry();
            return expiry != null && expiry * 1000 < System.currentTimeMillis();
        }

        public boolean has_any_secret() {
            return has_any_secret_int() != 0;
        }

        public boolean is_verified() {
            Integer verified = verified();
            return verified != null && verified == 1;
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
    }
}
