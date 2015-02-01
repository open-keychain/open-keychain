package org.sufficientlysecure.keychain.pgp;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** This parcel stores the input of one or more PgpSignEncrypt operations.
 * All operations will use the same general paramters, differing only in
 * input and output. Each input/output set depends on the paramters:
 *
 * - Each input uri is individually encrypted/signed
 * - If a byte array is supplied, it is treated as an input before uris are processed
 * - The number of output uris must match the number of input uris, plus one more
 *   if there is a byte array present.
 * - Once the output uris are empty, there must be exactly one input (uri xor bytes)
 *   left, which will be returned in a byte array as part of the result parcel.
 *
 */
public class SignEncryptParcel extends PgpSignEncryptInput implements Parcelable {

    public ArrayList<Uri> mInputUris = new ArrayList<>();
    public ArrayList<Uri> mOutputUris = new ArrayList<>();
    public byte[] mBytes;

    public SignEncryptParcel() {
        super();
    }

    public SignEncryptParcel(Parcel src) {

        // we do all of those here, so the PgpSignEncryptInput class doesn't have to be parcelable
        mVersionHeader = src.readString();
        mEnableAsciiArmorOutput  = src.readInt() == 1;
        mCompressionId = src.readInt();
        mEncryptionMasterKeyIds = src.createLongArray();
        mSymmetricPassphrase = src.readString();
        mSymmetricEncryptionAlgorithm = src.readInt();
        mSignatureMasterKeyId = src.readLong();
        mSignatureSubKeyId = src.readInt() == 1 ? src.readLong() : null;
        mSignatureHashAlgorithm = src.readInt();
        mSignaturePassphrase = src.readString();
        mAdditionalEncryptId = src.readLong();
        mNfcSignedHash = src.createByteArray();
        mNfcCreationTimestamp = src.readInt() == 1 ? new Date(src.readLong()) : null;
        mFailOnMissingEncryptionKeyIds = src.readInt() == 1;
        mCharset = src.readString();
        mCleartextSignature = src.readInt() == 1;
        mDetachedSignature = src.readInt() == 1;

        mInputUris = src.createTypedArrayList(Uri.CREATOR);
        mOutputUris = src.createTypedArrayList(Uri.CREATOR);
        mBytes = src.createByteArray();

    }

    public byte[] getBytes() {
        return mBytes;
    }

    public void setBytes(byte[] bytes) {
        mBytes = bytes;
    }

    public List<Uri> getInputUris() {
        return Collections.unmodifiableList(mInputUris);
    }

    public void addInputUris(Collection<Uri> inputUris) {
        mInputUris.addAll(inputUris);
    }

    public List<Uri> getOutputUris() {
        return Collections.unmodifiableList(mOutputUris);
    }

    public void addOutputUris(ArrayList<Uri> outputUris) {
        mOutputUris.addAll(outputUris);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mVersionHeader);
        dest.writeInt(mEnableAsciiArmorOutput ? 1 : 0);
        dest.writeInt(mCompressionId);
        dest.writeLongArray(mEncryptionMasterKeyIds);
        dest.writeString(mSymmetricPassphrase);
        dest.writeInt(mSymmetricEncryptionAlgorithm);
        dest.writeLong(mSignatureMasterKeyId);
        if (mSignatureSubKeyId != null) {
            dest.writeInt(1);
            dest.writeLong(mSignatureSubKeyId);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(mSignatureHashAlgorithm);
        dest.writeString(mSignaturePassphrase);
        dest.writeLong(mAdditionalEncryptId);
        dest.writeByteArray(mNfcSignedHash);
        if (mNfcCreationTimestamp != null) {
            dest.writeInt(1);
            dest.writeLong(mNfcCreationTimestamp.getTime());
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(mFailOnMissingEncryptionKeyIds ? 1 : 0);
        dest.writeString(mCharset);
        dest.writeInt(mCleartextSignature ? 1 : 0);
        dest.writeInt(mDetachedSignature ? 1 : 0);

        dest.writeTypedList(mInputUris);
        dest.writeTypedList(mOutputUris);
        dest.writeByteArray(mBytes);
    }

    public static final Creator<SignEncryptParcel> CREATOR = new Creator<SignEncryptParcel>() {
        public SignEncryptParcel createFromParcel(final Parcel source) {
            return new SignEncryptParcel(source);
        }

        public SignEncryptParcel[] newArray(final int size) {
            return new SignEncryptParcel[size];
        }
    };

}
