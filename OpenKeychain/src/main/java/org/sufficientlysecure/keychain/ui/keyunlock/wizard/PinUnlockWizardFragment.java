package org.sufficientlysecure.keychain.ui.keyunlock.wizard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.keyunlock.base.WizardFragment;

public class PinUnlockWizardFragment extends WizardFragment {
    public static final String STATE_SAVE_STATUS_TEXT = "STATE_SAVE_STATUS_ID";
    public static final String STATE_SAVE_CHECK_VISIBILITY = "STATE_SAVE_CHECK_VISIBILITY";
    public static final String STATE_SAVE_WRONG_VISIBILITY = "STATE_SAVE_WRONG_VISIBILITY";
    public static final String STATE_SAVE_STATUS_COLOR = "STATE_SAVE_STATUS_COLOR";

    private PinUnlockWizardFragmentViewModel mPinUnlockWizardFragmentViewModel;

    private TextView mFragmentPinUnlockTip;
    private ImageView mCheckImageView;
    private ImageView mWrongImageView;
    private TextView mPinUnlockDialogStatus;
    private Button mPinUnlockKey1;
    private Button mPinUnlockKey2;
    private Button mPinUnlockKey3;
    private Button mPinUnlockKey4;
    private Button mPinUnlockKey5;
    private Button mPinUnlockKey6;
    private Button mPinUnlockKey7;
    private Button mPinUnlockKey8;
    private Button mPinUnlockKey9;
    private Button mPinUnlockKey0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPinUnlockWizardFragmentViewModel = new PinUnlockWizardFragmentViewModel();
        mPinUnlockWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    /**
     * Handles pin key press.
     */
    private View.OnClickListener mOnKeyClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPinUnlockWizardFragmentViewModel.appendToCurrentKeyword(((TextView) v).getText());
        }
    };

    private void resetViewState() {
        mPinUnlockDialogStatus.setText("");
        mWrongImageView.setVisibility(View.INVISIBLE);
        mCheckImageView.setVisibility(View.INVISIBLE);
        mPinUnlockWizardFragmentViewModel.resetCurrentKeyword();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.unlock_pin_fragment, container, false);

        mPinUnlockKey0 = (Button) view.findViewById(R.id.unlockKey0);
        mPinUnlockKey0.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey9 = (Button) view.findViewById(R.id.unlockKey9);
        mPinUnlockKey9.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey8 = (Button) view.findViewById(R.id.unlockKey8);
        mPinUnlockKey8.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey7 = (Button) view.findViewById(R.id.unlockKey7);
        mPinUnlockKey7.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey6 = (Button) view.findViewById(R.id.unlockKey6);
        mPinUnlockKey6.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey5 = (Button) view.findViewById(R.id.unlockKey5);
        mPinUnlockKey5.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey4 = (Button) view.findViewById(R.id.unlockKey4);
        mPinUnlockKey4.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey3 = (Button) view.findViewById(R.id.unlockKey3);
        mPinUnlockKey3.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey2 = (Button) view.findViewById(R.id.unlockKey2);
        mPinUnlockKey2.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey1 = (Button) view.findViewById(R.id.unlockKey1);
        mPinUnlockKey1.setOnClickListener(mOnKeyClickListener);
        mPinUnlockDialogStatus = (TextView) view.findViewById(R.id.unlockDialogStatus);
        mWrongImageView = (ImageView) view.findViewById(R.id.wrong);
        mCheckImageView = (ImageView) view.findViewById(R.id.check);
        mFragmentPinUnlockTip = (TextView) view.findViewById(R.id.unlockTip);

        if (savedInstanceState == null) {
            resetViewState();
        } else {
            mWrongImageView.setVisibility(savedInstanceState.getInt(STATE_SAVE_WRONG_VISIBILITY));
            mCheckImageView.setVisibility(savedInstanceState.getInt(STATE_SAVE_CHECK_VISIBILITY));
            mPinUnlockDialogStatus.setTextColor(savedInstanceState.getInt(STATE_SAVE_STATUS_COLOR));
            mPinUnlockDialogStatus.setText(savedInstanceState.getCharSequence(STATE_SAVE_STATUS_TEXT));
        }

        if (mWizardFragmentListener != null) {
            mWizardFragmentListener.onHideNavigationButtons(false);
        }

        return view;
    }

    /**
     * Notifies the user of any errors that may have occurred
     */
    private void onOperationStateError() {
        mPinUnlockWizardFragmentViewModel.resetCurrentKeyword();
        mPinUnlockDialogStatus.setText(mPinUnlockWizardFragmentViewModel.getLastOperationError());
        mPinUnlockDialogStatus.setTextColor(getResources().getColor(R.color.android_red_dark));
        mWrongImageView.setVisibility(View.VISIBLE);
        mCheckImageView.setVisibility(View.INVISIBLE);
    }

    /**
     * Updates the view state by giving feedback to the user.
     */
    private void updateViewStateForOperation() {
        mPinUnlockDialogStatus.setText(mPinUnlockWizardFragmentViewModel.getOperationStatusText());
        mPinUnlockDialogStatus.setTextColor(getResources().
                getColor(R.color.android_green_dark));
        mWrongImageView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPinUnlockWizardFragmentViewModel.saveViewModelState(outState);

        outState.putCharSequence(STATE_SAVE_STATUS_TEXT, mPinUnlockDialogStatus.getText());
        outState.putInt(STATE_SAVE_CHECK_VISIBILITY, mCheckImageView.getVisibility());
        outState.putInt(STATE_SAVE_WRONG_VISIBILITY, mWrongImageView.getVisibility());
        outState.putInt(STATE_SAVE_STATUS_COLOR, mPinUnlockDialogStatus.getCurrentTextColor());
    }

    @Override
    public boolean onNextClicked() {
        if (!mPinUnlockWizardFragmentViewModel.isOperationCompleted()) {
            if (mPinUnlockWizardFragmentViewModel.updateOperationState()) {
                updateViewStateForOperation();
                mPinUnlockWizardFragmentViewModel.resetCurrentKeyword();
            } else {
                onOperationStateError();
            }

            if (mPinUnlockWizardFragmentViewModel.isOperationCompleted()) {
                mCheckImageView.setVisibility(View.VISIBLE);
                mWrongImageView.setVisibility(View.INVISIBLE);
            }
        } else {
            return true;
        }
        return false;
    }
}
