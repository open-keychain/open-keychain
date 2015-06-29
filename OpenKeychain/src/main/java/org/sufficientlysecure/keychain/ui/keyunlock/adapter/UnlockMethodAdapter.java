package org.sufficientlysecure.keychain.ui.keyunlock.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.sufficientlysecure.keychain.ui.keyunlock.wizard.UnlockWizardFragmentViewModel;

/**
 * Adapter that instantiates key unlocking fragments.
 */
public class UnlockMethodAdapter extends FragmentStatePagerAdapter {
    private UnlockWizardFragmentViewModel mViewModel;

    public UnlockMethodAdapter(FragmentManager fm, UnlockWizardFragmentViewModel viewModel) {
        super(fm);

        mViewModel = viewModel;
    }

    @Override
    public Fragment getItem(int position) {
        return mViewModel.getKeyUnlockFragmentInstanceForType(mViewModel.getWizardSecretTypes()
                .get(position));
    }

    @Override
    public int getCount() {
        return mViewModel.getWizardSecretTypes().size();
    }
}
