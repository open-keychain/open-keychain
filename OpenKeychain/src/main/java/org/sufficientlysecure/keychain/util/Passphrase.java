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

    public Passphrase(String passphrase) {
        mPassphrase = passphrase.toCharArray();
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
                    '}';
        } else {
            return "Passphrase: hidden";
        }
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
        if (!Arrays.equals(mPassphrase, that.mPassphrase)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return mPassphrase != null ? Arrays.hashCode(mPassphrase) : 0;
    }

    private Passphrase(Parcel source) {
        mPassphrase = source.createCharArray();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeCharArray(mPassphrase);
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
