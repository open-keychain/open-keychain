/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.pgp;

import android.text.TextUtils;

import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An abstract KeyRing.
 * <p/>
 * This is an abstract class for all KeyRing constructs. It serves as a common
 * denominator of available information, two implementations wrapping the same
 * keyring should in all cases agree on the output of all methods described
 * here.
 *
 * @see CanonicalizedKeyRing
 * @see org.sufficientlysecure.keychain.provider.CachedPublicKeyRing
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

    /**
     * Returns a composed user id. Returns null if name is null!
     *
     * @param name
     * @param email
     * @param comment
     * @return
     */
    public static String createUserId(String name, String email, String comment) {
        String userId = name; // consider name a required value
        if (userId != null && !TextUtils.isEmpty(comment)) {
            userId += " (" + comment + ")";
        }
        if (userId != null && !TextUtils.isEmpty(email)) {
            userId += " <" + email + ">";
        }

        return userId;
    }

}
