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
import org.sufficientlysecure.keychain.util.Log;

/**
 * This class can be used to build OpenPgpSignatureResult objects based on several checks.
 * It serves as a constraint which information are returned inside an OpenPgpSignatureResult object.
 */
public class OpenPgpSignatureResultBuilder {
    // OpenPgpSignatureResult
    private int mStatus = OpenPgpSignatureResult.SIGNATURE_ERROR;
    private boolean mSignatureOnly = false;
    private String mUserId;
    private long mKeyId;

    // builder
    private boolean mSignatureAvailable = false;
    private boolean mKnownKey = false;
    private boolean mValidSignature = false;
    private boolean mValidKeyBinding = false;
    private boolean mIsSignatureKeyCertified = false;

    public void status(int status) {
        this.mStatus = status;
    }

    public void signatureOnly(boolean signatureOnly) {
        this.mSignatureOnly = signatureOnly;
    }

    public void userId(String userId) {
        this.mUserId = userId;
    }

    public void keyId(long keyId) {
        this.mKeyId = keyId;
    }

    public void knownKey(boolean knownKey) {
        this.mKnownKey = knownKey;
    }

    public void validSignature(boolean validSignature) {
        this.mValidSignature = validSignature;
    }

    public void validKeyBinding(boolean validKeyBinding) {
        this.mValidKeyBinding = validKeyBinding;
    }

    public void signatureKeyCertified(boolean isSignatureKeyCertified) {
        this.mIsSignatureKeyCertified = isSignatureKeyCertified;
    }

    public void signatureAvailable(boolean signatureAvailable) {
        this.mSignatureAvailable = signatureAvailable;
    }

    public OpenPgpSignatureResult build() {
        if (mSignatureAvailable) {
            OpenPgpSignatureResult result = new OpenPgpSignatureResult();
            result.setSignatureOnly(mSignatureOnly);

            if (mValidKeyBinding && mValidSignature) {
                // valid sig!
                if (mKnownKey) {
                    result.setKeyId(mKeyId);
                    result.setUserId(mUserId);

                    if (mIsSignatureKeyCertified) {
                        Log.d(Constants.TAG, "SIGNATURE_SUCCESS_CERTIFIED");
                        result.setStatus(OpenPgpSignatureResult.SIGNATURE_SUCCESS_CERTIFIED);
                    } else {
                        Log.d(Constants.TAG, "SIGNATURE_SUCCESS_UNCERTIFIED");
                        result.setStatus(OpenPgpSignatureResult.SIGNATURE_SUCCESS_UNCERTIFIED);
                    }
                } else {
                    result.setKeyId(mKeyId);

                    Log.d(Constants.TAG, "SIGNATURE_UNKNOWN_PUB_KEY");
                    result.setStatus(OpenPgpSignatureResult.SIGNATURE_UNKNOWN_PUB_KEY);
                }
            } else {
                Log.d(Constants.TAG, "Error!\nvalidKeyBinding: " + mValidKeyBinding
                        + "\nvalidSignature: " + mValidSignature);
                result.setStatus(OpenPgpSignatureResult.SIGNATURE_ERROR);
            }

            return result;
        } else {
            Log.d(Constants.TAG, "no signature found!");

            return null;
        }
    }


}
