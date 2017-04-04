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
import android.support.annotation.VisibleForTesting;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.Constants;
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

    private KeyIdResult returnKeyIdsFromEmails(Intent data, String[] encryptionUserIds, String callingPackageName) {
        boolean hasUserIds = (encryptionUserIds != null && encryptionUserIds.length > 0);

        boolean anyKeyNotVerified = false;
        HashSet<Long> keyIds = new HashSet<>();
        ArrayList<String> missingEmails = new ArrayList<>();
        ArrayList<String> duplicateEmails = new ArrayList<>();
        HashMap<String,KeyRow> keyRows = new HashMap<>();
        if (hasUserIds) {
            Uri queryUri = EmailStatus.CONTENT_URI.buildUpon().appendPath(callingPackageName).build();
            Cursor cursor = contentResolver.query(queryUri, PROJECTION_KEY_SEARCH, null, encryptionUserIds, null);
            if (cursor == null) {
                throw new IllegalStateException("Internal error, received null cursor!");
            }

            try {
                while (cursor.moveToNext()) {
                    String queryAddress = cursor.getString(INDEX_EMAIL_ADDRESS);
                    Long masterKeyId = cursor.isNull(INDEX_MASTER_KEY_ID) ? null : cursor.getLong(INDEX_MASTER_KEY_ID);
                    int verified = cursor.getInt(INDEX_EMAIL_STATUS);

                    KeyRow row = new KeyRow(masterKeyId, verified == 2);
                    if (!keyRows.containsKey(queryAddress)) {
                        keyRows.put(queryAddress, row);
                        continue;
                    }

                    KeyRow previousRow = keyRows.get(queryAddress);
                    if (previousRow.masterKeyId == null) {
                        keyRows.put(queryAddress, row);
                    } else if (!previousRow.verified && row.verified) {
                        keyRows.put(queryAddress, row);
                    } else if (previousRow.verified == row.verified) {
                        previousRow.hasDuplicate = true;
                    }
                }
            } finally {
                cursor.close();
            }

            for (Entry<String, KeyRow> entry : keyRows.entrySet()) {
                String queriedAddress = entry.getKey();
                KeyRow keyRow = entry.getValue();

                if (keyRow.masterKeyId == null) {
                    missingEmails.add(queriedAddress);
                    continue;
                }

                keyIds.add(keyRow.masterKeyId);

                if (keyRow.hasDuplicate) {
                    duplicateEmails.add(queriedAddress);
                }

                if (!keyRow.verified) {
                    anyKeyNotVerified = true;
                }
            }

            if (keyRows.size() != encryptionUserIds.length) {
                Log.e(Constants.TAG, "Number of rows doesn't match number of retrieved rows! Probably a bug?");
            }
        }

        long[] keyIdsArray = KeyFormattingUtils.getUnboxedLongArray(keyIds);
        PendingIntent pi = apiPendingIntentFactory.createSelectPublicKeyPendingIntent(data, keyIdsArray,
                missingEmails, duplicateEmails, false);

        if (!missingEmails.isEmpty()) {
            return createMissingKeysResult(pi);
        }

        if (!duplicateEmails.isEmpty()) {
            return createDuplicateKeysResult(pi);
        }

        if (keyIds.isEmpty()) {
            return createNoKeysResult(pi);
        }

        boolean allKeysConfirmed = !anyKeyNotVerified;
        return createKeysOkResult(keyIds, allKeysConfirmed);
    }

    private static class KeyRow {
        private final Long masterKeyId;
        private final boolean verified;
        private boolean hasDuplicate;

        KeyRow(Long masterKeyId, boolean verified) {
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

    private static KeyIdResult createNoKeysResult(PendingIntent pendingIntent) {
        return new KeyIdResult(pendingIntent, KeyIdResultStatus.NO_KEYS);
    }

    private static KeyIdResult createNoKeysResult() {
        return new KeyIdResult(null, KeyIdResultStatus.NO_KEYS_ERROR);
    }

    private static KeyIdResult createDuplicateKeysResult(PendingIntent pendingIntent) {
        return new KeyIdResult(pendingIntent, KeyIdResultStatus.DUPLICATE);
    }

    private static KeyIdResult createMissingKeysResult(PendingIntent pendingIntent) {
        return new KeyIdResult(pendingIntent, KeyIdResultStatus.MISSING);
    }
}
