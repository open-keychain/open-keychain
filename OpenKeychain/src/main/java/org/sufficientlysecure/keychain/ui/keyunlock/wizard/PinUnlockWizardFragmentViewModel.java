package org.sufficientlysecure.keychain.ui.keyunlock.wizard;


import android.content.Context;
import android.os.Bundle;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.keyunlock.base.BaseViewModel;

public class PinUnlockWizardFragmentViewModel implements BaseViewModel {
    private OperationState mOperationState = OperationState.OPERATION_STATE_INPUT_FIRST_KEYWORD;
    private boolean mOperationCompleted = false;
    private StringBuilder mInputKeyword;
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
        }
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void onViewModelCreated() {

    }

    /**
     * Initializes the operation
     */
    public void initializeUnlockOperation() {
        mInputKeyword = new StringBuilder();
        mOperationState = OperationState.OPERATION_STATE_INPUT_FIRST_KEYWORD;
    }

    /**
     * Updates the operation state.
     *
     * @param inputData Data that may be used by an operation.
     * @return
     */
    public boolean updateOperationState(CharSequence inputData) {
        if (mOperationCompleted) {
            return true;
        }

        switch (mOperationState) {
            /**
             * Updates the input data
             */
            case OPERATION_STATE_INPUT_FIRST_KEYWORD: {
                if (inputData.length() == 0) {
                    setOperationError(OperationError.OPERATION_ERROR_EMPTY_PIN);
                    return false;
                }
                mInputKeyword.append(inputData);
                mOperationState = OperationState.
                        OPERATION_STATE_INPUT_SECOND_KEYWORD;
                return true;
            }
            /**
             * Pin Reenter operation
             */
            case OPERATION_STATE_INPUT_SECOND_KEYWORD: {
                if (!(mInputKeyword.toString().equals(inputData.toString()))) {
                    setOperationError(OperationError.OPERATION_ERROR_PIN_MISMATCH);
                    initializeUnlockOperation();
                    return false;
                } else if (inputData.length() == 0) {
                    setOperationError(OperationError.OPERATION_ERROR_EMPTY_PIN);
                    initializeUnlockOperation();
                    return false;
                }
                mOperationState = OperationState.OPERATION_STATE_FINISHED;
                mOperationCompleted = true;
                return true;
            }
            default:
                return false;
        }
    }

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

    public StringBuilder getInputKeyword() {
        return mInputKeyword;
    }

    public void setInputKeyword(StringBuilder inputKeyword) {
        this.mInputKeyword = inputKeyword;
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
}
