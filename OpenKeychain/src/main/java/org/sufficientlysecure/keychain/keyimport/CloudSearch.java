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

import java.net.Proxy;
import java.util.ArrayList;
import java.util.Vector;

import android.support.annotation.NonNull;


/**
 * Search two or more types of server for online keys.
 */
public class CloudSearch {

    private final static long SECONDS = 1000;

    public static ArrayList<ImportKeysListEntry> search(
            @NonNull final String query, Preferences.CloudSearchPrefs cloudPrefs, @NonNull Proxy proxy)
            throws Keyserver.CloudSearchFailureException {
        final ArrayList<Keyserver> servers = new ArrayList<>();

        // it's a Vector for sync, multiple threads might report problems
        final Vector<Keyserver.CloudSearchFailureException> problems = new Vector<>();

        if (cloudPrefs.searchKeyserver) {
            servers.add(new HkpKeyserver(cloudPrefs.keyserver, proxy));
        }
        if (cloudPrefs.searchKeybase) {
            servers.add(new KeybaseKeyserver(proxy));
        }
        final ImportKeysList results = new ImportKeysList(servers.size());

        ArrayList<Thread> searchThreads = new ArrayList<>();
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
            Thread searchThread = new Thread(r);
            searchThreads.add(searchThread);
            searchThread.start();
        }

        // wait for either all the searches to come back, or 10 seconds. If using proxy, wait 30 seconds.
        synchronized (results) {
            try {
                if (proxy == Proxy.NO_PROXY) {
                    results.wait(30 * SECONDS);
                } else {
                    results.wait(10 * SECONDS);
                }
                for (Thread thread : searchThreads) {
                    // kill threads that haven't returned yet
                    thread.interrupt();
		}
            } catch (InterruptedException ignored) {
            }
        }

        if (results.outstandingSuppliers() > 0) {
            String message = "Launched " + servers.size() + " cloud searchers, but " +
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
