package org.sufficientlysecure.keychain.provider;

import android.net.Uri;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.Log;

public class CachedPublicKeyRing extends KeyRing {

    final ProviderHelper mProviderHelper;
    final Uri mUri;

    public CachedPublicKeyRing(ProviderHelper providerHelper, Uri uri) {
        mProviderHelper = providerHelper;
        mUri = uri;
    }

    public long getMasterKeyId() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.MASTER_KEY_ID, ProviderHelper.FIELD_TYPE_INTEGER);
            return (Long) data;
        } catch (ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    /**
     * Find the master key id related to a given query. The id will either be extracted from the
     * query, which should work for all specific /key_rings/ queries, or will be queried if it can't.
     */
    public long extractOrGetMasterKeyId() throws PgpGeneralException {
        // try extracting from the uri first
        String firstSegment = mUri.getPathSegments().get(1);
        if (!firstSegment.equals("find")) try {
            return Long.parseLong(firstSegment);
        } catch (NumberFormatException e) {
            // didn't work? oh well.
            Log.d(Constants.TAG, "Couldn't get masterKeyId from URI, querying...");
        }
        return getMasterKeyId();
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
