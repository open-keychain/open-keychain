package org.sufficientlysecure.keychain.ui.keyunlock.wizard;


import android.content.Context;
import android.os.Bundle;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.ui.keyunlock.base.BaseViewModel;

public class UnlockChoiceWizardFragmentViewModel implements BaseViewModel {
    private CanonicalizedSecretKey.SecretKeyType secretKeyType;


    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {

    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {

    }

    @Override
    public void onViewModelCreated() {

    }

    /**
     * Updates the chosen unlock method.
     * @param id
     */
    public void updateUnlockMethodById(int id) {
        if (id == R.id.radioPinUnlock) {
            secretKeyType = CanonicalizedSecretKey.SecretKeyType.PIN;

        } else if (id == R.id.radioPatternUnlock) {
            secretKeyType = CanonicalizedSecretKey.SecretKeyType.PATTERN;
        }
    }

    /**
     * Checks if the data is ok before allowing the user to proceed.
     * @return
     */
    public boolean isUserDataReady() {
        return true;
    }

    public CanonicalizedSecretKey.SecretKeyType getSecretKeyType() {
        return secretKeyType;
    }

    public void setSecretKeyType(CanonicalizedSecretKey.SecretKeyType secretKeyType) {
        this.secretKeyType = secretKeyType;
    }
}
