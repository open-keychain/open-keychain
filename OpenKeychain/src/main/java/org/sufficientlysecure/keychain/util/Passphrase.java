/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.util;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.widget.EditText;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Passwords should not be stored as Strings in memory.
 * This class wraps a char[] that can be erased after it is no longer used.
 * See also:
 * <p/>
 * http://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html#PBEEx
 * https://github.com/c-a-m/passfault/blob/master/core/src/main/java/org/owasp/passfault/SecureString.java
 * http://stackoverflow.com/q/8881291
 * http://stackoverflow.com/a/15844273
 */
public class Passphrase implements Parcelable {
    private char[] mPassphrase;
    private CanonicalizedSecretKey.SecretKeyType mSecretKeyType = CanonicalizedSecretKey.
            SecretKeyType.PASSPHRASE;

    /**
     * According to http://stackoverflow.com/a/15844273 EditText is not using String internally
     * but char[]. Thus, we can get the char[] directly from it.
     */
    public Passphrase(Editable editable) {
        int pl = editable.length();
        mPassphrase = new char[pl];
        editable.getChars(0, pl, mPassphrase, 0);
        // TODO: clean up internal char[] of EditText after getting the passphrase?
//        editText.getText().replace()
    }

    public Passphrase(EditText editText) {
        this(editText.getText());
    }

    public Passphrase(char[] passphrase) {
        mPassphrase = passphrase;
    }

    public Passphrase(byte[] passphrase) throws UnsupportedEncodingException {
        mPassphrase = new String(passphrase, "ISO-8859-1").toCharArray();
    }

    public Passphrase(String passphrase) {
        mPassphrase = passphrase.toCharArray();
    }

    public CanonicalizedSecretKey.SecretKeyType getSecretKeyType() {
        return mSecretKeyType;
    }

    public void setSecretKeyType(CanonicalizedSecretKey.SecretKeyType secretKeyType) {
        mSecretKeyType = secretKeyType;
    }

    /**
     * Creates a passphrase object with an empty ("") passphrase
     */
    public Passphrase() {
        setEmpty();
    }

    public char[] getCharArray() {
        return mPassphrase;
    }

    public void setEmpty() {
        removeFromMemory();
        mPassphrase = new char[0];
    }

    public boolean isEmpty() {
        return (length() == 0);
    }

    public int length() {
        return mPassphrase.length;
    }

    public char charAt(int index) {
        return mPassphrase[index];
    }

    /**
     * Manually clear the underlying array holding the characters
     */
    public void removeFromMemory() {
        if (mPassphrase != null) {
            Arrays.fill(mPassphrase, ' ');
        }
    }

    @Override
    public void finalize() throws Throwable {
        removeFromMemory();
        super.finalize();
    }

    @Override
    public String toString() {
        if (Constants.DEBUG) {
            return "Passphrase{" +
                    "mPassphrase=" + Arrays.toString(mPassphrase) +
                    "mSecretKeyType=" + mSecretKeyType +
                    '}';
        } else {
            return "Passphrase: hidden";
        }
    }

    /**
     * Creates a new String from the char[]. This is considered unsafe!
     */
    public String toStringUnsafe() {
        return new String(mPassphrase);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Passphrase that = (Passphrase) o;
        return Arrays.equals(mPassphrase, that.mPassphrase);
    }

    @Override
    public int hashCode() {
        return mPassphrase != null ? Arrays.hashCode(mPassphrase) : 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeCharArray(mPassphrase);
        dest.writeInt(this.mSecretKeyType == null ? -1 : this.mSecretKeyType.ordinal());
    }

    protected Passphrase(Parcel in) {
        this.mPassphrase = in.createCharArray();
        int tmpMSecretKeyType = in.readInt();
        this.mSecretKeyType = tmpMSecretKeyType == -1 ? null :
                CanonicalizedSecretKey.SecretKeyType.values()[tmpMSecretKeyType];
    }

    public static final Creator<Passphrase> CREATOR = new Creator<Passphrase>() {
        public Passphrase createFromParcel(final Parcel source) {
            return new Passphrase(source);
        }

        public Passphrase[] newArray(final int size) {
            return new Passphrase[size];
        }
    };

    public int describeContents() {
        return 0;
    }
}
