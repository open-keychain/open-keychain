package org.sufficientlysecure.keychain.ui.wizard;

import android.app.Activity;
import android.os.Bundle;

import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.jcajce.provider.digest.SHA1;
import org.spongycastle.jcajce.provider.digest.SHA256;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.base.WizardFragmentListener;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PatternUnlockWizardFragmentViewModel implements BaseViewModel {
    private OperationState mOperationState = OperationState.OPERATION_STATE_INPUT_FIRST_PATTERN;
    private StringBuilder mLastInputKeyWord;
    private StringBuilder mCurrentInputKeyWord;
    private Activity mActivity;
    private OnViewModelEventBind mOnViewModelEventBind;
    private WizardFragmentListener mWizardFragmentListener;


    /**
     * View Model communication
     */
    public interface OnViewModelEventBind {
        void onOperationStateError(String error);

        void onOperationStateOK(String showText);

        void onOperationStateCompleted(String showText);

        void hideNavigationButtons(boolean hideBack, boolean hideNext);
    }

    /**
     * Operation state
     */
    public enum OperationState {
        OPERATION_STATE_INPUT_FIRST_PATTERN,
        OPERATION_STATE_INPUT_SECOND_PATTERN,
        OPERATION_STATE_FINISHED
    }

    public PatternUnlockWizardFragmentViewModel(OnViewModelEventBind viewModelEventBind,
                                                WizardFragmentListener wizardActivity) {
        mOnViewModelEventBind = viewModelEventBind;
        mWizardFragmentListener = wizardActivity;

        if (mOnViewModelEventBind == null || mWizardFragmentListener == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Activity activity) {
        mActivity = activity;
        if (savedInstanceState == null) {
            initializeUnlockOperation();
        } else {
            restoreViewModelState(savedInstanceState);
        }

        mOnViewModelEventBind.hideNavigationButtons(false, false);
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {

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

        mOperationState = OperationState.OPERATION_STATE_INPUT_FIRST_PATTERN;
    }


    public boolean onOperationStateInputFirstPattern() {
        mOnViewModelEventBind.onOperationStateOK("");
        if (mCurrentInputKeyWord.length() == 0) {
            mOnViewModelEventBind.onOperationStateError(mActivity.getString(R.string.error_no_pattern));
            resetCurrentKeyword();
            return false;
        }
        mLastInputKeyWord.append(mCurrentInputKeyWord);
        mOperationState = OperationState.OPERATION_STATE_INPUT_SECOND_PATTERN;
        resetCurrentKeyword();
        mOnViewModelEventBind.onOperationStateOK(mActivity.getString(R.string.reenter_pin));
        return true;
    }

    public boolean onOperationStateInputSecondPattern() {
        mOperationState = OperationState.OPERATION_STATE_FINISHED;
        resetCurrentKeyword();
        mOnViewModelEventBind.onOperationStateCompleted("");
        return true;
    }


    public boolean updateOperationState() {
        if (mOperationState == OperationState.OPERATION_STATE_FINISHED) {
            return true;
        }

        switch (mOperationState) {
            case OPERATION_STATE_INPUT_FIRST_PATTERN:
                return onOperationStateInputFirstPattern();
            case OPERATION_STATE_INPUT_SECOND_PATTERN:
                return onOperationStateInputSecondPattern();
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
     * Performs operations when the user clicks on the wizard next button.
     * The view model itself will callback to the wizard activity for non ui methods.
     *
     * @return
     */
    public boolean onNextClicked() {
        if (mOperationState != OperationState.OPERATION_STATE_FINISHED) {
            updateOperationState();
            return false;
        } else {

            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA-256");

                md.update(mLastInputKeyWord.toString().getBytes());
                byte[] digest = md.digest();

                Passphrase passphrase = new Passphrase(new String(digest, "ISO-8859-1").toCharArray());
                passphrase.setSecretKeyType(mWizardFragmentListener.getSecretKeyType());
                mWizardFragmentListener.setPassphrase(passphrase);
                return true;

            } catch (NoSuchAlgorithmException e) {
                return false;
            } catch (UnsupportedEncodingException e) {
                return false;
            }
        }
    }

    /**
     * Appends the input text to the current keyword.
     *
     * @param text
     */
    public void appendPattern(CharSequence text) {
        resetCurrentKeyword();
        mCurrentInputKeyWord.append(text);
    }
}
