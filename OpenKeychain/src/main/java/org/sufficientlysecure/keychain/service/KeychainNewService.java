/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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


import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;

import de.greenrobot.event.EventBus;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.BaseOperation;
import org.sufficientlysecure.keychain.operations.CertifyOperation;
import org.sufficientlysecure.keychain.operations.EditKeyOperation;
import org.sufficientlysecure.keychain.operations.SignEncryptOperation;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerify;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.SignEncryptParcel;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel.CertifyAction;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.util.Log;

/**
 * This Service contains all important long lasting operations for OpenKeychain. It receives Intents with
 * data from the activities or other apps, executes them, and stops itself after doing them.
 */
public class KeychainNewService extends Service implements Progressable {

    /* extras that can be given by intent */
    public static final String EXTRA_OPERATION_INPUT = "op_input";
    public static final String EXTRA_CRYPTO_INPUT = "crypto_input";

    // this attribute can possibly merged with the one above? not sure...
    private AtomicBoolean mActionCanceled = new AtomicBoolean(false);

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This is run on the main thread, we need to spawn a runnable which runs on another thread for the actual operation
     */
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        EventBus bus = EventBus.getDefault();
        if (!bus.isRegistered(this)) {
            bus.register(this);
        }

        Bundle extras = intent.getExtras();
        if (extras != null) {
            bus.post(extras);
        } else {
            Log.e(Constants.TAG, "Extras bundle is null!");
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEventAsync(Bundle bundle) {

        // Input
        Parcelable inputParcel = bundle.getParcelable(EXTRA_OPERATION_INPUT);
        CryptoInputParcel cryptoInput = bundle.getParcelable(EXTRA_CRYPTO_INPUT);

        // Operation
        BaseOperation op;

        if (inputParcel instanceof SignEncryptParcel) {
            op = new SignEncryptOperation(this, new ProviderHelper(this), this, mActionCanceled);
        } else if (inputParcel instanceof PgpDecryptVerifyInputParcel) {
            op = new PgpDecryptVerify(this, new ProviderHelper(this), this);
        } else if (inputParcel instanceof SaveKeyringParcel) {
            op = new EditKeyOperation(this, new ProviderHelper(this), this, mActionCanceled);
        } else if (inputParcel instanceof CertifyAction) {
            op = new CertifyOperation(this, new ProviderHelper(this), this, mActionCanceled);
        } else {
            return;
        }

        @SuppressWarnings("unchecked") // this is unchecked, we make sure it's the correct op above!
        OperationResult result = op.execute(inputParcel, cryptoInput);

        // Result
        EventBus.getDefault().post(result);

        stopSelf();

    }

    /**
     * Set progress of ProgressDialog by sending message to handler on UI thread
     */
    @Override
    public void setProgress(String message, int progress, int max) {
        Log.d(Constants.TAG, "Send message by setProgress with progress=" + progress + ", max="
                + max);

        ProgressEvent event = new ProgressEvent(message, progress, max);
        EventBus.getDefault().post(event);

    }

    @Override
    public void setProgress(int resourceId, int progress, int max) {
        setProgress(getString(resourceId), progress, max);
    }

    @Override
    public void setProgress(int progress, int max) {
        setProgress(null, progress, max);
    }

    @Override
    public void setPreventCancel() {
        // sendMessageToHandler(MessageStatus.PREVENT_CANCEL);
    }


}
