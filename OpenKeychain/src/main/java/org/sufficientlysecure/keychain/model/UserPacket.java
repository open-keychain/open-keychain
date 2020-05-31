package org.sufficientlysecure.keychain.model;


import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.UserPacketsModel;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;


@AutoValue
public abstract class UserPacket implements UserPacketsModel {
    public static final Factory<UserPacket> FACTORY = new Factory<>(AutoValue_UserPacket::new);
    public static final SelectUserIdsByMasterKeyIdMapper<UserId> USER_ID_MAPPER =
            FACTORY.selectUserIdsByMasterKeyIdMapper(AutoValue_UserPacket_UserId::new);
    public static final SelectUserAttributesByTypeAndMasterKeyIdMapper<UserAttribute> USER_ATTRIBUTE_MAPPER =
            FACTORY.selectUserAttributesByTypeAndMasterKeyIdMapper(AutoValue_UserPacket_UserAttribute::new);
    public static final UidStatusMapper<UidStatus> UID_STATUS_MAPPER =
            FACTORY.selectUserIdStatusByEmailMapper(AutoValue_UserPacket_UidStatus::new);

    public static UserPacket create(long masterKeyId, int rank, Long type, String userId, String name, String email,
            String comment, byte[] attribute_data, boolean isPrimary, boolean isRevoked) {
        return new AutoValue_UserPacket(masterKeyId, rank, type,
                userId, name, email, comment, attribute_data, isPrimary, isRevoked);
    }

    public static InsertUserPacket createInsertStatement(SupportSQLiteDatabase db) {
        return new InsertUserPacket(db);
    }

    public void bindTo(InsertUserPacket statement) {
        statement.bind(master_key_id(), rank(), type(), user_id(), name(), email(), comment(), attribute_data(),
                is_primary(), is_revoked());
    }

    @AutoValue
    public static abstract class UserId implements SelectUserIdsByMasterKeyIdModel {
        public boolean isVerified() {
            return verified() == VerificationStatus.VERIFIED_SECRET;
        }

        @NonNull
        public VerificationStatus verified() {
            return CustomColumnAdapters.VERIFICATON_STATUS_ADAPTER.decode(verified_int());
        }
    }

    @AutoValue
    public static abstract class UserAttribute implements SelectUserAttributesByTypeAndMasterKeyIdModel {
        public boolean isVerified() {
            return verified() == VerificationStatus.VERIFIED_SECRET;
        }

        @NonNull
        public VerificationStatus verified() {
            return CustomColumnAdapters.VERIFICATON_STATUS_ADAPTER.decode(verified_int());
        }
    }

    @AutoValue
    public static abstract class UidStatus implements UidStatusModel {
        public VerificationStatus keyStatus() {
            return CustomColumnAdapters.VERIFICATON_STATUS_ADAPTER.decode(key_status_int());
        }
    }
}
