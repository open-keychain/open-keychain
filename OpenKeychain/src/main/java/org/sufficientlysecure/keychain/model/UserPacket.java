package org.sufficientlysecure.keychain.model;


import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.UserPacketsModel;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;


@AutoValue
public abstract class UserPacket implements UserPacketsModel {
    public static final Factory<UserPacket> FACTORY = new Factory<>(AutoValue_UserPacket::new);
    public static final SelectUserIdsByMasterKeyIdMapper<UserId> USER_ID_MAPPER =
            FACTORY.selectUserIdsByMasterKeyIdMapper(AutoValue_UserPacket_UserId::new);
    public static final SelectUserAttributesByTypeAndMasterKeyIdMapper<UserAttribute> USER_ATTRIBUTE_MAPPER =
            FACTORY.selectUserAttributesByTypeAndMasterKeyIdMapper(AutoValue_UserPacket_UserAttribute::new);

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
}
