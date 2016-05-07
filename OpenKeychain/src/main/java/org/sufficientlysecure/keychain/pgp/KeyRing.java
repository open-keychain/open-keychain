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

import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;

import java.io.Serializable;
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

    abstract public long getMasterKeyId() throws PgpKeyNotFoundException;

    abstract public String getPrimaryUserId() throws PgpKeyNotFoundException;

    abstract public String getPrimaryUserIdWithFallback() throws PgpKeyNotFoundException;

    public UserId getSplitPrimaryUserIdWithFallback() throws PgpKeyNotFoundException {
        return splitUserId(getPrimaryUserIdWithFallback());
    }

    abstract public boolean isRevoked() throws PgpKeyNotFoundException;

    abstract public boolean canCertify() throws PgpKeyNotFoundException;

    abstract public long getEncryptId() throws PgpKeyNotFoundException;

    abstract public boolean hasEncrypt() throws PgpKeyNotFoundException;

    abstract public int getVerified() throws PgpKeyNotFoundException;

    private static final Pattern USER_ID_PATTERN = Pattern.compile("^(.*?)(?: \\((.*)\\))?(?: <(.*)>)?$");

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^.*@.*\\..*$");

    /**
     * Splits userId string into naming part, email part, and comment part
     * <p/>
     * User ID matching:
     * http://fiddle.re/t4p6f
     */
    public static UserId splitUserId(final String userId) {
        if (!TextUtils.isEmpty(userId)) {
            final Matcher matcher = USER_ID_PATTERN.matcher(userId);
            if (matcher.matches()) {
                String name = matcher.group(1).isEmpty() ? null : matcher.group(1);
                String comment = matcher.group(2);
                String email = matcher.group(3);
                if (comment == null && email == null && name != null && EMAIL_PATTERN.matcher(name).matches()) {
                    email = name;
                    name = null;
                }
                return new UserId(name, email, comment);
            }
        }
        return new UserId(null, null, null);
    }

    /**
     * Returns a composed user id. Returns null if name, email and comment are empty.
     */
    public static String createUserId(UserId userId) {
        StringBuilder userIdBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(userId.name)) {
            userIdBuilder.append(userId.name);
        }
        if (!TextUtils.isEmpty(userId.comment)) {
            userIdBuilder.append(" (");
            userIdBuilder.append(userId.comment);
            userIdBuilder.append(")");
        }
        if (!TextUtils.isEmpty(userId.email)) {
            userIdBuilder.append(" <");
            userIdBuilder.append(userId.email);
            userIdBuilder.append(">");
        }
        return userIdBuilder.length() == 0 ? null : userIdBuilder.toString();
    }

    public static class UserId implements Serializable {
        public final String name;
        public final String email;
        public final String comment;

        public UserId(String name, String email, String comment) {
            this.name = name;
            this.email = email;
            this.comment = comment;
        }
    }

}
