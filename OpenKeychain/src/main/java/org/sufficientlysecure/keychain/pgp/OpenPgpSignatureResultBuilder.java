/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;

/**
 * This class can be used to build OpenPgpSignatureResult objects based on several checks.
 * It serves as a constraint which information are returned inside an OpenPgpSignatureResult object.
 */
public class OpenPgpSignatureResultBuilder {
    // OpenPgpSignatureResult
    private String mPrimaryUserId;
    private ArrayList<String> mUserIds = new ArrayList<>();
    private long mKeyId;

    // builder
    private boolean mSignatureAvailable = false;
    private boolean mKnownKey = false;
    private boolean mValidSignature = false;
    private boolean mIsSignatureKeyCertified = false;
    private boolean mIsKeyRevoked = false;
    private boolean mIsKeyExpired = false;
    private boolean mInsecure = false;

    public void setPrimaryUserId(String userId) {
        this.mPrimaryUserId = userId;
    }

    public void setKeyId(long keyId) {
        this.mKeyId = keyId;
    }

    public void setKnownKey(boolean knownKey) {
        this.mKnownKey = knownKey;
    }

    public void setValidSignature(boolean validSignature) {
        this.mValidSignature = validSignature;
    }

    public void setInsecure(boolean insecure) {
        this.mInsecure = insecure;
    }

    public void setSignatureKeyCertified(boolean isSignatureKeyCertified) {
        this.mIsSignatureKeyCertified = isSignatureKeyCertified;
    }

    public void setSignatureAvailable(boolean signatureAvailable) {
        this.mSignatureAvailable = signatureAvailable;
    }

    public void setKeyRevoked(boolean keyRevoked) {
        this.mIsKeyRevoked = keyRevoked;
    }

    public void setKeyExpired(boolean keyExpired) {
        this.mIsKeyExpired = keyExpired;
    }

    public void setUserIds(ArrayList<String> userIds) {
        this.mUserIds = userIds;
    }

    public boolean isValidSignature() {
        return mValidSignature;
    }

    public boolean isInsecure() {
        return mInsecure;
    }

    public void initValid(CanonicalizedPublicKeyRing signingRing,
                          CanonicalizedPublicKey signingKey) {
        setSignatureAvailable(true);
        setKnownKey(true);

        // from RING
        setKeyId(signingRing.getMasterKeyId());
        try {
            setPrimaryUserId(signingRing.getPrimaryUserIdWithFallback());
        } catch (PgpKeyNotFoundException e) {
            Log.d(Constants.TAG, "No primary user id in keyring with master key id " + signingRing.getMasterKeyId());
        }
        setSignatureKeyCertified(signingRing.getVerified() > 0);
        Log.d(Constants.TAG, "signingRing.getUnorderedUserIds(): " + signingRing.getUnorderedUserIds());
        setUserIds(signingRing.getUnorderedUserIds());

        // either master key is expired/revoked or this specific subkey is expired/revoked
        setKeyExpired(signingRing.isExpired() || signingKey.isExpired());
        setKeyRevoked(signingRing.isRevoked() || signingKey.isRevoked());
    }

    public OpenPgpSignatureResult build() {
        OpenPgpSignatureResult result = new OpenPgpSignatureResult();

        if (!mSignatureAvailable) {
            Log.d(Constants.TAG, "RESULT_NO_SIGNATURE");
            result.setResult(OpenPgpSignatureResult.RESULT_NO_SIGNATURE);
            return result;
        }

        if (!mKnownKey) {
            result.setKeyId(mKeyId);

            Log.d(Constants.TAG, "RESULT_KEY_MISSING");
            result.setResult(OpenPgpSignatureResult.RESULT_KEY_MISSING);
            return result;
        }

        if (!mValidSignature) {
            Log.d(Constants.TAG, "RESULT_INVALID_SIGNATURE");
            result.setResult(OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE);
            return result;
        }

        result.setKeyId(mKeyId);
        result.setPrimaryUserId(mPrimaryUserId);
        result.setUserIds(mUserIds);

        if (mIsKeyRevoked) {
            Log.d(Constants.TAG, "RESULT_INVALID_KEY_REVOKED");
            result.setResult(OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED);
        } else if (mIsKeyExpired) {
            Log.d(Constants.TAG, "RESULT_INVALID_KEY_EXPIRED");
            result.setResult(OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED);
        } else if (mInsecure) {
            Log.d(Constants.TAG, "RESULT_INVALID_INSECURE");
            result.setResult(OpenPgpSignatureResult.RESULT_INVALID_INSECURE);
        } else if (mIsSignatureKeyCertified) {
            Log.d(Constants.TAG, "RESULT_VALID_CONFIRMED");
            result.setResult(OpenPgpSignatureResult.RESULT_VALID_CONFIRMED);
        } else {
            Log.d(Constants.TAG, "RESULT_VALID_UNCONFIRMED");
            result.setResult(OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED);
        }

        return result;
    }


}
