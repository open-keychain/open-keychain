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

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.util.Arrays;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;

import java.io.IOException;

import timber.log.Timber;


// 4.3.3.6 Algorithm Attributes
@AutoValue
public abstract class ECKeyFormat extends KeyFormat {

    public abstract byte[] oidField();

    @Nullable // TODO
    public abstract ECAlgorithmFormat ecAlgorithmFormat();

    private static final byte ATTRS_IMPORT_FORMAT_WITH_PUBKEY = (byte) 0xff;

    ECKeyFormat() {
        super(KeyFormatType.ECKeyFormatType);
    }

    public static KeyFormat getInstance(byte[] oidField, ECAlgorithmFormat from) {
        return new AutoValue_ECKeyFormat(oidField, from);
    }

    public static ECKeyFormat getInstance(ASN1ObjectIdentifier oidAsn1, ECAlgorithmFormat from) {
        byte[] oidField = asn1ToOidField(oidAsn1);
        return new AutoValue_ECKeyFormat(oidField, from);
    }

    public static KeyFormat getInstanceFromBytes(byte[] bytes) {
        if (bytes.length < 2) {
            throw new IllegalArgumentException("Bad length for EC attributes");
        }

        int len = bytes.length - 1;
        if (bytes[bytes.length - 1] == ATTRS_IMPORT_FORMAT_WITH_PUBKEY) {
            len -= 1;
        }

        final byte[] oidField = new byte[len];
        System.arraycopy(bytes, 1, oidField, 0, len);
        return getInstance(oidField, ECKeyFormat.ECAlgorithmFormat.from(bytes[0], bytes[bytes.length - 1]));
    }

    public byte[] toBytes(KeyType slot) {
        byte[] attrs = new byte[1 + oidField().length + 1];

        attrs[0] = ecAlgorithmFormat().getAlgorithmId();
        System.arraycopy(oidField(), 0, attrs, 1, oidField().length);
        attrs[attrs.length - 1] = ATTRS_IMPORT_FORMAT_WITH_PUBKEY;

        return attrs;
    }

    public ASN1ObjectIdentifier asn1ParseOid() {
        ASN1ObjectIdentifier asn1CurveOid = oidFieldToOidAsn1(oidField());
        String curveName = ECNamedCurveTable.getName(asn1CurveOid);
        if (curveName == null) {
            Timber.w("Unknown curve OID: %s. Could be YubiKey firmware bug < 5.2.8. Trying again with last byte removed.", asn1CurveOid.getId());

            // https://bugs.chromium.org/p/chromium/issues/detail?id=1120933#c10
            // The OpenPGP applet of a Yubikey with firmware version below 5.2.8 appends
            // a potentially arbitrary byte to the intended byte representation of an ECC
            // curve OID. This case is handled by retrying the decoding with the last
            // byte stripped if the resulting OID does not label a known curve.
            byte[] oidRemoveLastByte = Arrays.copyOf(oidField(), oidField().length - 1);
            ASN1ObjectIdentifier asn1CurveOidYubikey = oidFieldToOidAsn1(oidRemoveLastByte);
            curveName = ECNamedCurveTable.getName(asn1CurveOidYubikey);

            if (curveName != null) {
                Timber.w("Detected curve OID: %s", asn1CurveOidYubikey.getId());
                return asn1CurveOidYubikey;
            } else {
                Timber.e("Still Unknown curve OID: %s", asn1CurveOidYubikey.getId());
                return asn1CurveOid;
            }
        }

        return asn1CurveOid;
    }

    private static byte[] asn1ToOidField(ASN1ObjectIdentifier oidAsn1) {
        byte[] encodedAsn1Oid;
        try {
            encodedAsn1Oid = oidAsn1.getEncoded();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode curve OID!");
        }
        byte[] oidField = new byte[encodedAsn1Oid.length - 2];
        System.arraycopy(encodedAsn1Oid, 2, oidField, 0, encodedAsn1Oid.length - 2);

        return oidField;
    }

    private static ASN1ObjectIdentifier oidFieldToOidAsn1(byte[] oidField) {
        final byte[] boid = new byte[2 + oidField.length];
        boid[0] = (byte) 0x06;
        boid[1] = (byte) oidField.length;
        System.arraycopy(oidField, 0, boid, 2, oidField.length);
        return ASN1ObjectIdentifier.getInstance(boid);
    }

    public enum ECAlgorithmFormat {
        ECDH((byte) PublicKeyAlgorithmTags.ECDH, true, false),
        ECDH_WITH_PUBKEY((byte) PublicKeyAlgorithmTags.ECDH, true, true),
        ECDSA((byte) PublicKeyAlgorithmTags.ECDSA, false, false),
        ECDSA_WITH_PUBKEY((byte) PublicKeyAlgorithmTags.ECDSA, false, true);

        private final byte mAlgorithmId;
        private final boolean mIsECDH;
        private final boolean mWithPubkey;

        ECAlgorithmFormat(final byte algorithmId, final boolean isECDH, final boolean withPubkey) {
            mAlgorithmId = algorithmId;
            mIsECDH = isECDH;
            mWithPubkey = withPubkey;
        }

        public static ECKeyFormat.ECAlgorithmFormat from(final byte bFirst, final byte bLast) {
            for (ECKeyFormat.ECAlgorithmFormat format : values()) {
                if (format.mAlgorithmId == bFirst &&
                        ((bLast == ATTRS_IMPORT_FORMAT_WITH_PUBKEY) == format.isWithPubkey())) {
                    return format;
                }
            }
            return null;
        }

        public final byte getAlgorithmId() {
            return mAlgorithmId;
        }

        public final boolean isECDH() {
            return mIsECDH;
        }

        public final boolean isWithPubkey() {
            return mWithPubkey;
        }
    }

    public void addToSaveKeyringParcel(SaveKeyringParcel.Builder builder, int keyFlags) {
        ASN1ObjectIdentifier oidAsn1 = asn1ParseOid();
        final X9ECParameters params = NISTNamedCurves.getByOID(oidAsn1);
        final ECCurve curve = params.getCurve();

        SaveKeyringParcel.Algorithm algo = SaveKeyringParcel.Algorithm.ECDSA;
        if (((keyFlags & KeyFlags.ENCRYPT_COMMS) == KeyFlags.ENCRYPT_COMMS)
                || ((keyFlags & KeyFlags.ENCRYPT_STORAGE) == KeyFlags.ENCRYPT_STORAGE)) {
            algo = SaveKeyringParcel.Algorithm.ECDH;
        }

        SaveKeyringParcel.Curve scurve;
        if (oidAsn1.equals(NISTNamedCurves.getOID("P-256"))) {
            scurve = SaveKeyringParcel.Curve.NIST_P256;
        } else if (oidAsn1.equals(NISTNamedCurves.getOID("P-384"))) {
            scurve = SaveKeyringParcel.Curve.NIST_P384;
        } else if (oidAsn1.equals(NISTNamedCurves.getOID("P-521"))) {
            scurve = SaveKeyringParcel.Curve.NIST_P521;
        } else {
            throw new IllegalArgumentException("Unsupported curve " + oidAsn1);
        }

        builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(algo, curve.getFieldSize(), scurve, keyFlags, 0L));
    }
}
