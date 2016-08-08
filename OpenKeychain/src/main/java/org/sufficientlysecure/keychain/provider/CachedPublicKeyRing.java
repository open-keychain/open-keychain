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

package org.sufficientlysecure.keychain.provider;

import android.database.Cursor;
import android.net.Uri;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing.SecretKeyRingType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.ProviderHelper.NotFoundException;
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
    public long getMasterKeyId() throws PgpKeyNotFoundException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.MASTER_KEY_ID, Cursor.FIELD_TYPE_INTEGER);
            return (Long) data;
        } catch (ProviderHelper.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    /**
     * Find the master key id related to a given query. The id will either be extracted from the
     * query, which should work for all specific /key_rings/ queries, or will be queried if it can't.
     */
    public long extractOrGetMasterKeyId() throws PgpKeyNotFoundException {
        // try extracting from the uri first
        String firstSegment = mUri.getPathSegments().get(1);
        if (!"find".equals(firstSegment)) try {
            return Long.parseLong(firstSegment);
        } catch (NumberFormatException e) {
            // didn't work? oh well.
            Log.d(Constants.TAG, "Couldn't get masterKeyId from URI, querying...");
        }
        return getMasterKeyId();
    }

    public byte[] getFingerprint() throws PgpKeyNotFoundException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.FINGERPRINT, Cursor.FIELD_TYPE_BLOB);
            return (byte[]) data;
        } catch (ProviderHelper.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    @Override
    public String getPrimaryUserId() throws PgpKeyNotFoundException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.USER_ID,
                    Cursor.FIELD_TYPE_STRING);
            return (String) data;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    public String getPrimaryUserIdWithFallback() throws PgpKeyNotFoundException {
        return getPrimaryUserId();
    }

    @Override
    public boolean isRevoked() throws PgpKeyNotFoundException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.IS_REVOKED,
                    Cursor.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    @Override
    public boolean canCertify() throws PgpKeyNotFoundException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.HAS_CERTIFY,
                    Cursor.FIELD_TYPE_NULL);
            return !((Boolean) data);
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    @Override
    public long getEncryptId() throws PgpKeyNotFoundException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeyRings.HAS_ENCRYPT,
                    Cursor.FIELD_TYPE_INTEGER);
            return (Long) data;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    @Override
    public boolean hasEncrypt() throws PgpKeyNotFoundException {
        return getEncryptId() != 0;
    }

    /** Returns the key id which should be used for signing.
     *
     * This method returns keys which are actually available (ie. secret available, and not stripped,
     * revoked, or expired), hence only works on keyrings where a secret key is available!
     *
     */
    public long getSecretSignId() throws PgpKeyNotFoundException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeyRings.HAS_SIGN,
                    Cursor.FIELD_TYPE_INTEGER);
            return (Long) data;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    @Override
    public int getVerified() throws PgpKeyNotFoundException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.VERIFIED,
                    Cursor.FIELD_TYPE_INTEGER);
            return (Integer) data;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    public boolean hasAnySecret() throws PgpKeyNotFoundException {
        try {
            Object data = mProviderHelper.getGenericData(mUri,
                    KeychainContract.KeyRings.HAS_ANY_SECRET,
                    Cursor.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(ProviderHelper.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    private Cursor getSubkeys() throws PgpKeyNotFoundException {
        Uri keysUri = KeychainContract.Keys.buildKeysUri(extractOrGetMasterKeyId());
        return mProviderHelper.getContentResolver().query(keysUri, null, null, null, null);
    }

    public SecretKeyType getSecretKeyType(long keyId) throws NotFoundException {
        Object data = mProviderHelper.getGenericData(Keys.buildKeysUri(mUri),
                KeyRings.HAS_SECRET,
                Cursor.FIELD_TYPE_INTEGER,
                KeyRings.KEY_ID + " = " + Long.toString(keyId));
        return SecretKeyType.fromNum(((Long) data).intValue());
    }

    public SecretKeyRingType getSecretKeyringType() throws NotFoundException {
        Object data = mProviderHelper.getGenericData(KeyRings.buildUnifiedKeyRingUri(mUri),
                KeyRings.SECRET_RING_TYPE,
                Cursor.FIELD_TYPE_INTEGER);

        return SecretKeyRingType.fromNum(((Long) data).intValue());
    }
}
