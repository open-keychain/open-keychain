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
import org.openintents.openpgp.OpenPgpSignatureResult;

public class PgpDecryptVerifyResult implements Parcelable {
    boolean mSymmetricPassphraseNeeded;
    boolean mKeyPassphraseNeeded;
    OpenPgpSignatureResult mSignatureResult;

    public boolean isSymmetricPassphraseNeeded() {
        return mSymmetricPassphraseNeeded;
    }

    public void setSymmetricPassphraseNeeded(boolean symmetricPassphraseNeeded) {
        this.mSymmetricPassphraseNeeded = symmetricPassphraseNeeded;
    }

    public boolean isKeyPassphraseNeeded() {
        return mKeyPassphraseNeeded;
    }

    public void setKeyPassphraseNeeded(boolean keyPassphraseNeeded) {
        this.mKeyPassphraseNeeded = keyPassphraseNeeded;
    }

    public OpenPgpSignatureResult getSignatureResult() {
        return mSignatureResult;
    }

    public void setSignatureResult(OpenPgpSignatureResult signatureResult) {
        this.mSignatureResult = signatureResult;
    }

    public PgpDecryptVerifyResult() {

    }

    public PgpDecryptVerifyResult(PgpDecryptVerifyResult b) {
        this.mSymmetricPassphraseNeeded = b.mSymmetricPassphraseNeeded;
        this.mKeyPassphraseNeeded = b.mKeyPassphraseNeeded;
        this.mSignatureResult = b.mSignatureResult;
    }


    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mSymmetricPassphraseNeeded ? 1 : 0));
        dest.writeByte((byte) (mKeyPassphraseNeeded ? 1 : 0));
        dest.writeParcelable(mSignatureResult, 0);
    }

    public static final Creator<PgpDecryptVerifyResult> CREATOR = new Creator<PgpDecryptVerifyResult>() {
        public PgpDecryptVerifyResult createFromParcel(final Parcel source) {
            PgpDecryptVerifyResult vr = new PgpDecryptVerifyResult();
            vr.mSymmetricPassphraseNeeded = source.readByte() == 1;
            vr.mKeyPassphraseNeeded = source.readByte() == 1;
            vr.mSignatureResult = source.readParcelable(OpenPgpSignatureResult.class.getClassLoader());
            return vr;
        }

        public PgpDecryptVerifyResult[] newArray(final int size) {
            return new PgpDecryptVerifyResult[size];
        }
    };
}
