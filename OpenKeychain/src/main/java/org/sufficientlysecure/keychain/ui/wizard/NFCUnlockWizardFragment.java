package org.sufficientlysecure.keychain.ui.wizard;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;

import java.io.IOException;

public class NFCUnlockWizardFragment extends WizardFragment
        implements NFCUnlockWizardFragmentViewModel.OnViewModelEventBind,
        CreateKeyWizardActivity.NfcListenerFragment {

    private NFCUnlockWizardFragmentViewModel mNFCUnlockWizardFragmentViewModel;
    private TextView mUnlockTip;
    private FeedbackIndicatorView mUnlockUserFeedback;
    private ProgressBar mProgressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNFCUnlockWizardFragmentViewModel = new NFCUnlockWizardFragmentViewModel(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.unlock_nfc_fragment, container, false);
        mUnlockTip = (TextView) view.findViewById(R.id.unlockTip);
        mUnlockUserFeedback = (FeedbackIndicatorView) view.findViewById(R.id.unlockUserFeedback);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        if (mWizardFragmentListener != null) {
            mWizardFragmentListener.onHideNavigationButtons(false, true);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mNFCUnlockWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mNFCUnlockWizardFragmentViewModel.saveViewModelState(outState);
    }

    /**
     * Notifies the user of any errors that may have occurred
     */
    @Override
    public void onOperationStateError(String error) {
        mUnlockUserFeedback.showWrongTextMessage(error, true);
    }

    @Override
    public void onOperationStateOK(String showText) {
        mUnlockUserFeedback.showCorrectTextMessage(showText, false);
    }

    @Override
    public boolean onNextClicked() {
        return mNFCUnlockWizardFragmentViewModel.updateOperationState();
    }

    @Override
    public boolean onBackClicked() {
        return super.onBackClicked();
    }

    /**
     * Updates the view state by giving feedback to the user.
     */
    @Override
    public void onOperationStateCompleted(String showText) {
        mUnlockUserFeedback.showCorrectTextMessage(showText, true);
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

    /**
     * NFC handling
     *
     * @param exception
     */
    @Override
    public void onNfcError(Exception exception) {

    }

    @Override
    public void onNfcPreExecute() throws IOException {

    }

    @Override
    public void doNfcInBackground() throws IOException {

    }

    @Override
    public void onNfcPostExecute() throws IOException {

    }

    @Override
    public void onNfcTagDiscovery(Intent intent) {

    }
}
