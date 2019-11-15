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

package org.sufficientlysecure.keychain.ui.base;


import java.util.Date;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.core.os.CancellationSignal;

import org.sufficientlysecure.keychain.operations.results.InputPendingResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.service.KeychainServiceTask;
import org.sufficientlysecure.keychain.service.KeychainServiceTask.OperationCallback;
import org.sufficientlysecure.keychain.service.ProgressDialogManager;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.OrbotRequiredDialogActivity;
import org.sufficientlysecure.keychain.ui.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.ui.RetryUploadDialogActivity;
import org.sufficientlysecure.keychain.ui.SecurityTokenOperationActivity;
import timber.log.Timber;


/**
 * Designed to be integrated into activities or fragments used for CryptoOperations.
 * Encapsulates the execution of a crypto operation and handling of input pending cases.s
 *
 * @param <T> The type of input parcel sent to the operation
 * @param <S> The type of result returned by the operation
 */
public class CryptoOperationHelper<T extends Parcelable, S extends OperationResult> {

    private long operationStartTime;

    public interface Callback<T extends Parcelable, S extends OperationResult> {
        T createOperationInput();

        void onCryptoOperationSuccess(S result);

        void onCryptoOperationCancelled();

        void onCryptoOperationError(S result);

        boolean onCryptoSetProgress(String msg, int progress, int max);
    }

    public static abstract class AbstractCallback<T extends Parcelable, S extends OperationResult>
            implements Callback<T,S> {
        @Override
        public void onCryptoOperationCancelled() {
            throw new UnsupportedOperationException("Unexpectedly cancelled operation!!");
        }

        @Override
        public boolean onCryptoSetProgress(String msg, int progress, int max) {
            return false;
        }
    }

    // request codes from CryptoOperationHelper are created essentially
    // a static property, used to identify requestCodes meant for this
    // particular helper. a request code looks as follows:
    // (id << 9) + (1<<8) + REQUEST_CODE_X
    // that is, starting from LSB, there are 8 bits request code, 1
    // fixed bit set, then 7 bit helper-id code. the first two
    // summands are stored in the mHelperId for easy operation.
    private final int mHelperId;
    // bitmask for helperId is everything except the least 8 bits
    private static final int HELPER_ID_BITMASK = ~0xff;

    private static final int REQUEST_CODE_PASSPHRASE = 1;
    private static final int REQUEST_CODE_NFC = 2;
    private static final int REQUEST_CODE_ENABLE_ORBOT = 3;
    private static final int REQUEST_CODE_RETRY_UPLOAD = 4;

    private Integer mProgressMessageResource;
    private boolean mCancellable = false;
    private Long minimumOperationDelay;

    private FragmentActivity mActivity;
    private Fragment mFragment;
    private Callback<T, S> mCallback;

    private boolean mUseFragment; // short hand for mActivity == null

    /**
     * If OperationHelper is being integrated into an activity
     */
    public CryptoOperationHelper(int id, FragmentActivity activity, Callback<T, S> callback,
            Integer progressMessageString) {
        mHelperId = (id << 9) + (1<<8);
        mActivity = activity;
        mUseFragment = false;
        mCallback = callback;
        mProgressMessageResource = progressMessageString;
    }

    /**
     * if OperationHelper is being integrated into a fragment
     */
    public CryptoOperationHelper(int id, Fragment fragment, Callback<T, S> callback, Integer progressMessageString) {
        mHelperId = (id << 9) + (1<<8);
        mFragment = fragment;
        mUseFragment = true;
        mProgressMessageResource = progressMessageString;
        mCallback = callback;
    }

    public void setProgressMessageResource(int id) {
        mProgressMessageResource = id;
    }

    public void setOperationMinimumDelay(Long delay) {
        this.minimumOperationDelay = delay;
    }

    public void setProgressCancellable(boolean cancellable) {
        mCancellable = cancellable;
    }

    private void initiateInputActivity(RequiredInputParcel requiredInput,
                                       CryptoInputParcel cryptoInputParcel) {

        Activity activity = mUseFragment ? mFragment.getActivity() : mActivity;

        switch (requiredInput.mType) {
            // always use CryptoOperationHelper.startActivityForResult!
            case SECURITY_TOKEN_MOVE_KEY_TO_CARD:
            case SECURITY_TOKEN_DECRYPT:
            case SECURITY_TOKEN_SIGN: {
                Intent intent = new Intent(activity, SecurityTokenOperationActivity.class);
                intent.putExtra(SecurityTokenOperationActivity.EXTRA_REQUIRED_INPUT, requiredInput);
                intent.putExtra(SecurityTokenOperationActivity.EXTRA_CRYPTO_INPUT, cryptoInputParcel);
                startActivityForResult(intent, REQUEST_CODE_NFC);
                return;
            }

            case PASSPHRASE:
            case PASSPHRASE_SYMMETRIC:
            case BACKUP_CODE:
            case NUMERIC_9X4:
            case NUMERIC_9X4_AUTOCRYPT: {
                Intent intent = new Intent(activity, PassphraseDialogActivity.class);
                intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
                intent.putExtra(PassphraseDialogActivity.EXTRA_CRYPTO_INPUT, cryptoInputParcel);
                startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);
                return;
            }

            case ENABLE_ORBOT: {
                Intent intent = new Intent(activity, OrbotRequiredDialogActivity.class);
                intent.putExtra(OrbotRequiredDialogActivity.EXTRA_CRYPTO_INPUT, cryptoInputParcel);
                startActivityForResult(intent, REQUEST_CODE_ENABLE_ORBOT);
                return;
            }

            case UPLOAD_FAIL_RETRY: {
                Intent intent = new Intent(activity, RetryUploadDialogActivity.class);
                intent.putExtra(RetryUploadDialogActivity.EXTRA_CRYPTO_INPUT, cryptoInputParcel);
                startActivityForResult(intent, REQUEST_CODE_RETRY_UPLOAD);
                return;
            }

            default: {
                throw new RuntimeException("Unhandled pending result!");
            }
        }
    }

    protected void startActivityForResult(Intent intent, int requestCode) {
        if (mUseFragment) {
            mFragment.startActivityForResult(intent, mHelperId + requestCode);
        } else {
            mActivity.startActivityForResult(intent, mHelperId + requestCode);
        }
    }

    /**
     * Attempts the result of an activity started by this helper. Returns true if requestCode is
     * recognized, false otherwise.
     * @return true if requestCode was recognized, false otherwise
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        Timber.d("received activity result in OperationHelper");

        if ((requestCode & HELPER_ID_BITMASK) != mHelperId) {
            // this wasn't meant for us to handle
            return false;
        }
        Timber.d("handling activity result in OperationHelper");
        // filter out mHelperId from requestCode
        requestCode ^= mHelperId;

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
                }
                break;
            }

            case REQUEST_CODE_NFC: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    CryptoInputParcel cryptoInput =
                            data.getParcelableExtra(SecurityTokenOperationActivity.RESULT_CRYPTO_INPUT);
                    cryptoOperation(cryptoInput);
                }
                break;
            }

            case REQUEST_CODE_ENABLE_ORBOT: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    CryptoInputParcel cryptoInput =
                            data.getParcelableExtra(
                                    OrbotRequiredDialogActivity.RESULT_CRYPTO_INPUT);
                    cryptoOperation(cryptoInput);
                }
                break;
            }

            case REQUEST_CODE_RETRY_UPLOAD: {
                if (resultCode == Activity.RESULT_OK) {
                    CryptoInputParcel cryptoInput =
                            data.getParcelableExtra(
                                    RetryUploadDialogActivity.RESULT_CRYPTO_INPUT);
                    cryptoOperation(cryptoInput);
                }
                break;
            }
        }

        return true;
    }

    public void cryptoOperation(final CryptoInputParcel cryptoInput) {
        T operationInput = mCallback.createOperationInput();
        if (operationInput == null) {
            return;
        }

        FragmentActivity activity = mUseFragment ? mFragment.getActivity() : mActivity;
        if (activity == null) {
            throw new NullPointerException();
        }

        ProgressDialogManager progressDialogManager;
        if (mProgressMessageResource != null) {
            progressDialogManager = new ProgressDialogManager(activity);
        } else {
            progressDialogManager = null;
        }

        KeychainServiceTask keychainServiceTask = KeychainServiceTask.create(activity);
        OperationCallback operationCallback = new OperationCallback() {
            @Override
            public void operationFinished(OperationResult result) {
                if (progressDialogManager != null) {
                    progressDialogManager.dismissAllowingStateLoss();
                }
                onHandleResult(result);
            }

            @Override
            public void setProgress(Integer resourceId, int current, int total) {
                String msgString = resourceId != null ? activity.getString(resourceId) : null;
                if (mCallback.onCryptoSetProgress(msgString, current, total)) {
                    return;
                }
                if (progressDialogManager != null) {
                    progressDialogManager.onSetProgress(resourceId, current, total);
                }
            }

            @Override
            public void setPreventCancel() {
                if (progressDialogManager != null) {
                    progressDialogManager.setPreventCancel();
                }
            }
        };

        CancellationSignal cancellationSignal =
                keychainServiceTask.startOperationInBackground(operationInput, cryptoInput, operationCallback);

        if (progressDialogManager != null) {
            progressDialogManager.showProgressDialog(activity.getString(mProgressMessageResource),
                    ProgressDialog.STYLE_HORIZONTAL, mCancellable ? cancellationSignal : null);
        }
    }

    public void cryptoOperation() {
        operationStartTime = SystemClock.elapsedRealtime();
        cryptoOperation(CryptoInputParcel.createCryptoInputParcel(new Date()));
    }

    @UiThread
    private void onHandleResult(OperationResult result) {
        Timber.d("Handling result in OperationHelper success: %s", result.success());

        if (result instanceof InputPendingResult) {
            InputPendingResult pendingResult = (InputPendingResult) result;
            if (pendingResult.isPending()) {
                RequiredInputParcel requiredInput = pendingResult.getRequiredInputParcel();
                initiateInputActivity(requiredInput, pendingResult.mCryptoInputParcel);
                return;
            }
        }

        long elapsedTime = SystemClock.elapsedRealtime() - operationStartTime;
        if (minimumOperationDelay == null || elapsedTime > minimumOperationDelay) {
            returnResultToCallback(result);
            return;
        }

        long artificialDelay = minimumOperationDelay - elapsedTime;
        new Handler().postDelayed(() -> returnResultToCallback(result), artificialDelay);
    }

    private void returnResultToCallback(OperationResult result) {
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