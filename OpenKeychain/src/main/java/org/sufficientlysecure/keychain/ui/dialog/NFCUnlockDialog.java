package org.sufficientlysecure.keychain.ui.dialog;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;

import java.io.IOException;

public class NFCUnlockDialog extends UnlockDialog implements NFCUnlockDialogViewModel.OnViewModelEventBind,
        CreateKeyWizardActivity.NfcListenerFragment {
    private NFCUnlockDialogViewModel mNfcUnlockDialogViewModel;
    private TextView mUnlockTip;
    private FeedbackIndicatorView mUnlockUserFeedback;
    private ProgressBar mProgressBar;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mNfcUnlockDialogViewModel = new NFCUnlockDialogViewModel(this);

        // if the dialog is displayed from the application class, design is missing
        // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(getActivity(), R.style.KeychainTheme);

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);
        View view = LayoutInflater.from(theme).inflate(R.layout.unlock_nfc_fragment, null);
        alert.setView(view);
        setCancelable(false);

        mUnlockTip = (TextView) view.findViewById(R.id.unlockTip);
        mUnlockUserFeedback = (FeedbackIndicatorView) view.findViewById(R.id.unlockUserFeedback);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        alert.setNegativeButton(android.R.string.cancel, null);
        alert.setTitle(getString(R.string.title_unlock));

        mAlertDialog = alert.show();

        Button b = mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        b.setTextColor(getResources().getColor(R.color.primary));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNfcUnlockDialogViewModel.onOperationCancel();
                mAlertDialog.cancel();
            }
        });

        b = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        b.setVisibility(View.GONE);

        mNfcUnlockDialogViewModel.prepareViewModel(savedInstanceState, getArguments(), getActivity());
        return mAlertDialog;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mNfcUnlockDialogViewModel.saveViewModelState(outState);
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mNfcUnlockDialogViewModel.onDetachFromActivity();
    }

    @Override
    public void onOperationStateError(String error) {
        mUnlockUserFeedback.showWrongTextMessage(error, true);
    }

    @Override
    public void onOperationStateOK(String showText) {
        mUnlockUserFeedback.showCorrectTextMessage(showText, false);
    }

    @Override
    public void onTipTextUpdate(CharSequence text) {
        mUnlockTip.setText(text);
    }

    @Override
    public void onShowProgressBar(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onUpdateProgress(int progress) {
        mProgressBar.setProgress(progress);
    }

    @Override
    public void onProgressBarUpdateStyle(boolean indeterminate, int tint) {
        mProgressBar.setIndeterminate(indeterminate);
        DrawableCompat.setTint(mProgressBar.getIndeterminateDrawable(), tint);
        DrawableCompat.setTint(mProgressBar.getProgressDrawable(), tint);
    }

    @Override
    public void onUpdateDialogTitle(CharSequence text) {
        mAlertDialog.setTitle(text);
    }

    @Override
    public void onUnlockOperationSuccess(Intent serviceIntent) {
        mUnlockUserFeedback.showCorrectTextMessage(null, true);
        getActivity().setResult(Activity.RESULT_OK, serviceIntent);
        getActivity().finish();
    }

    @Override
    public void onNfcError(Exception exception) {
        mNfcUnlockDialogViewModel.onNfcError(exception);
    }

    @Override
    public void onNfcPreExecute() throws IOException {
        mNfcUnlockDialogViewModel.onNfcPreExecute();
    }

    @Override
    public Throwable doNfcInBackground() throws IOException {
        return mNfcUnlockDialogViewModel.doNfcInBackground();
    }

    @Override
    public void onNfcPostExecute() throws IOException {
        mNfcUnlockDialogViewModel.onNfcPostExecute();
    }

    @Override
    public void onNfcTagDiscovery(Intent intent) throws IOException {
        mNfcUnlockDialogViewModel.onNfcTagDiscovery(intent);
    }
}
