/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

    interface KeyRingsColumns {
        String MASTER_KEY_ID = "master_key_id"; // not a database id
        String KEY_RING_DATA = "key_ring_data"; // PGPPublicKeyRing / PGPSecretKeyRing blob
        String AWAITING_MERGE = "awaiting_merge"; // waiting to merge public key into secret
        String SECRET_RING_TYPE = "secret_ring_type";
    }

    interface KeysColumns {
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
        String HAS_SECRET = "has_secret";

        String CREATION = "creation";
        String EXPIRY = "expiry";
    }

    interface UpdatedKeysColumns {
        String MASTER_KEY_ID = "master_key_id"; // not a database id
        String LAST_UPDATED = "last_updated"; // time since epoch in seconds
    }

    interface UserPacketsColumns {
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

    interface CertsColumns {
        String MASTER_KEY_ID = "master_key_id";
        String RANK = "rank";
        String KEY_ID_CERTIFIER = "key_id_certifier";
        String TYPE = "type";
        String VERIFIED = "verified";
        String CREATION = "creation";
        String DATA = "data";
    }

    interface ApiAppsColumns {
        String PACKAGE_NAME = "package_name";
        String PACKAGE_CERTIFICATE = "package_signature";
    }

    interface ApiAppsAccountsColumns {
        String ACCOUNT_NAME = "account_name";
        String KEY_ID = "key_id"; // not a database id
        String ENCRYPTION_ALGORITHM = "encryption_algorithm";
        String HASH_ALORITHM = "hash_algorithm";
        String COMPRESSION = "compression";
        String PACKAGE_NAME = "package_name"; // foreign key to api_apps.package_name
    }

    interface ApiAppsAllowedKeysColumns {
        String KEY_ID = "key_id"; // not a database id
        String PACKAGE_NAME = "package_name"; // foreign key to api_apps.package_name
    }

    public static final String CONTENT_AUTHORITY = Constants.PROVIDER_AUTHORITY;

    private static final Uri BASE_CONTENT_URI_INTERNAL = Uri
            .parse("content://" + CONTENT_AUTHORITY);

    public static final String BASE_KEY_RINGS = "key_rings";

    public static final String BASE_UPDATED_KEYS = "updated_keys";

    public static final String PATH_UNIFIED = "unified";

    public static final String PATH_FIND = "find";
    public static final String PATH_BY_EMAIL = "email";
    public static final String PATH_BY_SUBKEY = "subkey";
    public static final String PATH_BY_USER_ID = "user_id";

    public static final String PATH_PUBLIC = "public";
    public static final String PATH_SECRET = "secret";
    public static final String PATH_USER_IDS = "user_ids";
    public static final String PATH_LINKED_IDS = "linked_ids";
    public static final String PATH_KEYS = "keys";
    public static final String PATH_CERTS = "certs";

    public static final String BASE_API_APPS = "api_apps";
    public static final String PATH_ACCOUNTS = "accounts";
    public static final String PATH_ALLOWED_KEYS = "allowed_keys";

    public static class KeyRings implements BaseColumns, KeysColumns, UserPacketsColumns {
        public static final String MASTER_KEY_ID = KeysColumns.MASTER_KEY_ID;
        public static final String IS_REVOKED = KeysColumns.IS_REVOKED;
        public static final String VERIFIED = CertsColumns.VERIFIED;
        public static final String IS_EXPIRED = "is_expired";
        public static final String HAS_ANY_SECRET = "has_any_secret";
        public static final String HAS_ENCRYPT = "has_encrypt";
        public static final String HAS_SIGN = "has_sign";
        public static final String HAS_CERTIFY = "has_certify";
        public static final String HAS_AUTHENTICATE = "has_authenticate";
        public static final String HAS_DUPLICATE_USER_ID = "has_duplicate_user_id";
        public static final String PUBKEY_DATA = "pubkey_data";
        public static final String PRIVKEY_DATA = "privkey_data";
        public static final String AWAITING_MERGE = "awaiting_merge";
        public static final String SECRET_RING_TYPE = "secret_ring_type";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        public static final String CONTENT_TYPE
                = "vnd.android.cursor.dir/vnd.org.sufficientlysecure.keychain.provider.key_rings";
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.org.sufficientlysecure.keychain.provider.key_rings";

        public static Uri buildUnifiedKeyRingsUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_UNIFIED).build();
        }

        public static Uri buildGenericKeyRingUri(long masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(masterKeyId)).build();
        }

        public static Uri buildGenericKeyRingUri(String masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(masterKeyId).build();
        }

        public static Uri buildGenericKeyRingUri(Uri uri) {
            return CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(1)).build();
        }

        public static Uri buildUnifiedKeyRingUri(long masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(masterKeyId))
                    .appendPath(PATH_UNIFIED).build();
        }

        public static Uri buildUnifiedKeyRingUri(Uri uri) {
            return CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(1))
                    .appendPath(PATH_UNIFIED).build();
        }

        public static Uri buildUnifiedKeyRingsFindByEmailUri(String email) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FIND)
                    .appendPath(PATH_BY_EMAIL).appendPath(email).build();
        }

        public static Uri buildUnifiedKeyRingsFindByUserIdUri(String query) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FIND)
                    .appendPath(PATH_BY_USER_ID).appendPath(query).build();
        }

        public static Uri buildUnifiedKeyRingsFindBySubkeyUri(long subkey) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FIND)
                    .appendPath(PATH_BY_SUBKEY).appendPath(Long.toString(subkey)).build();
        }

    }

    public static class KeyRingData implements KeyRingsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        public static final String CONTENT_TYPE
                = "vnd.android.cursor.dir/vnd.org.sufficientlysecure.keychain.provider.key_ring_data";
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.org.sufficientlysecure.keychain.provider.key_ring_data";

        public static Uri buildPublicKeyRingUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_PUBLIC).build();
        }

        public static Uri buildPublicKeyRingUri(long masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(masterKeyId)).appendPath(PATH_PUBLIC).build();
        }

        public static Uri buildPublicKeyRingUri(Uri uri) {
            return CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(1)).appendPath(PATH_PUBLIC).build();
        }

        public static Uri buildSecretKeyRingUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_SECRET).build();
        }

        public static Uri buildSecretKeyRingUri(long masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(masterKeyId)).appendPath(PATH_SECRET).build();
        }

        public static Uri buildSecretKeyRingUri(Uri uri) {
            return CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(1)).appendPath(PATH_SECRET).build();
        }

    }

    public static class Keys implements KeysColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE
                = "vnd.android.cursor.dir/vnd.org.sufficientlysecure.keychain.provider.keychain.keys";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.org.sufficientlysecure.keychain.provider.keychain.keys";

        public static Uri buildKeysUri(long masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(masterKeyId)).appendPath(PATH_KEYS).build();
        }

        public static Uri buildKeysUri(Uri uri) {
            return CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(1)).appendPath(PATH_KEYS).build();
        }

    }

    public static class UpdatedKeys implements UpdatedKeysColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_UPDATED_KEYS).build();

        public static final String CONTENT_TYPE
                = "vnd.android.cursor.dir/vnd.org.sufficientlysecure.keychain.provider.updated_keys";
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.org.sufficientlysecure.keychain.provider.updated_keys";
    }

    public static class UserPackets implements UserPacketsColumns, BaseColumns {
        public static final String VERIFIED = "verified";
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE
                = "vnd.android.cursor.dir/vnd.org.sufficientlysecure.keychain.provider.user_ids";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.org.sufficientlysecure.keychain.provider.user_ids";

        public static Uri buildUserIdsUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_USER_IDS).build();
        }

        public static Uri buildUserIdsUri(long masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(masterKeyId)).appendPath(PATH_USER_IDS).build();
        }

        public static Uri buildUserIdsUri(Uri uri) {
            return CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(1)).appendPath(PATH_USER_IDS).build();
        }

        public static Uri buildLinkedIdsUri(Uri uri) {
            return CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(1)).appendPath(PATH_LINKED_IDS).build();
        }

    }

    public static class ApiApps implements ApiAppsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_API_APPS).build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE
                = "vnd.android.cursor.dir/vnd.org.sufficientlysecure.keychain.provider.api_apps";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.org.sufficientlysecure.keychain.provider.api_apps";

        public static Uri buildByPackageNameUri(String packageName) {
            return CONTENT_URI.buildUpon().appendEncodedPath(packageName).build();
        }
    }

    public static class ApiAccounts implements ApiAppsAccountsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_API_APPS).build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE
                = "vnd.android.cursor.dir/vnd.org.sufficientlysecure.keychain.provider.api_apps.accounts";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.org.sufficientlysecure.keychain.provider.api_apps.accounts";

        public static Uri buildBaseUri(String packageName) {
            return CONTENT_URI.buildUpon().appendEncodedPath(packageName).appendPath(PATH_ACCOUNTS)
                    .build();
        }

        public static Uri buildByPackageAndAccountUri(String packageName, String accountName) {
            return CONTENT_URI.buildUpon().appendEncodedPath(packageName).appendPath(PATH_ACCOUNTS)
                    .appendEncodedPath(accountName).build();
        }
    }

    public static class ApiAllowedKeys implements ApiAppsAllowedKeysColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_API_APPS).build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE
                = "vnd.android.cursor.dir/vnd.org.sufficientlysecure.keychain.provider.api_apps.allowed_keys";

        public static Uri buildBaseUri(String packageName) {
            return CONTENT_URI.buildUpon().appendEncodedPath(packageName).appendPath(PATH_ALLOWED_KEYS)
                    .build();
        }
    }

    public static class Certs implements CertsColumns, BaseColumns {
        public static final String USER_ID = UserPacketsColumns.USER_ID;
        public static final String NAME = UserPacketsColumns.NAME;
        public static final String EMAIL = UserPacketsColumns.EMAIL;
        public static final String COMMENT = UserPacketsColumns.COMMENT;
        public static final String SIGNER_UID = "signer_user_id";

        public static final int UNVERIFIED = 0;
        public static final int VERIFIED_SECRET = 1;
        public static final int VERIFIED_SELF = 2;

        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        public static Uri buildCertsUri(long masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(masterKeyId)).appendPath(PATH_CERTS).build();
        }

        public static Uri buildCertsSpecificUri(long masterKeyId, long rank, long certifier) {
            return CONTENT_URI.buildUpon().appendPath(Long.toString(masterKeyId))
                    .appendPath(PATH_CERTS).appendPath(Long.toString(rank))
                    .appendPath(Long.toString(certifier)).build();
        }

        public static Uri buildCertsUri(Uri uri) {
            return CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(1))
                    .appendPath(PATH_CERTS).build();
        }

        public static Uri buildLinkedIdCertsUri(Uri uri, int rank) {
            return CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(1))
                    .appendPath(PATH_LINKED_IDS).appendPath(Integer.toString(rank))
                    .appendPath(PATH_CERTS).build();
        }

    }

    private KeychainContract() {
    }
}
