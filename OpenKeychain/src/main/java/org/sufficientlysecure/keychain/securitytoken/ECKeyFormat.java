package org.sufficientlysecure.keychain.securitytoken;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.math.ec.ECCurve;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;

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

    public ASN1ObjectIdentifier getCurveOID() { return mECCurveOID; }

    public enum ECAlgorithmFormat {
        ECDH((byte)18, true, false),
        ECDH_WITH_PUBKEY((byte)18, true, true),
        ECDSA((byte)19, false, false),
        ECDSA_WITH_PUBKEY((byte)19, false, true);

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
                if (format.mValue == bFirst && ((bLast == (byte)0xff) == format.isWithPubkey())) {
                    return format;
                }
            }
            return null;
        }

        public final byte getValue() { return mValue; }
        public final boolean isECDH() { return mIsECDH; }
        public final boolean isWithPubkey() { return mWithPubkey; }
    }

    public void addToKeyring(SaveKeyringParcel keyring, int keyFlags) {
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

        keyring.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(algo,
                curve.getFieldSize(), scurve, keyFlags, 0L));
    }
}
