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

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpUtils;
import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper.NotFoundException;
import org.sufficientlysecure.keychain.util.Log;

/**
 * This class can be used to build OpenPgpSignatureResult objects based on several checks.
 * It serves as a constraint which information are returned inside an OpenPgpSignatureResult object.
 */
public class OpenPgpSignatureResultBuilder {
    // injected
    private final ProviderHelper providerHelper;

    // OpenPgpSignatureResult
    private String primaryUserId;
    private ArrayList<String> userIds = new ArrayList<>();
    private ArrayList<String> confirmedUserIds;
    private long keyId;
    private String senderAddress;
    private int senderStatus;
    private byte[] signedMessageDigest;

    // builder
    private boolean signatureAvailable = false;
    private boolean knownKey = false;
    private boolean validSignature = false;
    private boolean isSignatureKeyCertified = false;
    private boolean isKeyRevoked = false;
    private boolean isKeyExpired = false;
    private boolean isInsecure = false;

    public OpenPgpSignatureResultBuilder(ProviderHelper providerHelper) {
        this.providerHelper = providerHelper;
    }

    public void setPrimaryUserId(String userId) {
        this.primaryUserId = userId;
    }

    public void setKeyId(long keyId) {
        this.keyId = keyId;
    }

    public void setKnownKey(boolean knownKey) {
        this.knownKey = knownKey;
    }

    public void setValidSignature(boolean validSignature) {
        this.validSignature = validSignature;
    }

    public void setInsecure(boolean insecure) {
        this.isInsecure = insecure;
    }

    public void setSignatureKeyCertified(boolean isSignatureKeyCertified) {
        this.isSignatureKeyCertified = isSignatureKeyCertified;
    }

    public void setSignatureAvailable(boolean signatureAvailable) {
        this.signatureAvailable = signatureAvailable;
    }

    public void setKeyRevoked(boolean keyRevoked) {
        this.isKeyRevoked = keyRevoked;
    }

    public void setKeyExpired(boolean keyExpired) {
        this.isKeyExpired = keyExpired;
    }

    public void setUserIds(ArrayList<String> userIds, ArrayList<String> confirmedUserIds) {
        this.userIds = userIds;
        this.confirmedUserIds = confirmedUserIds;
    }

    public boolean isInsecure() {
        return isInsecure;
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

        try {
            ArrayList<String> allUserIds = signingRing.getUnorderedUserIds();
            ArrayList<String> confirmedUserIds = providerHelper.getConfirmedUserIds(signingRing.getMasterKeyId());
            setUserIds(allUserIds, confirmedUserIds);

            if (senderAddress != null) {
                if (userIdListContainsAddress(senderAddress, confirmedUserIds)) {
                    setSenderStatus(OpenPgpSignatureResult.SENDER_RESULT_UID_CONFIRMED);
                } else if (allUserIds.contains(senderAddress)) {
                    setSenderStatus(OpenPgpSignatureResult.SENDER_RESULT_UID_UNCONFIRMED);
                } else {
                    setSenderStatus(OpenPgpSignatureResult.SENDER_RESULT_UID_MISSING);
                }
            } else {
                setSenderStatus(OpenPgpSignatureResult.SENDER_RESULT_NO_SENDER);
            }

        } catch (NotFoundException e) {
            throw new IllegalStateException("Key didn't exist anymore for user id query!", e);
        }

        // either master key is expired/revoked or this specific subkey is expired/revoked
        setKeyExpired(signingRing.isExpired() || signingKey.isExpired());
        setKeyRevoked(signingRing.isRevoked() || signingKey.isRevoked());
    }

    private static boolean userIdListContainsAddress(String senderAddress, ArrayList<String> confirmedUserIds) {
        for (String rawUserId : confirmedUserIds) {
            UserId userId = OpenPgpUtils.splitUserId(rawUserId);
            if (senderAddress.equals(userId.email)) {
                return true;
            }
        }
        return false;
    }

    public OpenPgpSignatureResult build() {
        if (!signatureAvailable) {
            Log.d(Constants.TAG, "RESULT_NO_SIGNATURE");
            return OpenPgpSignatureResult.createWithNoSignature();
        }

        if (!knownKey) {
            Log.d(Constants.TAG, "RESULT_KEY_MISSING");
            return OpenPgpSignatureResult.createWithKeyMissing(keyId);
        }

        if (!validSignature) {
            Log.d(Constants.TAG, "RESULT_INVALID_SIGNATURE");
            return OpenPgpSignatureResult.createWithInvalidSignature();
        }

        int signatureStatus;
        if (isKeyRevoked) {
            Log.d(Constants.TAG, "RESULT_INVALID_KEY_REVOKED");
            signatureStatus = OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED;
        } else if (isKeyExpired) {
            Log.d(Constants.TAG, "RESULT_INVALID_KEY_EXPIRED");
            signatureStatus = OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED;
        } else if (isInsecure) {
            Log.d(Constants.TAG, "RESULT_INVALID_INSECURE");
            signatureStatus = OpenPgpSignatureResult.RESULT_INVALID_KEY_INSECURE;
        } else if (isSignatureKeyCertified) {
            Log.d(Constants.TAG, "RESULT_VALID_CONFIRMED");
            signatureStatus = OpenPgpSignatureResult.RESULT_VALID_KEY_CONFIRMED;
        } else {
            Log.d(Constants.TAG, "RESULT_VALID_UNCONFIRMED");
            signatureStatus = OpenPgpSignatureResult.RESULT_VALID_KEY_UNCONFIRMED;
        }

        return OpenPgpSignatureResult.createWithValidSignature(signatureStatus,
                primaryUserId, keyId, userIds, confirmedUserIds, senderStatus, signedMessageDigest);
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public void setSenderStatus(int senderStatus) {
        this.senderStatus = senderStatus;
    }

    public void setSignedMessageDigest(byte[] signedMessageDigest) {
        this.signedMessageDigest = signedMessageDigest;
    }
}
