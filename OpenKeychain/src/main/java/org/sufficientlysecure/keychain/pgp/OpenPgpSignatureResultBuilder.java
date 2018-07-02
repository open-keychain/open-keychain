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

package org.sufficientlysecure.keychain.pgp;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.OpenPgpSignatureResult.SenderStatusResult;
import org.openintents.openpgp.util.OpenPgpUtils;
import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import timber.log.Timber;


/**
 * This class can be used to build OpenPgpSignatureResult objects based on several checks.
 * It serves as a constraint which information are returned inside an OpenPgpSignatureResult object.
 */
public class OpenPgpSignatureResultBuilder {
    // injected
    private final KeyRepository mKeyRepository;

    // OpenPgpSignatureResult
    private String mPrimaryUserId;
    private List<String> mUserIds = new ArrayList<>();
    private List<String> mConfirmedUserIds;
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

    public void setUserIds(List<String> userIds, List<String> confirmedUserIds) {
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
        setPrimaryUserId(signingRing.getPrimaryUserIdWithFallback());
        setSignatureKeyCertified(signingRing.getVerified() == VerificationStatus.VERIFIED_SECRET);

        List<String> allUserIds = signingRing.getUnorderedUserIds();
        List<String> confirmedUserIds = mKeyRepository.getConfirmedUserIds(signingRing.getMasterKeyId());
        setUserIds(allUserIds, confirmedUserIds);

        mSenderStatusResult = processSenderStatusResult(allUserIds, confirmedUserIds);

        // either master key is expired/revoked or this specific subkey is expired/revoked
        setKeyExpired(signingRing.isExpired() || signingKey.isExpired());
        setKeyRevoked(signingRing.isRevoked() || signingKey.isRevoked());
    }

    private SenderStatusResult processSenderStatusResult(
            List<String> allUserIds, List<String> confirmedUserIds) {
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

    private static boolean userIdListContainsAddress(String senderAddress, List<String> confirmedUserIds) {
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
            Timber.d("RESULT_NO_SIGNATURE");
            return OpenPgpSignatureResult.createWithNoSignature();
        }

        if (!mKnownKey) {
            Timber.d("RESULT_KEY_MISSING");
            return OpenPgpSignatureResult.createWithKeyMissing(mKeyId, mSignatureTimestamp);
        }

        if (!mValidSignature) {
            Timber.d("RESULT_INVALID_SIGNATURE");
            return OpenPgpSignatureResult.createWithInvalidSignature();
        }

        int signatureStatus;
        if (mIsKeyRevoked) {
            Timber.d("RESULT_INVALID_KEY_REVOKED");
            signatureStatus = OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED;
        } else if (mIsKeyExpired) {
            Timber.d("RESULT_INVALID_KEY_EXPIRED");
            signatureStatus = OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED;
        } else if (mInsecure) {
            Timber.d("RESULT_INVALID_INSECURE");
            signatureStatus = OpenPgpSignatureResult.RESULT_INVALID_KEY_INSECURE;
        } else if (mIsSignatureKeyCertified) {
            Timber.d("RESULT_VALID_CONFIRMED");
            signatureStatus = OpenPgpSignatureResult.RESULT_VALID_KEY_CONFIRMED;
        } else {
            Timber.d("RESULT_VALID_UNCONFIRMED");
            signatureStatus = OpenPgpSignatureResult.RESULT_VALID_KEY_UNCONFIRMED;
        }

        return OpenPgpSignatureResult.createWithValidSignature(
                signatureStatus, mPrimaryUserId, mKeyId, mUserIds, mConfirmedUserIds, mSenderStatusResult, mSignatureTimestamp);
    }

    public void setSenderAddress(String senderAddress) {
        mSenderAddress = senderAddress;
    }

}
