package org.sufficientlysecure.keychain.pgp;

import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** An abstract KeyRing.
 *
 * This is an abstract class for all KeyRing constructs. It serves as a common
 * denominator of available information, two implementations wrapping the same
 * keyring should in all cases agree on the output of all methods described
 * here.
 *
 * @see CanonicalizedKeyRing
 * @see org.sufficientlysecure.keychain.provider.CachedPublicKeyRing
 *
 */
public abstract class KeyRing {

    abstract public long getMasterKeyId() throws PgpGeneralException;

    abstract public String getPrimaryUserId() throws PgpGeneralException;

    abstract public String getPrimaryUserIdWithFallback() throws PgpGeneralException;

    public String[] getSplitPrimaryUserIdWithFallback() throws PgpGeneralException {
        return splitUserId(getPrimaryUserIdWithFallback());
    }

    abstract public boolean isRevoked() throws PgpGeneralException;

    abstract public boolean canCertify() throws PgpGeneralException;

    abstract public long getEncryptId() throws PgpGeneralException;

    abstract public boolean hasEncrypt() throws PgpGeneralException;

    abstract public long getSignId() throws PgpGeneralException;

    abstract public boolean hasSign() throws PgpGeneralException;

    abstract public int getVerified() throws PgpGeneralException;

    private static final Pattern USER_ID_PATTERN = Pattern.compile("^(.*?)(?: \\((.*)\\))?(?: <(.*)>)?$");

    /**
     * Splits userId string into naming part, email part, and comment part
     *
     * @param userId
     * @return array with naming (0), email (1), comment (2)
     */
    public static String[] splitUserId(String userId) {
        String[] result = new String[]{null, null, null};

        if (userId == null || userId.equals("")) {
            return result;
        }

        /*
         * User ID matching:
         * http://fiddle.re/t4p6f
         *
         * test cases:
         * "Max Mustermann (this is a comment) <max@example.com>"
         * "Max Mustermann <max@example.com>"
         * "Max Mustermann (this is a comment)"
         * "Max Mustermann [this is nothing]"
         */
        Matcher matcher = USER_ID_PATTERN.matcher(userId);
        if (matcher.matches()) {
            result[0] = matcher.group(1);
            result[1] = matcher.group(3);
            result[2] = matcher.group(2);
        }

        return result;
    }

}
