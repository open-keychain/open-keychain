package org.sufficientlysecure.keychain.provider;

import android.database.Cursor;
import android.net.Uri;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.Log;

/** This implementation of KeyRing provides a cached view of PublicKeyRing
 * objects based on database queries exclusively.
 *
 * This class should be used where only few points of data but no actual
 * cryptographic operations are required about a PublicKeyRing which is already
 * in the database.  This happens commonly in UI code, where parsing of a PGP
 * key for examination would be a very expensive operation.
 *
 * Each getter method is implemented using a more or less expensive database
 * query, while object construction is (almost) free. A common pattern is
 * mProviderHelper.getCachedKeyRing(uri).getterMethod()
 *
 * TODO Ensure that the values returned here always match the ones returned by
 * the parsed KeyRing!
 *
 */
public class CachedPublicKeyRing extends KeyRing {

    final ProviderHelper mProviderHelper;
    final Uri mUri;

    public CachedPublicKeyRing(ProviderHelper providerHelper, Uri uri) {
        mProviderHelper = providerHelper;
        mUri = uri;
    }

    @Override
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

    public byte[] getFingerprint() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.FINGERPRINT, ProviderHelper.FIELD_TYPE_BLOB);
            return (byte[]) data;
        } catch (ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    @Override
    public String getPrimaryUserId() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.USER_ID,
                    ProviderHelper.FIELD_TYPE_STRING);
            return (String) data;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    public String getPrimaryUserIdWithFallback() throws PgpGeneralException {
        return getPrimaryUserId();
    }

    @Override
    public boolean isRevoked() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.IS_REVOKED,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    @Override
    public boolean canCertify() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.CAN_CERTIFY,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    @Override
    public long getEncryptId() throws PgpGeneralException {
        try {
            Cursor subkeys = getSubkeys();
            if (subkeys != null) {
                try {
                    while (subkeys.moveToNext()) {
                        if (subkeys.getInt(subkeys.getColumnIndexOrThrow(KeychainContract.Keys.CAN_ENCRYPT)) != 0) {
                            return subkeys.getLong(subkeys.getColumnIndexOrThrow(KeychainContract.Keys.KEY_ID));
                        }
                    }
                } finally {
                    subkeys.close();
                }
            }
        } catch(Exception e) {
            throw new PgpGeneralException(e);
        }
        throw new PgpGeneralException("No encrypt key found");
    }

    @Override
    public boolean hasEncrypt() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.HAS_ENCRYPT,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    @Override
    public long getSignId() throws PgpGeneralException {
        try {
            Cursor subkeys = getSubkeys();
            if (subkeys != null) {
                try {
                    while (subkeys.moveToNext()) {
                        if (subkeys.getInt(subkeys.getColumnIndexOrThrow(KeychainContract.Keys.CAN_SIGN)) != 0) {
                            return subkeys.getLong(subkeys.getColumnIndexOrThrow(KeychainContract.Keys.KEY_ID));
                        }
                    }
                } finally {
                    subkeys.close();
                }
            }
        } catch(Exception e) {
            throw new PgpGeneralException(e);
        }
        throw new PgpGeneralException("No sign key found");
    }

    @Override
    public boolean hasSign() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.HAS_SIGN,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    @Override
    public int getVerified() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.VERIFIED,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Integer) data;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    public boolean hasAnySecret() throws PgpGeneralException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.HAS_ANY_SECRET,
                    ProviderHelper.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpGeneralException(e);
        }
    }

    private Cursor getSubkeys() throws PgpGeneralException {
        Uri keysUri = KeychainContract.Keys.buildKeysUri(Long.toString(extractOrGetMasterKeyId()));
        return mProviderHelper.getContentResolver().query(keysUri, null, null, null, null);
    }
}
