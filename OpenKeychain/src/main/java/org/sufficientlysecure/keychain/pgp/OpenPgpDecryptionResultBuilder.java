/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

public class OpenPgpDecryptionResultBuilder {

    // builder
    private boolean mInsecure = false;
    private boolean mEncrypted = false;
    private byte[] sessionKey;
    private byte[] decryptedSessionKey;

    public void setInsecure(boolean insecure) {
        this.mInsecure = insecure;
    }

    public void setEncrypted(boolean encrypted) {
        this.mEncrypted = encrypted;
    }

    public OpenPgpDecryptionResult build() {
        if (mInsecure) {
            Log.d(Constants.TAG, "RESULT_INSECURE");
            return new OpenPgpDecryptionResult(OpenPgpDecryptionResult.RESULT_INSECURE, sessionKey, decryptedSessionKey);
        }

        if (mEncrypted) {
            Log.d(Constants.TAG, "RESULT_ENCRYPTED");
            return new OpenPgpDecryptionResult(OpenPgpDecryptionResult.RESULT_ENCRYPTED, sessionKey, decryptedSessionKey);
        }

        Log.d(Constants.TAG, "RESULT_NOT_ENCRYPTED");
        return new OpenPgpDecryptionResult(OpenPgpDecryptionResult.RESULT_NOT_ENCRYPTED);
    }


    public void setSessionKey(byte[] sessionKey, byte[] decryptedSessionKey) {
        if ((sessionKey == null) != (decryptedSessionKey == null)) {
            throw new AssertionError("sessionKey must be null iff decryptedSessionKey is null!");
        }
        this.sessionKey = sessionKey;
        this.decryptedSessionKey = decryptedSessionKey;
    }
}
