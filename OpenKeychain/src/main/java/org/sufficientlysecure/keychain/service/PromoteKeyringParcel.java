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


import java.util.List;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;


@AutoValue
public abstract class PromoteKeyringParcel implements Parcelable {
    public abstract long getMasterKeyId();
    @Nullable
    @SuppressWarnings("mutable")
    public abstract byte[] getCardAid();
    @Nullable
    @SuppressWarnings("mutable")
    public abstract List<byte[]> getFingerprints();

    public static PromoteKeyringParcel createPromoteKeyringParcel(long keyRingId, byte[] cardAid,
            @Nullable List<byte[]> fingerprints) {
        return new AutoValue_PromoteKeyringParcel(keyRingId, cardAid, fingerprints);
    }
}