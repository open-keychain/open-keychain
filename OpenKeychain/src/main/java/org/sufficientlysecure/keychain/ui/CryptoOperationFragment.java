package org.sufficientlysecure.keychain.ui;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;

import org.sufficientlysecure.keychain.operations.results.CertifyResult;
import org.sufficientlysecure.keychain.operations.results.InputPendingResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;


public abstract class CryptoOperationFragment extends Fragment {

    public static final int REQUEST_CODE_PASSPHRASE = 0x00008001;
    public static final int REQUEST_CODE_NFC = 0x00008002;

    private void initiateInputActivity(RequiredInputParcel requiredInput) {

        switch (requiredInput.mType) {
            case NFC_DECRYPT:
            case NFC_SIGN: {
                Intent intent = new Intent(getActivity(), NfcOperationActivity.class);
                intent.putExtra(NfcOperationActivity.EXTRA_REQUIRED_INPUT, requiredInput);
                startActivityForResult(intent, REQUEST_CODE_NFC);
                return;
            }

            case PASSPHRASE: {
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
        switch (requestCode) {
            case REQUEST_CODE_PASSPHRASE: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    CryptoInputParcel cryptoInput =
                            data.getParcelableExtra(PassphraseDialogActivity.RESULT_DATA);
                    cryptoOperation(cryptoInput);
                }
                return;
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

    public boolean handlePendingMessage(Message message) {

        if (message.arg1 == ServiceProgressHandler.MessageStatus.OKAY.ordinal()) {
            Bundle data = message.getData();

            OperationResult result = data.getParcelable(CertifyResult.EXTRA_RESULT);
            if (result == null || ! (result instanceof InputPendingResult)) {
                return false;
            }

            InputPendingResult pendingResult = (InputPendingResult) result;
            if (pendingResult.isPending()) {
                RequiredInputParcel requiredInput = pendingResult.getRequiredInputParcel();
                initiateInputActivity(requiredInput);
                return true;
            }
        }

        return false;
    }

    protected abstract void cryptoOperation(CryptoInputParcel cryptoInput);

}
