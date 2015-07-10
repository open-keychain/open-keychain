package org.sufficientlysecure.keychain.ui.dialog;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;

/**
 * Passphrase unlock dialog (used for text based passwords).
 */
public class PassphraseUnlockDialog extends UnlockDialog implements
		PassphraseUnlockDialogViewModel.OnViewModelEventBind {

	private PassphraseUnlockDialogViewModel mPassphraseUnlockDialogViewModel;
	private Button mPositiveDialogButton;
	private EditText mPassphraseEditText;
	private TextView mPassphraseText;
	private View mProgressLayout;
	private View mInputLayout;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		mPassphraseUnlockDialogViewModel = new PassphraseUnlockDialogViewModel(this);

		// if the dialog is displayed from the application class, design is missing
		// hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
		ContextThemeWrapper theme = new ContextThemeWrapper(getActivity(), R.style.KeychainTheme);
		CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

		// No title, see http://www.google.com/design/spec/components/dialogs.html#dialogs-alerts
		//alert.setTitle()
		View view = LayoutInflater.from(theme).inflate(R.layout.passphrase_dialog, null);
		alert.setView(view);
		setCancelable(false);

		mPassphraseText = (TextView) view.findViewById(R.id.passphrase_text);
		mPassphraseEditText = (EditText) view.findViewById(R.id.passphrase_passphrase);
		mInputLayout = view.findViewById(R.id.input);
		mProgressLayout = view.findViewById(R.id.progress);

		// Hack to open keyboard.
		// This is the only method that I found to work across all Android versions
		// http://turbomanage.wordpress.com/2012/05/02/show-soft-keyboard-automatically-when-edittext-receives-focus/
		// Notes: * onCreateView can't be used because we want to add buttons to the dialog
		//        * opening in onActivityCreated does not work on Android 4.4
		mPassphraseEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				mPassphraseEditText.post(new Runnable() {
					@Override
					public void run() {
						if (getActivity() == null || mPassphraseEditText == null) {
							return;
						}
						InputMethodManager imm = (InputMethodManager) getActivity()
								.getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.showSoftInput(mPassphraseEditText, InputMethodManager.SHOW_IMPLICIT);
					}
				});
			}
		});
		mPassphraseEditText.requestFocus();
		mPassphraseEditText.setImeActionLabel(getString(android.R.string.ok), EditorInfo.IME_ACTION_DONE);
		mPassphraseEditText.setOnEditorActionListener(this);

		alert.setPositiveButton(getString(R.string.unlock_caps), null);
		alert.setNegativeButton(android.R.string.cancel, null);

		mAlertDialog = alert.show();
		mPositiveDialogButton = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
		mPositiveDialogButton.setTextColor(getResources().getColor(R.color.primary));
		mPositiveDialogButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mPassphraseUnlockDialogViewModel.appendPassword(mPassphraseEditText.getText());
				mPassphraseUnlockDialogViewModel.onOperationRequest();
			}
		});

		Button b = mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
		b.setTextColor(getResources().getColor(R.color.primary));
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mPassphraseUnlockDialogViewModel.onOperationCancel();
				mAlertDialog.cancel();
			}
		});

		//only call this method after the ui is initialized.
		mPassphraseUnlockDialogViewModel.prepareViewModel(savedInstanceState, getArguments(),
				getActivity());
		return mAlertDialog;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mPassphraseUnlockDialogViewModel.saveViewModelState(outState);
	}

	@Override
	public void onUpdateDialogTitle(CharSequence text) {
		mAlertDialog.setTitle(text);
	}

	@Override
	public void onTipTextUpdate(CharSequence text) {
		mPassphraseText.setText(text);
	}

	@Override
	public void onNoRetryAllowed() {
		mPositiveDialogButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
	}

	@Override
	public void onOperationStateError(int errorId, boolean showToast) {
		mInputLayout.setVisibility(View.VISIBLE);

		if (showToast) {
			Toast.makeText(getActivity(), errorId, Toast.LENGTH_SHORT).show();
		} else {
			mPassphraseText.setText(getString(errorId));
		}
	}

	@Override
	public void onShowProgressBar(boolean show) {
		mProgressLayout.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
	}

	@Override
	public void onUnlockOperationSuccess(Intent serviceIntent) {
		getActivity().setResult(Activity.RESULT_OK, serviceIntent);
		getActivity().finish();
	}

	@Override
	public void onOperationStarted() {
		mInputLayout.setVisibility(View.INVISIBLE);
		mPositiveDialogButton.setEnabled(false);
	}

	@Override
	public void onUpdateDialogButtonText(CharSequence text) {
		mPositiveDialogButton.setText(text);
	}
}
