/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

import org.bouncycastle.bcpg.S2K;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.ParcelableS2K;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;


/**
 * This class wraps a char[] array that is overwritten before the object is freed, to avoid
 * keeping passphrases in memory as much as possible.
 *
 * In addition to the raw passphrases, this class can cache the session key output of an applied
 * S2K algorithm for a given set of S2K parameters. Since S2K operations are very expensive, this
 * mechanism should be used to cache session keys whenever possible.
 *
 * See also:
 * <p/>
 * http://docs.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html#PBEEx
 * https://github.com/c-a-m/passfault/blob/master/core/src/main/java/org/owasp/passfault/SecureString.java
 * http://stackoverflow.com/q/8881291
 * http://stackoverflow.com/a/15844273
 */
public class Passphrase implements Parcelable {
    private char[] mPassphrase;
    private HashMap<ParcelableS2K, byte[]> mCachedSessionKeys;

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

    /** @return A cached session key, or null if none exists for the given parameters. */
    public byte[] getCachedSessionKeyForParameters(int keyEncryptionAlgorithm, S2K s2k) {
        if (mCachedSessionKeys == null) {
            return null;
        }
        return mCachedSessionKeys.get(ParcelableS2K.fromS2K(keyEncryptionAlgorithm, s2k));
    }

    /** Adds a session key for a set of s2k parameters to this Passphrase object's
     * cache. The caller should make sure that the supplied session key is the result
     * of an S2K operation applied to exactly the passphrase stored by this object
     * with the given parameters.
     */
    public void addCachedSessionKeyForParameters(int keyEncryptionAlgorithm, S2K s2k, byte[] sessionKey) {
        if (mCachedSessionKeys == null) {
            mCachedSessionKeys = new HashMap<>();
        }
        mCachedSessionKeys.put(ParcelableS2K.fromS2K(keyEncryptionAlgorithm, s2k), sessionKey);
    }

    /**
     * Manually clear the underlying array holding the characters
     */
    public void removeFromMemory() {
        if (mPassphrase != null) {
            Arrays.fill(mPassphrase, ' ');
        }
        if (mCachedSessionKeys == null) {
            return;
        }
        for (byte[] cachedSessionKey : mCachedSessionKeys.values()) {
            Arrays.fill(cachedSessionKey, (byte) 0);
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

    private Passphrase(Parcel source) {
        mPassphrase = source.createCharArray();
        int size = source.readInt();
        if (size == 0) {
            return;
        }
        mCachedSessionKeys = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            ParcelableS2K cachedS2K = source.readParcelable(getClass().getClassLoader());
            byte[] cachedSessionKey = source.createByteArray();
            mCachedSessionKeys.put(cachedS2K, cachedSessionKey);
        }
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeCharArray(mPassphrase);
        if (mCachedSessionKeys == null || mCachedSessionKeys.isEmpty()) {
            dest.writeInt(0);
            return;
        }
        dest.writeInt(mCachedSessionKeys.size());
        for (Entry<ParcelableS2K,byte[]> entry : mCachedSessionKeys.entrySet()) {
            dest.writeParcelable(entry.getKey(), 0);
            dest.writeByteArray(entry.getValue());
        }
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
