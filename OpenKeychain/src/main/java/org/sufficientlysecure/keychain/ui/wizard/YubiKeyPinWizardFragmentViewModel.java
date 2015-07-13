package org.sufficientlysecure.keychain.ui.wizard;

import android.content.Context;
import android.os.Bundle;

import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.tasks.YubiKeyPinAsyncTask;
import org.sufficientlysecure.keychain.util.Passphrase;


public class YubiKeyPinWizardFragmentViewModel implements BaseViewModel,
        YubiKeyPinAsyncTask.OnYubiKeyPinAsyncTaskListener {
    private YubiKeyPinAsyncTask mYubiKeyPinAsyncTask;
    private OnViewModelEventBind mOnViewModelEventBind;
    private Context mContext;

    private Passphrase mPin;
    private Passphrase mAdminPin;

    public interface OnViewModelEventBind {
        void updatePinText(CharSequence text);

        void updateAdminPinText(CharSequence text);
    }

    public YubiKeyPinWizardFragmentViewModel(OnViewModelEventBind onViewModelEventBind) {
        mOnViewModelEventBind = onViewModelEventBind;
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {
        mContext = context;

        if (savedInstanceState != null) {
            restoreViewModelState(savedInstanceState);
        }
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {

    }

    @Override
    public void onYubiKeyPinTaskResult(Passphrase pin, Passphrase adminPin) {
        mOnViewModelEventBind.updateAdminPinText(adminPin.toStringUnsafe());
        mOnViewModelEventBind.updatePinText(pin.toStringUnsafe());
    }

    /**
     * Sets Yubi key pins.
     *
     * @param pin
     * @param adminPin
     */
    public void onYubiKeyPinDataSet(Passphrase pin, Passphrase adminPin) {
        mAdminPin = adminPin;
        mPin = pin;

        if (pin == null) {
            launchYubiKeyTask();
        } else {
            mOnViewModelEventBind.updateAdminPinText(mAdminPin.toStringUnsafe());
            mOnViewModelEventBind.updatePinText(mPin.toStringUnsafe());
        }
    }

    /**
     * Launches the Yubi Key Pin task to generate the pins.
     */
    public void launchYubiKeyTask() {
        if (mYubiKeyPinAsyncTask != null) {
            mYubiKeyPinAsyncTask.cancel(true);
            mYubiKeyPinAsyncTask = null;
        }

        mYubiKeyPinAsyncTask = new YubiKeyPinAsyncTask(this);
        mYubiKeyPinAsyncTask.execute();
    }
}
