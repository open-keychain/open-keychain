package org.sufficientlysecure.keychain.operations;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.DeleteResult;
import org.sufficientlysecure.keychain.operations.results.InputPendingResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.RevokeResult;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.RevokeKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;

public class RevokeOperation extends  BaseOperation<RevokeKeyringParcel> {

    public RevokeOperation(Context context, ProviderHelper providerHelper, Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    @NonNull
    @Override
    public OperationResult execute(RevokeKeyringParcel revokeKeyringParcel,
                                   CryptoInputParcel cryptoInputParcel) {

        long masterKeyId = revokeKeyringParcel.mMasterKeyId;

        OperationResult.OperationLog log = new OperationResult.OperationLog();
        log.add(OperationResult.LogType.MSG_REVOKE, 0,
                KeyFormattingUtils.beautifyKeyId(masterKeyId));

        try {

            Uri secretUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(masterKeyId);
            CachedPublicKeyRing keyRing = mProviderHelper.getCachedPublicKeyRing(secretUri);

            // check if this is a master secret key we can work with
            switch (keyRing.getSecretKeyType(masterKeyId)) {
                case GNU_DUMMY:
                    log.add(OperationResult.LogType.MSG_EK_ERROR_DUMMY, 1);
                    return new RevokeResult(RevokeResult.RESULT_ERROR, log, masterKeyId);
            }

            SaveKeyringParcel saveKeyringParcel = getRevokedSaveKeyringParcel(masterKeyId,
                    keyRing.getFingerprint());

            // all revoke operations are made atomic as of now
            saveKeyringParcel.setUpdateOptions(revokeKeyringParcel.mUpload, true,
                    revokeKeyringParcel.mKeyserver);

            InputPendingResult revokeAndUploadResult = new EditKeyOperation(mContext,
                    mProviderHelper, mProgressable, mCancelled)
                    .execute(saveKeyringParcel, cryptoInputParcel);

            if (revokeAndUploadResult.isPending()) {
                return revokeAndUploadResult;
            }

            log.add(revokeAndUploadResult, 1);

            if (revokeAndUploadResult.success()) {
                log.add(OperationResult.LogType.MSG_REVOKE_OK, 1);
                return new RevokeResult(RevokeResult.RESULT_OK, log, masterKeyId);
            } else {
                log.add(OperationResult.LogType.MSG_REVOKE_KEY_FAIL, 1);
                return new RevokeResult(RevokeResult.RESULT_ERROR, log, masterKeyId);
            }

        } catch (PgpKeyNotFoundException | ProviderHelper.NotFoundException e) {
            Log.e(Constants.TAG, "could not find key to revoke", e);
            log.add(OperationResult.LogType.MSG_REVOKE_KEY_FAIL, 1);
            return new RevokeResult(RevokeResult.RESULT_ERROR, log, masterKeyId);
        }
    }

    private SaveKeyringParcel getRevokedSaveKeyringParcel(long masterKeyId, byte[] fingerprint) {
        final String[] SUBKEYS_PROJECTION = new String[]{
                KeychainContract.Keys.KEY_ID
        };
        final int INDEX_KEY_ID = 0;

        Uri keysUri = KeychainContract.Keys.buildKeysUri(masterKeyId);
        Cursor subKeyCursor =
                mContext.getContentResolver().query(keysUri, SUBKEYS_PROJECTION, null, null, null);

        SaveKeyringParcel saveKeyringParcel =
                new SaveKeyringParcel(masterKeyId, fingerprint);

        // add all subkeys, for revocation
        while (subKeyCursor != null && subKeyCursor.moveToNext()) {
            saveKeyringParcel.mRevokeSubKeys.add(subKeyCursor.getLong(INDEX_KEY_ID));
        }
        if (subKeyCursor != null) {
            subKeyCursor.close();
        }

        final String[] USER_IDS_PROJECTION = new String[]{
                KeychainContract.UserPackets.USER_ID
        };
        final int INDEX_USER_ID = 0;

        Uri userIdsUri = KeychainContract.UserPackets.buildUserIdsUri(masterKeyId);
        Cursor userIdCursor = mContext.getContentResolver().query(
                        userIdsUri, USER_IDS_PROJECTION, null, null, null);

        while (userIdCursor != null && userIdCursor.moveToNext()) {
            saveKeyringParcel.mRevokeUserIds.add(userIdCursor.getString(INDEX_USER_ID));
        }
        if (userIdCursor != null) {
            userIdCursor.close();
        }

        return  saveKeyringParcel;
    }
}
