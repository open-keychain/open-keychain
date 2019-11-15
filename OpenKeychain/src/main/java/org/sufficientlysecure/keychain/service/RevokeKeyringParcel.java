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

package org.sufficientlysecure.keychain.service;


import android.os.Parcelable;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverAddress;


@AutoValue
public abstract class RevokeKeyringParcel implements Parcelable {
    public abstract long getMasterKeyId();
    public abstract boolean isShouldUpload();
    @Nullable
    public abstract HkpKeyserverAddress getKeyserver();

    public static RevokeKeyringParcel createRevokeKeyringParcel(long masterKeyId, boolean upload,
            HkpKeyserverAddress keyserver) {
        return new AutoValue_RevokeKeyringParcel(masterKeyId, upload, keyserver);
    }
}