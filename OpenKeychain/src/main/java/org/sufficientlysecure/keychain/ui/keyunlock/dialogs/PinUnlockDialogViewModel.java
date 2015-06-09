package org.sufficientlysecure.keychain.ui.keyunlock.dialogs;

import android.content.Context;
import android.os.Bundle;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.keyunlock.base.BaseViewModel;

/**
 * Pin unlock dialog view Model.
 */
public class PinUnlockDialogViewModel implements BaseViewModel {
    //// TODO: 07/06/2015 move this out of this class
    public static final String EXTRA_PARAM_OPERATION_TYPE = "EXTRA_PARAM_OPERATION_TYPE";
    public static final String TAG = "PinUnlockDialogViewModel";

    private DialogUnlockOperationState mOperationState;
    private boolean mOperationCompleted = false;
    private DialogUnlockOperation mDialogUnlockType;
    private StringBuilder mInputKeyword;
    private String mOperationError;
    private Context mContext;

    /**
     * Operations allowed by this dialog
     */
    public enum DialogUnlockOperation {
        DIALOG_UNLOCK_TYPE_NEW_KEYWORD,
        DIALOG_UNLOCK_TYPE_UNLOCK_KEY
    }

    /**
     * Operation state
     */
    public enum DialogUnlockOperationState {
        DIALOG_UNLOCK_OPERATION_STATE_INPUT_FIRST_KEYWORD,
        DIALOG_UNLOCK_OPERATION_STATE_INPUT_SECOND_KEYWORD,
        DIALOG_UNLOCK_OPERATION_STATE_FINISHED,
        DIALOG_UNLOCK_OPERATION_STATE_UNLOCK_KEY
    }

    /**
     * Operation errors
     */
    public enum DialogUnlockOperationError {
        DIALOG_UNLOCK_OPERATION_ERROR_EMPTY_KEYWORD,
        DIALOG_UNLOCK_OPERATION_ERROR_MISMATCH_KEYWORDS
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {
        mContext = context;
        if (savedInstanceState == null) {
            initializeUnlockOperation(
                    (PinUnlockDialogViewModel.DialogUnlockOperation) arguments.
                            getSerializable(EXTRA_PARAM_OPERATION_TYPE));
        }
    }

    public DialogUnlockOperation getDialogUnlockType() {
        return mDialogUnlockType;
    }


    /**
     * Initializes the operation
     *
     * @param dialogUnlockType
     */
    public void initializeUnlockOperation(DialogUnlockOperation dialogUnlockType) {
        mDialogUnlockType = dialogUnlockType;
        mInputKeyword = new StringBuilder();

        switch (mDialogUnlockType) {
            case DIALOG_UNLOCK_TYPE_NEW_KEYWORD: {
                mOperationState = DialogUnlockOperationState.
                        DIALOG_UNLOCK_OPERATION_STATE_INPUT_FIRST_KEYWORD;
            }
            break;
            case DIALOG_UNLOCK_TYPE_UNLOCK_KEY: {
                mOperationState = DialogUnlockOperationState.
                        DIALOG_UNLOCK_OPERATION_STATE_UNLOCK_KEY;
            }
        }
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void onViewModelCreated() {

    }

    /**
     * Updates the operation state.
     * @param inputData Data that may be used by an operation.
     * @return
     */
    public boolean updateOperationState(CharSequence inputData) {
        if (mOperationCompleted) {
            return false;
        }

        switch (mOperationState) {
            /**
             * Updates the input data
             */
            case DIALOG_UNLOCK_OPERATION_STATE_INPUT_FIRST_KEYWORD: {
                if (inputData.length() == 0) {
                    setOperationError(DialogUnlockOperationError.
                            DIALOG_UNLOCK_OPERATION_ERROR_EMPTY_KEYWORD);
                    return false;
                }
                mInputKeyword.append(inputData);
                mOperationState = DialogUnlockOperationState.
                        DIALOG_UNLOCK_OPERATION_STATE_INPUT_SECOND_KEYWORD;
                return true;
            }
            /**
             * Pin Reenter operation
             */
            case DIALOG_UNLOCK_OPERATION_STATE_INPUT_SECOND_KEYWORD: {
                if (!(mInputKeyword.toString().equals(inputData.toString()))) {
                    setOperationError(DialogUnlockOperationError.
                            DIALOG_UNLOCK_OPERATION_ERROR_MISMATCH_KEYWORDS);
                    return false;
                } else if (inputData.length() == 0) {
                    setOperationError(DialogUnlockOperationError.
                            DIALOG_UNLOCK_OPERATION_ERROR_EMPTY_KEYWORD);
                    return false;
                }
                mOperationState = DialogUnlockOperationState.
                        DIALOG_UNLOCK_OPERATION_STATE_FINISHED;
                mOperationCompleted = true;
            }
            break;
            /**
             * Unlock operation
             */
            case DIALOG_UNLOCK_OPERATION_STATE_UNLOCK_KEY: {
                if (inputData.length() == 0) {
                    setOperationError(DialogUnlockOperationError.
                            DIALOG_UNLOCK_OPERATION_ERROR_EMPTY_KEYWORD);
                    return false;
                }
                //Todo: implement the actual unlock code from existing code.
                mOperationState = DialogUnlockOperationState.
                        DIALOG_UNLOCK_OPERATION_STATE_FINISHED;
                mOperationCompleted = true;
            }
            break;
            default:
                return false;
        }
        return false;
    }


    /**
     * Returns true if all operations were completed
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

    /**
     * Returns the current positive button string based on the state of the
     * operations.
     *
     * @return
     */
    public CharSequence getPositiveButtonStringForCurrentOperationState() {
        switch (mOperationState) {
            case DIALOG_UNLOCK_OPERATION_STATE_INPUT_FIRST_KEYWORD: {
                return mContext.getString(android.R.string.ok);
            }
            case DIALOG_UNLOCK_OPERATION_STATE_INPUT_SECOND_KEYWORD: {
                return mContext.getString(R.string.confirm_caps);
            }
            case DIALOG_UNLOCK_OPERATION_STATE_UNLOCK_KEY: {
                return mContext.getString(R.string.unlock_caps);
            }
            default: {
                return mContext.getString(R.string.unknown);
            }
        }
    }

    /**
     * Sets the last operation error that occurred.
     * @param operationError
     */
    private void setOperationError(DialogUnlockOperationError operationError) {
        switch (operationError) {
            case DIALOG_UNLOCK_OPERATION_ERROR_EMPTY_KEYWORD: {
                mOperationError = mContext.getString(R.string.error_no_pin);
            }
            break;
            case DIALOG_UNLOCK_OPERATION_ERROR_MISMATCH_KEYWORDS: {
                mOperationError = mContext.getString(R.string.error_pin_mismatch);
            }
            break;
            default: {
                mOperationError = mContext.getString(R.string.error_unknown_operation);
            }
        }
    }

    /**
     * Returns the current operation status to give some feedback to the user.
     * @return
     */
    public CharSequence getOperationStatusText() {

        switch (mOperationState) {
            case DIALOG_UNLOCK_OPERATION_STATE_INPUT_FIRST_KEYWORD: {
                return "";
            }
            case DIALOG_UNLOCK_OPERATION_STATE_INPUT_SECOND_KEYWORD: {
                return mContext.getString(R.string.reenter_pin);
            }
            case DIALOG_UNLOCK_OPERATION_STATE_UNLOCK_KEY: {
                return "";
            }
            default: {
                return "";
            }
        }
    }

    public CharSequence getLastOperationError() {
        return mOperationError;
    }

    public DialogUnlockOperationState getOperationState() {
        return mOperationState;
    }

    public void setmOperationState(DialogUnlockOperationState operationState) {
        this.mOperationState = operationState;
    }
}
