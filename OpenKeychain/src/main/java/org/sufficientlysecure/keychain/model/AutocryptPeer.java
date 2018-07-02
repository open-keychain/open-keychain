package org.sufficientlysecure.keychain.model;


import com.google.auto.value.AutoValue;
import com.squareup.sqldelight.RowMapper;
import org.sufficientlysecure.keychain.AutocryptPeersModel;


@AutoValue
public abstract class AutocryptPeer implements AutocryptPeersModel {

    public enum GossipOrigin {
        GOSSIP_HEADER, SIGNATURE, DEDUP
    }

    public static final Factory<AutocryptPeer> FACTORY = new Factory<AutocryptPeer>(AutoValue_AutocryptPeer::new,
    CustomColumnAdapters.DATE_ADAPTER, CustomColumnAdapters.DATE_ADAPTER, CustomColumnAdapters.DATE_ADAPTER,
            CustomColumnAdapters.GOSSIP_ORIGIN_ADAPTER);

    public static final RowMapper<AutocryptPeer> PEER_MAPPER = FACTORY.selectByIdentifiersMapper();
    public static final RowMapper<AutocryptKeyStatus> KEY_STATUS_MAPPER =
            FACTORY.selectAutocryptKeyStatusMapper(AutoValue_AutocryptPeer_AutocryptKeyStatus::new);

    @AutoValue
    public static abstract class AutocryptKeyStatus implements SelectAutocryptKeyStatusModel<AutocryptPeer> {
        public boolean hasGossipKey() {
            return autocryptPeer().gossip_master_key_id() != null;
        }

        public boolean isGossipKeyRevoked() {
            Boolean gossip_key_is_revoked = gossip_key_is_revoked_int();
            return gossip_key_is_revoked != null && gossip_key_is_revoked;
        }

        public boolean isGossipKeyExpired() {
            return gossip_key_is_expired_int() != 0;
        }

        public boolean isGossipKeyVerified() {
            return gossip_key_is_verified_int() != 0;
        }

        public boolean isKeyRevoked() {
            Boolean revoked = key_is_revoked_int();
            return revoked != null && revoked;
        }

        public boolean isKeyExpired() {
            return key_is_expired_int() != 0;
        }

        public boolean isKeyVerified() {
            return key_is_verified_int() != 0;
        }
    }

}
