package org.sufficientlysecure.keychain.remote;


import java.util.ArrayList;
import java.util.HashSet;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;

import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;


class OpenPgpServiceKeyIdExtractor {
    @VisibleForTesting
    static final String[] KEY_SEARCH_PROJECTION = new String[]{
            KeyRings._ID,
            KeyRings.MASTER_KEY_ID,
            KeyRings.IS_EXPIRED, // referenced in where clause!
            KeyRings.IS_REVOKED, // referenced in where clause!
    };
    private static final int INDEX_MASTER_KEY_ID = 1;

    // do not pre-select revoked or expired keys
    private static final String KEY_SEARCH_WHERE = Tables.KEYS + "." + KeychainContract.KeyRings.IS_REVOKED
            + " = 0 AND " + KeychainContract.KeyRings.IS_EXPIRED + " = 0";


    private final ApiPendingIntentFactory apiPendingIntentFactory;
    private final ContentResolver contentResolver;


    static OpenPgpServiceKeyIdExtractor getInstance(ContentResolver contentResolver, ApiPendingIntentFactory apiPendingIntentFactory) {
        return new OpenPgpServiceKeyIdExtractor(contentResolver, apiPendingIntentFactory);
    }

    private OpenPgpServiceKeyIdExtractor(ContentResolver contentResolver,
            ApiPendingIntentFactory apiPendingIntentFactory) {
        this.contentResolver = contentResolver;
        this.apiPendingIntentFactory = apiPendingIntentFactory;
    }


    KeyIdResult returnKeyIdsFromIntent(Intent data, boolean askIfNoUserIdsProvided) {
        HashSet<Long> encryptKeyIds = new HashSet<>();

        boolean hasKeysFromSelectPubkeyActivity = data.hasExtra(OpenPgpApi.EXTRA_KEY_IDS_SELECTED);
        if (hasKeysFromSelectPubkeyActivity) {
            for (long keyId : data.getLongArrayExtra(OpenPgpApi.EXTRA_KEY_IDS_SELECTED)) {
                encryptKeyIds.add(keyId);
            }
        } else if (data.hasExtra(OpenPgpApi.EXTRA_USER_IDS) || askIfNoUserIdsProvided) {
            String[] userIds = data.getStringArrayExtra(OpenPgpApi.EXTRA_USER_IDS);
            boolean isOpportunistic = data.getBooleanExtra(OpenPgpApi.EXTRA_OPPORTUNISTIC_ENCRYPTION, false);
            KeyIdResult result = returnKeyIdsFromEmails(data, userIds, isOpportunistic);

            if (result.mResultIntent != null) {
                return result;
            }
            encryptKeyIds.addAll(result.mKeyIds);
        }

        // add key ids from non-ambiguous key id extra
        if (data.hasExtra(OpenPgpApi.EXTRA_KEY_IDS)) {
            for (long keyId : data.getLongArrayExtra(OpenPgpApi.EXTRA_KEY_IDS)) {
                encryptKeyIds.add(keyId);
            }
        }

        if (encryptKeyIds.isEmpty()) {
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_ERROR,
                    new OpenPgpError(OpenPgpError.NO_USER_IDS, "No encryption keys or user ids specified!" +
                            "(pass empty user id array to get dialog without preselection)"));
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return new KeyIdResult(result);
        }

        return new KeyIdResult(encryptKeyIds);
    }

    private KeyIdResult returnKeyIdsFromEmails(Intent data, String[] encryptionUserIds, boolean isOpportunistic) {
        boolean hasUserIds = (encryptionUserIds != null && encryptionUserIds.length > 0);

        HashSet<Long> keyIds = new HashSet<>();
        ArrayList<String> missingEmails = new ArrayList<>();
        ArrayList<String> duplicateEmails = new ArrayList<>();
        if (hasUserIds) {
            for (String rawUserId : encryptionUserIds) {
                OpenPgpUtils.UserId userId = KeyRing.splitUserId(rawUserId);
                String email = userId.email != null ? userId.email : rawUserId;
                // try to find the key for this specific email
                Uri uri = KeyRings.buildUnifiedKeyRingsFindByEmailUri(email);
                Cursor cursor = contentResolver.query(uri, KEY_SEARCH_PROJECTION, KEY_SEARCH_WHERE, null, null);
                if (cursor == null) {
                    throw new IllegalStateException("Internal error, received null cursor!");
                }
                try {
                    // result should be one entry containing the key id
                    if (cursor.moveToFirst()) {
                        long id = cursor.getLong(INDEX_MASTER_KEY_ID);
                        keyIds.add(id);

                        // another entry for this email -> two keys with the same email inside user id
                        if (!cursor.isLast()) {
                            Log.d(Constants.TAG, "more than one user id with the same email");
                            duplicateEmails.add(email);

                            // also pre-select
                            while (cursor.moveToNext()) {
                                long duplicateId = cursor.getLong(INDEX_MASTER_KEY_ID);
                                keyIds.add(duplicateId);
                            }
                        }
                    } else {
                        missingEmails.add(email);
                        Log.d(Constants.TAG, "user id missing");
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        boolean hasMissingUserIds = !missingEmails.isEmpty();
        boolean hasDuplicateUserIds = !duplicateEmails.isEmpty();
        if (isOpportunistic && (!hasUserIds || hasMissingUserIds)) {
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_ERROR,
                    new OpenPgpError(OpenPgpError.OPPORTUNISTIC_MISSING_KEYS, "missing keys in opportunistic mode"));
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            return new KeyIdResult(result);
        }

        if (!hasUserIds || hasMissingUserIds || hasDuplicateUserIds) {
            long[] keyIdsArray = KeyFormattingUtils.getUnboxedLongArray(keyIds);
            PendingIntent pi = apiPendingIntentFactory.createSelectPublicKeyPendingIntent(data, keyIdsArray,
                    missingEmails, duplicateEmails, hasUserIds);

            // return PendingIntent to be executed by client
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_INTENT, pi);
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
            return new KeyIdResult(result);
        }

        if (keyIds.isEmpty()) {
            throw new AssertionError("keyIdsArray.length == 0, should never happen!");
        }

        return new KeyIdResult(keyIds);
    }

    static class KeyIdResult {
        private final Intent mResultIntent;
        private final HashSet<Long> mKeyIds;

        private KeyIdResult(Intent resultIntent) {
            mResultIntent = resultIntent;
            mKeyIds = null;
        }
        private KeyIdResult(HashSet<Long> keyIds) {
            mResultIntent = null;
            mKeyIds = keyIds;
        }

        boolean hasResultIntent() {
            return mResultIntent != null;
        }
        Intent getResultIntent() {
            if (mResultIntent == null) {
                throw new AssertionError("result intent must not be null when getResultIntent is called!");
            }
            if (mKeyIds != null) {
                throw new AssertionError("key ids must be null when getKeyIds is called!");
            }
            return mResultIntent;
        }
        long[] getKeyIds() {
            if (mResultIntent != null) {
                throw new AssertionError("result intent must be null when getKeyIds is called!");
            }
            if (mKeyIds == null) {
                throw new AssertionError("key ids must not be null when getKeyIds is called!");
            }
            return KeyFormattingUtils.getUnboxedLongArray(mKeyIds);
        }
    }

}
