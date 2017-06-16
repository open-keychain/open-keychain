/*
 * Copyright (C) 2016 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAutocryptPeerColumns;


public class KeychainExternalContract {
    public static final int KEY_STATUS_UNAVAILABLE = 0;
    public static final int KEY_STATUS_UNVERIFIED = 1;
    public static final int KEY_STATUS_VERIFIED = 2;

    // this is in KeychainExternalContract already, but we want to be double
    // sure this isn't mixed up with the internal one!
    public static final String CONTENT_AUTHORITY_EXTERNAL = Constants.PROVIDER_AUTHORITY + ".exported";
    private static final Uri BASE_CONTENT_URI_EXTERNAL = Uri
            .parse("content://" + CONTENT_AUTHORITY_EXTERNAL);
    public static final String BASE_EMAIL_STATUS = "email_status";
    public static final String BASE_AUTOCRYPT_PEER_STATUS = "autocrypt_status";
    public static final String BASE_AUTOCRYPT_PEERS = "autocrypt_peers";


    public static class EmailStatus implements BaseColumns {
        public static final String EMAIL_ADDRESS = "email_address";
        public static final String USER_ID = "user_id";
        public static final String USER_ID_STATUS = "email_status";
        public static final String MASTER_KEY_ID = "master_key_id";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI_EXTERNAL.buildUpon()
                .appendPath(BASE_EMAIL_STATUS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.sufficientlysecure.keychain.provider.email_status";
    }

    public static class AutocryptPeerStatus implements BaseColumns {
        public static final String EMAIL_ADDRESS = "email_address";
        public static final String MASTER_KEY_ID = "master_key_id";
        public static final String AUTOCRYPT_PEER_LAST_SEEN = "autocrypt_peer_last_seen";
        public static final String AUTOCRYPT_PEER_STATUS = "autocrypt_peer_state";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI_EXTERNAL.buildUpon()
                .appendPath(BASE_EMAIL_STATUS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.org.sufficientlysecure.keychain.provider.autocrypt_status";
    }

    public static class ApiAutocryptPeer implements ApiAutocryptPeerColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI_EXTERNAL.buildUpon()
                .appendPath(BASE_AUTOCRYPT_PEERS).build();

        public static Uri buildByPackageNameUri(String packageName) {
            return CONTENT_URI.buildUpon().appendEncodedPath(packageName).build();
        }

        public static Uri buildByPackageNameAndTrustIdUri(String packageName, String autocryptPeer) {
            return CONTENT_URI.buildUpon().appendEncodedPath(packageName).appendEncodedPath(autocryptPeer).build();
        }
    }

    private KeychainExternalContract() {
    }
}
