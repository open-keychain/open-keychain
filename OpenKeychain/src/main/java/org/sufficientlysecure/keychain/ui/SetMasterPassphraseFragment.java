package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.sufficientlysecure.keychain.KeychainApplication;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.widget.PassphraseEditText;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

public class SetMasterPassphraseFragment extends Fragment {

    // view
    private PassphraseEditText mPassphraseEdit;
    private EditText mPassphraseEditAgain;
    private CheckBox mUseSinglePasswordWorkflow;
    private Activity mActivity;

    /**
     * Checks if text of given EditText is not empty. If it is empty an error is
     * set and the EditText gets the focus.
     *
     * @param context
     * @param editText
     * @return true if EditText is not empty
     */
    private static boolean isEditTextNotEmpty(Context context, EditText editText) {
        boolean output = true;
        if (editText.getText().length() == 0) {
            editText.setError(context.getString(R.string.create_key_empty));
            editText.requestFocus();
            output = false;
        } else {
            editText.setError(null);
        }

        return output;
    }

    private static boolean areEditTextsEqual(EditText editText1, EditText editText2) {
        Passphrase p1 = new Passphrase(editText1);
        Passphrase p2 = new Passphrase(editText2);
        return (p1.equals(p2));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.set_master_passphrase_fragment, container, false);

        mPassphraseEdit = (PassphraseEditText) view.findViewById(R.id.master_passphrase);
        mPassphraseEditAgain = (EditText) view.findViewById(R.id.master_passphrase_again);
        CheckBox showPassphrase = (CheckBox) view.findViewById(R.id.show_master_passphrase);
        mUseSinglePasswordWorkflow = (CheckBox) view.findViewById(R.id.use_single_password_workflow);
        TextView helpSinglePasswordWorkflow = (TextView) view.findViewById(R.id.help_single_password_workflow);
        View nextButton = view.findViewById(R.id.set_master_passphrase_next_button);
        View backButton = view.findViewById(R.id.set_master_passphrase_back_button);

        helpSinglePasswordWorkflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), R.string.about_single_password_workflow, Toast.LENGTH_LONG).show();
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextClicked();
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                back();
            }
        });
        if (mActivity instanceof SetMasterPassphraseActivity) {
            backButton.setVisibility(View.GONE);
        }
        showPassphrase.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mPassphraseEdit.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    mPassphraseEditAgain.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    mPassphraseEdit.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    mPassphraseEditAgain.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isEditTextNotEmpty(getActivity(), mPassphraseEdit)) {
                    mPassphraseEditAgain.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    return;
                }

                if (areEditTextsEqual(mPassphraseEdit, mPassphraseEditAgain)) {
                    mPassphraseEditAgain.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_stat_retyped_ok, 0);
                } else {
                    mPassphraseEditAgain.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_stat_retyped_bad, 0);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        mPassphraseEdit.requestFocus();
        mPassphraseEdit.addTextChangedListener(textWatcher);
        mPassphraseEditAgain.addTextChangedListener(textWatcher);

        return view;
    }

    private void back() {
        hideKeyboard();
        if (mActivity instanceof MigrateSymmetricActivity) {
            ((MigrateSymmetricActivity) mActivity).loadFragment(null, MigrateSymmetricActivity.FragAction.TO_LEFT);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = getActivity();
    }

    private void nextClicked() {
        if (isEditTextNotEmpty(getActivity(), mPassphraseEdit)) {

            if (!areEditTextsEqual(mPassphraseEdit, mPassphraseEditAgain)) {
                mPassphraseEditAgain.setError(getActivity().getApplicationContext().getString(R.string.create_key_passphrases_not_equal));
                mPassphraseEditAgain.requestFocus();
                return;
            }

            mPassphraseEditAgain.setError(null);

            // save password & choice of workflow
            try {
                Passphrase passphrase = new Passphrase(mPassphraseEdit.getText());
                new ProviderHelper(getActivity()).write().saveMasterPassphrase(passphrase);
                // save till the next screen lock
                PassphraseCacheService.addMasterPassphrase(getActivity().getApplicationContext(), passphrase, 0);
            } catch (ByteArrayEncryptor.EncryptDecryptException e) {
                Toast.makeText(getActivity(), R.string.error_saving_master_password,
                        Toast.LENGTH_LONG).show();
            }
            Preferences preferences = Preferences.getPreferences(getActivity());
            preferences.setUsesSinglePassphraseWorkflow(mUseSinglePasswordWorkflow.isChecked());
            preferences.setHasMasterPassphrase(true);

            Activity activity = getActivity();
            if (activity instanceof MigrateSymmetricActivity) {
                ((MigrateSymmetricActivity) activity).finishedSettingMasterPassphrase();
            } else if (activity instanceof SetMasterPassphraseActivity) {
                Preferences.getPreferences(activity).setIsAppLockReady(true);
                activity.finish();
            }

            hideKeyboard();
        }
    }

    private void hideKeyboard() {
        if (getActivity() == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus
        View v = getActivity().getCurrentFocus();
        if (v == null)
            return;

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

}
