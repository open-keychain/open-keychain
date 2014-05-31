package org.sufficientlysecure.keychain.pgp;

import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;

/** An abstract KeyRing.
 *
 * This is an abstract class for all KeyRing constructs. It serves as a common
 * denominator of available information, two implementations wrapping the same
 * keyring should in all cases agree on the output of all methods described
 * here.
 *
 * @see org.sufficientlysecure.keychain.pgp.WrappedKeyRing
 * @see org.sufficientlysecure.keychain.provider.CachedPublicKeyRing
 *
 */
public abstract class KeyRing {

    abstract public long getMasterKeyId() throws PgpGeneralException;

    abstract public String getPrimaryUserId() throws PgpGeneralException;

    abstract public boolean isRevoked() throws PgpGeneralException;

    abstract public boolean canCertify() throws PgpGeneralException;

    abstract public long getEncryptId() throws PgpGeneralException;

    abstract public boolean hasEncrypt() throws PgpGeneralException;

    abstract public long getSignId() throws PgpGeneralException;

    abstract public boolean hasSign() throws PgpGeneralException;

    abstract public int getVerified() throws PgpGeneralException;

}
