package org.sufficientlysecure.keychain.ui.keyunlock.wizard;


import android.content.Context;
import android.os.Bundle;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.keyunlock.base.BaseViewModel;

public class PinUnlockWizardFragmentViewModel implements BaseViewModel {
    public static final String STATE_SAVE_LAST_KEYWORD = "STATE_SAVE_LAST_KEYWORD";
    public static final String STATE_SAVE_CURRENT_KEYWORD = "STATE_SAVE_CURRENT_KEYWORD";
    public static final String STATE_SAVE_OPERATION_COMPLETED = "STATE_SAVE_OPERATION_COMPLETED";
    public static final String STATE_SAVE_OPERATION_STATE = "STATE_SAVE_OPERATION_STATE";
    public static final String STATE_SAVE_OPERATION_ERROR = "STATE_SAVE_OPERATION_ERROR";

    private OperationState mOperationState = OperationState.OPERATION_STATE_INPUT_FIRST_KEYWORD;
    private boolean mOperationCompleted = false;
    private StringBuilder mLastInputKeyWord;
    private StringBuilder mCurrentInputKeyWord;
    private OperationError mOperationError = OperationError.OPERATION_ERROR_NONE;
    private Context mContext;

    /**
     * Operation state
     */
    public enum OperationState {
        OPERATION_STATE_INPUT_FIRST_KEYWORD,
        OPERATION_STATE_INPUT_SECOND_KEYWORD,
        OPERATION_STATE_FINISHED
    }

    public enum OperationError {
        OPERATION_ERROR_NONE,
        OPERATION_ERROR_EMPTY_PIN,
        OPERATION_ERROR_PIN_MISMATCH
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {
        mContext = context;
        if (savedInstanceState == null) {
            initializeUnlockOperation();
        } else {
            restoreViewModelState(savedInstanceState);
        }
    }

    @Override
    public void saveViewModelState(Bundle outState) {
        outState.putSerializable(STATE_SAVE_LAST_KEYWORD, mLastInputKeyWord);
        outState.putBoolean(STATE_SAVE_OPERATION_COMPLETED, mOperationCompleted);
        outState.putSerializable(STATE_SAVE_OPERATION_STATE, mOperationState);
        outState.putSerializable(STATE_SAVE_OPERATION_ERROR, mOperationError);
        outState.putSerializable(STATE_SAVE_CURRENT_KEYWORD, mCurrentInputKeyWord);
    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {
        mLastInputKeyWord = (StringBuilder) savedInstanceState.getSerializable(STATE_SAVE_LAST_KEYWORD);
        mOperationCompleted = savedInstanceState.getBoolean(STATE_SAVE_OPERATION_COMPLETED);
        mOperationState = (OperationState) savedInstanceState.getSerializable(STATE_SAVE_OPERATION_STATE);
        mOperationError = (OperationError) savedInstanceState.getSerializable(STATE_SAVE_OPERATION_ERROR);
        mCurrentInputKeyWord = (StringBuilder) savedInstanceState.getSerializable(STATE_SAVE_CURRENT_KEYWORD);
    }

    @Override
    public void onViewModelCreated() {

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
        mOperationError = OperationError.OPERATION_ERROR_NONE;
        mOperationCompleted = false;
    }

    /**
     * Resets the keyword input to its initial step, allowing the user to re-input the pin again.
     */
    public void resetKeywordInputToBegin() {
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
        mOperationCompleted = false;
    }

    /**
     * Updates the operation state.
     *
     * @return
     */
    public boolean updateOperationState() {
        if (mOperationCompleted) {
            return true;
        }

        switch (mOperationState) {
            /**
             * Updates the input data
             */
            case OPERATION_STATE_INPUT_FIRST_KEYWORD: {
                if (mCurrentInputKeyWord.length() == 0) {
                    setOperationError(OperationError.OPERATION_ERROR_EMPTY_PIN);
                    return false;
                }
                mLastInputKeyWord.append(mCurrentInputKeyWord);
                mOperationState = OperationState.
                        OPERATION_STATE_INPUT_SECOND_KEYWORD;
                return true;
            }
            /**
             * Pin Reenter operation
             */
            case OPERATION_STATE_INPUT_SECOND_KEYWORD: {
                if (!(mLastInputKeyWord.toString().equals(mCurrentInputKeyWord.toString()))) {
                    setOperationError(OperationError.OPERATION_ERROR_PIN_MISMATCH);
                    resetKeywordInputToBegin();
                    return false;
                } else if (mCurrentInputKeyWord.length() == 0) {
                    setOperationError(OperationError.OPERATION_ERROR_EMPTY_PIN);
                    resetKeywordInputToBegin();
                    return false;
                }
                mOperationState = OperationState.OPERATION_STATE_FINISHED;
                mOperationCompleted = true;
                return true;
            }
            case OPERATION_STATE_FINISHED: {
                //reset the fragment to be reusable if the user decides to go back.
                initializeUnlockOperation();
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * Returns the last operation error string to the view.
     * @return
     */
    public CharSequence getLastOperationError() {
        switch (mOperationError) {
            case OPERATION_ERROR_EMPTY_PIN: {
                return mContext.getString(R.string.error_no_pin);
            }
            case OPERATION_ERROR_PIN_MISMATCH: {
                return mContext.getString(R.string.error_pin_mismatch);
            }
            default: {
                return mContext.getString(R.string.error_unknown_operation);
            }
        }
    }

    /**
     * Returns the current operation status to give some feedback to the user.
     *
     * @return
     */
    public CharSequence getOperationStatusText() {

        switch (mOperationState) {
            case OPERATION_STATE_INPUT_FIRST_KEYWORD: {
                return "";
            }
            case OPERATION_STATE_INPUT_SECOND_KEYWORD: {
                return mContext.getString(R.string.reenter_pin);
            }
            default: {
                return "";
            }
        }
    }

    /**
     * Returns true if all operations were completed
     *
     * @return
     */
    public boolean isOperationCompleted() {
        return mOperationCompleted;
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

    public StringBuilder getLastInputKeyWord() {
        return mLastInputKeyWord;
    }

    public void setLastInputKeyWord(StringBuilder lastInputKeyWord) {
        this.mLastInputKeyWord = lastInputKeyWord;
    }

    public void setOperationError(OperationError operationError) {
        mOperationError = operationError;
    }

    public OperationError getOperationError() {
        return mOperationError;
    }

    public OperationState getOperationState() {
        return mOperationState;
    }

    public void setOperationState(OperationState operationState) {
        mOperationState = operationState;
    }

    public StringBuilder getCurrentInputKeyWord() {
        return mCurrentInputKeyWord;
    }

    public void setCurrentInputKeyWord(StringBuilder currentInputKeyWord) {
        mCurrentInputKeyWord = currentInputKeyWord;
    }

    /**
     * Resets the current input keyword.
     */
    public void resetCurrentKeyword() {
        mCurrentInputKeyWord.setLength(0);
    }

    /**
     * Appends the input text to the current keyword.
     * @param text
     */
    public void appendToCurrentKeyword(CharSequence text) {
        mCurrentInputKeyWord.append(text);
    }
}
