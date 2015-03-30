/*
 * Copyright (C) 2014 Daniel Albert
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

public class KeyUpdateHelper {

    /*

    public void updateAllKeys(Context context, KeychainIntentServiceHandler finishedHandler) {
        UpdateTask updateTask = new UpdateTask(context, finishedHandler);
        updateTask.execute();
    }

    private class UpdateTask extends AsyncTask<Void, Void, Void> {
        private Context mContext;
        private KeychainIntentServiceHandler mHandler;

        public UpdateTask(Context context, KeychainIntentServiceHandler handler) {
            this.mContext = context;
            this.mHandler = handler;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ProviderHelper providerHelper = new ProviderHelper(mContext);
            List<ImportKeysListEntry> keys = new ArrayList<ImportKeysListEntry>();
            String[] servers = Preferences.getPreferences(mContext).getKeyServers();

            if (servers != null && servers.length > 0) {
                // Load all the fingerprints in the database and prepare to import them
                for (String fprint : providerHelper.getAllFingerprints(KeychainContract.KeyRings.buildUnifiedKeyRingsUri())) {
                    ImportKeysListEntry key = new ImportKeysListEntry();
                    key.setFingerprintHex(fprint);
                    key.addOrigin(servers[0]);
                    keys.add(key);
                }

                // Start the service and update the keys
                Intent importIntent = new Intent(mContext, KeychainIntentService.class);
                importIntent.setAction(KeychainIntentService.ACTION_DOWNLOAD_AND_IMPORT_KEYS);

                Bundle importData = new Bundle();
                importData.putParcelableArrayList(KeychainIntentService.DOWNLOAD_KEY_LIST,
                        new ArrayList<ImportKeysListEntry>(keys));
                importIntent.putExtra(KeychainIntentService.EXTRA_SERVICE_INTENT, importData);

                importIntent.putExtra(KeychainIntentService.EXTRA_MESSENGER, new Messenger(mHandler));

                mContext.startService(importIntent);
            }
            return null;
        }
    }
    */

}
