/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.os.Parcel;
import android.os.Parcelable;

import org.openintents.openpgp.OpenPgpDecryptMetadata;
import org.openintents.openpgp.OpenPgpSignatureResult;

public class PgpDecryptVerifyResult implements Parcelable {
    public static final int SUCCESS = 1;
    public static final int KEY_PASSHRASE_NEEDED = 2;
    public static final int SYMMETRIC_PASSHRASE_NEEDED = 3;

    int mStatus;
    long mKeyIdPassphraseNeeded;

    OpenPgpSignatureResult mSignatureResult;
    OpenPgpDecryptMetadata mDecryptMetadata;

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int status) {
        mStatus = status;
    }

    public long getKeyIdPassphraseNeeded() {
        return mKeyIdPassphraseNeeded;
    }

    public void setKeyIdPassphraseNeeded(long keyIdPassphraseNeeded) {
        mKeyIdPassphraseNeeded = keyIdPassphraseNeeded;
    }

    public OpenPgpSignatureResult getSignatureResult() {
        return mSignatureResult;
    }

    public void setSignatureResult(OpenPgpSignatureResult signatureResult) {
        mSignatureResult = signatureResult;
    }

    public OpenPgpDecryptMetadata getDecryptMetadata() {
        return mDecryptMetadata;
    }

    public void setDecryptMetadata(OpenPgpDecryptMetadata decryptMetadata) {
        mDecryptMetadata = decryptMetadata;
    }

    public PgpDecryptVerifyResult() {

    }

    public PgpDecryptVerifyResult(PgpDecryptVerifyResult b) {
        this.mStatus = b.mStatus;
        this.mKeyIdPassphraseNeeded = b.mKeyIdPassphraseNeeded;
        this.mSignatureResult = b.mSignatureResult;
        this.mDecryptMetadata = b.mDecryptMetadata;
    }


    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mStatus);
        dest.writeLong(mKeyIdPassphraseNeeded);
        dest.writeParcelable(mSignatureResult, 0);
        dest.writeParcelable(mDecryptMetadata, 0);
    }

    public static final Creator<PgpDecryptVerifyResult> CREATOR = new Creator<PgpDecryptVerifyResult>() {
        public PgpDecryptVerifyResult createFromParcel(final Parcel source) {
            PgpDecryptVerifyResult vr = new PgpDecryptVerifyResult();
            vr.mStatus = source.readInt();
            vr.mKeyIdPassphraseNeeded = source.readLong();
            vr.mSignatureResult = source.readParcelable(OpenPgpSignatureResult.class.getClassLoader());
            vr.mDecryptMetadata = source.readParcelable(OpenPgpDecryptMetadata.class.getClassLoader());
            return vr;
        }

        public PgpDecryptVerifyResult[] newArray(final int size) {
            return new PgpDecryptVerifyResult[size];
        }
    };
}
