package org.sufficientlysecure.keychain.ui.wizard;


import android.content.Context;
import android.os.Bundle;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;

public class PinUnlockWizardFragmentViewModel implements BaseViewModel {
	public static final String STATE_SAVE_LAST_KEYWORD = "STATE_SAVE_LAST_KEYWORD";
	public static final String STATE_SAVE_CURRENT_KEYWORD = "STATE_SAVE_CURRENT_KEYWORD";
	public static final String STATE_SAVE_OPERATION_COMPLETED = "STATE_SAVE_OPERATION_COMPLETED";
	public static final String STATE_SAVE_OPERATION_STATE = "STATE_SAVE_OPERATION_STATE";

	private OperationState mOperationState = OperationState.OPERATION_STATE_INPUT_FIRST_KEYWORD;
	private boolean mOperationCompleted = false;
	private StringBuilder mLastInputKeyWord;
	private StringBuilder mCurrentInputKeyWord;
	private Context mContext;
	private OnViewModelEventBind mOnViewModelEventBind;

	/**
	 * View Model communication
	 */
	public interface OnViewModelEventBind {
		void onOperationStateError(String error);

		void onOperationStateOK(String showText);

		void onOperationStateCompleted(String showText);
	}

	/**
	 * Operation state
	 */
	public enum OperationState {
		OPERATION_STATE_INPUT_FIRST_KEYWORD,
		OPERATION_STATE_INPUT_SECOND_KEYWORD,
		OPERATION_STATE_FINISHED
	}

	public PinUnlockWizardFragmentViewModel(OnViewModelEventBind viewModelEventBind) {
		mOnViewModelEventBind = viewModelEventBind;

		if (mOnViewModelEventBind == null) {
			throw new UnsupportedOperationException();
		}
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
		outState.putSerializable(STATE_SAVE_CURRENT_KEYWORD, mCurrentInputKeyWord);
	}

	@Override
	public void restoreViewModelState(Bundle savedInstanceState) {
		mLastInputKeyWord = (StringBuilder) savedInstanceState.getSerializable(STATE_SAVE_LAST_KEYWORD);
		mOperationCompleted = savedInstanceState.getBoolean(STATE_SAVE_OPERATION_COMPLETED);
		mOperationState = (OperationState) savedInstanceState.getSerializable(STATE_SAVE_OPERATION_STATE);
		mCurrentInputKeyWord = (StringBuilder) savedInstanceState.getSerializable(STATE_SAVE_CURRENT_KEYWORD);
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
				mOnViewModelEventBind.onOperationStateOK("");
				if (mCurrentInputKeyWord.length() == 0) {
					mOnViewModelEventBind.onOperationStateError(mContext.
							getString(R.string.error_no_pin));
					resetCurrentKeyword();
					return false;
				}
				mLastInputKeyWord.append(mCurrentInputKeyWord);
				mOperationState = OperationState.OPERATION_STATE_INPUT_SECOND_KEYWORD;
				resetCurrentKeyword();
				mOnViewModelEventBind.onOperationStateOK(mContext.
						getString(R.string.reenter_pin));
				return true;
			}
			/**
			 * Pin Reenter operation
			 */
			case OPERATION_STATE_INPUT_SECOND_KEYWORD: {
				if (!(mLastInputKeyWord.toString().equals(mCurrentInputKeyWord.toString()))) {
					mOnViewModelEventBind.onOperationStateError(mContext.
							getString(R.string.error_pin_mismatch));
					resetKeywordInputToBegin();
					return false;
				} else if (mCurrentInputKeyWord.length() == 0) {
					mOnViewModelEventBind.onOperationStateError(mContext.
							getString(R.string.error_no_pin));
					resetKeywordInputToBegin();
					return false;
				}
				mOperationState = OperationState.OPERATION_STATE_FINISHED;
				mOperationCompleted = true;
				resetCurrentKeyword();
				mOnViewModelEventBind.onOperationStateCompleted("");
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
