package org.sufficientlysecure.keychain.ui.keyunlock.wizard;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.keyunlock.base.WizardFragmentListener;
import org.sufficientlysecure.keychain.ui.keyunlock.adapter.UnlockMethodAdapter;
import org.sufficientlysecure.keychain.ui.keyunlock.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.keyunlock.components.ViewPagerIndicator;

/**
 * Fragment that acts as a Key Unlock Wizard.
 * Child Fragments will be used for the user to swipe between the unlocking methods.
 * TODO: 09/06/2015 Bug when fragments are animated. The child fragments disapear.
 */
public class UnlockWizardFragment extends WizardFragment {
    public static final String TAG = "UnlockWizardFragment";
    public static final String SAVE_STATE_CURRENT_PAGE_INDEX = "SAVE_STATE_CURRENT_PAGE_INDEX";

    private ViewPager mUnlockWizardFragmentViewPager;
    private ViewPagerIndicator mViewPagerIndicator;
    private UnlockMethodAdapter mUnlockMethodAdapter;
    private UnlockWizardFragmentViewModel mUnlockWizardFragmentViewModel;

    public static UnlockWizardFragment newInstance() {
        return new UnlockWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUnlockWizardFragmentViewModel = new UnlockWizardFragmentViewModel();
        mUnlockWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());

        mUnlockMethodAdapter = new UnlockMethodAdapter(getChildFragmentManager(),
                mUnlockWizardFragmentViewModel);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.unlock_wizard_fragment, container, false);
        mUnlockWizardFragmentViewPager = (ViewPager) view.findViewById(R.id.
                unlockWizardFragmentViewPager);
        mUnlockWizardFragmentViewPager.setAdapter(mUnlockMethodAdapter);

        mViewPagerIndicator = (ViewPagerIndicator) view.findViewById(R.id.viewPagerIndicator);
        mViewPagerIndicator.initViewPagerIndicator(mUnlockWizardFragmentViewPager);

        mUnlockWizardFragmentViewPager.setOnPageChangeListener(mViewPagerIndicator);

        if(mWizardFragmentListener != null) {
            mWizardFragmentListener.onHideNavigationButtons(false);
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mUnlockWizardFragmentViewModel.saveViewModelState(outState);
        outState.putInt(SAVE_STATE_CURRENT_PAGE_INDEX, mUnlockWizardFragmentViewPager.
                getCurrentItem());
    }
}