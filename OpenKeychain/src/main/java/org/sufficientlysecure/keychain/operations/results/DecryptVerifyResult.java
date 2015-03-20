/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.operations.results;

import android.os.Parcel;

import org.openintents.openpgp.OpenPgpMetadata;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.util.Passphrase;

public class DecryptVerifyResult extends OperationResult {

    // the fourth bit indicates a "data pending" result! (it's also a form of non-success)
    public static final int RESULT_PENDING = RESULT_ERROR + 8;

    // fifth to sixth bit in addition indicate specific type of pending
    public static final int RESULT_PENDING_ASYM_PASSPHRASE = RESULT_PENDING + 16;
    public static final int RESULT_PENDING_SYM_PASSPHRASE = RESULT_PENDING + 32;
    public static final int RESULT_PENDING_NFC = RESULT_PENDING + 64;

    long mKeyIdPassphraseNeeded;

    long mNfcSubKeyId;
    byte[] mNfcSessionKey;
    Passphrase mNfcPassphrase;

    OpenPgpSignatureResult mSignatureResult;
    OpenPgpMetadata mDecryptMetadata;
    // This holds the charset which was specified in the ascii armor, if specified
    // https://tools.ietf.org/html/rfc4880#page56
    String mCharset;

    public long getKeyIdPassphraseNeeded() {
        return mKeyIdPassphraseNeeded;
    }

    public void setKeyIdPassphraseNeeded(long keyIdPassphraseNeeded) {
        mKeyIdPassphraseNeeded = keyIdPassphraseNeeded;
    }

    public void setNfcState(long subKeyId, byte[] sessionKey, Passphrase passphrase) {
        mNfcSubKeyId = subKeyId;
        mNfcSessionKey = sessionKey;
        mNfcPassphrase = passphrase;
    }

    public long getNfcSubKeyId() {
        return mNfcSubKeyId;
    }

    public byte[] getNfcEncryptedSessionKey() {
        return mNfcSessionKey;
    }

    public Passphrase getNfcPassphrase() {
        return mNfcPassphrase;
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

    public String getCharset () {
        return mCharset;
    }

    public void setCharset(String charset) {
        mCharset = charset;
    }

    public boolean isPending() {
        return (mResult & RESULT_PENDING) == RESULT_PENDING;
    }

    public DecryptVerifyResult(int result, OperationLog log) {
        super(result, log);
    }

    public DecryptVerifyResult(Parcel source) {
        super(source);
        mKeyIdPassphraseNeeded = source.readLong();
        mSignatureResult = source.readParcelable(OpenPgpSignatureResult.class.getClassLoader());
        mDecryptMetadata = source.readParcelable(OpenPgpMetadata.class.getClassLoader());
        mNfcSessionKey = source.readInt() != 0 ? source.createByteArray() : null;
        mNfcPassphrase = source.readParcelable(Passphrase.class.getClassLoader());
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(mKeyIdPassphraseNeeded);
        dest.writeParcelable(mSignatureResult, 0);
        dest.writeParcelable(mDecryptMetadata, 0);
        if (mNfcSessionKey != null) {
            dest.writeInt(1);
            dest.writeByteArray(mNfcSessionKey);
        } else {
            dest.writeInt(0);
        }
        dest.writeParcelable(mNfcPassphrase, flags);
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
