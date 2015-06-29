package org.sufficientlysecure.keychain.service.input;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;


public class RequiredInputParcel implements Parcelable {

    public enum RequiredInputType {
        PASSPHRASE, PASSPHRASE_SYMMETRIC, NFC_SIGN, NFC_DECRYPT, NFC_MOVE_KEY_TO_CARD
    }

    public Date mSignatureTime;

    public final RequiredInputType mType;

    public final byte[][] mInputHashes;
    public final int[] mSignAlgos;

    private Long mMasterKeyId;
    private Long mSubKeyId;

    private RequiredInputParcel(RequiredInputType type, byte[][] inputHashes,
            int[] signAlgos, Date signatureTime, Long masterKeyId, Long subKeyId) {
        mType = type;
        mInputHashes = inputHashes;
        mSignAlgos = signAlgos;
        mSignatureTime = signatureTime;
        mMasterKeyId = masterKeyId;
        mSubKeyId = subKeyId;
    }

    public RequiredInputParcel(Parcel source) {
        mType = RequiredInputType.values()[source.readInt()];

        // 0 = none, 1 = both, 2 = only hashes (decrypt)
        int hashTypes = source.readInt();
        if (hashTypes != 0) {
            int count = source.readInt();
            mInputHashes = new byte[count][];
            if (hashTypes == 1) {
                mSignAlgos = new int[count];
                for (int i = 0; i < count; i++) {
                    mInputHashes[i] = source.createByteArray();
                    mSignAlgos[i] = source.readInt();
                }
            } else {
                mSignAlgos = null;
                for (int i = 0; i < count; i++) {
                    mInputHashes[i] = source.createByteArray();
                }
            }
        } else {
            mInputHashes = null;
            mSignAlgos = null;
        }

        mSignatureTime = source.readInt() != 0 ? new Date(source.readLong()) : null;
        mMasterKeyId = source.readInt() != 0 ? source.readLong() : null;
        mSubKeyId = source.readInt() != 0 ? source.readLong() : null;

    }

    public Long getMasterKeyId() {
        return mMasterKeyId;
    }

    public Long getSubKeyId() {
        return mSubKeyId;
    }

    public static RequiredInputParcel createNfcSignOperation(
            long masterKeyId, long subKeyId,
            byte[] inputHash, int signAlgo, Date signatureTime) {
        return new RequiredInputParcel(RequiredInputType.NFC_SIGN,
                new byte[][] { inputHash }, new int[] { signAlgo },
                signatureTime, masterKeyId, subKeyId);
    }

    public static RequiredInputParcel createNfcDecryptOperation(
            long masterKeyId, long subKeyId, byte[] inputHash) {
        return new RequiredInputParcel(RequiredInputType.NFC_DECRYPT,
                new byte[][] { inputHash }, null, null, masterKeyId, subKeyId);
    }

    public static RequiredInputParcel createRequiredSignPassphrase(
            long masterKeyId, long subKeyId, Date signatureTime) {
        return new RequiredInputParcel(RequiredInputType.PASSPHRASE,
                null, null, signatureTime, masterKeyId, subKeyId);
    }

    public static RequiredInputParcel createRequiredDecryptPassphrase(
            long masterKeyId, long subKeyId) {
        return new RequiredInputParcel(RequiredInputType.PASSPHRASE,
                null, null, null, masterKeyId, subKeyId);
    }

    public static RequiredInputParcel createRequiredSymmetricPassphrase() {
        return new RequiredInputParcel(RequiredInputType.PASSPHRASE_SYMMETRIC,
                null, null, null, null, null);
    }

    public static RequiredInputParcel createRequiredPassphrase(
            RequiredInputParcel req) {
        return new RequiredInputParcel(RequiredInputType.PASSPHRASE,
                null, null, req.mSignatureTime, req.mMasterKeyId, req.mSubKeyId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mType.ordinal());
        if (mInputHashes != null) {
            dest.writeInt(mSignAlgos != null ? 1 : 2);
            dest.writeInt(mInputHashes.length);
            for (int i = 0; i < mInputHashes.length; i++) {
                dest.writeByteArray(mInputHashes[i]);
                if (mSignAlgos != null) {
                    dest.writeInt(mSignAlgos[i]);
                }
            }
        } else {
            dest.writeInt(0);
        }
        if (mSignatureTime != null) {
            dest.writeInt(1);
            dest.writeLong(mSignatureTime.getTime());
        } else {
            dest.writeInt(0);
        }
        if (mMasterKeyId != null) {
            dest.writeInt(1);
            dest.writeLong(mMasterKeyId);
        } else {
            dest.writeInt(0);
        }
        if (mSubKeyId != null) {
            dest.writeInt(1);
            dest.writeLong(mSubKeyId);
        } else {
            dest.writeInt(0);
        }

    }

    public static final Creator<RequiredInputParcel> CREATOR = new Creator<RequiredInputParcel>() {
        public RequiredInputParcel createFromParcel(final Parcel source) {
            return new RequiredInputParcel(source);
        }

        public RequiredInputParcel[] newArray(final int size) {
            return new RequiredInputParcel[size];
        }
    };

    public static class NfcSignOperationsBuilder {
        Date mSignatureTime;
        ArrayList<Integer> mSignAlgos = new ArrayList<>();
        ArrayList<byte[]> mInputHashes = new ArrayList<>();
        long mMasterKeyId;
        long mSubKeyId;

        public NfcSignOperationsBuilder(Date signatureTime, long masterKeyId, long subKeyId) {
            mSignatureTime = signatureTime;
            mMasterKeyId = masterKeyId;
            mSubKeyId = subKeyId;
        }

        public RequiredInputParcel build() {
            byte[][] inputHashes = new byte[mInputHashes.size()][];
            mInputHashes.toArray(inputHashes);
            int[] signAlgos = new int[mSignAlgos.size()];
            for (int i = 0; i < mSignAlgos.size(); i++) {
                signAlgos[i] = mSignAlgos.get(i);
            }

            return new RequiredInputParcel(RequiredInputType.NFC_SIGN,
                    inputHashes, signAlgos, mSignatureTime, mMasterKeyId, mSubKeyId);
        }

        public void addHash(byte[] hash, int algo) {
            mInputHashes.add(hash);
            mSignAlgos.add(algo);
        }

        public void addAll(RequiredInputParcel input) {
            if (!mSignatureTime.equals(input.mSignatureTime)) {
                throw new AssertionError("input times must match, this is a programming error!");
            }
            if (input.mType != RequiredInputType.NFC_SIGN) {
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

    public static class NfcKeyToCardOperationsBuilder {
        ArrayList<byte[]> mSubkeysToExport = new ArrayList<>();
        Long mMasterKeyId;

        public NfcKeyToCardOperationsBuilder(Long masterKeyId) {
            mMasterKeyId = masterKeyId;
        }

        public RequiredInputParcel build() {
            byte[][] inputHashes = new byte[mSubkeysToExport.size()][];
            mSubkeysToExport.toArray(inputHashes);
            ByteBuffer buf = ByteBuffer.wrap(mSubkeysToExport.get(0));

            // We need to pass in a subkey here...
            return new RequiredInputParcel(RequiredInputType.NFC_MOVE_KEY_TO_CARD,
                    inputHashes, null, null, mMasterKeyId, buf.getLong());
        }

        public void addSubkey(long subkeyId) {
            byte[] subKeyId = new byte[8];
            ByteBuffer buf = ByteBuffer.wrap(subKeyId);
            buf.putLong(subkeyId).rewind();
            mSubkeysToExport.add(subKeyId);
        }

        public void addAll(RequiredInputParcel input) {
            if (!mMasterKeyId.equals(input.mMasterKeyId)) {
                throw new AssertionError("Master keys must match, this is a programming error!");
            }
            if (input.mType != RequiredInputType.NFC_MOVE_KEY_TO_CARD) {
                throw new AssertionError("Operation types must match, this is a programming error!");
            }

            Collections.addAll(mSubkeysToExport, input.mInputHashes);
        }

        public boolean isEmpty() {
            return mSubkeysToExport.isEmpty();
        }

    }

}
