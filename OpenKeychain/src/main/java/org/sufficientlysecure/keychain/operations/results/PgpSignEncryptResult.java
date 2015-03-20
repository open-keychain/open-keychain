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

package org.sufficientlysecure.keychain.operations.results;

import android.os.Parcel;

import org.sufficientlysecure.keychain.util.Passphrase;

import java.util.Date;

public class PgpSignEncryptResult extends OperationResult {

    // the fourth bit indicates a "data pending" result! (it's also a form of non-success)
    public static final int RESULT_PENDING = RESULT_ERROR + 8;

    // fifth to sixth bit in addition indicate specific type of pending
    public static final int RESULT_PENDING_PASSPHRASE = RESULT_PENDING + 16;
    public static final int RESULT_PENDING_NFC = RESULT_PENDING + 32;

    long mKeyIdPassphraseNeeded;

    long mNfcKeyId;
    byte[] mNfcHash;
    int mNfcAlgo;
    Passphrase mNfcPassphrase;
    byte[] mDetachedSignature;

    public long getKeyIdPassphraseNeeded() {
        return mKeyIdPassphraseNeeded;
    }

    public void setKeyIdPassphraseNeeded(long keyIdPassphraseNeeded) {
        mKeyIdPassphraseNeeded = keyIdPassphraseNeeded;
    }

    public void setNfcData(long nfcKeyId, byte[] nfcHash, int nfcAlgo, Passphrase passphrase) {
        mNfcKeyId = nfcKeyId;
        mNfcHash = nfcHash;
        mNfcAlgo = nfcAlgo;
        mNfcPassphrase = passphrase;
    }

    public void setDetachedSignature(byte[] detachedSignature) {
        mDetachedSignature = detachedSignature;
    }

    public long getNfcKeyId() {
        return mNfcKeyId;
    }

    public byte[] getNfcHash() {
        return mNfcHash;
    }

    public int getNfcAlgo() {
        return mNfcAlgo;
    }

    public Passphrase getNfcPassphrase() {
        return mNfcPassphrase;
    }

    public byte[] getDetachedSignature() {
        return mDetachedSignature;
    }

    public boolean isPending() {
        return (mResult & RESULT_PENDING) == RESULT_PENDING;
    }

    public PgpSignEncryptResult(int result, OperationLog log) {
        super(result, log);
    }

    public PgpSignEncryptResult(Parcel source) {
        super(source);
        mNfcHash = source.readInt() != 0 ? source.createByteArray() : null;
        mNfcAlgo = source.readInt();
        mDetachedSignature = source.readInt() != 0 ? source.createByteArray() : null;
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
        if (mDetachedSignature != null) {
            dest.writeInt(1);
            dest.writeByteArray(mDetachedSignature);
        } else {
            dest.writeInt(0);
        }
    }

    public static final Creator<PgpSignEncryptResult> CREATOR = new Creator<PgpSignEncryptResult>() {
        public PgpSignEncryptResult createFromParcel(final Parcel source) {
            return new PgpSignEncryptResult(source);
        }

        public PgpSignEncryptResult[] newArray(final int size) {
            return new PgpSignEncryptResult[size];
        }
    };

}
