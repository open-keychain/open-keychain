/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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
    }

    interface KeysColumns {
        String MASTER_KEY_ID = "master_key_id"; // not a database id
        String RANK = "rank";

        String KEY_ID = "key_id"; // not a database id
        String ALGORITHM = "algorithm";
        String FINGERPRINT = "fingerprint";

        String KEY_SIZE = "key_size";
        String CAN_SIGN = "can_sign";
        String CAN_ENCRYPT = "can_encrypt";
        String CAN_CERTIFY = "can_certify";
        String IS_REVOKED = "is_revoked";

        String CREATION = "creation";
        String EXPIRY = "expiry";
    }

    interface UserIdsColumns {
        String MASTER_KEY_ID = "master_key_id"; // foreign key to key_rings._ID
        String USER_ID = "user_id"; // not a database id
        String RANK = "rank"; // ONLY used for sorting! no key, no nothing!
        String IS_PRIMARY = "is_primary";
    }

    interface ApiAppsColumns {
        String PACKAGE_NAME = "package_name";
        String PACKAGE_SIGNATURE = "package_signature";
    }

    interface ApiAppsAccountsColumns {
        String ACCOUNT_NAME = "account_name";
        String KEY_ID = "key_id"; // not a database id
        String ENCRYPTION_ALGORITHM = "encryption_algorithm";
        String HASH_ALORITHM = "hash_algorithm";
        String COMPRESSION = "compression";
        String PACKAGE_NAME = "package_name"; // foreign key to api_apps.package_name
    }

    public static final class KeyTypes {
        public static final int PUBLIC = 0;
        public static final int SECRET = 1;
    }

    public static final String CONTENT_AUTHORITY = Constants.PACKAGE_NAME + ".provider";

    private static final Uri BASE_CONTENT_URI_INTERNAL = Uri
            .parse("content://" + CONTENT_AUTHORITY);

    public static final String BASE_KEY_RINGS = "key_rings";
    public static final String BASE_DATA = "data";

    public static final String PATH_UNIFIED = "unified";

    public static final String PATH_FIND = "find";
    public static final String PATH_BY_EMAIL = "email";
    public static final String PATH_BY_SUBKEY = "subkey";

    public static final String PATH_PUBLIC = "public";
    public static final String PATH_SECRET = "secret";
    public static final String PATH_USER_IDS = "user_ids";
    public static final String PATH_KEYS = "keys";

    public static final String BASE_API_APPS = "api_apps";
    public static final String PATH_ACCOUNTS = "accounts";

    public static class KeyRings implements BaseColumns, KeysColumns, UserIdsColumns {
        public static final String MASTER_KEY_ID = "master_key_id";
        public static final String HAS_SECRET = "has_secret";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.sufficientlysecure.openkeychain.key_ring";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.sufficientlysecure.openkeychain.key_ring";

        public static Uri buildUnifiedKeyRingsUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_UNIFIED).build();
        }

        public static Uri buildGenericKeyRingUri(String masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(masterKeyId).build();
        }
        public static Uri buildUnifiedKeyRingUri(String masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(masterKeyId).appendPath(PATH_UNIFIED).build();
        }
        public static Uri buildUnifiedKeyRingUri(Uri uri) {
            return CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(1)).appendPath(PATH_UNIFIED).build();
        }

        public static Uri buildUnifiedKeyRingsFindByEmailUri(String email) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FIND).appendPath(PATH_BY_EMAIL).appendPath(email).build();
        }
        public static Uri buildUnifiedKeyRingsFindBySubkeyUri(String subkey) {
            return CONTENT_URI.buildUpon().appendPath(PATH_FIND).appendPath(PATH_BY_SUBKEY).appendPath(subkey).build();
        }

    }

    public static class KeyRingData implements KeyRingsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.sufficientlysecure.openkeychain.key_ring_data";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.sufficientlysecure.openkeychain.key_ring_data";

        public static Uri buildPublicKeyRingUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_PUBLIC).build();
        }
        public static Uri buildPublicKeyRingUri(String masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(masterKeyId).appendPath(PATH_PUBLIC).build();
        }
        public static Uri buildPublicKeyRingUri(Uri uri) {
            return CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(1)).appendPath(PATH_PUBLIC).build();
        }

        public static Uri buildSecretKeyRingUri(String masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(masterKeyId).appendPath(PATH_SECRET).build();
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
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.sufficientlysecure.openkeychain.key";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.sufficientlysecure.openkeychain.key";

        public static Uri buildKeysUri(String masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(masterKeyId).appendPath(PATH_KEYS).build();
        }
        public static Uri buildKeysUri(Uri uri) {
            return CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(1)).appendPath(PATH_KEYS).build();
        }

    }

    public static class UserIds implements UserIdsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_KEY_RINGS).build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.sufficientlysecure.openkeychain.user_id";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.sufficientlysecure.openkeychain.user_id";

        public static Uri buildUserIdsUri(String masterKeyId) {
            return CONTENT_URI.buildUpon().appendPath(masterKeyId).appendPath(PATH_USER_IDS).build();
        }
        public static Uri buildUserIdsUri(Uri uri) {
            return CONTENT_URI.buildUpon().appendPath(uri.getPathSegments().get(1)).appendPath(PATH_USER_IDS).build();
        }
    }

    public static class ApiApps implements ApiAppsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_API_APPS).build();

        /**
         * Use if multiple items get returned
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.sufficientlysecure.openkeychain.api_apps";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.sufficientlysecure.openkeychain.api_app";

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
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.sufficientlysecure.openkeychain.api_app.accounts";

        /**
         * Use if a single item is returned
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.sufficientlysecure.openkeychain.api_app.account";

        public static Uri buildBaseUri(String packageName) {
            return CONTENT_URI.buildUpon().appendEncodedPath(packageName).appendPath(PATH_ACCOUNTS)
                    .build();
        }

        public static Uri buildByPackageAndAccountUri(String packageName, String accountName) {
            return CONTENT_URI.buildUpon().appendEncodedPath(packageName).appendPath(PATH_ACCOUNTS)
                    .appendEncodedPath(accountName).build();
        }
    }

    public static class DataStream {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_INTERNAL.buildUpon()
                .appendPath(BASE_DATA).build();

        public static Uri buildDataStreamUri(String streamFilename) {
            return CONTENT_URI.buildUpon().appendPath(streamFilename).build();
        }
    }

    private KeychainContract() {
    }
}
