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
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.DatabaseInteractor.NotFoundException;
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

    final DatabaseInteractor mDatabaseInteractor;
    final Uri mUri;

    public CachedPublicKeyRing(DatabaseInteractor databaseInteractor, Uri uri) {
        mDatabaseInteractor = databaseInteractor;
        mUri = uri;
    }

    @Override
    public long getMasterKeyId() throws PgpKeyNotFoundException {
        try {
            Object data = mDatabaseInteractor.getGenericData(mUri,
                    KeychainContract.KeyRings.MASTER_KEY_ID, DatabaseInteractor.FIELD_TYPE_INTEGER);
            return (Long) data;
        } catch (DatabaseReadWriteInteractor.NotFoundException e) {
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
            Object data = mDatabaseInteractor.getGenericData(mUri,
                    KeychainContract.KeyRings.FINGERPRINT, DatabaseInteractor.FIELD_TYPE_BLOB);
            return (byte[]) data;
        } catch (DatabaseReadWriteInteractor.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    @Override
    public String getPrimaryUserId() throws PgpKeyNotFoundException {
        try {
            Object data = mDatabaseInteractor.getGenericData(mUri,
                    KeychainContract.KeyRings.USER_ID,
                    DatabaseInteractor.FIELD_TYPE_STRING);
            return (String) data;
        } catch(DatabaseReadWriteInteractor.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    public String getPrimaryUserIdWithFallback() throws PgpKeyNotFoundException {
        return getPrimaryUserId();
    }

    public String getName() throws PgpKeyNotFoundException {
        try {
            Object data = mDatabaseInteractor.getGenericData(mUri,
                    KeyRings.NAME,
                    DatabaseInteractor.FIELD_TYPE_STRING);
            return (String) data;
        } catch(DatabaseReadWriteInteractor.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    public String getEmail() throws PgpKeyNotFoundException {
        try {
            Object data = mDatabaseInteractor.getGenericData(mUri,
                    KeyRings.EMAIL,
                    DatabaseInteractor.FIELD_TYPE_STRING);
            return (String) data;
        } catch(DatabaseReadWriteInteractor.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }


    public String getComment() throws PgpKeyNotFoundException {
        try {
            Object data = mDatabaseInteractor.getGenericData(mUri,
                    KeyRings.COMMENT,
                    DatabaseInteractor.FIELD_TYPE_STRING);
            return (String) data;
        } catch(DatabaseReadWriteInteractor.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    @Override
    public boolean isRevoked() throws PgpKeyNotFoundException {
        try {
            Object data = mDatabaseInteractor.getGenericData(mUri,
                    KeychainContract.KeyRings.IS_REVOKED,
                    DatabaseInteractor.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(DatabaseReadWriteInteractor.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    @Override
    public boolean canCertify() throws PgpKeyNotFoundException {
        try {
            Object data = mDatabaseInteractor.getGenericData(mUri,
                    KeychainContract.KeyRings.HAS_CERTIFY,
                    DatabaseInteractor.FIELD_TYPE_NULL);
            return !((Boolean) data);
        } catch(DatabaseReadWriteInteractor.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    @Override
    public long getEncryptId() throws PgpKeyNotFoundException {
        try {
            Object data = mDatabaseInteractor.getGenericData(mUri,
                    KeyRings.HAS_ENCRYPT,
                    DatabaseInteractor.FIELD_TYPE_INTEGER);
            return (Long) data;
        } catch(DatabaseReadWriteInteractor.NotFoundException e) {
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
            Object data = mDatabaseInteractor.getGenericData(mUri,
                    KeyRings.HAS_SIGN,
                    DatabaseInteractor.FIELD_TYPE_INTEGER);
            return (Long) data;
        } catch(DatabaseReadWriteInteractor.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    @Override
    public int getVerified() throws PgpKeyNotFoundException {
        try {
            Object data = mDatabaseInteractor.getGenericData(mUri,
                    KeychainContract.KeyRings.VERIFIED,
                    DatabaseInteractor.FIELD_TYPE_INTEGER);
            return ((Long) data).intValue();
        } catch(DatabaseReadWriteInteractor.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    public boolean hasAnySecret() throws PgpKeyNotFoundException {
        try {
            Object data = mDatabaseInteractor.getGenericData(mUri,
                    KeychainContract.KeyRings.HAS_ANY_SECRET,
                    DatabaseInteractor.FIELD_TYPE_INTEGER);
            return (Long) data > 0;
        } catch(DatabaseReadWriteInteractor.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }

    private Cursor getSubkeys() throws PgpKeyNotFoundException {
        Uri keysUri = KeychainContract.Keys.buildKeysUri(extractOrGetMasterKeyId());
        return mDatabaseInteractor.getContentResolver().query(keysUri, null, null, null, null);
    }

    public SecretKeyType getSecretKeyType(long keyId) throws NotFoundException {
        Object data = mDatabaseInteractor.getGenericData(Keys.buildKeysUri(mUri),
                KeyRings.HAS_SECRET,
                DatabaseInteractor.FIELD_TYPE_INTEGER,
                KeyRings.KEY_ID + " = " + Long.toString(keyId));
        return SecretKeyType.fromNum(((Long) data).intValue());
    }

    public byte[] getEncoded() throws PgpKeyNotFoundException {
        try {
            return mDatabaseInteractor.loadPublicKeyRingData(getMasterKeyId());
        } catch(DatabaseReadWriteInteractor.NotFoundException e) {
            throw new PgpKeyNotFoundException(e);
        }
    }
}
