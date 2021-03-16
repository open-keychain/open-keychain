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

import com.google.auto.value.AutoValue;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.math.ec.ECCurve;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;


// OpenPGP Card Spec: Algorithm Attributes: ECC
@AutoValue
public abstract class EcKeyFormat extends KeyFormat {

    public abstract int algorithmId();

    public abstract ASN1ObjectIdentifier curveOid();

    public abstract boolean withPubkey();

    private static final byte ATTRS_IMPORT_FORMAT_WITH_PUBKEY = (byte) 0xff;

    public static EcKeyFormat getInstance(int algorithmId, ASN1ObjectIdentifier oid, boolean withPubkey) {
        return new AutoValue_EcKeyFormat(algorithmId, oid, withPubkey);
    }

    public static EcKeyFormat getInstanceForKeyGeneration(KeyType keyType, ASN1ObjectIdentifier oidAsn1) {
        if (keyType == KeyType.ENCRYPT) {
            return getInstance(PublicKeyAlgorithmTags.ECDH, oidAsn1, true);
        } else { // SIGN, AUTH
            if (EcObjectIdentifiers.ED25519.equals(oidAsn1)) {
                return getInstance(PublicKeyAlgorithmTags.EDDSA, oidAsn1, true);
            } else {
                return getInstance(PublicKeyAlgorithmTags.ECDSA, oidAsn1, true);
            }
        }
    }

    public static EcKeyFormat getInstanceFromBytes(byte[] bytes) {
        if (bytes.length < 2) {
            throw new IllegalArgumentException("Bad length for EC attributes");
        }

        int algorithmId = bytes[0];
        int oidLen = bytes.length - 1;

        boolean withPubkey = false;
        if (bytes[bytes.length - 1] == ATTRS_IMPORT_FORMAT_WITH_PUBKEY) {
            withPubkey = true;
            oidLen -= 1;
        }

        final byte[] oidField = new byte[oidLen];
        System.arraycopy(bytes, 1, oidField, 0, oidLen);
        ASN1ObjectIdentifier oid = EcObjectIdentifiers.parseOid(oidField);

        return getInstance(algorithmId, oid, withPubkey);
    }

    public byte[] toBytes(KeyType slot) {
        byte[] oidField = EcObjectIdentifiers.asn1ToOidField(curveOid());

        int len = 1 + oidField.length;
        if (withPubkey()) {
            len += 1;
        }
        byte[] attrs = new byte[len];

        attrs[0] = (byte) algorithmId();
        System.arraycopy(oidField, 0, attrs, 1, oidField.length);
        if (withPubkey()) {
            attrs[len - 1] = ATTRS_IMPORT_FORMAT_WITH_PUBKEY;
        }

        return attrs;
    }

    public boolean isX25519() {
        return EcObjectIdentifiers.X25519.equals(curveOid());
    }

    public final boolean isEdDsa() {
        return algorithmId() == PublicKeyAlgorithmTags.EDDSA;
    }

    public void addToSaveKeyringParcel(SaveKeyringParcel.Builder builder, int keyFlags) {
        final X9ECParameters params = NISTNamedCurves.getByOID(curveOid());
        final ECCurve curve = params.getCurve();

        SaveKeyringParcel.Algorithm algo = SaveKeyringParcel.Algorithm.ECDSA;
        if (((keyFlags & KeyFlags.ENCRYPT_COMMS) == KeyFlags.ENCRYPT_COMMS)
                || ((keyFlags & KeyFlags.ENCRYPT_STORAGE) == KeyFlags.ENCRYPT_STORAGE)) {
            algo = SaveKeyringParcel.Algorithm.ECDH;
        }

        SaveKeyringParcel.Curve scurve;
        if (EcObjectIdentifiers.NIST_P_256.equals(curveOid())) {
            scurve = SaveKeyringParcel.Curve.NIST_P256;
        } else if (EcObjectIdentifiers.NIST_P_384.equals(curveOid())) {
            scurve = SaveKeyringParcel.Curve.NIST_P384;
        } else if (EcObjectIdentifiers.NIST_P_521.equals(curveOid())) {
            scurve = SaveKeyringParcel.Curve.NIST_P521;
        } else {
            throw new IllegalArgumentException("Unsupported curve " + curveOid());
        }

        builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(algo, curve.getFieldSize(), scurve, keyFlags, 0L));
    }

}
