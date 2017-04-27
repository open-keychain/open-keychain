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

class OpenPgpDecryptionResultBuilder {

    // builder
    private boolean isInsecure = false;
    private boolean isEncrypted = false;
    private byte[] sessionKey;
    private byte[] decryptedSessionKey;

    public void setInsecure(boolean insecure) {
        this.isInsecure = insecure;
    }

    public void setEncrypted(boolean encrypted) {
        this.isEncrypted = encrypted;
    }

    public OpenPgpDecryptionResult build() {
        if (isInsecure) {
            Log.d(Constants.TAG, "RESULT_INSECURE");
            return new OpenPgpDecryptionResult(OpenPgpDecryptionResult.RESULT_INSECURE, sessionKey, decryptedSessionKey);
        }

        if (isEncrypted) {
            Log.d(Constants.TAG, "RESULT_ENCRYPTED");
            return new OpenPgpDecryptionResult(OpenPgpDecryptionResult.RESULT_ENCRYPTED, sessionKey, decryptedSessionKey);
        }

        Log.d(Constants.TAG, "RESULT_NOT_ENCRYPTED");
        return new OpenPgpDecryptionResult(OpenPgpDecryptionResult.RESULT_NOT_ENCRYPTED);
    }


    void setSessionKey(byte[] sessionKey, byte[] decryptedSessionKey) {
        if ((sessionKey == null) != (decryptedSessionKey == null)) {
            throw new AssertionError("sessionKey must be null iff decryptedSessionKey is null!");
        }
        this.sessionKey = sessionKey;
        this.decryptedSessionKey = decryptedSessionKey;
    }
}
