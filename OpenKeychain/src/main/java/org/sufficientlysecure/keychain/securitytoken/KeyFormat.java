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

package org.sufficientlysecure.keychain.securitytoken;

import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.CreateSecurityTokenAlgorithmFragment;

public abstract class KeyFormat {

    public static KeyFormat fromBytes(byte[] bytes) {
        switch (bytes[0]) {
            case PublicKeyAlgorithmTags.RSA_GENERAL:
                return RsaKeyFormat.getInstanceFromBytes(bytes);
            case PublicKeyAlgorithmTags.ECDH:
            case PublicKeyAlgorithmTags.ECDSA:
            case PublicKeyAlgorithmTags.EDDSA:
                return EcKeyFormat.getInstanceFromBytes(bytes);
            default:
                throw new IllegalArgumentException("Unsupported Algorithm id " + bytes[0]);
        }
    }

    public abstract byte[] toBytes(KeyType slot);

    public static KeyFormat fromCreationKeyType(CreateSecurityTokenAlgorithmFragment.SupportedKeyType t, boolean forEncryption) {
        final int elen = 17; //65537
        final int algorithmId = forEncryption ? PublicKeyAlgorithmTags.ECDH : PublicKeyAlgorithmTags.ECDSA;

        switch (t) {
            case RSA_2048:
                return RsaKeyFormat.getInstance(2048, elen, RsaKeyFormat.RsaImportFormat.CRT_WITH_MODULUS);
            case RSA_3072:
                return RsaKeyFormat.getInstance(3072, elen, RsaKeyFormat.RsaImportFormat.CRT_WITH_MODULUS);
            case RSA_4096:
                return RsaKeyFormat.getInstance(4096, elen, RsaKeyFormat.RsaImportFormat.CRT_WITH_MODULUS);
            case ECC_P256:
                return EcKeyFormat.getInstance(algorithmId, EcObjectIdentifiers.NIST_P_256, true);
            case ECC_P384:
                return EcKeyFormat.getInstance(algorithmId, EcObjectIdentifiers.NIST_P_384, true);
            case ECC_P521:
                return EcKeyFormat.getInstance(algorithmId, EcObjectIdentifiers.NIST_P_521, true);
        }

        throw new IllegalArgumentException("Unsupported Algorithm id " + t);
    }

    public abstract void addToSaveKeyringParcel(SaveKeyringParcel.Builder builder, int keyFlags);

}
