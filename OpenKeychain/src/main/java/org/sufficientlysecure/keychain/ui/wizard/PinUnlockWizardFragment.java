/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
package org.sufficientlysecure.keychain.ui.wizard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;

public class PinUnlockWizardFragment extends WizardFragment implements
        PinUnlockWizardFragmentViewModel.OnViewModelEventBind {
    private PinUnlockWizardFragmentViewModel mPinUnlockWizardFragmentViewModel;
    private FeedbackIndicatorView mFeedbackIndicatorView;
    private TextView mPinLengthText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPinUnlockWizardFragmentViewModel = new PinUnlockWizardFragmentViewModel(this,
                mWizardFragmentListener);
        mPinUnlockWizardFragmentViewModel.restoreViewModelState(savedInstanceState);
    }

    /**
     * Handles pin key press.
     */
    private View.OnClickListener mOnKeyClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPinUnlockWizardFragmentViewModel.appendToCurrentKeyword(((TextView) v).getText());
            mPinLengthText.setText(String.valueOf(mPinUnlockWizardFragmentViewModel.getPinLength()));
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.unlock_pin_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button pinUnlockKey = (Button) view.findViewById(R.id.unlockKey0);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey9);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey8);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey7);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey6);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey5);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey4);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey3);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey2);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey1);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);

        RelativeLayout pinLengthLayout = (RelativeLayout) view.findViewById(R.id.pinLengthLayout);
        pinLengthLayout.setVisibility(View.VISIBLE);

        mPinLengthText = (TextView) view.findViewById(R.id.pinLength);
        mFeedbackIndicatorView = (FeedbackIndicatorView) view.findViewById(R.id.unlockUserFeedback);
        mPinUnlockWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(), getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPinUnlockWizardFragmentViewModel.saveViewModelState(outState);
    }

    @Override
    public boolean onNextClicked() {
        if (mPinUnlockWizardFragmentViewModel.onNextClicked()) {
            //reset the view model because the user can navigate back
            mPinUnlockWizardFragmentViewModel = new PinUnlockWizardFragmentViewModel(this,
                    mWizardFragmentListener);
            mPinUnlockWizardFragmentViewModel.prepareViewModel(null, getArguments(), getActivity());
            return true;
        }
        return false;
    }

    /**
     * Notifies the user of any errors that may have occurred
     */
    @Override
    public void onOperationStateError(String error) {
        mFeedbackIndicatorView.showWrongTextMessage(error, true);
    }

    @Override
    public void onOperationStateOK(String showText) {
        mFeedbackIndicatorView.showCorrectTextMessage(showText, false);
    }

    /**
     * Updates the view state by giving feedback to the user.
     */
    @Override
    public void onOperationStateCompleted(String showText) {
        mFeedbackIndicatorView.showCorrectTextMessage(showText, true);
    }

    @Override
    public void hideNavigationButtons(boolean hideBack, boolean hideNext) {
        mWizardFragmentListener.onHideNavigationButtons(hideBack, hideNext);
    }
}
