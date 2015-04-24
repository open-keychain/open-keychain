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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * If Parcelables are above 1 MB, Android OS fails to send them via the Binder IPC:
 * JavaBinder  E  !!! FAILED BINDER TRANSACTION !!!
 * To overcome this issue this class allows to cache Parcelables, mapped by unique UUIDs,
 * which are written to the parcel instead of the whole Parcelable.
 */
public class ParcelableCache<E> {

    private static final UUID NULL_UUID = new UUID(0, 0);

    /**
     * A HashMap of UUID:Object
     * This is used such that when we become parceled, we are
     * well below the 1 MB boundary that is specified.
     */
    private ConcurrentHashMap<UUID, E> objectCache = new ConcurrentHashMap<>();

    /**
     * Dehydrate a Parcelable (such that it is available after deparcelization)
     * Returns the NULL uuid (0) if you hand it null.
     *
     * @param parcelable A Parcelable to dehydrate
     * @return a UUID, the ticket for your dehydrated Parcelable
     */
    private UUID dehydrateParcelable(E parcelable) {
        if (parcelable == null) {
            return NULL_UUID;
        } else {
            UUID uuid = UUID.randomUUID();
            objectCache.put(uuid, parcelable);
            return uuid;
        }
    }

    /**
     * Rehydrate a Parcelable after going through parcelization,
     * invalidating its place in the dehydration pool.
     * This is used such that when parcelized, the Parcelable is no larger than 1 MB.
     *
     * @param uuid A UUID ticket that identifies the log in question.
     * @return An OperationLog.
     */
    private E rehydrateParcelable(UUID uuid) {
        // UUID.equals isn't well documented; we use compareTo instead.
        if (NULL_UUID.compareTo(uuid) == 0) {
            return null;
        } else {
            E parcelable = objectCache.get(uuid);
            objectCache.remove(uuid);
            return parcelable;
        }
    }

    public E readFromParcelAndGetFromCache(Parcel source) {
        long mostSig = source.readLong();
        long leastSig = source.readLong();
        UUID mTicket = new UUID(mostSig, leastSig);
        // fetch the dehydrated parcelable out of storage (this removes it from the dehydration pool)
        return rehydrateParcelable(mTicket);
    }

    public void cacheAndWriteToParcel(E parcelable, Parcel dest) {
        // Get a ticket for our parcelable.
        UUID mTicket = dehydrateParcelable(parcelable);
        // And write out the UUID most and least significant bits.
        dest.writeLong(mTicket.getMostSignificantBits());
        dest.writeLong(mTicket.getLeastSignificantBits());
    }

}
