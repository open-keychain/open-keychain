package org.sufficientlysecure.keychain.remote;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract.EmailStatus;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;


class OpenPgpServiceKeyIdExtractor {
    @VisibleForTesting
    static final String[] PROJECTION_KEY_SEARCH = {
            "email_address",
            "master_key_id",
            "email_status",
    };
    private static final int INDEX_EMAIL_ADDRESS = 0;
    private static final int INDEX_MASTER_KEY_ID = 1;
    private static final int INDEX_EMAIL_STATUS = 2;


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


    KeyIdResult returnKeyIdsFromIntent(Intent data, boolean askIfNoUserIdsProvided, String callingPackageName) {
        boolean hasKeysFromSelectPubkeyActivity = data.hasExtra(OpenPgpApi.EXTRA_KEY_IDS_SELECTED);

        KeyIdResult result;
        if (hasKeysFromSelectPubkeyActivity) {
            HashSet<Long> encryptKeyIds = new HashSet<>();
            for (long keyId : data.getLongArrayExtra(OpenPgpApi.EXTRA_KEY_IDS_SELECTED)) {
                encryptKeyIds.add(keyId);
            }
            result = createKeysOkResult(encryptKeyIds, false);
        } else if (data.hasExtra(OpenPgpApi.EXTRA_USER_IDS) || askIfNoUserIdsProvided) {
            String[] userIds = data.getStringArrayExtra(OpenPgpApi.EXTRA_USER_IDS);
            result = returnKeyIdsFromEmails(data, userIds, callingPackageName);
        } else {
            result = createNoKeysResult();
        }

        // add key ids from non-ambiguous key id extra
        if (data.hasExtra(OpenPgpApi.EXTRA_KEY_IDS)) {
            HashSet<Long> explicitKeyIds = new HashSet<>();
            for (long keyId : data.getLongArrayExtra(OpenPgpApi.EXTRA_KEY_IDS)) {
                explicitKeyIds.add(keyId);
            }
            result = result.withExplicitKeyIds(explicitKeyIds);
        }

        return result;
    }

    private KeyIdResult returnKeyIdsFromEmails(Intent data, String[] encryptionAddresses, String callingPackageName) {
        boolean hasAddresses = (encryptionAddresses != null && encryptionAddresses.length > 0);

        boolean allKeysConfirmed = false;
        HashSet<Long> keyIds = new HashSet<>();
        ArrayList<String> missingEmails = new ArrayList<>();
        ArrayList<String> duplicateEmails = new ArrayList<>();

        if (hasAddresses) {
            HashMap<String, UserIdStatus> keyRows = getStatusMapForQueriedAddresses(encryptionAddresses, callingPackageName);

            boolean anyKeyNotVerified = false;
            for (Entry<String, UserIdStatus> entry : keyRows.entrySet()) {
                String queriedAddress = entry.getKey();
                UserIdStatus userIdStatus = entry.getValue();

                if (userIdStatus.masterKeyId == null) {
                    missingEmails.add(queriedAddress);
                    continue;
                }

                keyIds.add(userIdStatus.masterKeyId);

                if (userIdStatus.hasDuplicate) {
                    duplicateEmails.add(queriedAddress);
                }

                if (!userIdStatus.verified) {
                    anyKeyNotVerified = true;
                }
            }

            if (keyRows.size() != encryptionAddresses.length) {
                Log.e(Constants.TAG, "Number of rows doesn't match number of retrieved rows! Probably a bug?");
            }

            allKeysConfirmed = !anyKeyNotVerified;
        }

        if (!missingEmails.isEmpty()) {
            return createMissingKeysResult(data, keyIds, missingEmails, duplicateEmails);
        }

        if (!duplicateEmails.isEmpty()) {
            return createDuplicateKeysResult(data, keyIds, missingEmails, duplicateEmails);
        }

        if (keyIds.isEmpty()) {
            return createNoKeysResult(data, keyIds, missingEmails, duplicateEmails);
        }

        return createKeysOkResult(keyIds, allKeysConfirmed);
    }

    /** This method queries the KeychainExternalProvider for all addresses given in encryptionUserIds.
     * It returns a map with one UserIdStatus per queried address. If multiple key candidates exist,
     * the one with the highest verification status is selected. If two candidates with the same
     * verification status exist, the first one is returned and marked as having a duplicate.
     */
    @NonNull
    private HashMap<String, UserIdStatus> getStatusMapForQueriedAddresses(String[] encryptionUserIds, String callingPackageName) {
        HashMap<String,UserIdStatus> keyRows = new HashMap<>();
        Uri queryUri = EmailStatus.CONTENT_URI.buildUpon().appendPath(callingPackageName).build();
        Cursor cursor = contentResolver.query(queryUri, PROJECTION_KEY_SEARCH, null, encryptionUserIds, null);
        if (cursor == null) {
            throw new IllegalStateException("Internal error, received null cursor!");
        }

        try {
            while (cursor.moveToNext()) {
                String queryAddress = cursor.getString(INDEX_EMAIL_ADDRESS);
                Long masterKeyId = cursor.isNull(INDEX_MASTER_KEY_ID) ? null : cursor.getLong(INDEX_MASTER_KEY_ID);
                boolean isVerified = cursor.getInt(INDEX_EMAIL_STATUS) == KeychainExternalContract.KEY_STATUS_VERIFIED;
                UserIdStatus userIdStatus = new UserIdStatus(masterKeyId, isVerified);

                boolean seenBefore = keyRows.containsKey(queryAddress);
                if (!seenBefore) {
                    keyRows.put(queryAddress, userIdStatus);
                    continue;
                }

                UserIdStatus previousUserIdStatus = keyRows.get(queryAddress);
                if (previousUserIdStatus.masterKeyId == null) {
                    keyRows.put(queryAddress, userIdStatus);
                } else if (!previousUserIdStatus.verified && userIdStatus.verified) {
                    keyRows.put(queryAddress, userIdStatus);
                } else if (previousUserIdStatus.verified == userIdStatus.verified) {
                    previousUserIdStatus.hasDuplicate = true;
                }
            }
        } finally {
            cursor.close();
        }
        return keyRows;
    }

    private static class UserIdStatus {
        private final Long masterKeyId;
        private final boolean verified;
        private boolean hasDuplicate;

        UserIdStatus(Long masterKeyId, boolean verified) {
            this.masterKeyId = masterKeyId;
            this.verified = verified;
        }
    }

    static class KeyIdResult {
        private final PendingIntent mKeySelectionPendingIntent;
        private final HashSet<Long> mUserKeyIds;
        private final HashSet<Long> mExplicitKeyIds;
        private final KeyIdResultStatus mStatus;
        private final boolean mAllKeysConfirmed;

        private KeyIdResult(PendingIntent keySelectionPendingIntent, KeyIdResultStatus keyIdResultStatus) {
            mKeySelectionPendingIntent = keySelectionPendingIntent;
            mUserKeyIds = null;
            mAllKeysConfirmed = false;
            mStatus = keyIdResultStatus;
            mExplicitKeyIds = null;
        }
        private KeyIdResult(HashSet<Long> keyIds, boolean allKeysConfirmed, KeyIdResultStatus keyIdResultStatus) {
            mKeySelectionPendingIntent = null;
            mUserKeyIds = keyIds;
            mAllKeysConfirmed = allKeysConfirmed;
            mStatus = keyIdResultStatus;
            mExplicitKeyIds = null;
        }

        private KeyIdResult(KeyIdResult keyIdResult, HashSet<Long> explicitKeyIds) {
            mKeySelectionPendingIntent = keyIdResult.mKeySelectionPendingIntent;
            mUserKeyIds = keyIdResult.mUserKeyIds;
            mAllKeysConfirmed = keyIdResult.mAllKeysConfirmed;
            mStatus = keyIdResult.mStatus;
            mExplicitKeyIds = explicitKeyIds;
        }

        boolean hasKeySelectionPendingIntent() {
            return mKeySelectionPendingIntent != null;
        }

        PendingIntent getKeySelectionPendingIntent() {
            if (mKeySelectionPendingIntent == null) {
                throw new AssertionError("result intent must not be null when getResultIntent is called!");
            }
            if (mUserKeyIds != null) {
                throw new AssertionError("key ids must be null when getKeyIds is called!");
            }
            return mKeySelectionPendingIntent;
        }

        long[] getKeyIds() {
            if (mKeySelectionPendingIntent != null) {
                throw new AssertionError("result intent must be null when getKeyIds is called!");
            }
            HashSet<Long> allKeyIds = new HashSet<>();
            if (mUserKeyIds != null) {
                allKeyIds.addAll(mUserKeyIds);
            }
            if (mExplicitKeyIds != null) {
                allKeyIds.addAll(mExplicitKeyIds);
            }
            return KeyFormattingUtils.getUnboxedLongArray(allKeyIds);
        }

        boolean isAllKeysConfirmed() {
            return mAllKeysConfirmed;
        }

        private KeyIdResult withExplicitKeyIds(HashSet<Long> explicitKeyIds) {
            return new KeyIdResult(this, explicitKeyIds);
        }

        KeyIdResultStatus getStatus() {
            return mStatus;
        }
    }

    enum KeyIdResultStatus {
        OK, MISSING, DUPLICATE, NO_KEYS, NO_KEYS_ERROR
    }

    private KeyIdResult createKeysOkResult(HashSet<Long> encryptKeyIds, boolean allKeysConfirmed) {
        return new KeyIdResult(encryptKeyIds, allKeysConfirmed, KeyIdResultStatus.OK);
    }

    private KeyIdResult createNoKeysResult(Intent data,
            HashSet<Long> selectedKeyIds, ArrayList<String> missingEmails, ArrayList<String> duplicateEmails) {
        long[] keyIdsArray = KeyFormattingUtils.getUnboxedLongArray(selectedKeyIds);
        PendingIntent selectKeyPendingIntent = apiPendingIntentFactory.createSelectPublicKeyPendingIntent(
                data, keyIdsArray, missingEmails, duplicateEmails, false);

        return new KeyIdResult(selectKeyPendingIntent, KeyIdResultStatus.NO_KEYS);
    }

    private KeyIdResult createDuplicateKeysResult(Intent data,
            HashSet<Long> selectedKeyIds, ArrayList<String> missingEmails, ArrayList<String> duplicateEmails) {
        long[] keyIdsArray = KeyFormattingUtils.getUnboxedLongArray(selectedKeyIds);
        PendingIntent selectKeyPendingIntent = apiPendingIntentFactory.createSelectPublicKeyPendingIntent(
                data, keyIdsArray, missingEmails, duplicateEmails, false);

        return new KeyIdResult(selectKeyPendingIntent, KeyIdResultStatus.DUPLICATE);
    }

    private KeyIdResult createMissingKeysResult(Intent data,
            HashSet<Long> selectedKeyIds, ArrayList<String> missingEmails, ArrayList<String> duplicateEmails) {
        long[] keyIdsArray = KeyFormattingUtils.getUnboxedLongArray(selectedKeyIds);
        PendingIntent selectKeyPendingIntent = apiPendingIntentFactory.createSelectPublicKeyPendingIntent(
                data, keyIdsArray, missingEmails, duplicateEmails, false);

        return new KeyIdResult(selectKeyPendingIntent, KeyIdResultStatus.MISSING);
    }

    private KeyIdResult createNoKeysResult() {
        return new KeyIdResult(null, KeyIdResultStatus.NO_KEYS_ERROR);
    }
}
