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

package org.sufficientlysecure.keychain.remote;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiAutocryptPeer;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract.AutocryptStatus;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;


class OpenPgpServiceKeyIdExtractor {
    @VisibleForTesting
    static final String[] PROJECTION_MAIL_STATUS = {
            AutocryptStatus.ADDRESS,
            AutocryptStatus.UID_MASTER_KEY_ID,
            AutocryptStatus.UID_KEY_STATUS,
            AutocryptStatus.UID_CANDIDATES,
            AutocryptStatus.AUTOCRYPT_MASTER_KEY_ID,
            AutocryptStatus.AUTOCRYPT_KEY_STATUS,
            AutocryptStatus.AUTOCRYPT_PEER_STATE
    };
    private static final int INDEX_EMAIL_ADDRESS = 0;
    private static final int INDEX_MASTER_KEY_ID = 1;
    private static final int INDEX_USER_ID_STATUS = 2;
    private static final int INDEX_USER_ID_CANDIDATES = 3;
    private static final int INDEX_AUTOCRYPT_MASTER_KEY_ID = 4;
    private static final int INDEX_AUTOCRYPT_KEY_STATUS = 5;
    private static final int INDEX_AUTOCRYPT_PEER_STATE = 6;


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
            result = createKeysOkResult(encryptKeyIds, false, null);
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
        AutocryptState combinedAutocryptState = null;

        if (hasAddresses) {
            HashMap<String, AddressQueryResult> userIdEntries = getStatusMapForQueriedAddresses(
                    encryptionAddresses, callingPackageName);

            boolean anyKeyNotVerified = false;
            for (String queriedAddress : encryptionAddresses) {
                AddressQueryResult addressQueryResult = userIdEntries.get(queriedAddress);
                if (addressQueryResult == null) {
                    throw new IllegalStateException("No result for address - shouldn't happen!");
                }

                if (addressQueryResult.autocryptMasterKeyId != null) {
                    keyIds.add(addressQueryResult.autocryptMasterKeyId);

                    if (addressQueryResult.autocryptKeyStatus != KeychainExternalContract.KEY_STATUS_VERIFIED) {
                        anyKeyNotVerified = true;
                    }

                    if (combinedAutocryptState == null) {
                        combinedAutocryptState = addressQueryResult.autocryptState;
                    } else {
                        combinedAutocryptState = combinedAutocryptState.combineWith(addressQueryResult.autocryptState);
                    }

                    continue;
                }

                if (addressQueryResult.uidMasterKeyId != null) {
                    keyIds.add(addressQueryResult.uidMasterKeyId);
                    combinedAutocryptState = AutocryptState.EXTERNAL;

                    if (addressQueryResult.uidHasMultipleCandidates) {
                        duplicateEmails.add(queriedAddress);
                    }

                    if (addressQueryResult.uidKeyStatus != KeychainExternalContract.KEY_STATUS_VERIFIED) {
                        anyKeyNotVerified = true;
                    }

                    continue;
                }

                missingEmails.add(queriedAddress);
            }

            if (userIdEntries.size() != encryptionAddresses.length) {
                Log.e(Constants.TAG, "Number of rows doesn't match number of retrieved rows! Probably a bug?");
            }

            allKeysConfirmed = !anyKeyNotVerified;
        }

        if (!missingEmails.isEmpty()) {
            return createMissingKeysResult(data, keyIds, missingEmails, duplicateEmails);
        }

        if (!duplicateEmails.isEmpty()) {
            return createDuplicateKeysResult(data, callingPackageName, duplicateEmails);
        }

        if (keyIds.isEmpty()) {
            return createNoKeysResult(data, keyIds, missingEmails, duplicateEmails);
        }

        return createKeysOkResult(keyIds, allKeysConfirmed, combinedAutocryptState);
    }

    /** This method queries the KeychainExternalProvider for all addresses given in encryptionUserIds.
     * It returns a map with one UserIdStatus per queried address. If multiple key candidates exist,
     * the one with the highest verification status is selected. If two candidates with the same
     * verification status exist, the first one is returned and marked as having a duplicate.
     */
    @NonNull
    private HashMap<String, AddressQueryResult> getStatusMapForQueriedAddresses(String[] encryptionUserIds, String callingPackageName) {
        HashMap<String,AddressQueryResult> keyRows = new HashMap<>();
        Uri queryUri = AutocryptStatus.CONTENT_URI.buildUpon().appendPath(callingPackageName).build();
        Cursor cursor = contentResolver.query(queryUri, PROJECTION_MAIL_STATUS, null, encryptionUserIds, null);
        if (cursor == null) {
            throw new IllegalStateException("Internal error, received null cursor!");
        }

        try {
            while (cursor.moveToNext()) {
                String queryAddress = cursor.getString(INDEX_EMAIL_ADDRESS);
                Long uidMasterKeyId =
                        cursor.isNull(INDEX_MASTER_KEY_ID) ? null : cursor.getLong(INDEX_MASTER_KEY_ID);
                int uidKeyStatus = cursor.getInt(INDEX_USER_ID_STATUS);
                boolean uidHasMultipleCandidates = cursor.getInt(INDEX_USER_ID_CANDIDATES) > 1;

                Long autocryptMasterKeyId =
                        cursor.isNull(INDEX_AUTOCRYPT_MASTER_KEY_ID) ? null : cursor.getLong(INDEX_AUTOCRYPT_MASTER_KEY_ID);
                int autocryptKeyStatus = cursor.getInt(INDEX_AUTOCRYPT_KEY_STATUS);
                int autocryptPeerStatus = cursor.getInt(INDEX_AUTOCRYPT_PEER_STATE);

                AddressQueryResult status = new AddressQueryResult(
                                uidMasterKeyId, uidKeyStatus, uidHasMultipleCandidates, autocryptMasterKeyId,
                                autocryptKeyStatus, AutocryptState.fromDbValue(autocryptPeerStatus));

                keyRows.put(queryAddress, status);
            }
        } finally {
            cursor.close();
        }
        return keyRows;
    }

    private static class AddressQueryResult {
        private final Long uidMasterKeyId;
        private final int uidKeyStatus;
        private boolean uidHasMultipleCandidates;
        private final Long autocryptMasterKeyId;
        private final int autocryptKeyStatus;
        private final AutocryptState autocryptState;

        AddressQueryResult(Long uidMasterKeyId, int uidKeyStatus, boolean uidHasMultipleCandidates, Long autocryptMasterKeyId,
                int autocryptKeyStatus, AutocryptState autocryptState) {
            this.uidMasterKeyId = uidMasterKeyId;
            this.uidKeyStatus = uidKeyStatus;
            this.uidHasMultipleCandidates = uidHasMultipleCandidates;
            this.autocryptMasterKeyId = autocryptMasterKeyId;
            this.autocryptKeyStatus = autocryptKeyStatus;
            this.autocryptState = autocryptState;
        }
    }

    enum AutocryptState {
        EXTERNAL, RESET, GOSSIP, SELECTED, AVAILABLE, MUTUAL;

        static AutocryptState fromDbValue(int state) {
            switch (state) {
                case ApiAutocryptPeer.RESET:
                    return RESET;
                case ApiAutocryptPeer.AVAILABLE:
                    return AVAILABLE;
                case ApiAutocryptPeer.SELECTED:
                    return SELECTED;
                case ApiAutocryptPeer.GOSSIP:
                    return GOSSIP;
                case ApiAutocryptPeer.MUTUAL:
                    return MUTUAL;
                default:
                    throw new IllegalStateException();
            }
        }

        public AutocryptState combineWith(AutocryptState other) {
            if (this == EXTERNAL || other == EXTERNAL) {
                return EXTERNAL;
            }
            if (this == RESET || other == RESET) {
                return RESET;
            }
            if (this == GOSSIP || other == GOSSIP) {
                return GOSSIP;
            }
            if (this == SELECTED || other == SELECTED) {
                return SELECTED;
            }
            if (this == AVAILABLE || other == AVAILABLE) {
                return AVAILABLE;
            }
            if (this == MUTUAL && other == MUTUAL) {
                return MUTUAL;
            }
            throw new IllegalStateException("Bug: autocrypt states can't be combined!");
        }
    }

    static class KeyIdResult {
        private final PendingIntent mKeySelectionPendingIntent;
        private final HashSet<Long> mUserKeyIds;
        private final HashSet<Long> mExplicitKeyIds;
        private final KeyIdResultStatus mStatus;
        private final boolean mAllKeysConfirmed;
        private final AutocryptState mCombinedAutocryptState;

        private KeyIdResult(PendingIntent keySelectionPendingIntent, KeyIdResultStatus keyIdResultStatus) {
            mKeySelectionPendingIntent = keySelectionPendingIntent;
            mUserKeyIds = null;
            mAllKeysConfirmed = false;
            mStatus = keyIdResultStatus;
            mExplicitKeyIds = null;
            mCombinedAutocryptState = null;
        }

        private KeyIdResult(HashSet<Long> keyIds, boolean allKeysConfirmed, KeyIdResultStatus keyIdResultStatus,
                AutocryptState combinedAutocryptState) {
            mKeySelectionPendingIntent = null;
            mUserKeyIds = keyIds;
            mAllKeysConfirmed = allKeysConfirmed;
            mStatus = keyIdResultStatus;
            mExplicitKeyIds = null;
            mCombinedAutocryptState = combinedAutocryptState;
        }

        private KeyIdResult(KeyIdResult keyIdResult, HashSet<Long> explicitKeyIds) {
            mKeySelectionPendingIntent = keyIdResult.mKeySelectionPendingIntent;
            mUserKeyIds = keyIdResult.mUserKeyIds;
            mAllKeysConfirmed = keyIdResult.mAllKeysConfirmed;
            mStatus = keyIdResult.mStatus;
            mExplicitKeyIds = explicitKeyIds;
            mCombinedAutocryptState = keyIdResult.mCombinedAutocryptState;
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

        public AutocryptState getCombinedAutocryptState() {
            return mCombinedAutocryptState;
        }
    }

    enum KeyIdResultStatus {
        OK, MISSING, DUPLICATE, NO_KEYS, NO_KEYS_ERROR
    }

    private KeyIdResult createKeysOkResult(HashSet<Long> encryptKeyIds, boolean allKeysConfirmed,
            AutocryptState combinedAutocryptState) {
        return new KeyIdResult(encryptKeyIds, allKeysConfirmed, KeyIdResultStatus.OK, combinedAutocryptState);
    }

    private KeyIdResult createNoKeysResult(Intent data,
            HashSet<Long> selectedKeyIds, ArrayList<String> missingEmails, ArrayList<String> duplicateEmails) {
        long[] keyIdsArray = KeyFormattingUtils.getUnboxedLongArray(selectedKeyIds);
        PendingIntent selectKeyPendingIntent = apiPendingIntentFactory.createSelectPublicKeyPendingIntent(
                data, keyIdsArray, missingEmails, duplicateEmails, false);

        return new KeyIdResult(selectKeyPendingIntent, KeyIdResultStatus.NO_KEYS);
    }

    private KeyIdResult createDuplicateKeysResult(Intent data, String packageName, ArrayList<String> duplicateEmails) {
        PendingIntent selectKeyPendingIntent = apiPendingIntentFactory.createDeduplicatePendingIntent(
                packageName, data, duplicateEmails);

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
