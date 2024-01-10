package org.sufficientlysecure.keychain.model;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;


@AutoValue
public abstract class UnifiedKeyInfo {
    public static UnifiedKeyInfo create(long master_key_id, byte[] fingerprint, Long min,
            String user_id, String name, String email, String comment, long creation, Long expiry,
            boolean is_revoked, boolean is_secure, boolean can_certify,
            CanonicalizedKeyRing.VerificationStatus verified, boolean has_duplicate,
            boolean has_any_secret, boolean has_encrypt_key, boolean has_sign_key,
            boolean has_auth_key, String autocrypt_package_names_csv, String user_id_list) {
        return new AutoValue_UnifiedKeyInfo(master_key_id, fingerprint, min, user_id, name, email,
                comment, creation, expiry, is_revoked, is_secure, can_certify, verified,
                has_duplicate, has_any_secret, has_encrypt_key, has_sign_key, has_auth_key,
                autocrypt_package_names_csv, user_id_list);
    }

    private List<String> autocryptPackageNames;
    private String cachedUidSearchString;

    public abstract long master_key_id();

    public abstract byte[] fingerprint();

    @Nullable
    public abstract Long min();

    public abstract String user_id();

    @Nullable
    public abstract String name();

    @Nullable
    public abstract String email();

    @Nullable
    public abstract String comment();

    public abstract long creation();

    @Nullable
    public abstract Long expiry();

    public abstract boolean is_revoked();

    public abstract boolean is_secure();

    public abstract boolean can_certify();

    @Nullable
    public abstract VerificationStatus verified();

    public abstract boolean has_duplicate();

    public abstract boolean has_any_secret();

    public abstract boolean has_encrypt_key();

    public abstract boolean has_sign_key();

    public abstract boolean has_auth_key();

    @Nullable
    public abstract String autocrypt_package_names_csv();

    @Nullable
    public abstract String user_id_list();

    public boolean is_expired() {
        Long expiry = expiry();
        return expiry != null && expiry * 1000 < System.currentTimeMillis();
    }

    public boolean is_verified() {
        VerificationStatus verified = verified();
        return verified != null && verified == VerificationStatus.VERIFIED_SECRET;
    }

    public List<String> autocrypt_package_names() {
        if (autocryptPackageNames == null) {
            String csv = autocrypt_package_names_csv();
            autocryptPackageNames =
                    csv == null ? Collections.emptyList() : Arrays.asList(csv.split(","));
        }
        return autocryptPackageNames;
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
