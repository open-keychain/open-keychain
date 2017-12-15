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

package org.sufficientlysecure.keychain.util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.content.Context;

import de.measite.minidns.Client;
import de.measite.minidns.Question;
import de.measite.minidns.Record;
import de.measite.minidns.record.SRV;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverAddress;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverClient;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.KeyserverClient;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;

public class EmailKeyHelper {
    // TODO: Make this not require a proxy in it's constructor, redesign when it is to be used
    // to import keys, simply use CryptoOperationHelper with this callback
    public abstract class ImportContactKeysCallback
            implements CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> {

        private ArrayList<ParcelableKeyRing> mKeyList;
        private HkpKeyserverAddress mKeyserver;

        public ImportContactKeysCallback(Context context, HkpKeyserverAddress keyserver,
                                         ParcelableProxy proxy) {
            this(context, new ContactHelper(context).getContactMails(), keyserver, proxy);
        }

        public ImportContactKeysCallback(Context context, List<String> mails,
                                         HkpKeyserverAddress keyserver, ParcelableProxy proxy) {
            Set<ImportKeysListEntry> entries = new HashSet<>();
            for (String mail : mails) {
                entries.addAll(getEmailKeys(context, mail, proxy));
            }

            // Put them in a list and import
            ArrayList<ParcelableKeyRing> keys = new ArrayList<>(entries.size());
            for (ImportKeysListEntry entry : entries) {
                keys.add(ParcelableKeyRing.createFromReference(entry.getFingerprint(), entry.getKeyIdHex(), null, null));
            }
            mKeyList = keys;
            mKeyserver = keyserver;
        }

        @Override
        public ImportKeyringParcel createOperationInput() {
            return ImportKeyringParcel.createImportKeyringParcel(mKeyList, mKeyserver);
        }
    }

    public static Set<ImportKeysListEntry> getEmailKeys(Context context, String mail,
                                                        ParcelableProxy proxy) {
        Set<ImportKeysListEntry> keys = new HashSet<>();

        // Try _hkp._tcp SRV record first
        String[] mailparts = mail.split("@");
        if (mailparts.length == 2) {
            HkpKeyserverAddress hkp = findKeyserverFromDns(mailparts[1]);
            if (hkp != null) {
                keys.addAll(getEmailKeys(mail, hkp, proxy));
            }
        }

        if (keys.isEmpty()) {
            // Most users don't have the SRV record, so ask a default server as well
            HkpKeyserverAddress server = Preferences.getPreferences(context).getPreferredKeyserver();
            if (server != null) {
                keys.addAll(getEmailKeys(mail, server, proxy));
            }
        }
        return keys;
    }

    public static List<ImportKeysListEntry> getEmailKeys(String mail, HkpKeyserverAddress keyServer,
                                                         ParcelableProxy proxy) {
        Set<ImportKeysListEntry> keys = new HashSet<>();
        try {
            for (ImportKeysListEntry key : HkpKeyserverClient
                    .fromHkpKeyserverAddress(keyServer).search(mail, proxy)) {
                if (key.isRevokedOrExpiredOrInsecure()) continue;
                for (String userId : key.getUserIds()) {
                    if (userId.toLowerCase().contains(mail.toLowerCase(Locale.ENGLISH))) {
                        keys.add(key);
                    }
                }
            }
        } catch (KeyserverClient.CloudSearchFailureException ignored) {
        }
        return new ArrayList<>(keys);
    }

    public static HkpKeyserverAddress findKeyserverFromDns(String domain) {
        try {
            Record[] records = new Client().query(new Question("_hkp._tcp." + domain, Record.TYPE.SRV)).getAnswers();
            if (records.length > 0) {
                Arrays.sort(records, new Comparator<Record>() {
                    @Override
                    public int compare(Record lhs, Record rhs) {
                        if (lhs.getPayload().getType() != Record.TYPE.SRV) return 1;
                        if (rhs.getPayload().getType() != Record.TYPE.SRV) return -1;
                        return ((SRV) lhs.getPayload()).getPriority() - ((SRV) rhs.getPayload()).getPriority();
                    }
                });
                Record record = records[0]; // This is our best choice
                if (record.getPayload().getType() == Record.TYPE.SRV) {
                    SRV payload = (SRV) record.getPayload();
                    return HkpKeyserverAddress.createFromUri(payload.getName() + ":" + payload.getPort());
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
