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

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.CreateSecurityTokenAlgorithmFragment;

public abstract class KeyFormat {

    public enum KeyFormatType {
        RSAKeyFormatType,
        ECKeyFormatType
    }

    private final KeyFormatType mKeyFormatType;

    KeyFormat(final KeyFormatType keyFormatType) {
        mKeyFormatType = keyFormatType;
    }

    public final KeyFormatType keyFormatType() {
        return mKeyFormatType;
    }

    public static KeyFormat fromBytes(byte[] bytes) {
        switch (bytes[0]) {
            case PublicKeyAlgorithmTags.RSA_GENERAL:
                if (bytes.length < 6) {
                    throw new IllegalArgumentException("Bad length for RSA attributes");
                }
                return new RSAKeyFormat(bytes[1] << 8 | bytes[2],
                                        bytes[3] << 8 | bytes[4],
                                        RSAKeyFormat.RSAAlgorithmFormat.from(bytes[5]));

            case PublicKeyAlgorithmTags.ECDH:
            case PublicKeyAlgorithmTags.ECDSA:
                if (bytes.length < 2) {
                    throw new IllegalArgumentException("Bad length for RSA attributes");
                }
                int len = bytes.length - 1;
                if (bytes[bytes.length - 1] == (byte)0xff) {
                    len -= 1;
                }
                final byte[] boid = new byte[2 + len];
                boid[0] = (byte)0x06;
                boid[1] = (byte)len;
                System.arraycopy(bytes, 1, boid, 2, len);
                final ASN1ObjectIdentifier oid = ASN1ObjectIdentifier.getInstance(boid);
                return new ECKeyFormat(oid, ECKeyFormat.ECAlgorithmFormat.from(bytes[0], bytes[bytes.length - 1]));

            default:
                throw new IllegalArgumentException("Unsupported Algorithm id " + bytes[0]);
        }
    }

    public static KeyFormat fromCreationKeyType(CreateSecurityTokenAlgorithmFragment.SupportedKeyType t, boolean forEncryption) {
        final int elen = 17; //65537
        final ECKeyFormat.ECAlgorithmFormat kf =
                forEncryption ? ECKeyFormat.ECAlgorithmFormat.ECDH_WITH_PUBKEY : ECKeyFormat.ECAlgorithmFormat.ECDSA_WITH_PUBKEY;

        switch (t) {
            case RSA_2048:
                return new RSAKeyFormat(2048, elen, RSAKeyFormat.RSAAlgorithmFormat.CRT_WITH_MODULUS);
            case RSA_3072:
                return new RSAKeyFormat(3072, elen, RSAKeyFormat.RSAAlgorithmFormat.CRT_WITH_MODULUS);
            case RSA_4096:
                return new RSAKeyFormat(4096, elen, RSAKeyFormat.RSAAlgorithmFormat.CRT_WITH_MODULUS);
            case ECC_P256:
                return new ECKeyFormat(NISTNamedCurves.getOID("P-256"), kf);
            case ECC_P384:
                return new ECKeyFormat(NISTNamedCurves.getOID("P-384"), kf);
            case ECC_P521:
                return new ECKeyFormat(NISTNamedCurves.getOID("P-521"), kf);
        }

        throw new IllegalArgumentException("Unsupported Algorithm id " + t);
    }

    public abstract void addToSaveKeyringParcel(SaveKeyringParcel.Builder builder, int keyFlags);

}
