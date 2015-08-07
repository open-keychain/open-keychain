/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sufficientlysecure.keychain.ui.wizard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eftimoff.patternview.PatternView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;

public class PatternUnlockWizardFragment extends WizardFragment
        implements PatternUnlockWizardFragmentViewModel.OnViewModelEventBind {

    private PatternUnlockWizardFragmentViewModel mPatternUnlockWizardFragmentViewModel;
    private FeedbackIndicatorView mFeedbackIndicatorView;
    private PatternView mPatternView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPatternUnlockWizardFragmentViewModel = new PatternUnlockWizardFragmentViewModel(this,
                mWizardFragmentListener);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.unlock_pattern_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFeedbackIndicatorView = (FeedbackIndicatorView) view.findViewById(R.id.unlockUserFeedback);
        mPatternView = (PatternView) view.findViewById(R.id.patternView);
        mPatternUnlockWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(), getActivity());

        mPatternView.setOnPatternCellAddedListener(new PatternView.OnPatternCellAddedListener() {
            @Override
            public void onPatternCellAdded() {
                mPatternUnlockWizardFragmentViewModel.appendPattern(mPatternView.getPatternString());
            }
        });

        mPatternView.setOnPatternStartListener(new PatternView.OnPatternStartListener() {
            @Override
            public void onPatternStart() {
                mPatternUnlockWizardFragmentViewModel.resetCurrentKeyword();
            }
        });
    }

    @Override
    public int getPatternLength() {
        return mPatternView.getPattern().size();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPatternUnlockWizardFragmentViewModel.saveViewModelState(outState);
    }

    @Override
    public boolean onNextClicked() {
        return mPatternUnlockWizardFragmentViewModel.onNextClicked();
    }

    /**
     * Notifies the user of any errors that may have occurred
     */
    public void onOperationStateError(String error) {
        mFeedbackIndicatorView.showWrongTextMessage(error, true);
    }

    public void onOperationStateOK(String showText) {
        mFeedbackIndicatorView.showCorrectTextMessage(showText, false);
    }

    /**
     * Updates the view state by giving feedback to the user.
     */
    public void onOperationStateCompleted(String showText) {
        mFeedbackIndicatorView.showCorrectTextMessage(showText, true);
    }

    public void hideNavigationButtons(boolean hideBack, boolean hideNext) {
        mWizardFragmentListener.onHideNavigationButtons(hideBack, hideNext);
    }
}
