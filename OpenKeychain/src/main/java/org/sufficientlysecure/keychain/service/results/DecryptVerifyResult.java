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

package org.sufficientlysecure.keychain.service.results;

import android.os.Parcel;
import android.os.Parcelable;

import org.openintents.openpgp.OpenPgpMetadata;
import org.openintents.openpgp.OpenPgpSignatureResult;

public class DecryptVerifyResult extends OperationResultParcel {

    // the fourth bit indicates a "data pending" result!
    public static final int RESULT_PENDING = 8;

    // fifth to sixth bit in addition indicate specific type of pending
    public static final int RESULT_PENDING_ASYM_PASSPHRASE = RESULT_PENDING +16;
    public static final int RESULT_PENDING_SYM_PASSPHRASE = RESULT_PENDING +32;
    public static final int RESULT_PENDING_NFC = RESULT_PENDING +48;

    long mKeyIdPassphraseNeeded;
    byte[] mSessionKey;

    OpenPgpSignatureResult mSignatureResult;
    OpenPgpMetadata mDecryptMetadata;

    public long getKeyIdPassphraseNeeded() {
        return mKeyIdPassphraseNeeded;
    }

    public void setKeyIdPassphraseNeeded(long keyIdPassphraseNeeded) {
        mKeyIdPassphraseNeeded = keyIdPassphraseNeeded;
    }

    public void setNfcEncryptedSessionKey(byte[] sessionKey) {
        mSessionKey = sessionKey;
    }

    public OpenPgpSignatureResult getSignatureResult() {
        return mSignatureResult;
    }

    public void setSignatureResult(OpenPgpSignatureResult signatureResult) {
        mSignatureResult = signatureResult;
    }

    public OpenPgpMetadata getDecryptMetadata() {
        return mDecryptMetadata;
    }

    public void setDecryptMetadata(OpenPgpMetadata decryptMetadata) {
        mDecryptMetadata = decryptMetadata;
    }

    public boolean isPending() {
        return (mResult & RESULT_PENDING) != 0;
    }

    public DecryptVerifyResult(int result, OperationLog log) {
        super(result, log);
    }

    public DecryptVerifyResult(Parcel source) {
        super(source);
        mKeyIdPassphraseNeeded = source.readLong();
        mSignatureResult = source.readParcelable(OpenPgpSignatureResult.class.getClassLoader());
        mDecryptMetadata = source.readParcelable(OpenPgpMetadata.class.getClassLoader());
        mSessionKey = source.readInt() != 0 ? source.createByteArray() : null;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(mKeyIdPassphraseNeeded);
        dest.writeParcelable(mSignatureResult, 0);
        dest.writeParcelable(mDecryptMetadata, 0);
        if (mSessionKey != null) {
            dest.writeInt(1);
            dest.writeByteArray(mSessionKey);
        } else {
            dest.writeInt(0);
        }
    }

    public static final Creator<DecryptVerifyResult> CREATOR = new Creator<DecryptVerifyResult>() {
        public DecryptVerifyResult createFromParcel(final Parcel source) {
            return new DecryptVerifyResult(source);
        }

        public DecryptVerifyResult[] newArray(final int size) {
            return new DecryptVerifyResult[size];
        }
    };

}
