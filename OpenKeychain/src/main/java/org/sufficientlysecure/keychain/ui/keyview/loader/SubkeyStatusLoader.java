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

package org.sufficientlysecure.keychain.ui.keyview.loader;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.PgpSecurityConstants;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.KeySecurityProblem;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusLoader.KeySubkeyStatus;
import timber.log.Timber;


public class SubkeyStatusLoader extends AsyncTaskLoader<KeySubkeyStatus> {
    public static final String[] PROJECTION = new String[] {
            Keys.KEY_ID,
            Keys.CREATION,
            Keys.CAN_CERTIFY,
            Keys.CAN_SIGN,
            Keys.CAN_ENCRYPT,
            Keys.HAS_SECRET,
            Keys.EXPIRY,
            Keys.IS_REVOKED,
            Keys.ALGORITHM,
            Keys.KEY_SIZE,
            Keys.KEY_CURVE_OID
    };
    private static final int INDEX_KEY_ID = 0;
    private static final int INDEX_CREATION = 1;
    private static final int INDEX_CAN_CERTIFY = 2;
    private static final int INDEX_CAN_SIGN = 3;
    private static final int INDEX_CAN_ENCRYPT = 4;
    private static final int INDEX_HAS_SECRET = 5;
    private static final int INDEX_EXPIRY = 6;
    private static final int INDEX_IS_REVOKED = 7;
    private static final int INDEX_ALGORITHM = 8;
    private static final int INDEX_KEY_SIZE = 9;
    private static final int INDEX_KEY_CURVE_OID = 10;


    private final ContentResolver contentResolver;
    private final long masterKeyId;
    private final Comparator<SubKeyItem> comparator;

    private KeySubkeyStatus cachedResult;


    public SubkeyStatusLoader(Context context, ContentResolver contentResolver, long masterKeyId,
            Comparator<SubKeyItem> comparator) {
        super(context);

        this.contentResolver = contentResolver;
        this.masterKeyId = masterKeyId;
        this.comparator = comparator;
    }

    @Override
    public KeySubkeyStatus loadInBackground() {
        Cursor cursor = contentResolver.query(Keys.buildKeysUri(masterKeyId), PROJECTION, null, null, null);
        if (cursor == null) {
            Timber.e("Error loading key items!");
            return null;
        }

        try {
            SubKeyItem keyCertify = null;
            ArrayList<SubKeyItem> keysSign = new ArrayList<>();
            ArrayList<SubKeyItem> keysEncrypt = new ArrayList<>();
            while (cursor.moveToNext()) {
                SubKeyItem ski = new SubKeyItem(masterKeyId, cursor);

                if (ski.mKeyId == masterKeyId) {
                    keyCertify = ski;
                }

                if (ski.mCanSign) {
                    keysSign.add(ski);
                }
                if (ski.mCanEncrypt) {
                    keysEncrypt.add(ski);
                }
            }

            if (keyCertify == null) {
                throw new IllegalStateException("Certification key must be set at this point, it's a bug otherwise!");
            }

            Collections.sort(keysSign, comparator);
            Collections.sort(keysEncrypt, comparator);

            return new KeySubkeyStatus(keyCertify, keysSign, keysEncrypt);
        } finally {
            cursor.close();
        }
    }

    @Override
    public void deliverResult(KeySubkeyStatus keySubkeyStatus) {
        cachedResult = keySubkeyStatus;

        if (isStarted()) {
            super.deliverResult(keySubkeyStatus);
        }
    }

    @Override
    protected void onStartLoading() {
        if (cachedResult != null) {
            deliverResult(cachedResult);
        }

        if (takeContentChanged() || cachedResult == null) {
            forceLoad();
        }
    }

    public static class KeySubkeyStatus {
        @NonNull
        public final SubKeyItem keyCertify;
        public final List<SubKeyItem> keysSign;
        public final List<SubKeyItem> keysEncrypt;

        KeySubkeyStatus(@NonNull SubKeyItem keyCertify, List<SubKeyItem> keysSign, List<SubKeyItem> keysEncrypt) {
            this.keyCertify = keyCertify;
            this.keysSign = keysSign;
            this.keysEncrypt = keysEncrypt;
        }
    }

    public static class SubKeyItem {
        final int mPosition;
        final long mKeyId;
        final Date mCreation;
        public final SecretKeyType mSecretKeyType;
        public final boolean mIsRevoked, mIsExpired;
        public final Date mExpiry;
        final boolean mCanCertify, mCanSign, mCanEncrypt;
        public final KeySecurityProblem mSecurityProblem;

        SubKeyItem(long masterKeyId, Cursor cursor) {
            mPosition = cursor.getPosition();

            mKeyId = cursor.getLong(INDEX_KEY_ID);
            mCreation = new Date(cursor.getLong(INDEX_CREATION) * 1000);

            mSecretKeyType = SecretKeyType.fromNum(cursor.getInt(INDEX_HAS_SECRET));

            mIsRevoked = cursor.getInt(INDEX_IS_REVOKED) > 0;
            mExpiry = cursor.isNull(INDEX_EXPIRY) ? null : new Date(cursor.getLong(INDEX_EXPIRY) * 1000);
            mIsExpired = mExpiry != null && mExpiry.before(new Date());

            mCanCertify = cursor.getInt(INDEX_CAN_CERTIFY) > 0;
            mCanSign = cursor.getInt(INDEX_CAN_SIGN) > 0;
            mCanEncrypt = cursor.getInt(INDEX_CAN_ENCRYPT) > 0;

            int algorithm = cursor.getInt(INDEX_ALGORITHM);
            Integer bitStrength = cursor.isNull(INDEX_KEY_SIZE) ? null : cursor.getInt(INDEX_KEY_SIZE);
            String curveOid = cursor.getString(INDEX_KEY_CURVE_OID);

            mSecurityProblem = PgpSecurityConstants.getKeySecurityProblem(
                    masterKeyId, mKeyId, algorithm, bitStrength, curveOid);
        }

        public boolean newerThan(SubKeyItem other) {
            return mCreation.after(other.mCreation);
        }

        public boolean isValid() {
            return !mIsRevoked && !mIsExpired;
        }
    }
}
