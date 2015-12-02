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

    public static class EmailStatus implements BaseColumns {
        public static final String EMAIL_ADDRESS = "email_address";
        public static final String EMAIL_STATUS = "email_status";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI_EXTERNAL.buildUpon()
                .appendPath(BASE_EMAIL_STATUS).build();

        public static final String CONTENT_TYPE
                = "vnd.android.cursor.dir/vnd.org.sufficientlysecure.keychain.provider.email_status";
    }

    private KeychainExternalContract() {
    }

}
