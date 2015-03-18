package org.sufficientlysecure.keychain.service.input;

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;


public class NfcOperationsParcel implements Parcelable {

    public enum NfcOperationType {
        NFC_SIGN, NFC_DECRYPT
    }

    public Date mSignatureTime;
    public final NfcOperationType mType;
    public final byte[][] mInputHash;
    public final int[] mSignAlgo;

    private NfcOperationsParcel(NfcOperationType type, byte[] inputHash, int signAlgo, Date signatureTime) {
        mType = type;
        mInputHash = new byte[][] { inputHash };
        mSignAlgo = new int[] { signAlgo };
        mSignatureTime = signatureTime;
    }

    public NfcOperationsParcel(Parcel source) {
        mType = NfcOperationType.values()[source.readInt()];

        {
            int count = source.readInt();
            mInputHash = new byte[count][];
            mSignAlgo = new int[count];
            for (int i = 0; i < count; i++) {
                mInputHash[i] = source.createByteArray();
                mSignAlgo[i] = source.readInt();
            }
        }

        mSignatureTime = source.readInt() != 0 ? new Date(source.readLong()) : null;

    }

    public static NfcOperationsParcel createNfcSignOperation(
            byte[] inputHash, int signAlgo, Date signatureTime) {
        return new NfcOperationsParcel(NfcOperationType.NFC_SIGN, inputHash, signAlgo, signatureTime);
    }

    public static NfcOperationsParcel createNfcDecryptOperation(byte[] inputHash) {
        return new NfcOperationsParcel(NfcOperationType.NFC_DECRYPT, inputHash, 0, null);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mType.ordinal());
        dest.writeInt(mInputHash.length);
        for (int i = 0; i < mInputHash.length; i++) {
            dest.writeByteArray(mInputHash[i]);
            dest.writeInt(mSignAlgo[i]);
        }
        if (mSignatureTime != null) {
            dest.writeInt(1);
            dest.writeLong(mSignatureTime.getTime());
        } else {
            dest.writeInt(0);
        }

    }

    public static final Creator<NfcOperationsParcel> CREATOR = new Creator<NfcOperationsParcel>() {
        public NfcOperationsParcel createFromParcel(final Parcel source) {
            return new NfcOperationsParcel(source);
        }

        public NfcOperationsParcel[] newArray(final int size) {
            return new NfcOperationsParcel[size];
        }
    };

}
