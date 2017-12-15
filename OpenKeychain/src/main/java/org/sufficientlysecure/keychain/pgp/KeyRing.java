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

package org.sufficientlysecure.keychain.pgp;

import android.text.TextUtils;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.openintents.openpgp.util.OpenPgpUtils.UserId;
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

    /**
     * Splits userId string into naming part, email part, and comment part
     * <p/>
     * User ID matching:
     * http://fiddle.re/t4p6f
     */
    public static UserId splitUserId(final String userId) {
        return OpenPgpUtils.splitUserId(userId);
    }

    /**
     * Returns a composed user id. Returns null if name, email and comment are empty.
     */
    public static String createUserId(UserId userId) {
        return OpenPgpUtils.createUserId(userId);
    }


}
