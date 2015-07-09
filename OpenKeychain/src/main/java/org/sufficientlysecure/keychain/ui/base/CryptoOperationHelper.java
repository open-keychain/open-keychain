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
import org.sufficientlysecure.keychain.operations.results.InputPendingResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.service.KeychainService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.NfcOperationActivity;
import org.sufficientlysecure.keychain.ui.OrbotRequiredDialogActivity;
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

    public interface Callback<T extends Parcelable, S extends OperationResult> {
        T createOperationInput();

        void onCryptoOperationSuccess(S result);

        void onCryptoOperationCancelled();

        void onCryptoOperationError(S result);

        boolean onCryptoSetProgress(String msg, int progress, int max);
    }

    // request codes from CryptoOperationHelper are created essentially
    // a static property, used to identify requestCodes meant for this
    // particular helper. a request code looks as follows:
    // (id << 9) + (1<<8) + REQUEST_CODE_X
    // that is, starting from LSB, there are 8 bits request code, 1
    // fixed bit set, then 7 bit operator-id code. the first two
    // summands are stored in the mId for easy operation.
    private final int mId;

    public static final int REQUEST_CODE_PASSPHRASE = 1;
    public static final int REQUEST_CODE_NFC = 2;
    public static final int REQUEST_CODE_ENABLE_ORBOT = 3;

    private Integer mProgressMessageResource;

    private FragmentActivity mActivity;
    private Fragment mFragment;
    private Callback<T, S> mCallback;

    private boolean mUseFragment; // short hand for mActivity == null

    /**
     * If OperationHelper is being integrated into an activity
     */
    public CryptoOperationHelper(int id, FragmentActivity activity, Callback<T, S> callback,
            Integer progressMessageString) {
        mId = (id << 9) + (1<<8);
        mActivity = activity;
        mUseFragment = false;
        mCallback = callback;
        mProgressMessageResource = progressMessageString;
    }

    /**
     * if OperationHelper is being integrated into a fragment
     */
    public CryptoOperationHelper(int id, Fragment fragment, Callback<T, S> callback, Integer progressMessageString) {
        mId = (id << 9) + (1<<8);
        mFragment = fragment;
        mUseFragment = true;
        mProgressMessageResource = progressMessageString;
        mCallback = callback;
    }

    public void setProgressMessageResource(int id) {
        mProgressMessageResource = id;
    }

    private void initiateInputActivity(RequiredInputParcel requiredInput,
                                       CryptoInputParcel cryptoInputParcel) {

        Activity activity = mUseFragment ? mFragment.getActivity() : mActivity;

        switch (requiredInput.mType) {
            // TODO: Verify that all started activities add to cryptoInputParcel if necessary (like OrbotRequiredDialogActivity)
            // don't forget to set mRequestedCode!
            case NFC_MOVE_KEY_TO_CARD:
            case NFC_DECRYPT:
            case NFC_SIGN: {
                Intent intent = new Intent(activity, NfcOperationActivity.class);
                intent.putExtra(NfcOperationActivity.EXTRA_REQUIRED_INPUT, requiredInput);
                if (mUseFragment) {
                    mFragment.startActivityForResult(intent, mId + REQUEST_CODE_NFC);
                } else {
                    activity.startActivityForResult(intent, mId + REQUEST_CODE_NFC);
                }
                return;
            }

            case PASSPHRASE:
            case PASSPHRASE_SYMMETRIC: {
                Intent intent = new Intent(activity, PassphraseDialogActivity.class);
                intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
                if (mUseFragment) {
                    mFragment.startActivityForResult(intent, mId + REQUEST_CODE_PASSPHRASE);
                } else {
                    activity.startActivityForResult(intent, mId + REQUEST_CODE_PASSPHRASE);
                }
                return;
            }

            case ENABLE_ORBOT: {
                Intent intent = new Intent(activity, OrbotRequiredDialogActivity.class);
                intent.putExtra(OrbotRequiredDialogActivity.EXTRA_CRYPTO_INPUT, cryptoInputParcel);
                if (mUseFragment) {
                    mFragment.startActivityForResult(intent, mId + REQUEST_CODE_ENABLE_ORBOT);
                } else {
                    activity.startActivityForResult(intent, mId + REQUEST_CODE_ENABLE_ORBOT);
                }
                return;
            }

            default: {
                throw new RuntimeException("Unhandled pending result!");
            }
        }
    }

    /**
     * Attempts the result of an activity started by this helper. Returns true if requestCode is
     * recognized, false otherwise.
     * @return true if requestCode was recognized, false otherwise
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(Constants.TAG, "received activity result in OperationHelper");

        if ((requestCode & mId) != mId) {
            // this wasn't meant for us to handle
            return false;
        }
        // filter out mId from requestCode
        requestCode ^= mId;

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

            case REQUEST_CODE_ENABLE_ORBOT: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    CryptoInputParcel cryptoInput =
                            data.getParcelableExtra(
                                    OrbotRequiredDialogActivity.RESULT_CRYPTO_INPUT);
                    cryptoOperation(cryptoInput);
                    return true;
                }
            }
        }

        return false;
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

    public void cryptoOperation(final CryptoInputParcel cryptoInput) {

        FragmentActivity activity = mUseFragment ? mFragment.getActivity() : mActivity;

        T operationInput = mCallback.createOperationInput();
        if (operationInput == null) {
            return;
        }

        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(activity, KeychainService.class);

        intent.putExtra(KeychainService.EXTRA_OPERATION_INPUT, operationInput);
        intent.putExtra(KeychainService.EXTRA_CRYPTO_INPUT, cryptoInput);

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

                    onHandleResult(result, cryptoInput);
                }
            }

            @Override
            protected void onSetProgress(String msg, int progress, int max) {
                // allow handling of progress in fragment, or delegate upwards
                if (!mCallback.onCryptoSetProgress(msg, progress, max)) {
                    super.onSetProgress(msg, progress, max);
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainService.EXTRA_MESSENGER, messenger);

        if (mProgressMessageResource != null) {
            saveHandler.showProgressDialog(
                    activity.getString(mProgressMessageResource),
                    ProgressDialog.STYLE_HORIZONTAL, false);
        }

        activity.startService(intent);
    }

    public void cryptoOperation() {
        cryptoOperation(new CryptoInputParcel());
    }

    public void onHandleResult(OperationResult result, CryptoInputParcel oldCryptoInput) {
        Log.d(Constants.TAG, "Handling result in OperationHelper success: " + result.success());

        if (result instanceof InputPendingResult) {
            InputPendingResult pendingResult = (InputPendingResult) result;
            if (pendingResult.isPending()) {

                RequiredInputParcel requiredInput = pendingResult.getRequiredInputParcel();
                initiateInputActivity(requiredInput, oldCryptoInput);
                return;
            }
        }

        dismissProgress();

        try {
            if (result.success()) {
                // noinspection unchecked, because type erasure :(
                mCallback.onCryptoOperationSuccess((S) result);
            } else {
                // noinspection unchecked, because type erasure :(
                mCallback.onCryptoOperationError((S) result);
            }
        } catch (ClassCastException e) {
            throw new AssertionError("bad return class ("
                    + result.getClass().getSimpleName() + "), this is a programming error!");
        }
    }
}