package org.sufficientlysecure.keychain.model;


import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.UserPacketsModel;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;


@AutoValue
public abstract class UserPacket implements UserPacketsModel {
    public static final Factory<UserPacket> FACTORY = new Factory<>(AutoValue_UserPacket::new);
    public static final SelectUserIdsByMasterKeyIdMapper<UserId, Certification> USER_ID_MAPPER =
            FACTORY.selectUserIdsByMasterKeyIdMapper(AutoValue_UserPacket_UserId::new, Certification.FACTORY);
    public static final SelectUserAttributesByTypeAndMasterKeyIdMapper<UserAttribute, Certification> USER_ATTRIBUTE_MAPPER =
            FACTORY.selectUserAttributesByTypeAndMasterKeyIdMapper(AutoValue_UserPacket_UserAttribute::new, Certification.FACTORY);

    @AutoValue
    public static abstract class UserId implements SelectUserIdsByMasterKeyIdModel {
        public boolean isVerified() {
            return verified() == VerificationStatus.VERIFIED_SECRET;
        }
    }

    @AutoValue
    public static abstract class UserAttribute implements SelectUserAttributesByTypeAndMasterKeyIdModel {
        public boolean isVerified() {
            return verified() == VerificationStatus.VERIFIED_SECRET;
        }
    }
}
