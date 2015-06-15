package org.sufficientlysecure.keychain.ui.keyunlock.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;

/**
 * Pattern Unlocking dialog
 */
public class PinUnlockDialog extends UnlockDialog {
    private onKeyUnlockListener mKeyUnlockListener;
    private PinUnlockDialogViewModel mPinUnlockDialogViewModel;

    private TextView mFragmentPinUnlockTip;
    private ImageView mCheckImageView;
    private ImageView mWrongImageView;
    private TextView mPinUnlockDialogStatus;
    private Button mPinUnlockKey1;
    private Button mPinUnlockKey2;
    private Button mPinUnlockKey3;
    private Button mPinUnlockKey4;
    private Button mPinUnlockKey5;
    private Button mPinUnlockKey6;
    private Button mPinUnlockKey7;
    private Button mPinUnlockKey8;
    private Button mPinUnlockKey9;
    private Button mPinUnlockKey0;

    private StringBuilder mInputKeyword;

    /**
     * Communication interface between the unlock dialog and its activity
     */
    public interface onKeyUnlockListener {

        /**
         * Confirmation that the user inserted the new keyword properly.
         */
        void onNewUnlockKeywordConfirmed();

        void onNewUnlockMethodCancel();

        void onUnlockRequest();
    }

    /**
     * Handles pin key press.
     */
    private View.OnClickListener mOnKeyClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mInputKeyword.append(((TextView) v).getText());
        }
    };

    /**
     * Resets the view to its original state.
     */
    private void resetViewState() {
        mPinUnlockDialogStatus.setText("");
        mWrongImageView.setVisibility(View.INVISIBLE);
        mCheckImageView.setVisibility(View.INVISIBLE);
        mInputKeyword = new StringBuilder();
    }

    /**
     * Dialog setup for the unlock operation
     *
     * @param alertDialogBuilder
     * @return
     */
    public Dialog prepareUnlockDialog(CustomAlertDialogBuilder alertDialogBuilder) {

        mFragmentPinUnlockTip.setText(getString(R.string.input_pin));
        alertDialogBuilder.setTitle(R.string.title_unlock);
        alertDialogBuilder.setPositiveButton(mPinUnlockDialogViewModel.
                getPositiveButtonStringForCurrentOperationState(), null);

        alertDialogBuilder.setNegativeButton(android.R.string.cancel, null);

        final AlertDialog alertDialog = alertDialogBuilder.show();
        Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        b.setTextColor(getResources().getColor(R.color.primary));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPinUnlockDialogViewModel.updateOperationState(mInputKeyword)) {
                    updateViewStateForOperation();
                    mInputKeyword = new StringBuilder();
                } else {
                    onOperationStateError();
                }

                if (mPinUnlockDialogViewModel.isOperationCompleted()) {
                    alertDialog.dismiss();
                    if (mKeyUnlockListener != null) {
                        mKeyUnlockListener.onUnlockRequest();
                    }
                }
            }
        });

        b = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        b.setTextColor(getResources().getColor(R.color.primary));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mKeyUnlockListener != null) {
                    mKeyUnlockListener.onNewUnlockMethodCancel();
                }
                alertDialog.cancel();
            }
        });

        return alertDialog;
    }

    /**
     * Dialog setup for the new keyword operation
     *
     * @param alertDialogBuilder
     * @return
     */
    public Dialog prepareNewPinDialog(CustomAlertDialogBuilder alertDialogBuilder) {
        alertDialogBuilder.setTitle(R.string.pin);

        alertDialogBuilder.setPositiveButton(mPinUnlockDialogViewModel.
                getPositiveButtonStringForCurrentOperationState(), null);
        alertDialogBuilder.setNeutralButton(getString(R.string.reset), null);

        alertDialogBuilder.setNegativeButton(android.R.string.cancel, null);

        final AlertDialog alertDialog = alertDialogBuilder.show();
        Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        b.setTextColor(getResources().getColor(R.color.primary));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mPinUnlockDialogViewModel.updateOperationState(mInputKeyword)) {
                    updateViewStateForOperation();
                    mInputKeyword = new StringBuilder();
                } else {
                    onOperationStateError();
                }

                if (mPinUnlockDialogViewModel.isOperationCompleted()) {
                    alertDialog.dismiss();
                    if (mKeyUnlockListener != null) {
                        mKeyUnlockListener.onNewUnlockKeywordConfirmed();
                    }
                }
            }
        });

        b = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        b.setTextColor(getResources().getColor(R.color.primary));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mKeyUnlockListener != null) {
                    mKeyUnlockListener.onNewUnlockMethodCancel();
                }
                alertDialog.cancel();
            }
        });

        b = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        b.setTextColor(getResources().getColor(R.color.primary));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPinUnlockDialogViewModel.initializeUnlockOperation(
                        mPinUnlockDialogViewModel.getDialogUnlockType());
                resetViewState();

                Button b = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
                b.setText(mPinUnlockDialogViewModel.
                        getPositiveButtonStringForCurrentOperationState());
            }
        });
        return alertDialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mPinUnlockDialogViewModel = new PinUnlockDialogViewModel();
        mPinUnlockDialogViewModel.prepareViewModel(savedInstanceState, getArguments(), getActivity());

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(getActivity());


        View view = LayoutInflater.from(getActivity()).inflate(R.layout.unlock_pin_fragment,
                null);
        alert.setView(view);

        setCancelable(false);

        mPinUnlockKey0 = (Button) view.findViewById(R.id.unlockKey0);
        mPinUnlockKey0.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey9 = (Button) view.findViewById(R.id.unlockKey9);
        mPinUnlockKey9.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey8 = (Button) view.findViewById(R.id.unlockKey8);
        mPinUnlockKey8.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey7 = (Button) view.findViewById(R.id.unlockKey7);
        mPinUnlockKey7.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey6 = (Button) view.findViewById(R.id.unlockKey6);
        mPinUnlockKey6.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey5 = (Button) view.findViewById(R.id.unlockKey5);
        mPinUnlockKey5.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey4 = (Button) view.findViewById(R.id.unlockKey4);
        mPinUnlockKey4.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey3 = (Button) view.findViewById(R.id.unlockKey3);
        mPinUnlockKey3.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey2 = (Button) view.findViewById(R.id.unlockKey2);
        mPinUnlockKey2.setOnClickListener(mOnKeyClickListener);
        mPinUnlockKey1 = (Button) view.findViewById(R.id.unlockKey1);
        mPinUnlockKey1.setOnClickListener(mOnKeyClickListener);
        mPinUnlockDialogStatus = (TextView) view.findViewById(R.id.unlockDialogStatus);
        mWrongImageView = (ImageView) view.findViewById(R.id.wrong);
        mCheckImageView = (ImageView) view.findViewById(R.id.check);
        mFragmentPinUnlockTip = (TextView) view.findViewById(R.id.unlockTip);


        resetViewState();
        if (mPinUnlockDialogViewModel.getDialogUnlockType() ==
                PinUnlockDialogViewModel.DialogUnlockOperation.DIALOG_UNLOCK_TYPE_UNLOCK_KEY) {
            return prepareUnlockDialog(alert);
        } else {
            resetViewState();
            return prepareNewPinDialog(alert);
        }
    }

    /**
     * Updates the view state by giving feedback to the user.
     */
    private void updateViewStateForOperation() {
        mPinUnlockDialogStatus.setText(mPinUnlockDialogViewModel.getOperationStatusText());
        mPinUnlockDialogStatus.setTextColor(getResources().
                getColor(R.color.android_green_dark));

        Button b = ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
        b.setText(mPinUnlockDialogViewModel.getPositiveButtonStringForCurrentOperationState());
    }

    /**
     * Notifies the user of any errors that may have occurred
     */
    private void onOperationStateError() {
        mPinUnlockDialogStatus.setText(mPinUnlockDialogViewModel.getLastOperationError());
        mPinUnlockDialogStatus.setTextColor(getResources().getColor(R.color.android_red_dark));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mKeyUnlockListener = (onKeyUnlockListener) activity;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPinUnlockDialogViewModel.saveViewModelState(outState);
    }
}
