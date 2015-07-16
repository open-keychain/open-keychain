package org.sufficientlysecure.keychain.ui.wizard;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;

import java.io.IOException;

public class NFCUnlockWizardFragment extends WizardFragment
        implements NFCUnlockWizardFragmentViewModel.OnViewModelEventBind,
        CreateKeyWizardActivity.NfcListenerFragment{
    private NFCUnlockWizardFragmentViewModel mNFCUnlockWizardFragmentViewModel;
    private RelativeLayout mNfcFeedbackLayout;
    private LinearLayout mUnlockTipLayout;
    private TextView mUnlockTip;
    private FeedbackIndicatorView mUnlockUserFeedback;
    private ProgressBar mProgressBar;
    private RelativeLayout mUnlockInputLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNFCUnlockWizardFragmentViewModel = new NFCUnlockWizardFragmentViewModel(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.unlock_nfc_fragment, container, false);
        mNfcFeedbackLayout = (RelativeLayout) view.findViewById(R.id.nfcFeedbackLayout);
        mUnlockTipLayout = (LinearLayout) view.findViewById(R.id.unlockTipLayout);
        mUnlockTip = (TextView) view.findViewById(R.id.unlockTip);
        mUnlockUserFeedback = (FeedbackIndicatorView) view.findViewById(R.id.unlockUserFeedback);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mUnlockInputLayout = (RelativeLayout) view.findViewById(R.id.unlockInputLayout);

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
        return super.onNextClicked();
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
    public void doNfcInBackground() throws IOException {

    }

    @Override
    public void onNfcPostExecute() throws IOException {

    }
}
