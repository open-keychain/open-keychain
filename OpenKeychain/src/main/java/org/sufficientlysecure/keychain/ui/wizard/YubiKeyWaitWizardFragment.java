package org.sufficientlysecure.keychain.ui.wizard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;

/**
 * Waits for the user to use a yubi key on the cellphone.
 * The fragment will stay visible until a notification is sent that the yubi key was
 * detected.
 */
public class YubiKeyWaitWizardFragment extends WizardFragment {

    public static YubiKeyWaitWizardFragment newInstance() {
        return new YubiKeyWaitWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (mWizardFragmentListener != null) {
            mWizardFragmentListener.onHideNavigationButtons(false, true);

        }
        return inflater.inflate(R.layout.wizard_yubi_wait_fragment, container, false);
    }
}
