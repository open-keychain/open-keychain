package org.sufficientlysecure.keychain.ui.keyunlock.wizard;


import android.content.Context;
import android.os.Bundle;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.ui.keyunlock.base.BaseViewModel;

public class UnlockChoiceWizardFragmentViewModel implements BaseViewModel {
    public static final String STATE_SAVE_UNLOCK_METHOD = "STATE_SAVE_UNLOCK_METHOD";
    private CanonicalizedSecretKey.SecretKeyType mSecretKeyType;

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {
        if (savedInstanceState != null) {
            restoreViewModelState(savedInstanceState);
        }
    }

    @Override
    public void saveViewModelState(Bundle outState) {
        outState.putSerializable(STATE_SAVE_UNLOCK_METHOD, mSecretKeyType);
    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {
        mSecretKeyType = (CanonicalizedSecretKey.SecretKeyType)
                savedInstanceState.getSerializable(STATE_SAVE_UNLOCK_METHOD);
    }

    @Override
    public void onViewModelCreated() {

    }

    /**
     * Updates the chosen unlock method.
     *
     * @param id
     */
    public void updateUnlockMethodById(int id) {
        if (id == R.id.radioPinUnlock) {
            mSecretKeyType = CanonicalizedSecretKey.SecretKeyType.PIN;

        } else if (id == R.id.radioPatternUnlock) {
            mSecretKeyType = CanonicalizedSecretKey.SecretKeyType.PATTERN;
        }
    }

    /**
     * Checks if the data is ok before allowing the user to proceed.
     *
     * @return
     */
    public boolean isUserDataReady() {
        return true;
    }

    public CanonicalizedSecretKey.SecretKeyType getSecretKeyType() {
        return mSecretKeyType;
    }

    public void setSecretKeyType(CanonicalizedSecretKey.SecretKeyType secretKeyType) {
        this.mSecretKeyType = secretKeyType;
    }
}
