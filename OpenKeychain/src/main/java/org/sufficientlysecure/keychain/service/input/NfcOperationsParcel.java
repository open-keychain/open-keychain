package org.sufficientlysecure.keychain.service.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;


public class NfcOperationsParcel implements Parcelable {

    public enum NfcOperationType {
        NFC_SIGN, NFC_DECRYPT
    }

    public Date mSignatureTime;
    public final NfcOperationType mType;
    public final byte[][] mInputHashes;
    public final int[] mSignAlgos;

    private NfcOperationsParcel(NfcOperationType type, byte[][] inputHashes,
            int[] signAlgos, Date signatureTime) {
        mType = type;
        mInputHashes = inputHashes;
        mSignAlgos = signAlgos;
        mSignatureTime = signatureTime;
    }

    public NfcOperationsParcel(Parcel source) {
        mType = NfcOperationType.values()[source.readInt()];

        {
            int count = source.readInt();
            mInputHashes = new byte[count][];
            mSignAlgos = new int[count];
            for (int i = 0; i < count; i++) {
                mInputHashes[i] = source.createByteArray();
                mSignAlgos[i] = source.readInt();
            }
        }

        mSignatureTime = source.readInt() != 0 ? new Date(source.readLong()) : null;

    }

    public static NfcOperationsParcel createNfcSignOperation(
            byte[] inputHash, int signAlgo, Date signatureTime) {
        return new NfcOperationsParcel(NfcOperationType.NFC_SIGN,
                new byte[][] { inputHash }, new int[] { signAlgo }, signatureTime);
    }

    public static NfcOperationsParcel createNfcDecryptOperation(byte[] inputHash) {
        return new NfcOperationsParcel(NfcOperationType.NFC_DECRYPT,
                new byte[][] { inputHash }, null, null);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mType.ordinal());
        dest.writeInt(mInputHashes.length);
        for (int i = 0; i < mInputHashes.length; i++) {
            dest.writeByteArray(mInputHashes[i]);
            dest.writeInt(mSignAlgos[i]);
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

    public static class NfcSignOperationsBuilder {
        Date mSignatureTime;
        ArrayList<Integer> mSignAlgos = new ArrayList<>();
        ArrayList<byte[]> mInputHashes = new ArrayList<>();

        public NfcSignOperationsBuilder(Date signatureTime) {
            mSignatureTime = signatureTime;
        }

        public NfcOperationsParcel build() {
            byte[][] inputHashes = new byte[mInputHashes.size()][];
            mInputHashes.toArray(inputHashes);
            int[] signAlgos = new int[mSignAlgos.size()];
            for (int i = 0; i < mSignAlgos.size(); i++) {
                signAlgos[i] = mSignAlgos.get(i);
            }

            return new NfcOperationsParcel(NfcOperationType.NFC_SIGN,
                    inputHashes, signAlgos, mSignatureTime);
        }

        public void addHash(byte[] hash, int algo) {
            mInputHashes.add(hash);
            mSignAlgos.add(algo);
        }

        public void addAll(NfcOperationsParcel input) {
            if (!mSignatureTime.equals(input.mSignatureTime)) {
                throw new AssertionError("input times must match, this is a programming error!");
            }
            if (input.mType != NfcOperationType.NFC_SIGN) {
                throw new AssertionError("operation types must match, this is a progrmming error!");
            }

            Collections.addAll(mInputHashes, input.mInputHashes);
            for (int signAlgo : input.mSignAlgos) {
                mSignAlgos.add(signAlgo);
            }
        }

        public boolean isEmpty() {
            return mInputHashes.isEmpty();
        }

    }

}
