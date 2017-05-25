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

package org.sufficientlysecure.keychain.service;


import android.net.Uri;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;


@AutoValue
public abstract class InputDataParcel implements Parcelable {
    public abstract Uri getInputUri();
    @Nullable
    public abstract PgpDecryptVerifyInputParcel getDecryptInput();
    public abstract boolean getMimeDecode(); // TODO static value - ditch this?

    public static InputDataParcel createInputDataParcel(Uri inputUri, PgpDecryptVerifyInputParcel decryptInput) {
        return new AutoValue_InputDataParcel(inputUri, decryptInput, true);
    }
}

