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

package org.sufficientlysecure.keychain.pgp;


import android.net.Uri;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;


@AutoValue
public abstract class PgpSignEncryptInputParcel implements Parcelable {
    public abstract PgpSignEncryptData getData();
    @Nullable
    public abstract Uri getOutputUri();
    @Nullable
    public abstract Uri getInputUri();
    @Nullable
    @SuppressWarnings("mutable")
    public abstract byte[] getInputBytes();

    public static PgpSignEncryptInputParcel createForBytes(
            PgpSignEncryptData signEncryptData, Uri outputUri, byte[] inputBytes) {
        return new AutoValue_PgpSignEncryptInputParcel(signEncryptData, outputUri, null, inputBytes);
    }

    public static PgpSignEncryptInputParcel createForInputUri(
            PgpSignEncryptData signEncryptData, Uri outputUri, Uri inputUri) {
        return new AutoValue_PgpSignEncryptInputParcel(signEncryptData, outputUri, inputUri, null);
    }
}

