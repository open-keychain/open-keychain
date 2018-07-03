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

package org.sufficientlysecure.keychain.service;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.daos.KeyWritableRepository;
import org.sufficientlysecure.keychain.operations.BackupOperation;
import org.sufficientlysecure.keychain.operations.BaseOperation;
import org.sufficientlysecure.keychain.operations.BenchmarkOperation;
import org.sufficientlysecure.keychain.operations.CertifyOperation;
import org.sufficientlysecure.keychain.operations.ChangeUnlockOperation;
import org.sufficientlysecure.keychain.operations.DeleteOperation;
import org.sufficientlysecure.keychain.operations.EditKeyOperation;
import org.sufficientlysecure.keychain.operations.ImportOperation;
import org.sufficientlysecure.keychain.operations.InputDataOperation;
import org.sufficientlysecure.keychain.operations.KeySyncOperation;
import org.sufficientlysecure.keychain.operations.KeySyncParcel;
import org.sufficientlysecure.keychain.operations.KeybaseVerificationOperation;
import org.sufficientlysecure.keychain.operations.PromoteKeyOperation;
import org.sufficientlysecure.keychain.operations.RevokeOperation;
import org.sufficientlysecure.keychain.operations.SignEncryptOperation;
import org.sufficientlysecure.keychain.operations.UploadOperation;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyOperation;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.SignEncryptParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;


public class KeychainService {
    private static KeychainService keychainService;

    public static KeychainService getInstance(Context context) {
        if (keychainService == null) {
            keychainService = new KeychainService(context.getApplicationContext());
        }
        return keychainService;
    }

    private KeychainService(Context context) {
        this.context = context;
        this.threadPoolExecutor = new ThreadPoolExecutor(0, 4, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.keyRepository = KeyWritableRepository.create(context);
    }

    private final Context context;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final KeyWritableRepository keyRepository;

    // this attribute can possibly merged with the one above? not sure...
    private AtomicBoolean operationCancelledBoolean = new AtomicBoolean(false);

    public void startOperationInBackground(Parcelable inputParcel, CryptoInputParcel cryptoInput,
            Progressable progressable, OperationCallback operationCallback) {
        operationCancelledBoolean.set(false);

        Runnable actionRunnable = () -> {
            BaseOperation op;

            if (inputParcel instanceof SignEncryptParcel) {
                op = new SignEncryptOperation(context, keyRepository, progressable, operationCancelledBoolean);
            } else if (inputParcel instanceof PgpDecryptVerifyInputParcel) {
                op = new PgpDecryptVerifyOperation(context, keyRepository, progressable);
            } else if (inputParcel instanceof SaveKeyringParcel) {
                op = new EditKeyOperation(context, keyRepository, progressable, operationCancelledBoolean);
            } else if (inputParcel instanceof  ChangeUnlockParcel) {
                op = new ChangeUnlockOperation(context, keyRepository, progressable);
            } else if (inputParcel instanceof RevokeKeyringParcel) {
                op = new RevokeOperation(context, keyRepository, progressable);
            } else if (inputParcel instanceof CertifyActionsParcel) {
                op = new CertifyOperation(context, keyRepository, progressable, operationCancelledBoolean);
            } else if (inputParcel instanceof DeleteKeyringParcel) {
                op = new DeleteOperation(context, keyRepository, progressable);
            } else if (inputParcel instanceof PromoteKeyringParcel) {
                op = new PromoteKeyOperation(context, keyRepository, progressable, operationCancelledBoolean);
            } else if (inputParcel instanceof ImportKeyringParcel) {
                op = new ImportOperation(context, keyRepository, progressable, operationCancelledBoolean);
            } else if (inputParcel instanceof BackupKeyringParcel) {
                op = new BackupOperation(context, keyRepository, progressable, operationCancelledBoolean);
            } else if (inputParcel instanceof UploadKeyringParcel) {
                op = new UploadOperation(context, keyRepository, progressable, operationCancelledBoolean);
            } else if (inputParcel instanceof KeybaseVerificationParcel) {
                op = new KeybaseVerificationOperation(context, keyRepository, progressable);
            } else if (inputParcel instanceof InputDataParcel) {
                op = new InputDataOperation(context, keyRepository, progressable);
            } else if (inputParcel instanceof BenchmarkInputParcel) {
                op = new BenchmarkOperation(context, keyRepository, progressable);
            } else if (inputParcel instanceof KeySyncParcel) {
                op = new KeySyncOperation(context, keyRepository, progressable, operationCancelledBoolean);
            } else {
                throw new AssertionError("Unrecognized input parcel in KeychainService!");
            }

            @SuppressWarnings("unchecked") // this is unchecked, we make sure it's the correct op above!
            OperationResult result = op.execute(inputParcel, cryptoInput);
            operationCallback.operationFinished(result);
        };

        threadPoolExecutor.execute(actionRunnable);
    }

    public void cancelRunningTask() {
        if (operationCancelledBoolean != null) {
            operationCancelledBoolean.set(true);
        }
    }

    public interface OperationCallback {
        void operationFinished(OperationResult data);
    }
}
