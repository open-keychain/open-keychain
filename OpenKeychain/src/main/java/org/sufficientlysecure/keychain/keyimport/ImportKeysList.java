/*
 * Copyright (C) 2014 Tim Bray <tbray@textuality.com>
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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Just an ArrayList, only with a synchronized dupe-merging add/addAll, and a sign-off method
 */
public class ImportKeysList extends ArrayList<ImportKeysListEntry> {

    private int mSupplierCount;

    public ImportKeysList(int supplierCount) {
        mSupplierCount = supplierCount;
    }

    @Override
    public boolean add(ImportKeysListEntry toAdd) {
        addOrMerge(toAdd);
        return true; // that’s what the ArrayList#add contract says
    }

    @Override
    public boolean addAll(Collection<? extends ImportKeysListEntry> addThese) {
        boolean modified = false;
        for (ImportKeysListEntry toAdd : addThese) {
            modified = addOrMerge(toAdd) || modified;
        }
        return modified;
    }

    // NOTE: side-effects
    // NOTE: synchronized
    private synchronized boolean addOrMerge(ImportKeysListEntry toAdd) {
        for (ImportKeysListEntry existing : this) {
            if (toAdd.hasSameKeyAs(existing)) {
                return mergeDupes(toAdd, existing);
            }
        }
        return super.add(toAdd);
    }

    // being a little anal about the ArrayList#addAll contract here
    private boolean mergeDupes(ImportKeysListEntry incoming, ImportKeysListEntry existing) {
        boolean modified = false;

        // if any source thinks it’s expired/revoked, it is
        if (incoming.isRevoked()) {
            existing.setRevoked(true);
            modified = true;
        }
        if (incoming.isExpired()) {
            existing.setExpired(true);
            modified = true;
        }

        // keep track if this key result is from a HKP keyserver
        boolean incomingFromHkpServer = true;
        // we’re going to want to try to fetch the key from everywhere we found it, so remember
        //  all the origins
        for (String origin : incoming.getOrigins()) {
            existing.addOrigin(origin);

            // to work properly, Keybase-sourced entries need to pass along the extra
            if (KeybaseKeyserver.ORIGIN.equals(origin)) {
                existing.setExtraData(incoming.getExtraData());
                // one of the origins is not a HKP keyserver
                incomingFromHkpServer = false;
            }
        }

        ArrayList<String> incomingIDs = incoming.getUserIds();
        ArrayList<String> existingIDs = existing.getUserIds();
        for (String incomingID : incomingIDs) {
            if (!existingIDs.contains(incomingID)) {
                // prepend  HKP server results to the start of the list,
                // so that the UI (for cloud key search, which is picking the first list item)
                // shows the right main email address, as mail addresses returned by HKP servers
                // are preferred over keybase.io IDs
                if (incomingFromHkpServer) {
                    existingIDs.add(0, incomingID);
                } else {
                    existingIDs.add(incomingID);
                }
                modified = true;
            }
        }
        existing.updateMergedUserIds();
        return modified;
    }

    // NOTE: synchronized
    public synchronized void finishedAdding() {
        mSupplierCount--;
        if (mSupplierCount == 0) {
            this.notify();
        }
    }

    public int outstandingSuppliers() {
        return mSupplierCount;
    }
}
