/*
 * Copyright (C) 2015 Joey Castillo <joey@joeycastillo.com>
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

package org.sufficientlysecure.keychain.operations;

import android.content.Context;

import org.spongycastle.bcpg.PublicKeyAlgorithmTags;
import org.sufficientlysecure.keychain.operations.results.NfcKeyToCardResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

public class NfcKeyToCardOperation extends BaseOperation {
    public NfcKeyToCardOperation(Context context, ProviderHelper providerHelper, Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    public NfcKeyToCardResult execute(long subKeyId) {
        OperationResult.OperationLog log = new OperationResult.OperationLog();
        int indent = 0;
        long masterKeyId;

        try {
            // fetch the indicated master key id
            masterKeyId = mProviderHelper.getMasterKeyId(subKeyId);
            CanonicalizedSecretKeyRing keyRing =
                    mProviderHelper.getCanonicalizedSecretKeyRing(masterKeyId);

            log.add(OperationResult.LogType.MSG_KC_SECRET, indent,
                    KeyFormattingUtils.convertKeyIdToHex(masterKeyId));

            // fetch the specific subkey
            CanonicalizedSecretKey subKey = keyRing.getSecretKey(subKeyId);

            // Key algorithm must be RSA
            int algorithm = subKey.getAlgorithm();
            if (algorithm != PublicKeyAlgorithmTags.RSA_ENCRYPT &&
                algorithm != PublicKeyAlgorithmTags.RSA_SIGN &&
                algorithm != PublicKeyAlgorithmTags.RSA_GENERAL) {
                log.add(OperationResult.LogType.MSG_K2C_ERROR_BAD_ALGO, indent + 1);
                return new NfcKeyToCardResult(NfcKeyToCardResult.RESULT_ERROR, log);
            }

            // Key size must be 2048
            int keySize = subKey.getBitStrength();
            if (keySize != 2048) {
                log.add(OperationResult.LogType.MSG_K2C_ERROR_BAD_SIZE, indent + 1);
                return new NfcKeyToCardResult(NfcKeyToCardResult.RESULT_ERROR, log);
            }

            // Secret key parts must be available
            CanonicalizedSecretKey.SecretKeyType type = subKey.getSecretKeyType();
            if (type == CanonicalizedSecretKey.SecretKeyType.DIVERT_TO_CARD ||
                type == CanonicalizedSecretKey.SecretKeyType.GNU_DUMMY) {
                log.add(OperationResult.LogType.MSG_K2C_ERROR_BAD_STRIPPED, indent + 1);
                return new NfcKeyToCardResult(NfcKeyToCardResult.RESULT_ERROR, log);
            }

            if (type == CanonicalizedSecretKey.SecretKeyType.PIN ||
                type == CanonicalizedSecretKey.SecretKeyType.PATTERN ||
                type == CanonicalizedSecretKey.SecretKeyType.PASSPHRASE ||
                type == CanonicalizedSecretKey.SecretKeyType.PASSPHRASE_EMPTY) {
                log.add(OperationResult.LogType.MSG_PSE_PENDING_NFC, indent);
                return new NfcKeyToCardResult(log, RequiredInputParcel
                        .createNfcKeyToCardOperation(masterKeyId, subKeyId));
            }

            throw new AssertionError("Unhandled SecretKeyType! (should not happen)");
        } catch (ProviderHelper.NotFoundException e) {
            log.add(OperationResult.LogType.MSG_PSE_ERROR_UNLOCK, indent);
            return new NfcKeyToCardResult(NfcKeyToCardResult.RESULT_ERROR, log);
        }
    }
}
