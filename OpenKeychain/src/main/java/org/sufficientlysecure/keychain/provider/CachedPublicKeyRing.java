package org.sufficientlysecure.keychain.provider;

import android.net.Uri;

import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;

public class CachedPublicKeyRing extends KeyRing {

    final ProviderHelper mProviderHelper;
    final Uri mUri;

    public CachedPublicKeyRing(ProviderHelper providerHelper, Uri uri) {
        mProviderHelper = providerHelper;
        mUri = uri;
    }

    public long getMasterKeyId() throws PgpGeneralException {
        try {
            return mProviderHelper.getMasterKeyId(mUri);
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    public String getPrimaryUserId() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.MASTER_KEY_ID,
                    ProviderHelper.FIELD_TYPE_STRING);
            return (String) data;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    public boolean isRevoked() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.MASTER_KEY_ID,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    public boolean canCertify() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.MASTER_KEY_ID,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    public long getEncryptId() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.MASTER_KEY_ID,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Long) data;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    public boolean hasEncrypt() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.MASTER_KEY_ID,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    public long getSignId() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.MASTER_KEY_ID,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Long) data;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    public boolean hasSign() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.MASTER_KEY_ID,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    public int getVerified() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.MASTER_KEY_ID,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Integer) data;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    public boolean hasAnySecret() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.MASTER_KEY_ID,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }

    }
}
