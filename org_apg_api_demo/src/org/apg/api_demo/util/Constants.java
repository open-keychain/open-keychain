package org.apg.api_demo.util;

import java.util.regex.Pattern;

import android.net.Uri;

public class Constants {
    public static final String NAME = "apg";

    public static final String APG_PACKAGE_NAME = "org.apg";
    public static final int MIN_REQUIRED_VERSION = 50;

    public static final String AUTHORITY = "org.apg.provider";
    public static final Uri CONTENT_URI_SECRET_KEY_RING_BY_KEY_ID = Uri.parse("content://"
            + AUTHORITY + "/key_rings/secret/key_id/");
    public static final Uri CONTENT_URI_SECRET_KEY_RING_BY_EMAILS = Uri.parse("content://"
            + AUTHORITY + "/key_rings/secret/emails/");

    public static final Uri CONTENT_URI_PUBLIC_KEY_RING_BY_KEY_ID = Uri.parse("content://"
            + AUTHORITY + "/key_rings/public/key_id/");
    public static final Uri CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS = Uri.parse("content://"
            + AUTHORITY + "/key_rings/public/emails/");

    public static class Intent {
        public static final String DECRYPT = APG_PACKAGE_NAME + ".intent.DECRYPT";
        public static final String ENCRYPT = APG_PACKAGE_NAME + ".intent.ENCRYPT";
        public static final String DECRYPT_FILE = APG_PACKAGE_NAME + ".intent.DECRYPT_FILE";
        public static final String ENCRYPT_FILE = APG_PACKAGE_NAME + ".intent.ENCRYPT_FILE";
        public static final String DECRYPT_AND_RETURN = APG_PACKAGE_NAME
                + ".intent.DECRYPT_AND_RETURN";
        public static final String ENCRYPT_AND_RETURN = APG_PACKAGE_NAME
                + ".intent.ENCRYPT_AND_RETURN";
        public static final String SELECT_PUBLIC_KEYS = APG_PACKAGE_NAME
                + ".intent.SELECT_PUBLIC_KEYS";
        public static final String SELECT_SECRET_KEY = APG_PACKAGE_NAME
                + ".intent.SELECT_SECRET_KEY";
    }

    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_DECRYPTED_MESSAGE = "decryptedMessage";
    public static final String EXTRA_ENCRYPTED_MESSAGE = "encryptedMessage";
    public static final String EXTRA_SIGNATURE = "signature";
    public static final String EXTRA_SIGNATURE_KEY_ID = "signatureKeyId";
    public static final String EXTRA_SIGNATURE_USER_ID = "signatureUserId";
    public static final String EXTRA_SIGNATURE_SUCCESS = "signatureSuccess";
    public static final String EXTRA_SIGNATURE_UNKNOWN = "signatureUnknown";
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_KEY_ID = "keyId";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = "encryptionKeyIds";
    public static final String EXTRA_SELECTION = "selection";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_INTENT_VERSION = "intentVersion";

    public static final String INTENT_VERSION = "1";

    public static final int DECRYPT_MESSAGE = 0x21070001;
    public static final int ENCRYPT_MESSAGE = 0x21070002;
    public static final int SELECT_PUBLIC_KEYS = 0x21070003;
    public static final int SELECT_SECRET_KEY = 0x21070004;

    // public static Pattern PGP_MESSAGE = Pattern.compile(
    // ".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*", Pattern.DOTALL);

    // public static Pattern PGP_SIGNED_MESSAGE = Pattern
    // .compile(
    // ".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
    // Pattern.DOTALL);
}
