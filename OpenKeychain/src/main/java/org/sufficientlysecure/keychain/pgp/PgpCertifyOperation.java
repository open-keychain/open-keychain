package org.sufficientlysecure.keychain.pgp;

import android.content.Context;

import org.spongycastle.openpgp.PGPException;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper.NotFoundException;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel.CertifyAction;
import org.sufficientlysecure.keychain.service.results.CertifyResult;
import org.sufficientlysecure.keychain.service.results.EditKeyResult;
import org.sufficientlysecure.keychain.service.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.service.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.service.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class PgpCertifyOperation {

    private AtomicBoolean mCancelled;

    private ProviderHelper mProviderHelper;

    public PgpCertifyOperation(ProviderHelper providerHelper, AtomicBoolean cancelled) {
        mProviderHelper = providerHelper;

        mCancelled = cancelled;
    }

    private boolean checkCancelled() {
        return mCancelled != null && mCancelled.get();
    }

    public CertifyResult certify(CertifyActionsParcel parcel, String passphrase) {

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
        }

        ArrayList<UncachedKeyRing> certifiedKeys = new ArrayList<UncachedKeyRing>();

        log.add(LogType.MSG_CRT_CERTIFYING, 1);

        int certifyOk = 0, certifyError = 0;

        // Work through all requested certifications
        for (CertifyAction action : parcel.mCertifyActions) {

            // Check if we were cancelled
            if (checkCancelled()) {
                log.add(LogType.MSG_OPERATION_CANCELLED, 0);
                return new CertifyResult(CertifyResult.RESULT_CANCELLED, log);
            }

            try {

                if (action.mUserIds == null) {
                    log.add(LogType.MSG_CRT_CERTIFY_ALL, 2,
                            KeyFormattingUtils.convertKeyIdToHex(action.mMasterKeyId));
                } else {
                    log.add(LogType.MSG_CRT_CERTIFY_SOME, 2, action.mUserIds.size(),
                            KeyFormattingUtils.convertKeyIdToHex(action.mMasterKeyId));
                }

                CanonicalizedPublicKeyRing publicRing =
                        mProviderHelper.getCanonicalizedPublicKeyRing(action.mMasterKeyId);
                if ( ! Arrays.equals(publicRing.getFingerprint(), action.mFingerprint)) {
                    log.add(LogType.MSG_CRT_FP_MISMATCH, 3);
                    certifyError += 1;
                    continue;
                }

                UncachedKeyRing certifiedKey = certificationKey.certifyUserIds(publicRing, action.mUserIds, null, null);
                certifiedKeys.add(certifiedKey);

            } catch (NotFoundException e) {
                certifyError += 1;
                log.add(LogType.MSG_CRT_WARN_NOT_FOUND, 3);
            } catch (PGPException e) {
                certifyError += 1;
                log.add(LogType.MSG_CRT_WARN_CERT_FAILED, 3);
                Log.e(Constants.TAG, "Encountered PGPException during certification", e);
            }

        }

        log.add(LogType.MSG_CRT_SAVING, 1);

        // Check if we were cancelled
        if (checkCancelled()) {
            log.add(LogType.MSG_OPERATION_CANCELLED, 0);
            return new CertifyResult(CertifyResult.RESULT_CANCELLED, log);
        }

        // Write all certified keys into the database
        for (UncachedKeyRing certifiedKey : certifiedKeys) {

            // Check if we were cancelled
            if (checkCancelled()) {
                log.add(LogType.MSG_OPERATION_CANCELLED, 0);
                return new CertifyResult(CertifyResult.RESULT_CANCELLED, log, certifyOk, certifyError);
            }

            log.add(LogType.MSG_CRT_SAVE, 2,
                    KeyFormattingUtils.convertKeyIdToHex(certifiedKey.getMasterKeyId()));
            // store the signed key in our local cache
            SaveKeyringResult result = mProviderHelper.savePublicKeyRing(certifiedKey);

            if (result.success()) {
                certifyOk += 1;
            } else {
                log.add(LogType.MSG_CRT_WARN_SAVE_FAILED, 3);
            }

            // TODO do something with import results

        }

        log.add(LogType.MSG_CRT_SUCCESS, 0);
        return new CertifyResult(CertifyResult.RESULT_OK, log, certifyOk, certifyError);

    }

}
