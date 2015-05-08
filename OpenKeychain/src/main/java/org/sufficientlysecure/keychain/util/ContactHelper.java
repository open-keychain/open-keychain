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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Patterns;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

public class ContactHelper {

    private static final Map<Long, Bitmap> photoCache = new HashMap<>();

    public static List<String> getPossibleUserEmails(Context context) {
        Set<String> accountMails = getAccountEmails(context);
        accountMails.addAll(getMainProfileContactEmails(context));

        // remove items that are not an email
        Iterator<String> it = accountMails.iterator();
        while (it.hasNext()) {
            String email = it.next();
            Matcher emailMatcher = Patterns.EMAIL_ADDRESS.matcher(email);
            if (!emailMatcher.matches()) {
                it.remove();
            }
        }

        // now return the Set (without duplicates) as a List
        return new ArrayList<>(accountMails);
    }

    public static List<String> getPossibleUserNames(Context context) {
        Set<String> accountMails = getAccountEmails(context);
        Set<String> names = getContactNamesFromEmails(context, accountMails);
        names.addAll(getMainProfileContactName(context));

        // remove items that are an email
        Iterator<String> it = names.iterator();
        while (it.hasNext()) {
            String email = it.next();
            Matcher emailMatcher = Patterns.EMAIL_ADDRESS.matcher(email);
            if (emailMatcher.matches()) {
                it.remove();
            }
        }

        return new ArrayList<>(names);
    }

    /**
     * Get emails from AccountManager
     *
     * @param context
     * @return
     */
    private static Set<String> getAccountEmails(Context context) {
        final Account[] accounts = AccountManager.get(context).getAccounts();
        final Set<String> emailSet = new HashSet<>();
        for (Account account : accounts) {
            emailSet.add(account.name);
        }
        return emailSet;
    }

    /**
     * Search for contact names based on a list of emails (to find out the names of the
     * device owner based on the email addresses from AccountsManager)
     *
     * @param context
     * @param emails
     * @return
     */
    private static Set<String> getContactNamesFromEmails(Context context, Set<String> emails) {
        Set<String> names = new HashSet<>();
        for (String email : emails) {
            ContentResolver resolver = context.getContentResolver();
            Cursor profileCursor = resolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Email.ADDRESS,
                            ContactsContract.Contacts.DISPLAY_NAME
                    },
                    ContactsContract.CommonDataKinds.Email.ADDRESS + "=?",
                    new String[]{email}, null
            );
            if (profileCursor == null) {
                return null;
            }

            Set<String> currNames = new HashSet<>();
            while (profileCursor.moveToNext()) {
                String name = profileCursor.getString(1);
                if (name != null) {
                    currNames.add(name);
                }
            }
            profileCursor.close();
            names.addAll(currNames);
        }
        return names;
    }

    /**
     * Retrieves the emails of the primary profile contact
     * http://developer.android.com/reference/android/provider/ContactsContract.Profile.html
     *
     * @param context
     * @return
     */
    private static Set<String> getMainProfileContactEmails(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor profileCursor = resolver.query(
                Uri.withAppendedPath(
                        ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
                new String[]{
                        ContactsContract.CommonDataKinds.Email.ADDRESS,
                        ContactsContract.CommonDataKinds.Email.IS_PRIMARY
                },
                // Selects only email addresses
                ContactsContract.Contacts.Data.MIMETYPE + "=?",
                new String[]{
                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                },
                // Show primary rows first. Note that there won't be a primary email address if the
                // user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC"
        );
        if (profileCursor == null) {
            return null;
        }

        Set<String> emails = new HashSet<>();
        while (profileCursor.moveToNext()) {
            String email = profileCursor.getString(0);
            if (email != null) {
                emails.add(email);
            }
        }
        profileCursor.close();
        return emails;
    }

    /**
     * Retrieves the name of the primary profile contact
     * http://developer.android.com/reference/android/provider/ContactsContract.Profile.html
     *
     * @param context
     * @return
     */
    public static List<String> getMainProfileContactName(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor profileCursor = resolver.query(
                ContactsContract.Profile.CONTENT_URI,
                new String[]{
                        ContactsContract.Profile.DISPLAY_NAME
                },
                null, null, null);
        if (profileCursor == null) {
            return null;
        }

        Set<String> names = new HashSet<>();
        // should only contain one entry!
        while (profileCursor.moveToNext()) {
            String name = profileCursor.getString(0);
            if (name != null) {
                names.add(name);
            }
        }
        profileCursor.close();
        return new ArrayList<>(names);
    }

    /**
     * returns the CONTACT_ID of the main ("me") contact
     * http://developer.android.com/reference/android/provider/ContactsContract.Profile.html
     *
     * @param resolver
     * @return
     */
    public static long getMainProfileContactId(ContentResolver resolver) {
        Cursor profileCursor = resolver.query(ContactsContract.Profile.CONTENT_URI,
                new String[]{ContactsContract.Profile._ID}, null, null, null);

        if (profileCursor != null && profileCursor.getCount() != 0 && profileCursor.moveToNext()) {
            long contactId = profileCursor.getLong(0);
            profileCursor.close();
            return contactId;
        } else {
            if (profileCursor != null) {
                profileCursor.close();
            }
            return -1;
        }
    }

    /**
     * loads the profile picture of the main ("me") contact
     * http://developer.android.com/reference/android/provider/ContactsContract.Profile.html
     *
     * @param contentResolver
     * @param highRes         true for large image if present, false for thumbnail
     * @return bitmap of loaded photo
     */
    public static Bitmap loadMainProfilePhoto(ContentResolver contentResolver, boolean highRes) {
        try {
            long mainProfileContactId = getMainProfileContactId(contentResolver);

            Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,
                    Long.toString(mainProfileContactId));
            InputStream photoInputStream =
                    ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, contactUri, highRes);
            if (photoInputStream == null) {
                return null;
            }
            return BitmapFactory.decodeStream(photoInputStream);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static List<String> getContactMails(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor mailCursor = resolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Email.DATA},
                null, null, null);
        if (mailCursor == null) {
            return new ArrayList<>();
        }

        Set<String> mails = new HashSet<>();
        while (mailCursor.moveToNext()) {
            String email = mailCursor.getString(0);
            if (email != null) {
                mails.add(email);
            }
        }
        mailCursor.close();
        return new ArrayList<>(mails);
    }

    public static List<String> getContactNames(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                null, null, null);
        if (cursor == null) {
            return new ArrayList<>();
        }

        Set<String> names = new HashSet<>();
        while (cursor.moveToNext()) {
            String name = cursor.getString(0);
            if (name != null) {
                names.add(name);
            }
        }
        cursor.close();
        return new ArrayList<>(names);
    }

    public static Uri dataUriFromContactUri(Context context, Uri contactUri) {
        Cursor contactMasterKey = context.getContentResolver().query(contactUri,
                new String[]{ContactsContract.Data.DATA2}, null, null, null);
        if (contactMasterKey != null) {
            if (contactMasterKey.moveToNext()) {
                return KeychainContract.KeyRings.buildGenericKeyRingUri(contactMasterKey.getLong(0));
            }
            contactMasterKey.close();
        }
        return null;
    }

    /**
     * returns the CONTACT_ID of the raw contact to which a masterKeyId is associated, if the
     * raw contact has not been marked for deletion.
     *
     * @param resolver
     * @param masterKeyId
     * @return CONTACT_ID (id of aggregated contact) linked to masterKeyId
     */
    public static long findContactId(ContentResolver resolver, long masterKeyId) {
        long contactId = -1;
        Cursor raw = resolver.query(ContactsContract.RawContacts.CONTENT_URI,
                new String[]{
                        ContactsContract.RawContacts.CONTACT_ID
                },
                ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND " +
                        ContactsContract.RawContacts.SOURCE_ID + "=? AND " +
                        ContactsContract.RawContacts.DELETED + "=?",
                new String[]{//"0" for "not deleted"
                        Constants.ACCOUNT_TYPE,
                        Long.toString(masterKeyId),
                        "0"
                }, null);
        if (raw != null) {
            if (raw.moveToNext()) {
                contactId = raw.getLong(0);
            }
            raw.close();
        }
        return contactId;
    }

    /**
     * Returns the display name of the system contact associated with contactId, null if the
     * contact does not exist
     *
     * @param resolver
     * @param contactId
     * @return primary display name of system contact associated with contactId, null if it does
     * not exist
     */
    public static String getContactName(ContentResolver resolver, long contactId) {
        String contactName = null;
        Cursor raw = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
                },
                ContactsContract.Contacts._ID + "=?",
                new String[]{//"0" for "not deleted"
                        Long.toString(contactId)
                }, null);
        if (raw != null) {
            if (raw.moveToNext()) {
                contactName = raw.getString(0);
            }
            raw.close();
        }
        return contactName;
    }

    public static Bitmap getCachedPhotoByMasterKeyId(ContentResolver contentResolver, long masterKeyId) {
        if (masterKeyId == -1) {
            return null;
        }
        if (!photoCache.containsKey(masterKeyId)) {
            photoCache.put(masterKeyId, loadPhotoByMasterKeyId(contentResolver, masterKeyId, false));
        }
        return photoCache.get(masterKeyId);
    }

    public static Bitmap loadPhotoByMasterKeyId(ContentResolver contentResolver, long masterKeyId,
                                                boolean highRes) {
        if (masterKeyId == -1) {
            return null;
        }
        try {
            long contactId = findContactId(contentResolver, masterKeyId);
            return loadPhotoByContactId(contentResolver, contactId, highRes);

        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Bitmap loadPhotoByContactId(ContentResolver contentResolver, long contactId,
                                              boolean highRes) {
        if (contactId == -1) {
            return null;
        }
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        // older android versions (tested on API level 15) fail on lookupuris being passed to
        // openContactPhotoInputStream
        // http://stackoverflow.com/a/21214524/3000919
        // Uri lookupUri = ContactsContract.Contacts.getLookupUri(contentResolver, contactUri);
        // Also, we don't need a permanent shortcut to the contact since we load it afresh each time

        InputStream photoInputStream = ContactsContract.Contacts.openContactPhotoInputStream(
                contentResolver,
                contactUri,
                highRes);

        if (photoInputStream == null) {
            return null;
        }
        return BitmapFactory.decodeStream(photoInputStream);
    }

    public static final String[] KEYS_TO_CONTACT_PROJECTION = new String[]{
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.KeyRings.USER_ID,
            KeychainContract.KeyRings.IS_EXPIRED,
            KeychainContract.KeyRings.IS_REVOKED,
            KeychainContract.KeyRings.VERIFIED,
            KeychainContract.KeyRings.HAS_SECRET,
            KeychainContract.KeyRings.HAS_ANY_SECRET};

    public static final int INDEX_MASTER_KEY_ID = 0;
    public static final int INDEX_USER_ID = 1;
    public static final int INDEX_IS_EXPIRED = 2;
    public static final int INDEX_IS_REVOKED = 3;
    public static final int INDEX_VERIFIED = 4;
    public static final int INDEX_HAS_SECRET = 5;
    public static final int INDEX_HAS_ANY_SECRET = 6;

    /**
     * Write/Update the current OpenKeychain keys to the contact db
     */
    public static void writeKeysToContacts(Context context) {
        ContentResolver resolver = context.getContentResolver();

        if (Constants.DEBUG_SYNC_REMOVE_CONTACTS) {
            debugDeleteRawContacts(resolver);
        }

        writeKeysToMainProfileContact(context, resolver);

        writeKeysToNormalContacts(context, resolver);
    }

    private static void writeKeysToNormalContacts(Context context, ContentResolver resolver) {
        // delete raw contacts flagged for deletion by user so they can be reinserted
        deleteFlaggedNormalRawContacts(resolver);

        Set<Long> deletedKeys = getRawContactMasterKeyIds(resolver);

        // Load all public Keys from OK
        // TODO: figure out why using selectionArgs does not work in this case
        Cursor cursor = resolver.query(KeychainContract.KeyRings.buildUnifiedKeyRingsUri(),
                KEYS_TO_CONTACT_PROJECTION,
                KeychainContract.KeyRings.HAS_ANY_SECRET + "=0",
                null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long masterKeyId = cursor.getLong(INDEX_MASTER_KEY_ID);
                KeyRing.UserId userIdSplit = KeyRing.splitUserId(cursor.getString(INDEX_USER_ID));
                boolean isExpired = cursor.getInt(INDEX_IS_EXPIRED) != 0;
                boolean isRevoked = cursor.getInt(INDEX_IS_REVOKED) > 0;
                boolean isVerified = cursor.getInt(INDEX_VERIFIED) > 0;

                Log.d(Constants.TAG, "masterKeyId: " + masterKeyId);

                deletedKeys.remove(masterKeyId);

                // get raw contact to this master key id
                long rawContactId = findRawContactId(resolver, masterKeyId);
                Log.d(Constants.TAG, "rawContactId: " + rawContactId);

                ArrayList<ContentProviderOperation> ops = new ArrayList<>();

                // Do not store expired or revoked or unverified keys in contact db - and
                // remove them if they already exist. Secret keys do not reach this point
                if (isExpired || isRevoked || !isVerified) {
                    Log.d(Constants.TAG, "Expired or revoked or unverified: Deleting rawContactId "
                            + rawContactId);
                    if (rawContactId != -1) {
                        deleteRawContactById(resolver, rawContactId);
                    }
                } else if (userIdSplit.name != null) {

                    // Create a new rawcontact with corresponding key if it does not exist yet
                    if (rawContactId == -1) {
                        Log.d(Constants.TAG, "Insert new raw contact with masterKeyId " + masterKeyId);

                        insertContact(ops, context, masterKeyId);
                        writeContactKey(ops, context, rawContactId, masterKeyId, userIdSplit.name);
                    }

                    // We always update the display name (which is derived from primary user id)
                    // and email addresses from user id
                    writeContactDisplayName(ops, rawContactId, userIdSplit.name);
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

        // Delete master key ids that are no longer present in OK
        for (Long masterKeyId : deletedKeys) {
            Log.d(Constants.TAG, "Delete raw contact with masterKeyId " + masterKeyId);
            deleteRawContactByMasterKeyId(resolver, masterKeyId);
        }
    }

    /**
     * Links all keys with secrets to the main ("me") contact
     * http://developer.android.com/reference/android/provider/ContactsContract.Profile.html
     *
     * @param context
     */
    public static void writeKeysToMainProfileContact(Context context, ContentResolver resolver) {
        // deletes contacts hidden by the user so they can be reinserted if necessary
        deleteFlaggedMainProfileRawContacts(resolver);

        Set<Long> keysToDelete = getMainProfileMasterKeyIds(resolver);

        // get all keys which have associated secret keys
        // TODO: figure out why using selectionArgs does not work in this case
        Cursor cursor = resolver.query(KeychainContract.KeyRings.buildUnifiedKeyRingsUri(),
                KEYS_TO_CONTACT_PROJECTION,
                KeychainContract.KeyRings.HAS_ANY_SECRET + "!=0",
                null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long masterKeyId = cursor.getLong(INDEX_MASTER_KEY_ID);
                boolean isExpired = cursor.getInt(INDEX_IS_EXPIRED) != 0;
                boolean isRevoked = cursor.getInt(INDEX_IS_REVOKED) > 0;
                KeyRing.UserId userIdSplit = KeyRing.splitUserId(cursor.getString(INDEX_USER_ID));

                if (!isExpired && !isRevoked && userIdSplit.name != null) {
                    // if expired or revoked will not be removed from keysToDelete or inserted
                    // into main profile ("me" contact)
                    boolean existsInMainProfile = keysToDelete.remove(masterKeyId);
                    if (!existsInMainProfile) {
                        long rawContactId = -1;//new raw contact

                        Log.d(Constants.TAG, "masterKeyId with secret " + masterKeyId);

                        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
                        insertMainProfileRawContact(ops, masterKeyId);
                        writeContactKey(ops, context, rawContactId, masterKeyId, userIdSplit.name);

                        try {
                            resolver.applyBatch(ContactsContract.AUTHORITY, ops);
                        } catch (Exception e) {
                            Log.w(Constants.TAG, e);
                        }
                    }
                }
            }
        }

        for (long masterKeyId : keysToDelete) {
            deleteMainProfileRawContactByMasterKeyId(resolver, masterKeyId);
            Log.d(Constants.TAG, "Delete main profile raw contact with masterKeyId " + masterKeyId);
        }
    }

    /**
     * Inserts a raw contact into the table defined by ContactsContract.Profile
     * http://developer.android.com/reference/android/provider/ContactsContract.Profile.html
     *
     * @param ops
     * @param masterKeyId
     */
    private static void insertMainProfileRawContact(ArrayList<ContentProviderOperation> ops,
                                                    long masterKeyId) {
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, Constants.ACCOUNT_NAME)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE)
                .withValue(ContactsContract.RawContacts.SOURCE_ID, Long.toString(masterKeyId))
                .build());
    }

    /**
     * deletes a raw contact from the main profile table ("me" contact)
     * http://developer.android.com/reference/android/provider/ContactsContract.Profile.html
     *
     * @param resolver
     * @param masterKeyId
     * @return number of rows deleted
     */
    private static int deleteMainProfileRawContactByMasterKeyId(ContentResolver resolver,
                                                                long masterKeyId) {
        // CALLER_IS_SYNCADAPTER allows us to actually wipe the RawContact from the device, otherwise
        // would be just flagged for deletion
        Uri deleteUri = ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI.buildUpon().
                appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();

        return resolver.delete(deleteUri,
                ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND " +
                        ContactsContract.RawContacts.SOURCE_ID + "=?",
                new String[]{
                        Constants.ACCOUNT_TYPE, Long.toString(masterKeyId)
                });
    }

    /**
     * deletes all raw contact entries in the "me" contact flagged for deletion ('hidden'),
     * presumably by the user
     *
     * @param resolver
     * @return number of raw contacts deleted
     */
    private static int deleteFlaggedMainProfileRawContacts(ContentResolver resolver) {
        // CALLER_IS_SYNCADAPTER allows us to actually wipe the RawContact from the device, otherwise
        // would be just flagged for deletion
        Uri deleteUri = ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI.buildUpon().
                appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();

        return resolver.delete(deleteUri,
                ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND " +
                        ContactsContract.RawContacts.DELETED + "=?",
                new String[]{
                        Constants.ACCOUNT_TYPE,
                        "1"
                });
    }

    /**
     * Delete all raw contacts associated to OpenKeychain, including those from "me" contact
     * defined by ContactsContract.Profile
     *
     * @return number of rows deleted
     */
    private static int debugDeleteRawContacts(ContentResolver resolver) {
        // CALLER_IS_SYNCADAPTER allows us to actually wipe the RawContact from the device, otherwise
        // would be just flagged for deletion
        Uri deleteUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon().
                appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();

        Log.d(Constants.TAG, "Deleting all raw contacts associated to OK...");
        int delete = resolver.delete(deleteUri,
                ContactsContract.RawContacts.ACCOUNT_TYPE + "=?",
                new String[]{
                        Constants.ACCOUNT_TYPE
                });

        Uri mainProfileDeleteUri = ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();

        delete += resolver.delete(mainProfileDeleteUri,
                ContactsContract.RawContacts.ACCOUNT_TYPE + "=?",
                new String[]{
                        Constants.ACCOUNT_TYPE
                });

        return delete;
    }

    /**
     * Deletes raw contacts from ContactsContract.RawContacts based on rawContactId. Does not
     * delete contacts from the "me" contact defined in ContactsContract.Profile
     *
     * @param resolver
     * @param rawContactId
     * @return number of rows deleted
     */
    private static int deleteRawContactById(ContentResolver resolver, long rawContactId) {
        // CALLER_IS_SYNCADAPTER allows us to actually wipe the RawContact from the device, otherwise
        // would be just flagged for deletion
        Uri deleteUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon().
                appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();

        return resolver.delete(deleteUri,
                ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND " +
                        ContactsContract.RawContacts._ID + "=?",
                new String[]{
                        Constants.ACCOUNT_TYPE, Long.toString(rawContactId)
                });
    }

    /**
     * Deletes raw contacts from ContactsContract.RawContacts based on masterKeyId. Does not
     * delete contacts from the "me" contact defined in ContactsContract.Profile
     *
     * @param resolver
     * @param masterKeyId
     * @return number of rows deleted
     */
    private static int deleteRawContactByMasterKeyId(ContentResolver resolver, long masterKeyId) {
        // CALLER_IS_SYNCADAPTER allows us to actually wipe the RawContact from the device, otherwise
        // would be just flagged for deletion
        Uri deleteUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon().
                appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();

        return resolver.delete(deleteUri,
                ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND " +
                        ContactsContract.RawContacts.SOURCE_ID + "=?",
                new String[]{
                        Constants.ACCOUNT_TYPE, Long.toString(masterKeyId)
                });
    }

    private static int deleteFlaggedNormalRawContacts(ContentResolver resolver) {
        // CALLER_IS_SYNCADAPTER allows us to actually wipe the RawContact from the device, otherwise
        // would be just flagged for deletion
        Uri deleteUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon().
                appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();

        return resolver.delete(deleteUri,
                ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND " +
                        ContactsContract.RawContacts.DELETED + "=?",
                new String[]{
                        Constants.ACCOUNT_TYPE,
                        "1"
                });
    }

    /**
     * @return a set of all key master key ids currently present in the contact db
     */
    private static Set<Long> getRawContactMasterKeyIds(ContentResolver resolver) {
        HashSet<Long> result = new HashSet<>();
        Cursor masterKeyIds = resolver.query(ContactsContract.RawContacts.CONTENT_URI,
                new String[]{
                        ContactsContract.RawContacts.SOURCE_ID
                },
                ContactsContract.RawContacts.ACCOUNT_TYPE + "=?",
                new String[]{
                        Constants.ACCOUNT_TYPE
                }, null);
        if (masterKeyIds != null) {
            while (masterKeyIds.moveToNext()) {
                result.add(masterKeyIds.getLong(0));
            }
            masterKeyIds.close();
        }
        return result;
    }

    /**
     * @return a set of all key master key ids currently present in the contact db
     */
    private static Set<Long> getMainProfileMasterKeyIds(ContentResolver resolver) {
        HashSet<Long> result = new HashSet<>();
        Cursor masterKeyIds = resolver.query(ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI,
                new String[]{
                        ContactsContract.RawContacts.SOURCE_ID
                },
                ContactsContract.RawContacts.ACCOUNT_TYPE + "=?",
                new String[]{
                        Constants.ACCOUNT_TYPE
                }, null);
        if (masterKeyIds != null) {
            while (masterKeyIds.moveToNext()) {
                result.add(masterKeyIds.getLong(0));
            }
            masterKeyIds.close();
        }
        return result;
    }

    /**
     * This will search the contact db for a raw contact with a given master key id
     *
     * @return raw contact id or -1 if not found
     */
    private static long findRawContactId(ContentResolver resolver, long masterKeyId) {
        long rawContactId = -1;
        Cursor raw = resolver.query(ContactsContract.RawContacts.CONTENT_URI,
                new String[]{
                        ContactsContract.RawContacts._ID
                },
                ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND " + ContactsContract.RawContacts.SOURCE_ID + "=?",
                new String[]{
                        Constants.ACCOUNT_TYPE, Long.toString(masterKeyId)
                }, null);
        if (raw != null) {
            if (raw.moveToNext()) {
                rawContactId = raw.getLong(0);
            }
            raw.close();
        }
        return rawContactId;
    }

    /**
     * Creates a empty raw contact with a given masterKeyId
     */
    private static void insertContact(ArrayList<ContentProviderOperation> ops, Context context, long masterKeyId) {
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, Constants.ACCOUNT_NAME)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE)
                .withValue(ContactsContract.RawContacts.SOURCE_ID, Long.toString(masterKeyId))
                .build());
    }

    /**
     * Adds a key id to the given raw contact.
     * <p/>
     * This creates the link to OK in contact details
     */
    private static void writeContactKey(ArrayList<ContentProviderOperation> ops, Context context, long rawContactId,
                                        long masterKeyId, String keyName) {
        ops.add(referenceRawContact(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI), rawContactId)
                .withValue(ContactsContract.Data.MIMETYPE, Constants.CUSTOM_CONTACT_DATA_MIME_TYPE)
                .withValue(ContactsContract.Data.DATA1, context.getString(R.string.contact_show_key, keyName))
                .withValue(ContactsContract.Data.DATA2, masterKeyId)
                .build());
    }

    /**
     * Write all known email addresses of a key (derived from user ids) to a given raw contact
     */
    private static void writeContactEmail(ArrayList<ContentProviderOperation> ops, ContentResolver resolver,
                                          long rawContactId, long masterKeyId) {
        ops.add(selectByRawContactAndItemType(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI),
                rawContactId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE).build());
        Cursor ids = resolver.query(UserPackets.buildUserIdsUri(masterKeyId),
                new String[]{
                        UserPackets.USER_ID
                },
                UserPackets.IS_REVOKED + "=0",
                null, null);
        if (ids != null) {
            while (ids.moveToNext()) {
                KeyRing.UserId userId = KeyRing.splitUserId(ids.getString(0));
                if (userId.email != null) {
                    ops.add(referenceRawContact(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI),
                            rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE,
                                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.DATA, userId.email)
                            .build());
                }
            }
            ids.close();
        }
    }

    private static void writeContactDisplayName(ArrayList<ContentProviderOperation> ops, long rawContactId,
                                                String displayName) {
        if (displayName != null) {
            ops.add(insertOrUpdateForRawContact(ContactsContract.Data.CONTENT_URI, rawContactId,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                    .build());
        }
    }

    private static ContentProviderOperation.Builder referenceRawContact(ContentProviderOperation.Builder builder,
                                                                        long rawContactId) {
        return rawContactId == -1 ?
                builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0) :
                builder.withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
    }

    private static ContentProviderOperation.Builder insertOrUpdateForRawContact(Uri uri, long rawContactId,
                                                                                String itemType) {
        if (rawContactId == -1) {
            return referenceRawContact(ContentProviderOperation.newInsert(uri), rawContactId).withValue(
                    ContactsContract.Data.MIMETYPE, itemType);
        } else {
            return selectByRawContactAndItemType(ContentProviderOperation.newUpdate(uri), rawContactId, itemType);
        }
    }

    private static ContentProviderOperation.Builder selectByRawContactAndItemType(
            ContentProviderOperation.Builder builder, long rawContactId, String itemType) {
        return builder.withSelection(
                ContactsContract.Data.RAW_CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                new String[]{
                        Long.toString(rawContactId), itemType
                });
    }
}
