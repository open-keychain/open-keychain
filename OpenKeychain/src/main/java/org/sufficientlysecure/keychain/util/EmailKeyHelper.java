/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.Context;

import org.sufficientlysecure.keychain.keyimport.HkpKeyserver;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.Keyserver;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EmailKeyHelper {
    // to import keys, simply use CryptoOperationHelper with this callback
    public abstract class ImportContactKeysCallback
            implements CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> {

        private ArrayList<ParcelableKeyRing> mKeyList;
        private String mKeyserver;

        public ImportContactKeysCallback(Context context, String keyserver) {
            this(context, ContactHelper.getContactMails(context), keyserver);
        }

        public ImportContactKeysCallback(Context context, List<String> mails, String keyserver) {
            Set<ImportKeysListEntry> entries = new HashSet<>();
            for (String mail : mails) {
                entries.addAll(getEmailKeys(context, mail));
            }

            // Put them in a list and import
            ArrayList<ParcelableKeyRing> keys = new ArrayList<>(entries.size());
            for (ImportKeysListEntry entry : entries) {
                keys.add(new ParcelableKeyRing(entry.getFingerprintHex(), entry.getKeyIdHex(), null));
            }
            mKeyList = keys;
            mKeyserver = keyserver;
        }
        @Override
        public ImportKeyringParcel createOperationInput() {
            return new ImportKeyringParcel(mKeyList, mKeyserver);
        }
    }

    public static Set<ImportKeysListEntry> getEmailKeys(Context context, String mail) {
        Set<ImportKeysListEntry> keys = new HashSet<>();

        // Try _hkp._tcp SRV record first
        String[] mailparts = mail.split("@");
        if (mailparts.length == 2) {
            HkpKeyserver hkp = HkpKeyserver.resolve(mailparts[1]);
            if (hkp != null) {
                keys.addAll(getEmailKeys(mail, hkp));
            }
        }

        if (keys.isEmpty()) {
            // Most users don't have the SRV record, so ask a default server as well
            String server = Preferences.getPreferences(context).getPreferredKeyserver();
            if (server != null) {
                HkpKeyserver hkp = new HkpKeyserver(server);
                keys.addAll(getEmailKeys(mail, hkp));
            }
        }
        return keys;
    }

    public static List<ImportKeysListEntry> getEmailKeys(String mail, Keyserver keyServer) {
        Set<ImportKeysListEntry> keys = new HashSet<>();
        try {
            for (ImportKeysListEntry key : keyServer.search(mail)) {
                if (key.isRevoked() || key.isExpired()) continue;
                for (String userId : key.getUserIds()) {
                    if (userId.toLowerCase().contains(mail.toLowerCase(Locale.ENGLISH))) {
                        keys.add(key);
                    }
                }
            }
        } catch (Keyserver.QueryFailedException ignored) {
        } catch (Keyserver.QueryNeedsRepairException ignored) {
        }
        return new ArrayList<>(keys);
    }
}
