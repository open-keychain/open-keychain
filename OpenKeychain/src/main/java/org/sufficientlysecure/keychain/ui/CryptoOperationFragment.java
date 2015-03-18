package org.sufficientlysecure.keychain.ui;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;

import org.sufficientlysecure.keychain.operations.results.CertifyResult;
import org.sufficientlysecure.keychain.operations.results.InputPendingResult;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler.MessageStatus;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.NfcOperationsParcel;


public abstract class CryptoOperationFragment extends Fragment {

    public static final int REQUEST_CODE_PASSPHRASE = 0x00008001;
    public static final int REQUEST_CODE_NFC = 0x00008002;

    private void startPassphraseDialog(long subkeyId) {
        Intent intent = new Intent(getActivity(), PassphraseDialogActivity.class);
        intent.putExtra(PassphraseDialogActivity.EXTRA_SUBKEY_ID, subkeyId);
        startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);
    }

    private void initiateNfcInput(NfcOperationsParcel nfcOps) {
        Intent intent = new Intent(getActivity(), NfcOperationActivity.class);
        intent.putExtra(NfcOperationActivity.EXTRA_PIN, "123456");
        intent.putExtra(NfcOperationActivity.EXTRA_NFC_OPS, nfcOps);
        startActivityForResult(intent, REQUEST_CODE_NFC);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PASSPHRASE: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String passphrase = data.getStringExtra(PassphraseDialogActivity.MESSAGE_DATA_PASSPHRASE);
                    cryptoOperation(passphrase);
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

        if (message.arg1 == MessageStatus.OKAY.ordinal()) {
            Bundle data = message.getData();

            InputPendingResult result = data.getParcelable(CertifyResult.EXTRA_RESULT);

            if (result != null && result.isPending()) {
                if (result.isPassphrasePending()) {
                    startPassphraseDialog(result.getPassphraseKeyId());
                    return true;
                } else if (result.isNfcPending()) {
                    NfcOperationsParcel requiredInput = result.getNfcOperationsParcel();
                    initiateNfcInput(requiredInput);
                    return true;
                } else {
                    throw new RuntimeException("Unhandled pending result!");
                }
            }
        }

        return false;
    }

    protected abstract void cryptoOperation(CryptoInputParcel cryptoInput);
    protected abstract void cryptoOperation(String passphrase);

}
