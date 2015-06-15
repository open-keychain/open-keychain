package org.sufficientlysecure.keychain.ui.keyunlock.wizard;

import android.content.Context;
import android.os.Bundle;

import org.sufficientlysecure.keychain.ui.keyunlock.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.keyunlock.base.UnlockOptionFragment;
import org.sufficientlysecure.keychain.ui.keyunlock.options.PassphraseUnlockOptionFragment;
import org.sufficientlysecure.keychain.ui.keyunlock.options.PinUnlockOptionFragment;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;

import java.util.ArrayList;

/**
 * UnlockWizardFragment View Model
 */
public class UnlockWizardFragmentViewModel implements BaseViewModel {
    private ArrayList<CanonicalizedSecretKey.SecretKeyType> wizardSecretTypes;
    private Context mContext;

    public UnlockWizardFragmentViewModel() {
        wizardSecretTypes = new ArrayList<>();
    }

    /**
     * Prepares the view model with data.
     */
    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {
        mContext = context;
        wizardSecretTypes.add(CanonicalizedSecretKey.SecretKeyType.PASSPHRASE);
        wizardSecretTypes.add(CanonicalizedSecretKey.SecretKeyType.PIN);
        wizardSecretTypes.add(CanonicalizedSecretKey.SecretKeyType.PATTERN);

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

    public ArrayList<CanonicalizedSecretKey.SecretKeyType> getWizardSecretTypes() {
        return wizardSecretTypes;
    }

    public void setWizardSecretTypes(
            ArrayList<CanonicalizedSecretKey.SecretKeyType> wizardSecretTypes) {
        this.wizardSecretTypes = wizardSecretTypes;
    }

    /**
     * Instantiates and returns a key unlock fragment by type
     *
     * @param secretKeyType
     * @return
     */
    public UnlockOptionFragment getKeyUnlockFragmentInstanceForType(
            CanonicalizedSecretKey.SecretKeyType secretKeyType) {

        switch (secretKeyType) {
            case PIN: {
                return PinUnlockOptionFragment.newInstance();
            }
            case PASSPHRASE: {
                return PassphraseUnlockOptionFragment.newInstance();
            }
            case PATTERN: {
                return UnlockOptionFragment.newInstance();
            }
            default:
                return null;
        }
    }
}
