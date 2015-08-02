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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;

/**
 * Pin Unlocking dialog
 */
public class PinUnlockDialog extends UnlockDialog
        implements PinUnlockDialogViewModel.OnViewModelEventBind {

    private PinUnlockDialogViewModel mPinUnlockDialogViewModel;
    private TextView mUnlockTip;
    private ProgressBar mProgressBar;
    private Button mPositiveDialogButton;
    private FeedbackIndicatorView mFeedbackIndicatorView;

    /**
     * Handles pin key press.
     */
    private View.OnClickListener mOnKeyClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPinUnlockDialogViewModel.appendPinNumber(((TextView) v).getText());
        }
    };

    /**
     * Dialog setup for the unlock operation
     *
     * @param alertDialogBuilder
     * @return
     */
    public Dialog prepareUnlockDialog(CustomAlertDialogBuilder alertDialogBuilder) {
        alertDialogBuilder.setTitle(getString(R.string.title_unlock));
        alertDialogBuilder.setPositiveButton(getString(R.string.unlock_caps), null);
        alertDialogBuilder.setNegativeButton(android.R.string.cancel, null);

        mAlertDialog = alertDialogBuilder.show();
        mPositiveDialogButton = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        mPositiveDialogButton.setTextColor(getResources().getColor(R.color.primary));
        mPositiveDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPinUnlockDialogViewModel.onOperationRequest();
            }
        });

        Button b = mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        b.setTextColor(getResources().getColor(R.color.primary));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPinUnlockDialogViewModel.onOperationCancel();
                mAlertDialog.cancel();
            }
        });

        return mAlertDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        mPinUnlockDialogViewModel = new PinUnlockDialogViewModel(this);

        // if the dialog is displayed from the application class, design is missing
        // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(getActivity(), R.style.KeychainTheme);

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);
        View view = LayoutInflater.from(theme).inflate(R.layout.unlock_pin_fragment, null);
        alert.setView(view);
        setCancelable(false);

        Button pinUnlockKey = (Button) view.findViewById(R.id.unlockKey0);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey9);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey8);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey7);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey6);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey5);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey4);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey3);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey2);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        pinUnlockKey = (Button) view.findViewById(R.id.unlockKey1);
        pinUnlockKey.setOnClickListener(mOnKeyClickListener);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mUnlockTip = (TextView) view.findViewById(R.id.unlockTip);
        mFeedbackIndicatorView = (FeedbackIndicatorView) view.findViewById(R.id.unlockUserFeedback);

        //only call this method after the ui is initialized.
        mPinUnlockDialogViewModel.prepareViewModel(savedInstanceState, getArguments(), getActivity());
        return prepareUnlockDialog(alert);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPinUnlockDialogViewModel.onDetachFromActivity();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPinUnlockDialogViewModel.saveViewModelState(outState);
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
