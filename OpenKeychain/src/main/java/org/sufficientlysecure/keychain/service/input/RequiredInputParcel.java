package org.sufficientlysecure.keychain.service.input;

import android.os.Parcel;
import android.os.Parcelable;

import android.support.annotation.NonNull;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


public class RequiredInputParcel implements Parcelable {

    public enum RequiredInputType {
        PASSPHRASE_TOKEN_UNLOCK, PASSPHRASE_IMPORT_KEY, PASSPHRASE_KEYRING_UNLOCK, PASSPHRASE_SYMMETRIC, BACKUP_CODE, SECURITY_TOKEN_SIGN, SECURITY_TOKEN_DECRYPT,
        SECURITY_TOKEN_MOVE_KEY_TO_CARD, SECURITY_TOKEN_RESET_CARD, ENABLE_ORBOT, UPLOAD_FAIL_RETRY,
    }

    public Date mSignatureTime;

    public final RequiredInputType mType;

    public final byte[][] mInputData;
    public final int[] mSignAlgos;

    private Long mMasterKeyId;
    private Long mSubKeyId;
    private Passphrase mKeyringPassphrase;
    private ParcelableKeyRing mParcelableKeyRing;

    public boolean mSkipCaching = false;

    private RequiredInputParcel(RequiredInputType type, byte[][] inputData,
            int[] signAlgos, Date signatureTime, Long masterKeyId, Long subKeyId,
                                Passphrase keyringPassphrase,
                                ParcelableKeyRing parcelableKeyRing) {
        mType = type;
        mInputData = inputData;
        mSignAlgos = signAlgos;
        mSignatureTime = signatureTime;
        mMasterKeyId = masterKeyId;
        mSubKeyId = subKeyId;
        mKeyringPassphrase = keyringPassphrase;
        mParcelableKeyRing = parcelableKeyRing;
    }

    public RequiredInputParcel(Parcel source) {
        mType = RequiredInputType.values()[source.readInt()];

        // 0 = none, 1 = signAlgos + inputData, 2 = only inputData (decrypt)
        int inputDataType = source.readInt();
        if (inputDataType != 0) {
            int count = source.readInt();
            mInputData = new byte[count][];
            if (inputDataType == 1) {
                mSignAlgos = new int[count];
                for (int i = 0; i < count; i++) {
                    mInputData[i] = source.createByteArray();
                    mSignAlgos[i] = source.readInt();
                }
            } else {
                mSignAlgos = null;
                for (int i = 0; i < count; i++) {
                    mInputData[i] = source.createByteArray();
                }
            }
        } else {
            mInputData = null;
            mSignAlgos = null;
        }

        mSignatureTime = source.readInt() != 0 ? new Date(source.readLong()) : null;
        mMasterKeyId = source.readInt() != 0 ? source.readLong() : null;
        mSubKeyId = source.readInt() != 0 ? source.readLong() : null;
        mKeyringPassphrase = source.readInt() != 0
                ? (Passphrase) source.readParcelable(Passphrase.class.getClassLoader())
                : null;
        mParcelableKeyRing = source.readInt() != 0
                ? (ParcelableKeyRing) source.readParcelable(ParcelableKeyRing.class.getClassLoader())
                : null;
        mSkipCaching = source.readInt() != 0;

    }

    public Long getMasterKeyId() {
        return mMasterKeyId;
    }

    public Long getSubKeyId() {
        return mSubKeyId;
    }

    public boolean hasKeyringPassphrase() {
        return mKeyringPassphrase != null;
    }

    public Passphrase getKeyringPassphrase() {
        return mKeyringPassphrase;
    }

    public void setKeyringPassphrase(Passphrase passphrase) {
        mKeyringPassphrase = passphrase;
    }

    public boolean hasParcelableKeyRing() {
        return mParcelableKeyRing != null;
    }

    public ParcelableKeyRing getParcelableKeyRing() {
        return mParcelableKeyRing;
    }

    public static RequiredInputParcel createRetryUploadOperation() {
        return new RequiredInputParcel(RequiredInputType.UPLOAD_FAIL_RETRY,
                null, null, null, 0L, 0L, null, null);
    }

    public static RequiredInputParcel createOrbotRequiredOperation() {
        return new RequiredInputParcel(RequiredInputType.ENABLE_ORBOT, null, null, null, 0L, 0L, null, null);
    }

    public static RequiredInputParcel createSecurityTokenSignOperation(
            long masterKeyId, long subKeyId,
            byte[] inputHash, int signAlgo, Date signatureTime) {
        return new RequiredInputParcel(RequiredInputType.SECURITY_TOKEN_SIGN,
                new byte[][] { inputHash }, new int[] { signAlgo },
                signatureTime, masterKeyId, subKeyId, null, null);
    }

    public static RequiredInputParcel createSecurityTokenDecryptOperation(
            long masterKeyId, long subKeyId, byte[] encryptedSessionKey) {
        return new RequiredInputParcel(RequiredInputType.SECURITY_TOKEN_DECRYPT,
                new byte[][] { encryptedSessionKey }, null, null, masterKeyId, subKeyId, null, null);
    }

    public static RequiredInputParcel createSecurityTokenReset() {
        return new RequiredInputParcel(RequiredInputType.SECURITY_TOKEN_RESET_CARD,
                null, null, null, null, null, null, null);
    }

    public static RequiredInputParcel createRequiredTokenPassphrase(
            RequiredInputParcel req, @NonNull Passphrase keyringPassphrase) {
        return new RequiredInputParcel(RequiredInputType.PASSPHRASE_TOKEN_UNLOCK,
                null, null, req.mSignatureTime, req.mMasterKeyId, req.mSubKeyId, keyringPassphrase, null);
    }

    public static RequiredInputParcel createRequiredImportKeyPassphrase(
            long masterKeyId, long subKeyId, @NonNull ParcelableKeyRing parcelableKeyRing) {
        return new RequiredInputParcel(RequiredInputType.PASSPHRASE_IMPORT_KEY,
                null, null, null, masterKeyId, subKeyId, null, parcelableKeyRing);
    }

    public static RequiredInputParcel createRequiredSymmetricPassphrase() {
        return new RequiredInputParcel(RequiredInputType.PASSPHRASE_SYMMETRIC,
                null, null, null, null, null, null, null);
    }

    public static RequiredInputParcel createRequiredBackupCode() {
        return new RequiredInputParcel(RequiredInputType.BACKUP_CODE,
                null, null, null, null, null, null, null);
    }

    public static RequiredInputParcel createRequiredKeyringPassphrase(long masterKeyId) {
        return new RequiredInputParcel(RequiredInputType.PASSPHRASE_KEYRING_UNLOCK,
                null, null, null, masterKeyId, masterKeyId, null, null);
    }

    public static RequiredInputParcel createRequiredKeyringPassphrase(RequiredInputParcel req) {
        return new RequiredInputParcel(RequiredInputType.PASSPHRASE_KEYRING_UNLOCK,
                null, null, req.mSignatureTime, req.mMasterKeyId, req.mSubKeyId, null, null);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mType.ordinal());
        if (mInputData != null) {
            dest.writeInt(mSignAlgos != null ? 1 : 2);
            dest.writeInt(mInputData.length);
            for (int i = 0; i < mInputData.length; i++) {
                dest.writeByteArray(mInputData[i]);
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
        if (mKeyringPassphrase != null) {
            dest.writeInt(1);
            dest.writeParcelable(mKeyringPassphrase, 0);
        } else {
            dest.writeInt(0);
        }
        if (mParcelableKeyRing != null) {
            dest.writeInt(1);
            dest.writeParcelable(mParcelableKeyRing, 0);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(mSkipCaching ? 1 : 0);

    }

    public static final Creator<RequiredInputParcel> CREATOR = new Creator<RequiredInputParcel>() {
        public RequiredInputParcel createFromParcel(final Parcel source) {
            return new RequiredInputParcel(source);
        }

        public RequiredInputParcel[] newArray(final int size) {
            return new RequiredInputParcel[size];
        }
    };

    public static class SecurityTokenSignOperationsBuilder {
        Date mSignatureTime;
        ArrayList<Integer> mSignAlgos = new ArrayList<>();
        ArrayList<byte[]> mInputHashes = new ArrayList<>();
        long mMasterKeyId;
        long mSubKeyId;

        public SecurityTokenSignOperationsBuilder(Date signatureTime, long masterKeyId, long subKeyId) {
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

            return new RequiredInputParcel(RequiredInputType.SECURITY_TOKEN_SIGN,
                    inputHashes, signAlgos, mSignatureTime, mMasterKeyId, mSubKeyId, null, null);
        }

        public void addHash(byte[] hash, int algo) {
            mInputHashes.add(hash);
            mSignAlgos.add(algo);
        }

        public void addAll(RequiredInputParcel input) {
            if (!mSignatureTime.equals(input.mSignatureTime)) {
                throw new AssertionError("input times must match, this is a programming error!");
            }
            if (input.mType != RequiredInputType.SECURITY_TOKEN_SIGN) {
                throw new AssertionError("operation types must match, this is a progrmming error!");
            }

            Collections.addAll(mInputHashes, input.mInputData);
            for (int signAlgo : input.mSignAlgos) {
                mSignAlgos.add(signAlgo);
            }
        }

        public boolean isEmpty() {
            return mInputHashes.isEmpty();
        }

    }

    public static class SecurityTokenKeyToCardOperationsBuilder {
        ArrayList<byte[]> mSubkeysToExport = new ArrayList<>();
        Long mMasterKeyId;
        byte[] mPin;
        byte[] mAdminPin;

        public SecurityTokenKeyToCardOperationsBuilder(Long masterKeyId) {
            mMasterKeyId = masterKeyId;
        }

        public RequiredInputParcel build() {
            byte[][] inputData = new byte[mSubkeysToExport.size() + 2][];

            // encode all subkeys into inputData
            byte[][] subkeyData = new byte[mSubkeysToExport.size()][];
            mSubkeysToExport.toArray(subkeyData);

            // first two are PINs
            inputData[0] = mPin;
            inputData[1] = mAdminPin;
            // then subkeys
            System.arraycopy(subkeyData, 0, inputData, 2, subkeyData.length);

            ByteBuffer buf = ByteBuffer.wrap(mSubkeysToExport.get(0));

            // We need to pass in a subkey here...
            return new RequiredInputParcel(RequiredInputType.SECURITY_TOKEN_MOVE_KEY_TO_CARD,
                    inputData, null, null, mMasterKeyId, buf.getLong(), null, null);
        }

        public void addSubkey(long subkeyId) {
            byte[] subKeyId = new byte[8];
            ByteBuffer buf = ByteBuffer.wrap(subKeyId);
            buf.putLong(subkeyId).rewind();
            mSubkeysToExport.add(subKeyId);
        }

        public void setPin(Passphrase pin) {
            mPin = pin.toStringUnsafe().getBytes();
        }

        public void setAdminPin(Passphrase adminPin) {
            mAdminPin = adminPin.toStringUnsafe().getBytes();
        }

        public void addAll(RequiredInputParcel input) {
            if (!mMasterKeyId.equals(input.mMasterKeyId)) {
                throw new AssertionError("Master keys must match, this is a programming error!");
            }
            if (input.mType != RequiredInputType.SECURITY_TOKEN_MOVE_KEY_TO_CARD) {
                throw new AssertionError("Operation types must match, this is a programming error!");
            }

            Collections.addAll(mSubkeysToExport, input.mInputData);
        }

        public boolean isEmpty() {
            return mSubkeysToExport.isEmpty();
        }

    }

}
