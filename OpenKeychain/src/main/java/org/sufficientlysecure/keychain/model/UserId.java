package org.sufficientlysecure.keychain.model;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;


@AutoValue
public abstract class UserId {
    public static UserId create(long master_key_id, int rank, String user_id,
            String name, String email, String comment, boolean is_primary, boolean is_revoked, Long verified_int) {
        return new AutoValue_UserId(master_key_id, rank, user_id, name, email, comment,
                is_primary, is_revoked, verified_int
        );
    }

    public abstract long master_key_id();

    public abstract int rank();

    @Nullable
    public abstract String user_id();

    @Nullable
    public abstract String name();

    @Nullable
    public abstract String email();

    @Nullable
    public abstract String comment();

    public abstract boolean is_primary();

    public abstract boolean is_revoked();

    public abstract Long verified_int();

    public boolean isVerified() {
        return verified() == VerificationStatus.VERIFIED_SECRET;
    }

    @NonNull
    public VerificationStatus verified() {
        return CustomColumnAdapters.VERIFICATON_STATUS_ADAPTER.decode(verified_int());
    }
}
