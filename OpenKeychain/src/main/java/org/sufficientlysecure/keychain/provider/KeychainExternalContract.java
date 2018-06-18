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


public class KeychainExternalContract {

    // this is in KeychainExternalContract already, but we want to be double
    // sure this isn't mixed up with the internal one!
    public static final String CONTENT_AUTHORITY_EXTERNAL = Constants.PROVIDER_AUTHORITY + ".exported";
    private static final Uri BASE_CONTENT_URI_EXTERNAL = Uri
            .parse("content://" + CONTENT_AUTHORITY_EXTERNAL);
    public static final String BASE_EMAIL_STATUS = "email_status";
    public static final String BASE_AUTOCRYPT_STATUS = "autocrypt_status";

    public static final int KEY_STATUS_UNAVAILABLE = 0;
    public static final int KEY_STATUS_UNVERIFIED = 1;
    public static final int KEY_STATUS_VERIFIED = 2;

    public static class EmailStatus implements BaseColumns {
        public static final String EMAIL_ADDRESS = "email_address";
        public static final String USER_ID = "user_id";
        public static final String USER_ID_STATUS = "email_status";
        public static final String MASTER_KEY_ID = "master_key_id";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI_EXTERNAL.buildUpon()
                .appendPath(BASE_EMAIL_STATUS).build();

        public static final String CONTENT_TYPE
                = "vnd.android.cursor.dir/vnd.org.sufficientlysecure.keychain.provider.email_status";
    }

    public static class AutocryptStatus implements BaseColumns {
        public static final String ADDRESS = "address";

        public static final String UID_ADDRESS = "uid_address";
        public static final String UID_KEY_STATUS = "uid_key_status";
        public static final String UID_MASTER_KEY_ID = "uid_master_key_id";
        public static final String UID_CANDIDATES = "uid_candidates";

        public static final String AUTOCRYPT_MASTER_KEY_ID = "autocrypt_master_key_id";
        public static final String AUTOCRYPT_KEY_STATUS = "autocrypt_key_status";
        public static final String AUTOCRYPT_PEER_STATE = "autocrypt_peer_state";

        public static final int AUTOCRYPT_PEER_DISABLED = 0;
        public static final int AUTOCRYPT_PEER_DISCOURAGED_OLD = 10;
        public static final int AUTOCRYPT_PEER_GOSSIP = 20;
        public static final int AUTOCRYPT_PEER_AVAILABLE_EXTERNAL = 30;
        public static final int AUTOCRYPT_PEER_AVAILABLE = 40;
        public static final int AUTOCRYPT_PEER_MUTUAL = 50;

        public static final Uri CONTENT_URI = BASE_CONTENT_URI_EXTERNAL.buildUpon()
                .appendPath(BASE_AUTOCRYPT_STATUS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.sufficientlysecure.keychain.provider.email_status";
    }

    private KeychainExternalContract() {
    }
}
