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
    private boolean mSignatureOnly = false;
    private String mUserId;
    private long mKeyId;

    // builder
    private boolean mSignatureAvailable = false;
    private boolean mKnownKey = false;
    private boolean mValidSignature = false;
    private boolean mIsSignatureKeyCertified = false;

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

    public void signatureKeyCertified(boolean isSignatureKeyCertified) {
        this.mIsSignatureKeyCertified = isSignatureKeyCertified;
    }

    public void signatureAvailable(boolean signatureAvailable) {
        this.mSignatureAvailable = signatureAvailable;
    }

    public boolean isValidSignature() {
        return mValidSignature;
    }

    public OpenPgpSignatureResult build() {
        if (mSignatureAvailable) {
            OpenPgpSignatureResult result = new OpenPgpSignatureResult();
            result.setSignatureOnly(mSignatureOnly);

            // valid sig!
            if (mKnownKey) {
                if (mValidSignature) {
                    result.setKeyId(mKeyId);
                    result.setPrimaryUserId(mUserId);

                    if (mIsSignatureKeyCertified) {
                        Log.d(Constants.TAG, "SIGNATURE_SUCCESS_CERTIFIED");
                        result.setStatus(OpenPgpSignatureResult.SIGNATURE_SUCCESS_CERTIFIED);
                    } else {
                        Log.d(Constants.TAG, "SIGNATURE_SUCCESS_UNCERTIFIED");
                        result.setStatus(OpenPgpSignatureResult.SIGNATURE_SUCCESS_UNCERTIFIED);
                    }
                } else {
                    Log.d(Constants.TAG, "Error! Invalid signature.");
                    result.setStatus(OpenPgpSignatureResult.SIGNATURE_ERROR);
                }
            } else {
                result.setKeyId(mKeyId);

                Log.d(Constants.TAG, "SIGNATURE_KEY_MISSING");
                result.setStatus(OpenPgpSignatureResult.SIGNATURE_KEY_MISSING);
            }

            return result;
        } else {
            Log.d(Constants.TAG, "no signature found!");

            return null;
        }
    }


}
