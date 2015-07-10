package org.sufficientlysecure.keychain.ui.wizard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.ImportKeysActivity;
import org.sufficientlysecure.keychain.ui.MainActivity;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

public class WelcomeWizardFragment extends WizardFragment {
	public static final int REQUEST_CODE_IMPORT_KEY = 0x00007012;

	public static WelcomeWizardFragment newInstance() {
		return new WelcomeWizardFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.wizard_welcome_fragment, container, false);
		TextView textView = (TextView) view.findViewById(R.id.create_key_cancel);
		textView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mWizardFragmentListener != null) {
					mWizardFragmentListener.cancelRequest();
				}
			}
		});

		textView = (TextView) view.findViewById(R.id.create_key_create_key_button);
		textView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mWizardFragmentListener != null) {
					mWizardFragmentListener.onAdvanceToNextWizardStep();
				}
			}
		});

		textView = (TextView) view.findViewById(R.id.create_key_import_button);
		textView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(), ImportKeysActivity.class);
				intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN);
				startActivityForResult(intent, REQUEST_CODE_IMPORT_KEY);
			}
		});

		textView = (TextView) view.findViewById(R.id.create_key_yubikey_button);
		textView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mWizardFragmentListener != null) {
					mWizardFragmentListener.setUseYubiKey();
					mWizardFragmentListener.onAdvanceToNextWizardStep();
				}
			}
		});

		if (mWizardFragmentListener != null) {
			mWizardFragmentListener.onHideNavigationButtons(true, true);
		}
		return view;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_CODE_IMPORT_KEY) {
			if (resultCode == Activity.RESULT_OK) {
				if (mWizardFragmentListener != null) {
					if (mWizardFragmentListener.isFirstTime()) {
						Preferences prefs = Preferences.getPreferences(getActivity());
						prefs.setFirstTime(false);
						Intent intent = new Intent(getActivity(), MainActivity.class);
						intent.putExtras(data);
						startActivity(intent);
						getActivity().finish();
					} else {
						// just finish activity and return data
						getActivity().setResult(Activity.RESULT_OK, data);
						getActivity().finish();
					}
				}
			}
		} else {
			Log.e(Constants.TAG, "No valid request code!");
		}
	}

	public boolean onBackClicked() {
		return false;
	}
}
