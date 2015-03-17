/*
 * Copyright (C) 2014-2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.operations;

import android.content.Context;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserver;
import org.sufficientlysecure.keychain.keyimport.Keyserver.AddKeyException;
import org.sufficientlysecure.keychain.operations.results.CertifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKeyRing;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper.NotFoundException;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel.CertifyAction;
import org.sufficientlysecure.keychain.service.ContactSyncAdapterService;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** An operation which implements a high level user id certification operation.
 *
 * This operation takes a specific CertifyActionsParcel as its input. These
 * contain a masterKeyId to be used for certification, and a list of
 * masterKeyIds and related user ids to certify.
 *
 * @see CertifyActionsParcel
 *
 */
public class CertifyOperation extends BaseOperation {

    public CertifyOperation(Context context, ProviderHelper providerHelper, Progressable progressable, AtomicBoolean cancelled) {
        super(context, providerHelper, progressable, cancelled);
    }

    public CertifyResult certify(CertifyActionsParcel parcel, String keyServerUri) {

        OperationLog log = new OperationLog();
        log.add(LogType.MSG_CRT, 0);

        // Retrieve and unlock secret key
        CanonicalizedSecretKey certificationKey;
        try {

            log.add(LogType.MSG_CRT_MASTER_FETCH, 1);
            CanonicalizedSecretKeyRing secretKeyRing =
                    mProviderHelper.getCanonicalizedSecretKeyRing(parcel.mMasterKeyId);
            log.add(LogType.MSG_CRT_UNLOCK, 1);
            certificationKey = secretKeyRing.getSecretKey();
            if (certificationKey.getSecretKeyType() == SecretKeyType.DIVERT_TO_CARD) {
                log.add(LogType.MSG_CRT_ERROR_DIVERT, 2);
                return new CertifyResult(CertifyResult.RESULT_ERROR, log);
            }

            // certification is always with the master key id, so use that one
            String passphrase = getCachedPassphrase(parcel.mMasterKeyId, parcel.mMasterKeyId);

            if (!certificationKey.unlock(passphrase)) {
                log.add(LogType.MSG_CRT_ERROR_UNLOCK, 2);
                return new CertifyResult(CertifyResult.RESULT_ERROR, log);
            }
        } catch (PgpGeneralException e) {
            log.add(LogType.MSG_CRT_ERROR_UNLOCK, 2);
            return new CertifyResult(CertifyResult.RESULT_ERROR, log);
        } catch (NotFoundException e) {
            log.add(LogType.MSG_CRT_ERROR_MASTER_NOT_FOUND, 2);
            return new CertifyResult(CertifyResult.RESULT_ERROR, log);
        } catch (NoSecretKeyException e) {
            log.add(LogType.MSG_CRT_ERROR_MASTER_NOT_FOUND, 2);
            return new CertifyResult(CertifyResult.RESULT_ERROR, log);
        }

        ArrayList<UncachedKeyRing> certifiedKeys = new ArrayList<>();

        log.add(LogType.MSG_CRT_CERTIFYING, 1);

        int certifyOk = 0, certifyError = 0, uploadOk = 0, uploadError = 0;

        // Work through all requested certifications
        for (CertifyAction action : parcel.mCertifyActions) {

            // Check if we were cancelled
            if (checkCancelled()) {
                log.add(LogType.MSG_OPERATION_CANCELLED, 0);
                return new CertifyResult(CertifyResult.RESULT_CANCELLED, log);
            }

            try {

                if (action.mMasterKeyId == parcel.mMasterKeyId) {
                    log.add(LogType.MSG_CRT_ERROR_SELF, 2);
                    certifyError += 1;
                    continue;
                }

                CanonicalizedPublicKeyRing publicRing =
                        mProviderHelper.getCanonicalizedPublicKeyRing(action.mMasterKeyId);

                UncachedKeyRing certifiedKey = null;
                if (action.mUserIds != null) {
                    log.add(LogType.MSG_CRT_CERTIFY_UIDS, 2, action.mUserIds.size(),
                            KeyFormattingUtils.convertKeyIdToHex(action.mMasterKeyId));

                    certifiedKey = certificationKey.certifyUserIds(
                            publicRing, action.mUserIds, null, null);
                }

                if (action.mUserAttributes != null) {
                    log.add(LogType.MSG_CRT_CERTIFY_UATS, 2, action.mUserAttributes.size(),
                            KeyFormattingUtils.convertKeyIdToHex(action.mMasterKeyId));

                    certifiedKey = certificationKey.certifyUserAttributes(
                            publicRing, action.mUserAttributes, null, null);
                }

                if (certifiedKey == null) {
                    certifyError += 1;
                    log.add(LogType.MSG_CRT_WARN_CERT_FAILED, 3);
                }
                certifiedKeys.add(certifiedKey);

            } catch (NotFoundException e) {
                certifyError += 1;
                log.add(LogType.MSG_CRT_WARN_NOT_FOUND, 3);
            }

        }

        log.add(LogType.MSG_CRT_SAVING, 1);

        // Check if we were cancelled
        if (checkCancelled()) {
            log.add(LogType.MSG_OPERATION_CANCELLED, 0);
            return new CertifyResult(CertifyResult.RESULT_CANCELLED, log);
        }

        HkpKeyserver keyServer = null;
        ImportExportOperation importExportOperation = null;
        if (keyServerUri != null) {
            keyServer = new HkpKeyserver(keyServerUri);
            importExportOperation = new ImportExportOperation(mContext, mProviderHelper, mProgressable);
        }

        // Write all certified keys into the database
        for (UncachedKeyRing certifiedKey : certifiedKeys) {

            // Check if we were cancelled
            if (checkCancelled()) {
                log.add(LogType.MSG_OPERATION_CANCELLED, 0);
                return new CertifyResult(CertifyResult.RESULT_CANCELLED, log, certifyOk, certifyError, uploadOk, uploadError);
            }

            log.add(LogType.MSG_CRT_SAVE, 2,
                    KeyFormattingUtils.convertKeyIdToHex(certifiedKey.getMasterKeyId()));
            // store the signed key in our local cache
            mProviderHelper.clearLog();
            SaveKeyringResult result = mProviderHelper.savePublicKeyRing(certifiedKey);

            if (importExportOperation != null) {
                // TODO use subresult, get rid of try/catch!
                try {
                    importExportOperation.uploadKeyRingToServer(keyServer, certifiedKey);
                    uploadOk += 1;
                } catch (AddKeyException e) {
                    Log.e(Constants.TAG, "error uploading key", e);
                    uploadError += 1;
                }
            }

            if (result.success()) {
                certifyOk += 1;
            } else {
                log.add(LogType.MSG_CRT_WARN_SAVE_FAILED, 3);
            }

            log.add(result, 2);

        }

        if (certifyOk == 0) {
            log.add(LogType.MSG_CRT_ERROR_NOTHING, 0);
            return new CertifyResult(CertifyResult.RESULT_ERROR, log, certifyOk, certifyError, uploadOk, uploadError);
        }

        log.add(LogType.MSG_CRT_SUCCESS, 0);
        //since only verified keys are synced to contacts, we need to initiate a sync now
        ContactSyncAdapterService.requestSync();
        
        return new CertifyResult(CertifyResult.RESULT_OK, log, certifyOk, certifyError, uploadOk, uploadError);

    }

}
