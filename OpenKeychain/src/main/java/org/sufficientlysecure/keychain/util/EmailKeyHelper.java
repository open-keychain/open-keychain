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

import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.Keyserver;
import org.sufficientlysecure.keychain.keyimport.ParcelableHkpKeyserver;
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
    // TODO: Make this not require a proxy in it's constructor, redesign when it is to be used
    // to import keys, simply use CryptoOperationHelper with this callback
    public abstract class ImportContactKeysCallback
            implements CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> {

        private ArrayList<ParcelableKeyRing> mKeyList;
        private ParcelableHkpKeyserver mKeyserver;

        public ImportContactKeysCallback(Context context, ParcelableHkpKeyserver keyserver,
                                         ParcelableProxy proxy) {
            this(context, new ContactHelper(context).getContactMails(), keyserver, proxy);
        }

        public ImportContactKeysCallback(Context context, List<String> mails,
                                         ParcelableHkpKeyserver keyserver, ParcelableProxy proxy) {
            Set<ImportKeysListEntry> entries = new HashSet<>();
            for (String mail : mails) {
                entries.addAll(getEmailKeys(context, mail, proxy));
            }

            // Put them in a list and import
            ArrayList<ParcelableKeyRing> keys = new ArrayList<>(entries.size());
            for (ImportKeysListEntry entry : entries) {
                keys.add(new ParcelableKeyRing(entry.getFingerprintHex(), entry.getKeyIdHex(), null,
                        null));
            }
            mKeyList = keys;
            mKeyserver = keyserver;
        }

        @Override
        public ImportKeyringParcel createOperationInput() {
            return new ImportKeyringParcel(mKeyList, mKeyserver);
        }
    }

    public static Set<ImportKeysListEntry> getEmailKeys(Context context, String mail,
                                                        ParcelableProxy proxy) {
        Set<ImportKeysListEntry> keys = new HashSet<>();

        // Try _hkp._tcp SRV record first
        String[] mailparts = mail.split("@");
        if (mailparts.length == 2) {
            ParcelableHkpKeyserver hkp = ParcelableHkpKeyserver.resolve(mailparts[1]);
            if (hkp != null) {
                keys.addAll(getEmailKeys(mail, hkp, proxy));
            }
        }

        if (keys.isEmpty()) {
            // Most users don't have the SRV record, so ask a default server as well
            ParcelableHkpKeyserver server = Preferences.getPreferences(context).getPreferredKeyserver();
            if (server != null) {
                keys.addAll(getEmailKeys(mail, server, proxy));
            }
        }
        return keys;
    }

    public static List<ImportKeysListEntry> getEmailKeys(String mail, Keyserver keyServer,
                                                         ParcelableProxy proxy) {
        Set<ImportKeysListEntry> keys = new HashSet<>();
        try {
            for (ImportKeysListEntry key : keyServer.search(mail, proxy)) {
                if (key.isRevokedOrExpiredOrInsecure()) continue;
                for (String userId : key.getUserIds()) {
                    if (userId.toLowerCase().contains(mail.toLowerCase(Locale.ENGLISH))) {
                        keys.add(key);
                    }
                }
            }
        } catch (Keyserver.CloudSearchFailureException ignored) {
        }
        return new ArrayList<>(keys);
    }
}
