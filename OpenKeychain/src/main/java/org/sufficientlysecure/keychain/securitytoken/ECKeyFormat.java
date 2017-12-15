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
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.math.ec.ECCurve;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;


// 4.3.3.6 Algorithm Attributes
public class ECKeyFormat extends KeyFormat {

    private final ECAlgorithmFormat mECAlgorithmFormat;
    private final ASN1ObjectIdentifier mECCurveOID;

    public ECKeyFormat(final ASN1ObjectIdentifier ecCurveOid,
                       final ECAlgorithmFormat ecAlgorithmFormat) {
        super(KeyFormatType.ECKeyFormatType);
        mECAlgorithmFormat = ecAlgorithmFormat;
        mECCurveOID = ecCurveOid;
    }

    public ECKeyFormat.ECAlgorithmFormat getAlgorithmFormat() {
        return mECAlgorithmFormat;
    }

    public ASN1ObjectIdentifier getCurveOID() {
        return mECCurveOID;
    }

    public enum ECAlgorithmFormat {
        ECDH((byte) 18, true, false),
        ECDH_WITH_PUBKEY((byte) 18, true, true),
        ECDSA((byte) 19, false, false),
        ECDSA_WITH_PUBKEY((byte) 19, false, true);

        private final byte mValue;
        private final boolean mIsECDH;
        private final boolean mWithPubkey;

        ECAlgorithmFormat(final byte value, final boolean isECDH, final boolean withPubkey) {
            mValue = value;
            mIsECDH = isECDH;
            mWithPubkey = withPubkey;
        }

        public static ECKeyFormat.ECAlgorithmFormat from(final byte bFirst, final byte bLast) {
            for (ECKeyFormat.ECAlgorithmFormat format : values()) {
                if (format.mValue == bFirst && ((bLast == (byte) 0xff) == format.isWithPubkey())) {
                    return format;
                }
            }
            return null;
        }

        public final byte getValue() {
            return mValue;
        }

        public final boolean isECDH() {
            return mIsECDH;
        }

        public final boolean isWithPubkey() {
            return mWithPubkey;
        }
    }

    public void addToSaveKeyringParcel(SaveKeyringParcel.Builder builder, int keyFlags) {
        final X9ECParameters params = NISTNamedCurves.getByOID(mECCurveOID);
        final ECCurve curve = params.getCurve();

        SaveKeyringParcel.Algorithm algo = SaveKeyringParcel.Algorithm.ECDSA;
        if (((keyFlags & KeyFlags.ENCRYPT_COMMS) == KeyFlags.ENCRYPT_COMMS)
                || ((keyFlags & KeyFlags.ENCRYPT_STORAGE) == KeyFlags.ENCRYPT_STORAGE)) {
            algo = SaveKeyringParcel.Algorithm.ECDH;
        }

        SaveKeyringParcel.Curve scurve;
        if (mECCurveOID.equals(NISTNamedCurves.getOID("P-256"))) {
            scurve = SaveKeyringParcel.Curve.NIST_P256;
        } else if (mECCurveOID.equals(NISTNamedCurves.getOID("P-384"))) {
            scurve = SaveKeyringParcel.Curve.NIST_P384;
        } else if (mECCurveOID.equals(NISTNamedCurves.getOID("P-521"))) {
            scurve = SaveKeyringParcel.Curve.NIST_P521;
        } else {
            throw new IllegalArgumentException("Unsupported curve " + mECCurveOID);
        }

        builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(algo, curve.getFieldSize(), scurve, keyFlags, 0L));
    }
}
