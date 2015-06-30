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

import org.sufficientlysecure.keychain.operations.results.ConsolidateResult;
import org.sufficientlysecure.keychain.operations.results.DeleteResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRingData;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.ContactSyncAdapterService;
import org.sufficientlysecure.keychain.service.DeleteKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

/** An operation which implements a high level keyring delete operation.
 *
 * This operation takes a list of masterKeyIds as input, deleting all
 * corresponding public keyrings from the database. Secret keys can
 * be deleted as well, but only explicitly and individually, not as
 * a list.
 *
 */
public class DeleteOperation extends BaseOperation<DeleteKeyringParcel> {

    public DeleteOperation(Context context, ProviderHelper providerHelper, Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    @Override
    public DeleteResult execute(DeleteKeyringParcel deleteKeyringParcel,
                                CryptoInputParcel cryptoInputParcel) {

        long[] masterKeyIds = deleteKeyringParcel.mMasterKeyIds;
        boolean isSecret = deleteKeyringParcel.mIsSecret;

        OperationLog log = new OperationLog();

        if (masterKeyIds.length == 0) {
            log.add(LogType.MSG_DEL_ERROR_EMPTY, 0);
            return new DeleteResult(DeleteResult.RESULT_ERROR, log, 0, 0);
        }

        if (isSecret && masterKeyIds.length > 1) {
            log.add(LogType.MSG_DEL_ERROR_MULTI_SECRET, 0);
            return new DeleteResult(DeleteResult.RESULT_ERROR, log, 0, 0);
        }

        log.add(LogType.MSG_DEL, 0, masterKeyIds.length);

        boolean cancelled = false;
        int success = 0, fail = 0;
        for (long masterKeyId : masterKeyIds) {
            if (checkCancelled()) {
                cancelled = true;
                break;
            }
            int count = mProviderHelper.getContentResolver().delete(
                    KeyRingData.buildPublicKeyRingUri(masterKeyId), null, null
            );
            if (count > 0) {
                log.add(LogType.MSG_DEL_KEY, 1, KeyFormattingUtils.beautifyKeyId(masterKeyId));
                success += 1;
            } else {
                log.add(LogType.MSG_DEL_KEY_FAIL, 1, KeyFormattingUtils.beautifyKeyId(masterKeyId));
                fail += 1;
            }
        }

        if (isSecret && success > 0) {
            log.add(LogType.MSG_DEL_CONSOLIDATE, 1);
            ConsolidateResult sub = mProviderHelper.consolidateDatabaseStep1(mProgressable);
            log.add(sub, 2);
        }

        int result = DeleteResult.RESULT_OK;
        if (success > 0) {
            // make sure new data is synced into contacts
            ContactSyncAdapterService.requestSync();

            log.add(LogType.MSG_DEL_OK, 0, success);
        }
        if (fail > 0) {
            log.add(LogType.MSG_DEL_FAIL, 0, fail);
            result |= DeleteResult.RESULT_WARNINGS;
            if (success == 0) {
                result |= DeleteResult.RESULT_ERROR;
            }
        }
        if (cancelled) {
            log.add(LogType.MSG_OPERATION_CANCELLED, 0);
            result |= DeleteResult.RESULT_CANCELLED;
        }

        return new DeleteResult(result, log, success, fail);

    }

}
