/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import org.sufficientlysecure.keychain.Constants;

public class KeychainContract {

    public interface KeyRingsColumns {
        String MASTER_KEY_ID = "master_key_id"; // not a database id
        String KEY_RING_DATA = "key_ring_data"; // PGPPublicKeyRing / PGPSecretKeyRing blob
    }

    public interface KeysColumns {
        String MASTER_KEY_ID = "master_key_id"; // not a database id
        String RANK = "rank";

        String KEY_ID = "key_id"; // not a database id
        String ALGORITHM = "algorithm";
        String FINGERPRINT = "fingerprint";

        String KEY_SIZE = "key_size";
        String KEY_CURVE_OID = "key_curve_oid";
        String CAN_SIGN = "can_sign";
        String CAN_ENCRYPT = "can_encrypt";
        String CAN_CERTIFY = "can_certify";
        String CAN_AUTHENTICATE = "can_authenticate";
        String IS_REVOKED = "is_revoked";
        String IS_SECURE = "is_secure";
        String HAS_SECRET = "has_secret";

        String CREATION = "creation";
        String EXPIRY = "expiry";
    }

    public interface KeySignaturesColumns {
        String MASTER_KEY_ID = "master_key_id"; // not a database id
        String SIGNER_KEY_ID = "signer_key_id";
    }

    public interface UserPacketsColumns {
        String MASTER_KEY_ID = "master_key_id"; // foreign key to key_rings._ID
        String TYPE = "type"; // not a database id
        String USER_ID = "user_id"; // not a database id
        String NAME = "name";
        String EMAIL = "email";
        String COMMENT = "comment";
        String ATTRIBUTE_DATA = "attribute_data"; // not a database id
        String RANK = "rank"; // ONLY used for sorting! no key, no nothing!
        String IS_PRIMARY = "is_primary";
        String IS_REVOKED = "is_revoked";
    }

    public interface CertsColumns {
        String MASTER_KEY_ID = "master_key_id";
        String RANK = "rank";
        String KEY_ID_CERTIFIER = "key_id_certifier";
        String TYPE = "type";
        String VERIFIED = "verified";
        String CREATION = "creation";
        String DATA = "data";
    }

    public interface ApiAppsAllowedKeysColumns {
        String KEY_ID = "key_id"; // not a database id
        String PACKAGE_NAME = "package_name"; // foreign key to api_apps.package_name
    }

    public interface OverriddenWarnings {
        String IDENTIFIER = "identifier";
    }

    public static final String CONTENT_AUTHORITY = Constants.PROVIDER_AUTHORITY;

    private static final Uri BASE_CONTENT_URI_INTERNAL = Uri
            .parse("content://" + CONTENT_AUTHORITY);

    public static final String BASE_KEY_RINGS = "key_rings";

    public static final String BASE_KEY_SIGNATURES = "key_signatures";

    public static final String PATH_PUBLIC = "public";
    public static final String PATH_USER_IDS = "user_ids";
    public static final String PATH_KEYS = "keys";
    public static final String PATH_CERTS = "certs";

    public static class KeyRings implements BaseColumns, KeysColumns, UserPacketsColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();
    }

    public static class KeyRingData implements KeyRingsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        public static Uri buildPublicKeyRingUri(long masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(masterKeyId)).appendPath(PATH_PUBLIC).build();
        }
    }

    public static class Keys implements KeysColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        public static Uri buildKeysUri(long masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(masterKeyId)).appendPath(PATH_KEYS).build();
        }

    }

    public static class KeySignatures implements KeySignaturesColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_SIGNATURES).build();
    }

    public static class UserPackets implements UserPacketsColumns, BaseColumns {
        public static final String VERIFIED = "verified";
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        public static Uri buildUserIdsUri(long masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(masterKeyId)).appendPath(PATH_USER_IDS).build();
        }
    }

    public static class Certs implements CertsColumns, BaseColumns {
        public static final int VERIFIED_SECRET = 1;
        public static final int VERIFIED_SELF = 2;

        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        public static Uri buildCertsUri(long masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(masterKeyId)).appendPath(PATH_CERTS).build();
        }
    }

    private KeychainContract() {
    }
}
