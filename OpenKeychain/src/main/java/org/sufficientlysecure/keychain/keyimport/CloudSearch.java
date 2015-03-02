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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Search two or more types of server for online keys.
 */
public class CloudSearch {

    private final static long SECONDS = 1000;

    public static ArrayList<ImportKeysListEntry> search(final String query, Preferences.CloudSearchPrefs cloudPrefs)
            throws Keyserver.CloudSearchFailureException {
        final ArrayList<Keyserver> servers = new ArrayList<>();

        // it's a Vector for sync, multiple threads might report problems
        final Vector<Keyserver.CloudSearchFailureException> problems = new Vector<>();

        if (cloudPrefs.searchKeyserver) {
            servers.add(new HkpKeyserver(cloudPrefs.keyserver));
        }
        if (cloudPrefs.searchKeybase) {
            servers.add(new KeybaseKeyserver());
        }
        final ImportKeysList results = new ImportKeysList(servers.size());

        for (final Keyserver keyserver : servers) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        results.addAll(keyserver.search(query));
                    } catch (Keyserver.CloudSearchFailureException e) {
                        problems.add(e);
                    }
                    results.finishedAdding(); // notifies if all searchers done
                }
            };
            new Thread(r).start();
        }

        // wait for either all the searches to come back, or 10 seconds
        synchronized(results) {
            try {
                results.wait(10 * SECONDS);
            } catch (InterruptedException e) {
            }
        }

        if (results.outstandingSuppliers() > 0) {
            String message =  "Launched " + servers.size() + " cloud searchers, but" +
                    results.outstandingSuppliers() + "failed to complete.";
            problems.add(new Keyserver.QueryFailedException(message));
        }

        if (!problems.isEmpty()) {
            for (Keyserver.CloudSearchFailureException e : problems) {
                Log.d(Constants.TAG, "Cloud search exception: " + e.getLocalizedMessage());
            }

            // only throw exception if we didn’t get any results
            if (results.isEmpty()) {
                throw problems.get(0);
            }
        }

        return results;
    }
}
