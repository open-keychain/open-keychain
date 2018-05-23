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

package org.sufficientlysecure.keychain.keyimport;

import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Search two or more types of server for online keys.
 */
public class CloudSearch {

    private final static long SECONDS = 1000;

    public static ArrayList<ImportKeysListEntry> search(
            @NonNull final String query, Preferences.CloudSearchPrefs cloudPrefs, @NonNull final ParcelableProxy proxy)
            throws KeyserverClient.CloudSearchFailureException {

        final ArrayList<KeyserverClient> servers = new ArrayList<>();
        // it's a Vector for sync, multiple threads might report problems
        final Vector<KeyserverClient.CloudSearchFailureException> problems = new Vector<>();

        if (cloudPrefs.isKeyserverEnabled()) {
            servers.add(HkpKeyserverClient.fromHkpKeyserverAddress(cloudPrefs.getKeyserver()));
        }
        if (cloudPrefs.isKeybaseEnabled()) {
            servers.add(KeybaseKeyserverClient.getInstance());
        }
        if (cloudPrefs.isFacebookEnabled()) {
            servers.add(FacebookKeyserverClient.getInstance());
        }
        if (cloudPrefs.isWebKeyDirectoryEnabled()) {
            servers.add(WebKeyDirectoryClient.getInstance());
        }

        int numberOfServers = servers.size();
        final ImportKeysList results = new ImportKeysList(numberOfServers);

        if (numberOfServers > 0) {
            ArrayList<Thread> searchThreads = new ArrayList<>();
            for (final KeyserverClient keyserverClient : servers) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            results.addAll(keyserverClient.search(query, proxy));
                        } catch (KeyserverClient.CloudSearchFailureException e) {
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
                    results.wait((proxy.getProxy() == Proxy.NO_PROXY ? 30 : 10) * SECONDS);
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
                problems.add(new KeyserverClient.QueryFailedException(message));
            }
        } else {
            problems.add(new KeyserverClient.QueryNoEnabledSourceException());
        }

        if (!problems.isEmpty()) {
            for (KeyserverClient.CloudSearchFailureException e : problems) {
                Timber.d("Cloud search exception: " + e.getLocalizedMessage());
            }

            // only throw exception if we didn’t get any results
            if (results.isEmpty()) {
                throw problems.get(0);
            }
        }

        return results;
    }
}
