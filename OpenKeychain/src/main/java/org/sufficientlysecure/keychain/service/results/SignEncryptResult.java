/*
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

package org.sufficientlysecure.keychain.service.results;

import android.os.Parcel;

import java.util.Date;

public class SignEncryptResult extends OperationResult {

    // the fourth bit indicates a "data pending" result! (it's also a form of non-success)
    public static final int RESULT_PENDING = RESULT_ERROR + 8;

    // fifth to sixth bit in addition indicate specific type of pending
    public static final int RESULT_PENDING_PASSPHRASE = RESULT_PENDING + 16;
    public static final int RESULT_PENDING_NFC = RESULT_PENDING + 32;

    long mKeyIdPassphraseNeeded;

    byte[] mNfcHash;
    int mNfcAlgo;
    Date mNfcTimestamp;

    public long getKeyIdPassphraseNeeded() {
        return mKeyIdPassphraseNeeded;
    }

    public void setKeyIdPassphraseNeeded(long keyIdPassphraseNeeded) {
        mKeyIdPassphraseNeeded = keyIdPassphraseNeeded;
    }

    public void setNfcData(byte[] sessionKey, int nfcAlgo, Date nfcTimestamp) {
        mNfcHash = sessionKey;
        mNfcAlgo = nfcAlgo;
        mNfcTimestamp = nfcTimestamp;
    }

    public byte[] getNfcHash() {
        return mNfcHash;
    }

    public int getNfcAlgo() {
        return mNfcAlgo;
    }

    public Date getNfcTimestamp() {
        return mNfcTimestamp;
    }

    public boolean isPending() {
        return (mResult & RESULT_PENDING) == RESULT_PENDING;
    }

    public SignEncryptResult(int result, OperationLog log) {
        super(result, log);
    }

    public SignEncryptResult(Parcel source) {
        super(source);
        mNfcHash = source.readInt() != 0 ? source.createByteArray() : null;
        mNfcAlgo = source.readInt();
        mNfcTimestamp = source.readInt() != 0 ? new Date(source.readLong()) : null;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        if (mNfcHash != null) {
            dest.writeInt(1);
            dest.writeByteArray(mNfcHash);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(mNfcAlgo);
        if (mNfcTimestamp != null) {
            dest.writeInt(1);
            dest.writeLong(mNfcTimestamp.getTime());
        } else {
            dest.writeInt(0);
        }
    }

    public static final Creator<SignEncryptResult> CREATOR = new Creator<SignEncryptResult>() {
        public SignEncryptResult createFromParcel(final Parcel source) {
            return new SignEncryptResult(source);
        }

        public SignEncryptResult[] newArray(final int size) {
            return new SignEncryptResult[size];
        }
    };

}