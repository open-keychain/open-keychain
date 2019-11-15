package org.sufficientlysecure.keychain.model;


import java.util.Date;

import androidx.annotation.NonNull;

import com.squareup.sqldelight.ColumnAdapter;
import org.sufficientlysecure.keychain.model.AutocryptPeer.GossipOrigin;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;


public final class CustomColumnAdapters {

    private CustomColumnAdapters() { }

    static final ColumnAdapter<Date,Long> DATE_ADAPTER = new ColumnAdapter<Date,Long>() {
        @NonNull
        @Override
        public Date decode(Long databaseValue) {
            // Both SQLite and OpenPGP prefer a second granularity for timestamps - so we'll translate here
            return new Date(databaseValue * 1000);
        }

        @Override
        public Long encode(@NonNull Date value) {
            return value.getTime() / 1000;
        }
    };

    static final ColumnAdapter<GossipOrigin,Long> GOSSIP_ORIGIN_ADAPTER = new ColumnAdapter<GossipOrigin,Long>() {
        @NonNull
        @Override
        public GossipOrigin decode(Long databaseValue) {
            switch (databaseValue.intValue()) {
                case 0: return GossipOrigin.GOSSIP_HEADER;
                case 10: return GossipOrigin.SIGNATURE;
                case 20: return GossipOrigin.DEDUP;
                default: throw new IllegalArgumentException("Unhandled database value!");
            }
        }

        @Override
        public Long encode(@NonNull GossipOrigin value) {
            switch (value) {
                case GOSSIP_HEADER: return 0L;
                case SIGNATURE: return 10L;
                case DEDUP: return 20L;
                default: throw new IllegalArgumentException("Unhandled database value!");
            }
        }
    };

    public static final ColumnAdapter<SecretKeyType,Long> SECRET_KEY_TYPE_ADAPTER = new ColumnAdapter<SecretKeyType, Long>() {
        @NonNull
        @Override
        public SecretKeyType decode(Long databaseValue) {
            return databaseValue == null ? SecretKeyType.UNAVAILABLE : SecretKeyType.fromNum(databaseValue.intValue());
        }

        @Override
        public Long encode(@NonNull SecretKeyType value) {
            return (long) value.getNum();
        }
    };

    public static final ColumnAdapter<VerificationStatus,Long> VERIFICATON_STATUS_ADAPTER = new ColumnAdapter<VerificationStatus, Long>() {
        @NonNull
        @Override
        public VerificationStatus decode(Long databaseValue) {
            if (databaseValue == null) {
                return VerificationStatus.UNVERIFIED;
            }
            switch (databaseValue.intValue()) {
                case 0: return VerificationStatus.UNVERIFIED;
                case 1: return VerificationStatus.VERIFIED_SECRET;
                case 2: return VerificationStatus.VERIFIED_SELF;
                default: throw new IllegalArgumentException();
            }
        }

        @Override
        public Long encode(@NonNull VerificationStatus value) {
            switch (value) {
                case UNVERIFIED: return 0L;
                case VERIFIED_SECRET: return 1L;
                case VERIFIED_SELF: return 2L;
                default: throw new IllegalArgumentException();
            }
        }
    };
}
