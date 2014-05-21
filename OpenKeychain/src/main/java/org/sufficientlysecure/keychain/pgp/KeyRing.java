package org.sufficientlysecure.keychain.pgp;

import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;

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
