/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.pgp;


import java.util.HashSet;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class PgpDecryptVerifyInputParcel implements Parcelable {

    private Uri mInputUri;
    private Uri mOutputUri;
    private byte[] mInputBytes;

    private boolean mAllowSymmetricDecryption;
    private HashSet<Long> mAllowedKeyIds;
    private boolean mDecryptMetadataOnly;
    private byte[] mDetachedSignature;
    private String mRequiredSignerFingerprint;
    private String mSenderAddress;

    public PgpDecryptVerifyInputParcel() {
    }

    public PgpDecryptVerifyInputParcel(Uri inputUri, Uri outputUri) {
        mInputUri = inputUri;
        mOutputUri = outputUri;
    }

    public PgpDecryptVerifyInputParcel(byte[] inputBytes) {
        mInputBytes = inputBytes;
    }

    PgpDecryptVerifyInputParcel(Parcel source) {
        // we do all of those here, so the PgpSignEncryptInput class doesn't have to be parcelable
        mInputUri = source.readParcelable(getClass().getClassLoader());
        mOutputUri = source.readParcelable(getClass().getClassLoader());
        mInputBytes = source.createByteArray();

        mAllowSymmetricDecryption = source.readInt() != 0;
        mAllowedKeyIds  = (HashSet<Long>) source.readSerializable();
        mDecryptMetadataOnly = source.readInt() != 0;
        mDetachedSignature = source.createByteArray();
        mRequiredSignerFingerprint = source.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mInputUri, 0);
        dest.writeParcelable(mOutputUri, 0);
        dest.writeByteArray(mInputBytes);

        dest.writeInt(mAllowSymmetricDecryption ? 1 : 0);
        dest.writeSerializable(mAllowedKeyIds);
        dest.writeInt(mDecryptMetadataOnly ? 1 : 0);
        dest.writeByteArray(mDetachedSignature);
        dest.writeString(mRequiredSignerFingerprint);
    }

    byte[] getInputBytes() {
        return mInputBytes;
    }

    public PgpDecryptVerifyInputParcel setInputUri(Uri uri) {
        mInputUri = uri;
        return this;
    }

    Uri getInputUri() {
        return mInputUri;
    }

    public PgpDecryptVerifyInputParcel setOutputUri(Uri uri) {
        mOutputUri = uri;
        return this;
    }

    Uri getOutputUri() {
        return mOutputUri;
    }

    boolean isAllowSymmetricDecryption() {
        return mAllowSymmetricDecryption;
    }

    public PgpDecryptVerifyInputParcel setAllowSymmetricDecryption(boolean allowSymmetricDecryption) {
        mAllowSymmetricDecryption = allowSymmetricDecryption;
        return this;
    }

    HashSet<Long> getAllowedKeyIds() {
        return mAllowedKeyIds;
    }

    public PgpDecryptVerifyInputParcel setAllowedKeyIds(HashSet<Long> allowedKeyIds) {
        mAllowedKeyIds = allowedKeyIds;
        return this;
    }

    boolean isDecryptMetadataOnly() {
        return mDecryptMetadataOnly;
    }

    public PgpDecryptVerifyInputParcel setDecryptMetadataOnly(boolean decryptMetadataOnly) {
        mDecryptMetadataOnly = decryptMetadataOnly;
        return this;
    }

    byte[] getDetachedSignature() {
        return mDetachedSignature;
    }

    public PgpDecryptVerifyInputParcel setDetachedSignature(byte[] detachedSignature) {
        mDetachedSignature = detachedSignature;
        return this;
    }

    public PgpDecryptVerifyInputParcel setSenderAddress(String senderAddress) {
        mSenderAddress = senderAddress;
        return this;
    }

    public String getSenderAddress() {
        return mSenderAddress;
    }

    String getRequiredSignerFingerprint() {
        return mRequiredSignerFingerprint;
    }

    public PgpDecryptVerifyInputParcel setRequiredSignerFingerprint(String requiredSignerFingerprint) {
        mRequiredSignerFingerprint = requiredSignerFingerprint;
        return this;
    }

    public static final Creator<PgpDecryptVerifyInputParcel> CREATOR = new Creator<PgpDecryptVerifyInputParcel>() {
        public PgpDecryptVerifyInputParcel createFromParcel(final Parcel source) {
            return new PgpDecryptVerifyInputParcel(source);
        }

        public PgpDecryptVerifyInputParcel[] newArray(final int size) {
            return new PgpDecryptVerifyInputParcel[size];
        }
    };
}

