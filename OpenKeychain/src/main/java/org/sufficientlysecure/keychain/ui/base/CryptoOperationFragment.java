/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.InputPendingResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.service.KeychainNewService;
import org.sufficientlysecure.keychain.service.KeychainService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.NfcOperationActivity;
import org.sufficientlysecure.keychain.ui.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;


/**
 * All fragments executing crypto operations need to extend this class.
 */
public abstract class CryptoOperationFragment <T extends Parcelable, S extends OperationResult>
        extends Fragment {

    public static final int REQUEST_CODE_PASSPHRASE = 0x00008001;
    public static final int REQUEST_CODE_NFC = 0x00008002;

    private void initiateInputActivity(RequiredInputParcel requiredInput) {

        switch (requiredInput.mType) {
            case NFC_KEYTOCARD:
            case NFC_DECRYPT:
            case NFC_SIGN: {
                Intent intent = new Intent(getActivity(), NfcOperationActivity.class);
                intent.putExtra(NfcOperationActivity.EXTRA_REQUIRED_INPUT, requiredInput);
                startActivityForResult(intent, REQUEST_CODE_NFC);
                return;
            }

            case PASSPHRASE:
            case PASSPHRASE_SYMMETRIC: {
                Intent intent = new Intent(getActivity(), PassphraseDialogActivity.class);
                intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
                startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);
                return;
            }
        }

        throw new RuntimeException("Unhandled pending result!");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            onCryptoOperationCancelled();
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_PASSPHRASE: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    CryptoInputParcel cryptoInput =
                            data.getParcelableExtra(PassphraseDialogActivity.RESULT_CRYPTO_INPUT);
                    cryptoOperation(cryptoInput);
                    return;
                }
                break;
            }

            case REQUEST_CODE_NFC: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    CryptoInputParcel cryptoInput =
                            data.getParcelableExtra(NfcOperationActivity.RESULT_DATA);
                    cryptoOperation(cryptoInput);
                    return;
                }
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    protected void dismissProgress() {

        ProgressDialogFragment progressDialogFragment =
                (ProgressDialogFragment) getFragmentManager().findFragmentByTag("progressDialog");

        if (progressDialogFragment == null) {
            return;
        }

        progressDialogFragment.dismissAllowingStateLoss();

    }

    protected abstract T createOperationInput();

    protected void cryptoOperation(CryptoInputParcel cryptoInput) {

        T operationInput = createOperationInput();
        if (operationInput == null) {
            return;
        }

        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(getActivity(), KeychainNewService.class);

        intent.putExtra(KeychainNewService.EXTRA_OPERATION_INPUT, operationInput);
        intent.putExtra(KeychainNewService.EXTRA_CRYPTO_INPUT, cryptoInput);

        ServiceProgressHandler saveHandler = new ServiceProgressHandler(getActivity()) {
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

            @Override
            protected void onSetProgress(String msg, int progress, int max) {
                // allow handling of progress in fragment, or delegate upwards
                if ( ! onCryptoSetProgress(msg, progress, max)) {
                    super.onSetProgress(msg, progress, max);
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainService.EXTRA_MESSENGER, messenger);

        saveHandler.showProgressDialog(
                getString(R.string.progress_building_key),
                ProgressDialog.STYLE_HORIZONTAL, false);

        getActivity().startService(intent);

    }

    protected void cryptoOperation() {
        cryptoOperation(new CryptoInputParcel());
    }

    protected void onCryptoOperationResult(S result) {
        if (result.success()) {
            onCryptoOperationSuccess(result);
        } else {
            onCryptoOperationError(result);
        }
    }

    abstract protected void onCryptoOperationSuccess(S result);

    protected void onCryptoOperationError(S result) {
        result.createNotify(getActivity()).show();
    }

    protected void onCryptoOperationCancelled() {
    }

    public void onHandleResult(OperationResult result) {

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

    protected boolean onCryptoSetProgress(String msg, int progress, int max) {
        return false;
    }

}
