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
    boolean symmetricPassphraseNeeded;
    boolean keyPassphraseNeeded;
    OpenPgpSignatureResult signatureResult;

    public boolean isSymmetricPassphraseNeeded() {
        return symmetricPassphraseNeeded;
    }

    public void setSymmetricPassphraseNeeded(boolean symmetricPassphraseNeeded) {
        this.symmetricPassphraseNeeded = symmetricPassphraseNeeded;
    }

    public boolean isKeyPassphraseNeeded() {
        return keyPassphraseNeeded;
    }

    public void setKeyPassphraseNeeded(boolean keyPassphraseNeeded) {
        this.keyPassphraseNeeded = keyPassphraseNeeded;
    }

    public OpenPgpSignatureResult getSignatureResult() {
        return signatureResult;
    }

    public void setSignatureResult(OpenPgpSignatureResult signatureResult) {
        this.signatureResult = signatureResult;
    }

    public PgpDecryptVerifyResult() {

    }

    public PgpDecryptVerifyResult(PgpDecryptVerifyResult b) {
        this.symmetricPassphraseNeeded = b.symmetricPassphraseNeeded;
        this.keyPassphraseNeeded = b.keyPassphraseNeeded;
        this.signatureResult = b.signatureResult;
    }


    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (symmetricPassphraseNeeded ? 1 : 0));
        dest.writeByte((byte) (keyPassphraseNeeded ? 1 : 0));
        dest.writeParcelable(signatureResult, 0);
    }

    public static final Creator<PgpDecryptVerifyResult> CREATOR = new Creator<PgpDecryptVerifyResult>() {
        public PgpDecryptVerifyResult createFromParcel(final Parcel source) {
            PgpDecryptVerifyResult vr = new PgpDecryptVerifyResult();
            vr.symmetricPassphraseNeeded = source.readByte() == 1;
            vr.keyPassphraseNeeded = source.readByte() == 1;
            vr.signatureResult = source.readParcelable(OpenPgpSignatureResult.class.getClassLoader());
            return vr;
        }

        public PgpDecryptVerifyResult[] newArray(final int size) {
            return new PgpDecryptVerifyResult[size];
        }
    };
}
