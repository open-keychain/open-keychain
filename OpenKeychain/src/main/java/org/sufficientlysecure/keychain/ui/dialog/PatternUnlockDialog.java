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
package org.sufficientlysecure.keychain.ui.dialog;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eftimoff.patternview.PatternView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;

public class PatternUnlockDialog extends UnlockDialog
        implements PatternUnlockDialogViewModel.OnViewModelEventBind {
    private PatternUnlockDialogViewModel mPatternUnlockDialogViewModel;
    private FeedbackIndicatorView mFeedbackIndicatorView;
    private ProgressBar mProgressBar;
    private TextView mUnlockTip;
    private Button mPositiveDialogButton;
    private PatternView mPatternView;

    public Dialog prepareUnlockDialog(CustomAlertDialogBuilder alertDialogBuilder) {
        alertDialogBuilder.setTitle(getString(R.string.title_unlock));
        alertDialogBuilder.setPositiveButton(getString(R.string.unlock_caps), null);
        alertDialogBuilder.setNegativeButton(android.R.string.cancel, null);

        mAlertDialog = alertDialogBuilder.show();
        mPositiveDialogButton = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        mPositiveDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPatternUnlockDialogViewModel.onOperationRequest();
            }
        });

        Button b = mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPatternUnlockDialogViewModel.onOperationCancel();
                mAlertDialog.cancel();
            }
        });

        mAlertDialog.setCanceledOnTouchOutside(false);

        mPatternView.setOnPatternCellAddedListener(new PatternView.OnPatternCellAddedListener() {
            @Override
            public void onPatternCellAdded() {
                mPatternUnlockDialogViewModel.appendPattern(mPatternView.getPatternString());
            }
        });

        mPatternView.setOnPatternStartListener(new PatternView.OnPatternStartListener() {
            @Override
            public void onPatternStart() {
                mPatternUnlockDialogViewModel.resetCurrentKeyword();
            }
        });

        return mAlertDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        mPatternUnlockDialogViewModel = new PatternUnlockDialogViewModel(this);

        // if the dialog is displayed from the application class, design is missing
        // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(getActivity(), R.style.KeychainTheme);

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);
        View view = LayoutInflater.from(theme).inflate(R.layout.unlock_pattern_fragment, null);
        alert.setView(view);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mUnlockTip = (TextView) view.findViewById(R.id.unlockTip);
        mFeedbackIndicatorView = (FeedbackIndicatorView) view.findViewById(R.id.unlockUserFeedback);
        mPatternView = (PatternView) view.findViewById(R.id.patternView);

        //only call this method after the ui is initialized.
        mPatternUnlockDialogViewModel.prepareViewModel(savedInstanceState, getArguments(), getActivity());
        return prepareUnlockDialog(alert);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPatternUnlockDialogViewModel.onDetachFromActivity();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPatternUnlockDialogViewModel.saveViewModelState(outState);
    }

    @Override
    public void onUnlockOperationSuccess(Intent serviceIntent) {
        getActivity().setResult(Activity.RESULT_OK, serviceIntent);
        getActivity().finish();
    }

    /**
     * Notifies the user of any errors that may have occurred
     */
    @Override
    public void onOperationStateError(int errorId, boolean showToast) {
        if (showToast) {
            Toast.makeText(getActivity(), errorId, Toast.LENGTH_SHORT).show();
        } else {
            mFeedbackIndicatorView.showWrongTextMessage(getString(errorId), true);
        }
    }

    /**
     * Shows the progress bar.
     *
     * @param show
     */
    @Override
    public void onShowProgressBar(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Updates the dialog button.
     *
     * @param text
     */
    @Override
    public void onUpdateDialogButtonText(CharSequence text) {
        mPositiveDialogButton.setText(text);
    }

    /**
     * Notifies the dialog that the unlock operation has started.
     */
    @Override
    public void onOperationStarted() {
        mFeedbackIndicatorView.hideMessageAndIcon();
    }

    /**
     * Notifies the view that no retries are allowed. The next positive action will be the dialog
     * dismiss.
     */
    @Override
    public void onNoRetryAllowed() {
        mPositiveDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    /**
     * updates the dialog title.
     *
     * @param text
     */
    @Override
    public void onUpdateDialogTitle(CharSequence text) {
        mAlertDialog.setTitle(text);
    }

    /**
     * updates the dialog tip text.
     *
     * @param text
     */
    @Override
    public void onTipTextUpdate(CharSequence text) {
        mUnlockTip.setText(text);
    }
}
