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

package org.sufficientlysecure.keychain.helper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Patterns;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactHelper {

    public static final String[] KEYS_TO_CONTACT_PROJECTION = new String[]{
            KeychainContract.KeyRings.USER_ID,
            KeychainContract.KeyRings.FINGERPRINT,
            KeychainContract.KeyRings.KEY_ID,
            KeychainContract.KeyRings.MASTER_KEY_ID};
    public static final String[] RAW_CONTACT_ID_PROJECTION = new String[]{ContactsContract.RawContacts._ID};
    public static final String FIND_RAW_CONTACT_SELECTION =
            ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND " + ContactsContract.RawContacts.SOURCE_ID + "=?";

    public static final List<String> getMailAccounts(Context context) {
        final Account[] accounts = AccountManager.get(context).getAccounts();
        final Set<String> emailSet = new HashSet<String>();
        for (Account account : accounts) {
            if (Patterns.EMAIL_ADDRESS.matcher(account.name).matches()) {
                emailSet.add(account.name);
            }
        }
        return new ArrayList<String>(emailSet);
    }

    public static List<String> getContactMails(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor mailCursor = resolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Email.DATA},
                null, null, null);
        if (mailCursor == null) return null;

        Set<String> mails = new HashSet<String>();
        while (mailCursor.moveToNext()) {
            String email = mailCursor.getString(0);
            if (email != null) {
                mails.add(email);
            }
        }
        mailCursor.close();
        return new ArrayList<String>(mails);
    }

    public static Uri dataUriFromContactUri(Context context, Uri contactUri) {
        Cursor contactMasterKey = context.getContentResolver().query(contactUri, new String[]{ContactsContract.Data.DATA2}, null, null, null, null);
        if (contactMasterKey != null) {
            if (contactMasterKey.moveToNext()) {
                return KeychainContract.KeyRings.buildGenericKeyRingUri(contactMasterKey.getLong(0));
            }
            contactMasterKey.close();
        }
        return null;
    }

    public static void writeKeysToContacts(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(KeychainContract.KeyRings.buildUnifiedKeyRingsUri(), KEYS_TO_CONTACT_PROJECTION,
                null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String[] userId = PgpKeyHelper.splitUserId(cursor.getString(0));
                String fingerprint = PgpKeyHelper.convertFingerprintToHex(cursor.getBlob(1));
                String keyIdShort = PgpKeyHelper.convertKeyIdToHexShort(cursor.getLong(2));
                long masterKeyId = cursor.getLong(3);
                int rawContactId = -1;
                Cursor raw = resolver.query(ContactsContract.RawContacts.CONTENT_URI, RAW_CONTACT_ID_PROJECTION,
                        FIND_RAW_CONTACT_SELECTION, new String[]{Constants.PACKAGE_NAME, fingerprint}, null, null);
                if (raw != null) {
                    if (raw.moveToNext()) {
                        rawContactId = raw.getInt(0);
                    }
                    raw.close();
                }
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                if (rawContactId == -1) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, context.getString(R.string.app_name))
                            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, Constants.PACKAGE_NAME)
                            .withValue(ContactsContract.RawContacts.SOURCE_ID, fingerprint)
                            .build());
                    if (userId[0] != null) {
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, userId[0])
                                .build());
                    }
                    if (userId[1] != null) {
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.DATA, userId[1])
                                .build());
                    }
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, Constants.CUSTOM_CONTACT_DATA_MIME_TYPE)
                            .withValue(ContactsContract.Data.DATA1, String.format(context.getString(R.string.contact_show_key), keyIdShort))
                            .withValue(ContactsContract.Data.DATA2, masterKeyId)
                            .build());
                }
                try {
                    resolver.applyBatch(ContactsContract.AUTHORITY, ops);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }
    }
}
