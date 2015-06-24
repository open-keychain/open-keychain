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

package org.sufficientlysecure.keychain.ui.base;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import android.support.v4.app.FragmentManager;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.InputPendingResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.service.KeychainNewService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.NfcOperationActivity;
import org.sufficientlysecure.keychain.ui.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

/**
 * Designed to be integrated into activities or fragments used for CryptoOperations.
 * Encapsulates the execution of a crypto operation and handling of input pending cases.s
 *
 * @param <T> The type of input parcel sent to the operation
 * @param <S> The type of result returned by the operation
 */
public class CryptoOperationHelper<T extends Parcelable, S extends OperationResult> {

    public interface Callback <T extends Parcelable, S extends  OperationResult> {
        T createOperationInput();
        void onCryptoOperationSuccess(S result);
        void onCryptoOperationCancelled();
        void onCryptoOperationError(S result);
    }

    public static final int REQUEST_CODE_PASSPHRASE = 0x00008001;
    public static final int REQUEST_CODE_NFC = 0x00008002;

    // keeps track of request code used to start an activity from this CryptoOperationHelper.
    // this is necessary when multiple CryptoOperationHelpers are used in the same fragment/activity
    // otherwise all CryptoOperationHandlers may respond to the same onActivityResult
    private int mRequestedCode = -1;

    private int mProgressMessageResource;

    private FragmentActivity mActivity;
    private Fragment mFragment;
    private Callback<T, S> mCallback;

    private boolean mUseFragment; // short hand for mActivity == null

    /**
     * If OperationHelper is being integrated into an activity
     *
     * @param activity
     */
    public CryptoOperationHelper(FragmentActivity activity, Callback<T, S> callback, int progressMessageString) {
        mActivity = activity;
        mUseFragment = false;
        mCallback = callback;
        mProgressMessageResource = progressMessageString;
    }

    /**
     * if OperationHelper is being integrated into a fragment
     *
     * @param fragment
     */
    public CryptoOperationHelper(Fragment fragment, Callback<T, S> callback, int progressMessageString) {
        mFragment = fragment;
        mUseFragment = true;
        mProgressMessageResource = progressMessageString;
        mCallback = callback;
    }

    /**
     * if OperationHelper is being integrated into a fragment with default message for the progress dialog
     *
     * @param fragment
     */
    public CryptoOperationHelper(Fragment fragment, Callback<T, S> callback) {
        mFragment = fragment;
        mUseFragment = true;
        mProgressMessageResource = R.string.progress_building_key;
        mCallback = callback;
    }

    public void setProgressMessageResource(int id) {
        mProgressMessageResource = id;
    }

    private void initiateInputActivity(RequiredInputParcel requiredInput) {

        Activity activity = mUseFragment ? mFragment.getActivity() : mActivity;

        switch (requiredInput.mType) {
            case NFC_KEYTOCARD:
            case NFC_DECRYPT:
            case NFC_SIGN: {
                Intent intent = new Intent(activity, NfcOperationActivity.class);
                intent.putExtra(NfcOperationActivity.EXTRA_REQUIRED_INPUT, requiredInput);
                mRequestedCode = REQUEST_CODE_NFC;
                if (mUseFragment) {
                    mFragment.startActivityForResult(intent, mRequestedCode);
                } else {
                    mActivity.startActivityForResult(intent, mRequestedCode);
                }
                return;
            }

            case PASSPHRASE:
            case PASSPHRASE_SYMMETRIC: {
                Intent intent = new Intent(activity, PassphraseDialogActivity.class);
                intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
                mRequestedCode = REQUEST_CODE_PASSPHRASE;
                if (mUseFragment) {
                    mFragment.startActivityForResult(intent, mRequestedCode);
                } else {
                    mActivity.startActivityForResult(intent, mRequestedCode);
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
        Log.d(Constants.TAG, "received activity result in OperationHelper");

        if (mRequestedCode != requestCode) {
            // this wasn't meant for us to handle
            return false;
        } else {
            mRequestedCode = -1;
        }
        if (resultCode == Activity.RESULT_CANCELED) {
            mCallback.onCryptoOperationCancelled();
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

            default: {
                return false;
            }
        }
        return true;
    }

    protected void dismissProgress() {
        FragmentManager fragmentManager =
                mUseFragment ? mFragment.getFragmentManager() :
                        mActivity.getSupportFragmentManager();

        if (fragmentManager == null) { // the fragment holding us has died
            // fragmentManager was null when used with DialogFragments. (they close on click?)
            return;
        }

        ProgressDialogFragment progressDialogFragment =
                (ProgressDialogFragment) fragmentManager.findFragmentByTag(
                        ServiceProgressHandler.TAG_PROGRESS_DIALOG);

        if (progressDialogFragment == null) {
            return;
        }

        progressDialogFragment.dismissAllowingStateLoss();

    }

    public void cryptoOperation(CryptoInputParcel cryptoInput) {

        FragmentActivity activity = mUseFragment ? mFragment.getActivity() : mActivity;

        T operationInput = mCallback.createOperationInput();
        if (operationInput == null) {
            return;
        }

        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(activity, KeychainNewService.class);

        intent.putExtra(KeychainNewService.EXTRA_OPERATION_INPUT, operationInput);
        intent.putExtra(KeychainNewService.EXTRA_CRYPTO_INPUT, cryptoInput);

        ServiceProgressHandler saveHandler = new ServiceProgressHandler(activity) {
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
                activity.getString(mProgressMessageResource),
                ProgressDialog.STYLE_HORIZONTAL, false);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainNewService.EXTRA_MESSENGER, messenger);

        activity.startService(intent);

    }

    public void cryptoOperation() {
        cryptoOperation(new CryptoInputParcel());
    }

    protected void onCryptoOperationResult(S result) {
        if (result.success()) {
            mCallback.onCryptoOperationSuccess(result);
        } else {
            mCallback.onCryptoOperationError(result);
        }
    }

    public void onHandleResult(OperationResult result) {
        Log.d(Constants.TAG, "Handling result in OperationHelper success: " + result.success());

        if (result instanceof InputPendingResult) {
            InputPendingResult pendingResult = (InputPendingResult) result;
            if (pendingResult.isPending()) {

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