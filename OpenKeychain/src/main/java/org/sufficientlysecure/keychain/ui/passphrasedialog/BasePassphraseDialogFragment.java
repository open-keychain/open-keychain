package org.sufficientlysecure.keychain.ui.passphrasedialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.util.Passphrase;

abstract class BasePassphraseDialogFragment extends DialogFragment implements TextView.OnEditorActionListener {
    protected boolean mIsCancelled = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof PassphraseDialogActivity)) {
            throw new RuntimeException("Activity must be a " +
                    PassphraseDialogActivity.class.getSimpleName());
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        // note we need no synchronization here, this variable is only accessed in the ui thread
        mIsCancelled = true;

        getActivity().setResult(Activity.RESULT_CANCELED);
        getActivity().finish();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        hideKeyboard();
    }

    private void hideKeyboard() {
        if (getActivity() == null) {
            return;
        }

        InputMethodManager inputManager = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        // Associate the "done" button on the soft keyboard with the okay button in the view
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            AlertDialog dialog = ((AlertDialog) getDialog());
            Button bt = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            bt.performClick();
            return true;
        }
        return false;
    }

    void returnWithPassphrase(Passphrase passphrase) {
        // any indication this isn't needed anymore, don't do it.
        if (mIsCancelled || getActivity() == null) {
            return;
        }
        CryptoInputParcel cryptoParcel = getArguments().getParcelable(PassphraseDialogActivity.EXTRA_CRYPTO_INPUT);

        // noinspection ConstantConditions, non-null cryptoParcel is handled in PassphraseDialogActivity.onCreate()
        cryptoParcel.mPassphrase = passphrase;

        ((PassphraseDialogActivity) getActivity()).handleResult(cryptoParcel);

        dismiss();
        getActivity().finish();
    }
}
