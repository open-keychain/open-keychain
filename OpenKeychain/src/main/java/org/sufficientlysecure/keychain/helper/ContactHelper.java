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
import android.provider.ContactsContract;
import android.util.Patterns;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.util.Log;

import java.util.*;

public class ContactHelper {

    public static final String[] KEYS_TO_CONTACT_PROJECTION = new String[]{
            KeychainContract.KeyRings.USER_ID,
            KeychainContract.KeyRings.FINGERPRINT,
            KeychainContract.KeyRings.KEY_ID,
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.KeyRings.EXPIRY,
            KeychainContract.KeyRings.IS_REVOKED};
    public static final String[] USER_IDS_PROJECTION = new String[]{
            KeychainContract.UserIds.USER_ID
    };

    public static final String NON_REVOKED_SELECTION = KeychainContract.UserIds.IS_REVOKED + "=0";

    public static final String[] ID_PROJECTION = new String[]{ContactsContract.RawContacts._ID};
    public static final String[] SOURCE_ID_PROJECTION = new String[]{ContactsContract.RawContacts.SOURCE_ID};

    public static final String ACCOUNT_TYPE_AND_SOURCE_ID_SELECTION =
            ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND " + ContactsContract.RawContacts.SOURCE_ID + "=?";
    public static final String ACCOUNT_TYPE_SELECTION = ContactsContract.RawContacts.ACCOUNT_TYPE + "=?";
    public static final String RAW_CONTACT_AND_MIMETYPE_SELECTION =
            ContactsContract.Data.RAW_CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?";
    public static final String ID_SELECTION = ContactsContract.RawContacts._ID + "=?";

    public static List<String> getMailAccounts(Context context) {
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
        Cursor contactMasterKey = context.getContentResolver().query(contactUri,
                new String[]{ContactsContract.Data.DATA2}, null, null, null, null);
        if (contactMasterKey != null) {
            if (contactMasterKey.moveToNext()) {
                return KeychainContract.KeyRings.buildGenericKeyRingUri(contactMasterKey.getLong(0));
            }
            contactMasterKey.close();
        }
        return null;
    }

    /**
     * Write the current Keychain to the contact db
     */
    public static void writeKeysToContacts(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Set<String> contactFingerprints = getRawContactFingerprints(resolver);

        // Load all Keys from OK
        Cursor cursor = resolver.query(KeychainContract.KeyRings.buildUnifiedKeyRingsUri(), KEYS_TO_CONTACT_PROJECTION,
                null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String[] primaryUserId = KeyRing.splitUserId(cursor.getString(0));
                String fingerprint = PgpKeyHelper.convertFingerprintToHex(cursor.getBlob(1));
                contactFingerprints.remove(fingerprint);
                String keyIdShort = PgpKeyHelper.convertKeyIdToHexShort(cursor.getLong(2));
                long masterKeyId = cursor.getLong(3);
                boolean isExpired = !cursor.isNull(4) && new Date(cursor.getLong(4) * 1000).before(new Date());
                boolean isRevoked = cursor.getInt(5) > 0;
                int rawContactId = findRawContactId(resolver, fingerprint);
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

                // Do not store expired or revoked keys in contact db - and remove them if they already exist
                if (isExpired || isRevoked) {
                    if (rawContactId != -1) {
                        resolver.delete(ContactsContract.RawContacts.CONTENT_URI, ID_SELECTION,
                                new String[]{Integer.toString(rawContactId)});
                    }
                } else {

                    // Create a new rawcontact with corresponding key if it does not exist yet
                    if (rawContactId == -1) {
                        insertContact(ops, context, fingerprint);
                        writeContactKey(ops, context, rawContactId, masterKeyId, keyIdShort);
                    }

                    // We always update the display name (which is derived from primary user id)
                    // and email addresses from user id
                    writeContactDisplayName(ops, rawContactId, primaryUserId[0]);
                    writeContactEmail(ops, resolver, rawContactId, masterKeyId);
                    try {
                        resolver.applyBatch(ContactsContract.AUTHORITY, ops);
                    } catch (Exception e) {
                        Log.w(Constants.TAG, e);
                    }
                }
            }
            cursor.close();
        }

        // Delete fingerprints that are no longer present in OK
        for (String fingerprint : contactFingerprints) {
            resolver.delete(ContactsContract.RawContacts.CONTENT_URI, ACCOUNT_TYPE_AND_SOURCE_ID_SELECTION,
                    new String[]{Constants.PACKAGE_NAME, fingerprint});
        }

    }

    /**
     * @return a set of all key fingerprints currently present in the contact db
     */
    private static Set<String> getRawContactFingerprints(ContentResolver resolver) {
        HashSet<String> result = new HashSet<String>();
        Cursor fingerprints = resolver.query(ContactsContract.RawContacts.CONTENT_URI, SOURCE_ID_PROJECTION,
                ACCOUNT_TYPE_SELECTION, new String[]{Constants.PACKAGE_NAME}, null);
        if (fingerprints != null) {
            while (fingerprints.moveToNext()) {
                result.add(fingerprints.getString(0));
            }
            fingerprints.close();
        }
        return result;
    }

    /**
     * This will search the contact db for a raw contact with a given fingerprint
     *
     * @return raw contact id or -1 if not found
     */
    private static int findRawContactId(ContentResolver resolver, String fingerprint) {
        int rawContactId = -1;
        Cursor raw = resolver.query(ContactsContract.RawContacts.CONTENT_URI, ID_PROJECTION,
                ACCOUNT_TYPE_AND_SOURCE_ID_SELECTION, new String[]{Constants.PACKAGE_NAME, fingerprint}, null, null);
        if (raw != null) {
            if (raw.moveToNext()) {
                rawContactId = raw.getInt(0);
            }
            raw.close();
        }
        return rawContactId;
    }

    /**
     * Creates a empty raw contact with a given fingerprint
     */
    private static void insertContact(ArrayList<ContentProviderOperation> ops, Context context, String fingerprint) {
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, context.getString(R.string.app_name))
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, Constants.PACKAGE_NAME)
                .withValue(ContactsContract.RawContacts.SOURCE_ID, fingerprint)
                .build());
    }

    /**
     * Adds a key id to the given raw contact.
     * <p/>
     * This creates the link to OK in contact details
     */
    private static void writeContactKey(ArrayList<ContentProviderOperation> ops, Context context, int rawContactId,
                                        long masterKeyId, String keyIdShort) {
        ops.add(referenceRawContact(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI), rawContactId)
                .withValue(ContactsContract.Data.MIMETYPE, Constants.CUSTOM_CONTACT_DATA_MIME_TYPE)
                .withValue(ContactsContract.Data.DATA1, context.getString(R.string.contact_show_key, keyIdShort))
                .withValue(ContactsContract.Data.DATA2, masterKeyId)
                .build());
    }

    /**
     * Write all known email addresses of a key (derived from user ids) to a given raw contact
     */
    private static void writeContactEmail(ArrayList<ContentProviderOperation> ops, ContentResolver resolver,
                                          int rawContactId, long masterKeyId) {
        ops.add(selectByRawContactAndItemType(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI),
                rawContactId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE).build());
        Cursor ids = resolver.query(KeychainContract.UserIds.buildUserIdsUri(Long.toString(masterKeyId)),
                USER_IDS_PROJECTION, NON_REVOKED_SELECTION, null, null);
        if (ids != null) {
            while (ids.moveToNext()) {
                String[] userId = KeyRing.splitUserId(ids.getString(0));
                if (userId[1] != null) {
                    ops.add(referenceRawContact(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI),
                            rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE,
                                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.DATA, userId[1])
                            .build());
                }
            }
            ids.close();
        }
    }

    private static void writeContactDisplayName(ArrayList<ContentProviderOperation> ops, int rawContactId,
                                                String displayName) {
        if (displayName != null) {
            ops.add(insertOrUpdateForRawContact(ContactsContract.Data.CONTENT_URI, rawContactId,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .build());
        }
    }

    private static ContentProviderOperation.Builder referenceRawContact(ContentProviderOperation.Builder builder,
                                                                        int rawContactId) {
        return rawContactId == -1 ?
                builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0) :
                builder.withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
    }

    private static ContentProviderOperation.Builder insertOrUpdateForRawContact(Uri uri, int rawContactId,
                                                                                String itemType) {
        if (rawContactId == -1) {
            return referenceRawContact(ContentProviderOperation.newInsert(uri), rawContactId).withValue(
                    ContactsContract.Data.MIMETYPE, itemType);
        } else {
            return selectByRawContactAndItemType(ContentProviderOperation.newUpdate(uri), rawContactId, itemType);
        }
    }

    private static ContentProviderOperation.Builder selectByRawContactAndItemType(
            ContentProviderOperation.Builder builder, int rawContactId, String itemType) {
        return builder.withSelection(RAW_CONTACT_AND_MIMETYPE_SELECTION,
                new String[]{Integer.toString(rawContactId), itemType});
    }
}
