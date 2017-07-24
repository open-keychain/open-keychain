/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Adithya Abraham Philip <adithyaphilip@gmail.com>
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


import java.util.ArrayList;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverAddress;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;

@AutoValue
public abstract class ImportKeyringParcel implements Parcelable {
    @Nullable // If null, keys are expected to be read from a cache file in ImportExportOperations
    public abstract ArrayList<ParcelableKeyRing> getKeyList();
    @Nullable // must be set if keys are to be imported from a keyserver
    public abstract HkpKeyserverAddress getKeyserver();
    public abstract boolean isSkipSave();

    public static ImportKeyringParcel createImportKeyringParcel(ArrayList<ParcelableKeyRing> keyList,
            HkpKeyserverAddress keyserver) {
        return new AutoValue_ImportKeyringParcel(keyList, keyserver, false);
    }

    public static ImportKeyringParcel createWithSkipSave(ArrayList<ParcelableKeyRing> keyList,
            HkpKeyserverAddress keyserver) {
        return new AutoValue_ImportKeyringParcel(keyList, keyserver, true);
    }

    public static ImportKeyringParcel createFromFileCacheWithSkipSave() {
        return new AutoValue_ImportKeyringParcel(null, null, true);
    }

    public static ImportKeyringParcel createFromFileCache() {
        return new AutoValue_ImportKeyringParcel(null, null, false);
    }
}