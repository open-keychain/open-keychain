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

package org.sufficientlysecure.keychain.keyimport;


import android.os.Parcelable;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;


/**
 * This class is a parcelable representation of either a keyring as raw data,
 * or a (unique) reference to one as a fingerprint, or keyid.
 */
@AutoValue
public abstract class ParcelableKeyRing implements Parcelable {
    @Nullable
    @SuppressWarnings("mutable")
    public abstract byte[] getBytes();

    // dual role!
    @Nullable
    @SuppressWarnings("mutable")
    public abstract byte[] getExpectedFingerprint();
    @Nullable
    public abstract String getKeyIdHex();
    @Nullable
    public abstract String getFbUsername();

    public static ParcelableKeyRing createFromEncodedBytes(byte[] bytes) {
        return new AutoValue_ParcelableKeyRing(bytes, null, null, null);
    }

    public static ParcelableKeyRing createFromReference(
            byte[] expectedFingerprint, String keyIdHex, String fbUsername) {
        return new AutoValue_ParcelableKeyRing(null, expectedFingerprint, keyIdHex, fbUsername);
    }
}
