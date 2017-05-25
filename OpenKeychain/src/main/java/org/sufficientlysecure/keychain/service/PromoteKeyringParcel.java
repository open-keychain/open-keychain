/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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
    public abstract long[] getSubKeyIds();

    public static PromoteKeyringParcel createPromoteKeyringParcel(long keyRingId, byte[] cardAid,
            @Nullable long[] subKeyIds) {
        return new AutoValue_PromoteKeyringParcel(keyRingId, cardAid, subKeyIds);
    }
}