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
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;
import org.sufficientlysecure.keychain.util.Passphrase;

public class PinUnlockWizardFragment extends WizardFragment {
    public static final int MIN_PIN_LENGTH = 4;
    public static final int MAX_PIN_LENGTH = 14;
    public static final String STATE_SAVE_LAST_KEYWORD = "STATE_SAVE_LAST_KEYWORD";
    public static final String STATE_SAVE_CURRENT_KEYWORD = "STATE_SAVE_CURRENT_KEYWORD";
    public static final String STATE_SAVE_OPERATION_STATE = "STATE_SAVE_OPERATION_STATE";
    private FeedbackIndicatorView mFeedbackIndicatorView;
    private TextView mPinLengthText;
    private OperationState mOperationState = OperationState.OPERATION_STATE_INPUT_FIRST_KEYWORD;
    private StringBuilder mLastInputKeyWord;
    private StringBuilder mCurrentInputKeyWord;

    /**
     * Operation state
     */
    public enum OperationState {
        OPERATION_STATE_INPUT_FIRST_KEYWORD,
        OPERATION_STATE_INPUT_SECOND_KEYWORD,
        OPERATION_STATE_FINISHED
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mLastInputKeyWord = (StringBuilder) savedInstanceState.getSerializable(STATE_SAVE_LAST_KEYWORD);
            mOperationState = (OperationState) savedInstanceState.getSerializable(STATE_SAVE_OPERATION_STATE);
            mCurrentInputKeyWord = (StringBuilder) savedInstanceState.getSerializable(STATE_SAVE_CURRENT_KEYWORD);
        }
    }

    /**
     * Handles pin key press.
     */
    private View.OnClickListener mOnKeyClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            appendToCurrentKeyword(((TextView) v).getText());
            mPinLengthText.setText(String.valueOf(mCurrentInputKeyWord.length()));
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

        view.setPadding(0, 0, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                48, getResources().getDisplayMetrics()));

        if (savedInstanceState == null) {
            initializeUnlockOperation();
        }

        hideNavigationButtons(false, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_SAVE_LAST_KEYWORD, mLastInputKeyWord);
        outState.putSerializable(STATE_SAVE_OPERATION_STATE, mOperationState);
        outState.putSerializable(STATE_SAVE_CURRENT_KEYWORD, mCurrentInputKeyWord);
    }

    @Override
    public boolean onNextClicked() {
        if (mOperationState != OperationState.OPERATION_STATE_FINISHED) {
            updateOperationState();
            return false;
        } else {
            Passphrase passphrase = new Passphrase(mLastInputKeyWord.toString());
            passphrase.setSecretKeyType(mWizardFragmentListener.getSecretKeyType());
            mWizardFragmentListener.setPassphrase(passphrase);
            return true;
        }
    }

    /**
     * Notifies the user of any errors that may have occurred
     */
    public void onOperationStateError(String error) {
        mFeedbackIndicatorView.showWrongTextMessage(error, true);
    }

    public void onOperationStateOK(String showText) {
        mFeedbackIndicatorView.showCorrectTextMessage(showText, false);
    }

    /**
     * Updates the view state by giving feedback to the user.
     */
    public void onOperationStateCompleted(String showText) {
        mFeedbackIndicatorView.showCorrectTextMessage(showText, true);
    }

    public void hideNavigationButtons(boolean hideBack, boolean hideNext) {
        mWizardFragmentListener.onHideNavigationButtons(hideBack, hideNext);
    }

    /**
     * Initializes the operation
     */
    public void initializeUnlockOperation() {
        if (mLastInputKeyWord == null) {
            mLastInputKeyWord = new StringBuilder();
        } else {
            clearInputKeyword();
        }

        if (mCurrentInputKeyWord == null) {
            mCurrentInputKeyWord = new StringBuilder();
        } else {
            clearInputKeyword();
        }

        mOperationState = OperationState.OPERATION_STATE_INPUT_FIRST_KEYWORD;
    }

    /**
     * Handles the first pin input operation.
     *
     * @return
     */
    public boolean onOperationStateInputFirstKeyword() {
        onOperationStateOK("");
        if (mCurrentInputKeyWord.length() < MIN_PIN_LENGTH || mCurrentInputKeyWord.length() > MAX_PIN_LENGTH) {
            onOperationStateError(getString(R.string.error_pin_length));
            resetCurrentKeyword();
            return false;
        }
        mLastInputKeyWord.append(mCurrentInputKeyWord);
        mOperationState = OperationState.OPERATION_STATE_INPUT_SECOND_KEYWORD;
        resetCurrentKeyword();
        onOperationStateOK(getString(R.string.reenter_pin));
        return true;
    }

    /**
     * handles the second pin input operation.
     *
     * @return
     */
    public boolean onOperationStateInputSecondKeyword() {
        if (!(mLastInputKeyWord.toString().equals(mCurrentInputKeyWord.toString()))) {
            onOperationStateError(getString(R.string.error_pin_mismatch));
            initializeUnlockOperation();
            return false;
        } else if (mCurrentInputKeyWord.length() < MIN_PIN_LENGTH || mCurrentInputKeyWord.length() > MAX_PIN_LENGTH) {
            onOperationStateError(getString(R.string.error_pin_length));
            initializeUnlockOperation();
            return false;
        }
        mOperationState = OperationState.OPERATION_STATE_FINISHED;
        resetCurrentKeyword();
        onOperationStateCompleted("");
        return true;
    }

    /**
     * Updates the operation state.
     *
     * @return
     */
    public boolean updateOperationState() {
        if (mOperationState == OperationState.OPERATION_STATE_FINISHED) {
            return true;
        }

        switch (mOperationState) {
            case OPERATION_STATE_INPUT_FIRST_KEYWORD: {
                return onOperationStateInputFirstKeyword();
            }
            case OPERATION_STATE_INPUT_SECOND_KEYWORD: {
                return onOperationStateInputSecondKeyword();
            }
            default:
                return false;
        }
    }

    /**
     * Clears all input keywords if they were initialized.
     */
    private void clearInputKeyword() {
        if (mLastInputKeyWord != null) {
            mLastInputKeyWord.setLength(0);
        }
        if (mCurrentInputKeyWord != null) {
            mCurrentInputKeyWord.setLength(0);
        }
    }

    /**
     * Resets the current input keyword.
     */
    public void resetCurrentKeyword() {
        mCurrentInputKeyWord.setLength(0);
    }

    /**
     * Appends the input text to the current keyword.
     *
     * @param text
     */
    public void appendToCurrentKeyword(CharSequence text) {
        mCurrentInputKeyWord.append(text);
    }
}
