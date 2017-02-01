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


import java.util.ArrayList;
import java.util.Date;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.OpenPgpSignatureResult.SenderStatusResult;
import org.openintents.openpgp.util.OpenPgpUtils;
import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.util.Log;

/**
 * This class can be used to build OpenPgpSignatureResult objects based on several checks.
 * It serves as a constraint which information are returned inside an OpenPgpSignatureResult object.
 */
public class OpenPgpSignatureResultBuilder {
    // injected
    private final KeyRepository mKeyRepository;

    // OpenPgpSignatureResult
    private String mPrimaryUserId;
    private ArrayList<String> mUserIds = new ArrayList<>();
    private ArrayList<String> mConfirmedUserIds;
    private long mKeyId;
    private SenderStatusResult mSenderStatusResult;

    // builder
    private boolean mSignatureAvailable = false;
    private boolean mKnownKey = false;
    private boolean mValidSignature = false;
    private boolean mIsSignatureKeyCertified = false;
    private boolean mIsKeyRevoked = false;
    private boolean mIsKeyExpired = false;
    private boolean mInsecure = false;
    private String mSenderAddress;
    private Date mSignatureTimestamp;

    public OpenPgpSignatureResultBuilder(KeyRepository keyRepository) {
        this.mKeyRepository = keyRepository;
    }

    public void setPrimaryUserId(String userId) {
        this.mPrimaryUserId = userId;
    }

    public void setKeyId(long keyId) {
        this.mKeyId = keyId;
    }

    public void setSignatureTimestamp(Date signatureTimestamp) {
        mSignatureTimestamp = signatureTimestamp;
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

    public void setUserIds(ArrayList<String> userIds, ArrayList<String> confirmedUserIds) {
        this.mUserIds = userIds;
        this.mConfirmedUserIds = confirmedUserIds;
    }

    public boolean isInsecure() {
        return mInsecure;
    }

    public void initValid(CanonicalizedPublicKey signingKey) {
        setSignatureAvailable(true);
        setKnownKey(true);

        CanonicalizedKeyRing signingRing = signingKey.getKeyRing();

        // from RING
        setKeyId(signingRing.getMasterKeyId());
        try {
            setPrimaryUserId(signingRing.getPrimaryUserIdWithFallback());
        } catch (PgpKeyNotFoundException e) {
            Log.d(Constants.TAG, "No primary user id in keyring with master key id " + signingRing.getMasterKeyId());
        }
        setSignatureKeyCertified(signingRing.getVerified() > 0);

        ArrayList<String> allUserIds = signingRing.getUnorderedUserIds();
        ArrayList<String> confirmedUserIds;
        try {
            confirmedUserIds = mKeyRepository.getConfirmedUserIds(signingRing.getMasterKeyId());
        } catch (NotFoundException e) {
            throw new IllegalStateException("Key didn't exist anymore for user id query!", e);
        }
        setUserIds(allUserIds, confirmedUserIds);

        mSenderStatusResult = processSenderStatusResult(allUserIds, confirmedUserIds);

        // either master key is expired/revoked or this specific subkey is expired/revoked
        setKeyExpired(signingRing.isExpired() || signingKey.isExpired());
        setKeyRevoked(signingRing.isRevoked() || signingKey.isRevoked());
    }

    private SenderStatusResult processSenderStatusResult(
            ArrayList<String> allUserIds, ArrayList<String> confirmedUserIds) {
        if (mSenderAddress == null) {
            return SenderStatusResult.UNKNOWN;
        }

        if (userIdListContainsAddress(mSenderAddress, confirmedUserIds)) {
            return SenderStatusResult.USER_ID_CONFIRMED;
        } else if (userIdListContainsAddress(mSenderAddress, allUserIds)) {
            return SenderStatusResult.USER_ID_UNCONFIRMED;
        } else {
            return SenderStatusResult.USER_ID_MISSING;
        }
    }

    private static boolean userIdListContainsAddress(String senderAddress, ArrayList<String> confirmedUserIds) {
        for (String rawUserId : confirmedUserIds) {
            UserId userId = OpenPgpUtils.splitUserId(rawUserId);
            if (senderAddress.equalsIgnoreCase(userId.email)) {
                return true;
            }
        }
        return false;
    }

    public OpenPgpSignatureResult build() {
        if (!mSignatureAvailable) {
            Log.d(Constants.TAG, "RESULT_NO_SIGNATURE");
            return OpenPgpSignatureResult.createWithNoSignature();
        }

        if (!mKnownKey) {
            Log.d(Constants.TAG, "RESULT_KEY_MISSING");
            return OpenPgpSignatureResult.createWithKeyMissing(mKeyId, mSignatureTimestamp);
        }

        if (!mValidSignature) {
            Log.d(Constants.TAG, "RESULT_INVALID_SIGNATURE");
            return OpenPgpSignatureResult.createWithInvalidSignature();
        }

        int signatureStatus;
        if (mIsKeyRevoked) {
            Log.d(Constants.TAG, "RESULT_INVALID_KEY_REVOKED");
            signatureStatus = OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED;
        } else if (mIsKeyExpired) {
            Log.d(Constants.TAG, "RESULT_INVALID_KEY_EXPIRED");
            signatureStatus = OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED;
        } else if (mInsecure) {
            Log.d(Constants.TAG, "RESULT_INVALID_INSECURE");
            signatureStatus = OpenPgpSignatureResult.RESULT_INVALID_KEY_INSECURE;
        } else if (mIsSignatureKeyCertified) {
            Log.d(Constants.TAG, "RESULT_VALID_CONFIRMED");
            signatureStatus = OpenPgpSignatureResult.RESULT_VALID_KEY_CONFIRMED;
        } else {
            Log.d(Constants.TAG, "RESULT_VALID_UNCONFIRMED");
            signatureStatus = OpenPgpSignatureResult.RESULT_VALID_KEY_UNCONFIRMED;
        }

        return OpenPgpSignatureResult.createWithValidSignature(
                signatureStatus, mPrimaryUserId, mKeyId, mUserIds, mConfirmedUserIds, mSenderStatusResult, mSignatureTimestamp);
    }

    public void setSenderAddress(String senderAddress) {
        mSenderAddress = senderAddress;
    }

}
