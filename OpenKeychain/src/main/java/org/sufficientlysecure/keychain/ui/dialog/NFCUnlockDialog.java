package org.sufficientlysecure.keychain.ui.dialog;


import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;

public class NFCUnlockDialog extends UnlockDialog {
    private NFCUnlockDialogViewModel mNfcUnlockDialogViewModel;
    private RelativeLayout mNfcFeedbackLayout;
    private LinearLayout mUnlockTipLayout;
    private TextView mUnlockTip;
    private FeedbackIndicatorView mUnlockUserFeedback;
    private ProgressBar mProgressBar;
    private RelativeLayout mUnlockInputLayout;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mNfcUnlockDialogViewModel = new NFCUnlockDialogViewModel();

        // if the dialog is displayed from the application class, design is missing
        // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(getActivity(), R.style.KeychainTheme);

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);
        View view = LayoutInflater.from(theme).inflate(R.layout.unlock_nfc_fragment, null);
        alert.setView(view);
        setCancelable(false);

        mNfcFeedbackLayout = (RelativeLayout) view.findViewById(R.id.nfcFeedbackLayout);
        mUnlockTipLayout = (LinearLayout) view.findViewById(R.id.unlockTipLayout);
        mUnlockTip = (TextView) view.findViewById(R.id.unlockTip);
        mUnlockUserFeedback = (FeedbackIndicatorView) view.findViewById(R.id.unlockUserFeedback);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mUnlockInputLayout = (RelativeLayout) view.findViewById(R.id.unlockInputLayout);

        alert.setPositiveButton(getString(R.string.unlock_caps), null);
        alert.setNegativeButton(android.R.string.cancel, null);

        mAlertDialog = alert.show();

        mNfcUnlockDialogViewModel.prepareViewModel(savedInstanceState, getArguments(), getActivity());
        return mAlertDialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mNfcUnlockDialogViewModel.saveViewModelState(outState);
    }
}
