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
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ByteArrayEncryptor;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.widget.PassphraseEditText;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

public class SetMasterPassphraseFragment extends Fragment {

    public static final String ARG_FROM_APP_SETTINGS = "from_app_settings";
    public static final String ARG_CURRENT_PASSPHRASE = "current_passphrase";

    private boolean mFromAppSettings;
    private Activity mActivity;
    private Preferences mPreferences;
    private Passphrase mCurrentMasterPassphrase;

    private PassphraseEditText mPassphraseEdit;
    private EditText mPassphraseEditAgain;
    private CheckBox mUseSinglePasswordWorkflow;

    public static SetMasterPassphraseFragment newInstance(boolean fromSettingsPage, Passphrase passphrase) {
        if (fromSettingsPage && passphrase == null) {
            throw new AssertionError("Should have passphrase when coming from settings menu!");
        }

        Bundle args = new Bundle();
        args.putBoolean(ARG_FROM_APP_SETTINGS, fromSettingsPage);
        args.putParcelable(ARG_CURRENT_PASSPHRASE, passphrase);

        SetMasterPassphraseFragment fragment = new SetMasterPassphraseFragment();
        fragment.setArguments(args);

        return fragment;
    }

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
        mPreferences = Preferences.getPreferences(getActivity());

        View view = inflater.inflate(R.layout.set_master_passphrase_fragment, container, false);

        mPassphraseEdit = (PassphraseEditText) view.findViewById(R.id.master_passphrase);
        mPassphraseEditAgain = (EditText) view.findViewById(R.id.master_passphrase_again);
        CheckBox showPassphrase = (CheckBox) view.findViewById(R.id.show_master_passphrase);
        mUseSinglePasswordWorkflow = (CheckBox) view.findViewById(R.id.use_single_password_workflow);
        TextView helpSinglePasswordWorkflow = (TextView) view.findViewById(R.id.help_single_password_workflow);
        TextView nextButton = (TextView) view.findViewById(R.id.set_master_passphrase_next_button);
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

        mFromAppSettings = getArguments().getBoolean(ARG_FROM_APP_SETTINGS);
        if (mFromAppSettings) {
            // don't show workflow selection
            view.findViewById(R.id.workflow_selection_layout).setVisibility(View.GONE);
            backButton.setVisibility(View.GONE);
            nextButton.setText(R.string.set_master_passphrase_button);

            // also get passphrase from user
            mCurrentMasterPassphrase = getArguments().getParcelable(ARG_CURRENT_PASSPHRASE);
        }

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

            hideKeyboard();
            mPassphraseEditAgain.setError(null);

            // saving the master passphrase
            try {
                Passphrase newPassphrase = new Passphrase(mPassphraseEdit.getText());
                ProviderHelper helper = new ProviderHelper(mActivity);

                boolean success = (mPreferences.hasMasterPassphrase())
                        ? helper.write().changeMasterPassphrase(newPassphrase, mCurrentMasterPassphrase)
                        : helper.write().saveMasterPassphrase(newPassphrase);

                if (!success) {
                    Toast.makeText(getActivity(),
                            R.string.error_write_to_db_master_password,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                // save till the next screen lock
                PassphraseCacheService.addMasterPassphrase(getActivity().getApplicationContext(), newPassphrase, 0);
                mPreferences.setHasMasterPassphrase(true);

            } catch (ByteArrayEncryptor.EncryptDecryptException | ByteArrayEncryptor.IncorrectPassphraseException e) {
                // passphrase should never be wrong, check method of retrieval
                Toast.makeText(getActivity(), R.string.error_saving_master_password,
                        Toast.LENGTH_LONG).show();
                return;
            }

            // save choice of workflow & finish
            if (mActivity instanceof MigrateSymmetricActivity) {
                // do not finish, MigrateSymmetricActivity takes over from here
                ((MigrateSymmetricActivity) mActivity).finishedSettingMasterPassphrase();
                mPreferences.setUsesSinglePassphraseWorkflow(mUseSinglePasswordWorkflow.isChecked());
            } else if (mActivity instanceof SetMasterPassphraseActivity) {
                Preferences.getPreferences(mActivity).setIsAppLockReady(true);
                mPreferences.setUsesSinglePassphraseWorkflow(mUseSinglePasswordWorkflow.isChecked());
                mActivity.finish();
            } else if (mFromAppSettings) {
                // don't save choice of workflow, we have hidden it if coming from app settings
                mActivity.finish();
            }
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
