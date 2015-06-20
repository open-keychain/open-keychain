/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2015 Adithya Abraham Philip <adithyaphilip@gmail.com>
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

package org.sufficientlysecure.keychain.util.operation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.InputPendingResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.service.KeychainNewService;
import org.sufficientlysecure.keychain.service.KeychainService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.NfcOperationActivity;
import org.sufficientlysecure.keychain.ui.OrbotRequiredDialogActivity;
import org.sufficientlysecure.keychain.ui.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableProxy;

/**
 * Designed to be intergrated into activities or fragments used for CryptoOperations.
 * Encapsulates the execution of a crypto operation and handling of input pending cases.s
 *
 * @param <T> The type of input parcel sent to the operation
 * @param <S> The type of result retruend by the operation
 */
public abstract class OperationHelper<T extends Parcelable, S extends OperationResult> {
    public static final int REQUEST_CODE_PASSPHRASE = 0x00008001;
    public static final int REQUEST_CODE_NFC = 0x00008002;
    public static final int REQUEST_ENABLE_ORBOT = 0x00008004;

    private int mProgressMessageString;

    private FragmentActivity mActivity;
    private Fragment mFragment;

    private boolean mUseFragment;

    /**
     * If OperationHelper is being integrated into an activity
     *
     * @param activity
     */
    public OperationHelper(FragmentActivity activity, int progressMessageString) {
        mActivity = activity;
        mUseFragment = false;
        mProgressMessageString = progressMessageString;
    }

    /**
     * if OperationHelper is being integrated into a fragment
     *
     * @param fragment
     */
    public OperationHelper(Fragment fragment, int progressMessageString) {
        mFragment = fragment;
        mActivity = fragment.getActivity();
        mUseFragment = true;
        mProgressMessageString = progressMessageString;
    }

    private void initiateInputActivity(RequiredInputParcel requiredInput) {

        Log.d("PHILIP", "Initating input " + requiredInput.mType);
        switch (requiredInput.mType) {
            case NFC_KEYTOCARD:
            case NFC_DECRYPT:
            case NFC_SIGN: {
                Intent intent = new Intent(mActivity, NfcOperationActivity.class);
                intent.putExtra(NfcOperationActivity.EXTRA_REQUIRED_INPUT, requiredInput);
                if (mUseFragment) {
                    mFragment.startActivityForResult(intent, REQUEST_CODE_NFC);
                } else {
                    mActivity.startActivityForResult(intent, REQUEST_CODE_NFC);
                }
                return;
            }

            case PASSPHRASE:
            case PASSPHRASE_SYMMETRIC: {
                Intent intent = new Intent(mActivity, PassphraseDialogActivity.class);
                intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
                if (mUseFragment) {
                    mFragment.startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);
                } else {
                    mActivity.startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);
                }
                return;
            }

            case ENABLE_ORBOT: {
                Intent intent = new Intent(mActivity, OrbotRequiredDialogActivity.class);
                if (mUseFragment) {
                    mFragment.startActivityForResult(intent, REQUEST_ENABLE_ORBOT);
                } else {
                    mActivity.startActivityForResult(intent, REQUEST_ENABLE_ORBOT);
                }
                return;
            }
        }

        throw new RuntimeException("Unhandled pending result!");
    }

    /**
     * Attempts the result of an activity started by this helper. Returns true if requestCode is recognized,
     * false otherwise.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     * @return true if requestCode was recognized, false otherwise
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("PHILIP", "received activity result in OperationHelper");
        if (resultCode == Activity.RESULT_CANCELED) {
            onCryptoOperationCancelled();
            return true;
        }

        switch (requestCode) {
            case REQUEST_CODE_PASSPHRASE: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    CryptoInputParcel cryptoInput =
                            data.getParcelableExtra(PassphraseDialogActivity.RESULT_CRYPTO_INPUT);
                    cryptoOperation(cryptoInput);
                    return true;
                }
                break;
            }

            case REQUEST_CODE_NFC: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    CryptoInputParcel cryptoInput =
                            data.getParcelableExtra(NfcOperationActivity.RESULT_DATA);
                    cryptoOperation(cryptoInput);
                    return true;
                }
                break;
            }

            case REQUEST_ENABLE_ORBOT: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (data.getBooleanExtra(OrbotRequiredDialogActivity.RESULT_IGNORE_TOR, false)) {
                        cryptoOperation(new CryptoInputParcel(ParcelableProxy.getForNoProxy()));
                    }
                    return true;
                }
                break;
            }

            default: {
                return false;
            }
        }
        return true;
    }

    protected void dismissProgress() {
        ProgressDialogFragment progressDialogFragment =
                (ProgressDialogFragment) mActivity.getSupportFragmentManager().findFragmentByTag("progressDialog");

        if (progressDialogFragment == null) {
            return;
        }

        progressDialogFragment.dismissAllowingStateLoss();

    }

    public abstract T createOperationInput();

    public void cryptoOperation(CryptoInputParcel cryptoInput) {

        T operationInput = createOperationInput();
        if (operationInput == null) {
            return;
        }

        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(mActivity, KeychainNewService.class);

        intent.putExtra(KeychainNewService.EXTRA_OPERATION_INPUT, operationInput);
        intent.putExtra(KeychainNewService.EXTRA_CRYPTO_INPUT, cryptoInput);

        ServiceProgressHandler saveHandler = new ServiceProgressHandler(mActivity) {
            @Override
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {

                    // get returned data bundle
                    Bundle returnData = message.getData();
                    if (returnData == null) {
                        return;
                    }

                    final OperationResult result =
                            returnData.getParcelable(OperationResult.EXTRA_RESULT);

                    onHandleResult(result);
                }
            }
        };

        saveHandler.showProgressDialog(
                mActivity.getString(mProgressMessageString),
                ProgressDialog.STYLE_HORIZONTAL, false);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainService.EXTRA_MESSENGER, messenger);

        mActivity.startService(intent);

    }

    public void cryptoOperation() {
        cryptoOperation(new CryptoInputParcel());
    }

    protected void onCryptoOperationResult(S result) {
        Log.d("PHILIP", "cryptoResult " + result.success());
        if (result.success()) {
            onCryptoOperationSuccess(result);
        } else {
            onCryptoOperationError(result);
        }
    }

    abstract protected void onCryptoOperationSuccess(S result);

    protected void onCryptoOperationError(S result) {
        result.createNotify(mActivity).show();
    }

    protected void onCryptoOperationCancelled() {
    }

    public void onHandleResult(OperationResult result) {
        Log.d("PHILIP", "Handling result in OperationHelper");

        if (result instanceof InputPendingResult) {
            Log.d("PHILIP", "is pending result");
            InputPendingResult pendingResult = (InputPendingResult) result;
            if (pendingResult.isPending()) {

                Log.d("PHILIP", "Is pending");
                RequiredInputParcel requiredInput = pendingResult.getRequiredInputParcel();
                initiateInputActivity(requiredInput);
                return;
            }
        }

        dismissProgress();

        try {
            // noinspection unchecked, because type erasure :(
            onCryptoOperationResult((S) result);
        } catch (ClassCastException e) {
            throw new AssertionError("bad return class ("
                    + result.getClass().getSimpleName() + "), this is a programming error!");
        }

    }
}
