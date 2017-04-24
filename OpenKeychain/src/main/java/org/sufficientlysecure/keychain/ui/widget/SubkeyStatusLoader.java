package org.sufficientlysecure.keychain.ui.widget;


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
import org.sufficientlysecure.keychain.ui.widget.SubkeyStatusLoader.KeySubkeyStatus;


class SubkeyStatusLoader extends AsyncTaskLoader<KeySubkeyStatus> {
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


    SubkeyStatusLoader(Context context, ContentResolver contentResolver, long masterKeyId, Comparator<SubKeyItem> comparator) {
        super(context);

        this.contentResolver = contentResolver;
        this.masterKeyId = masterKeyId;
        this.comparator = comparator;
    }

    @Override
    public KeySubkeyStatus loadInBackground() {
        Cursor cursor = contentResolver.query(Keys.buildKeysUri(masterKeyId), PROJECTION, null, null, null);
        if (cursor == null) {
            Log.e(Constants.TAG, "Error loading key items!");
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

    static class KeySubkeyStatus {
        @NonNull
        final SubKeyItem keyCertify;
        final List<SubKeyItem> keysSign;
        final List<SubKeyItem> keysEncrypt;

        KeySubkeyStatus(@NonNull SubKeyItem keyCertify, List<SubKeyItem> keysSign, List<SubKeyItem> keysEncrypt) {
            this.keyCertify = keyCertify;
            this.keysSign = keysSign;
            this.keysEncrypt = keysEncrypt;
        }
    }

    static class SubKeyItem {
        final int mPosition;
        final long mKeyId;
        final Date mCreation;
        final SecretKeyType mSecretKeyType;
        final boolean mIsRevoked, mIsExpired;
        final Date mExpiry;
        final boolean mCanCertify, mCanSign, mCanEncrypt;
        final KeySecurityProblem mSecurityProblem;

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

        boolean newerThan(SubKeyItem other) {
            return mCreation.after(other.mCreation);
        }

        boolean isValid() {
            return !mIsRevoked && !mIsExpired;
        }
    }
}
