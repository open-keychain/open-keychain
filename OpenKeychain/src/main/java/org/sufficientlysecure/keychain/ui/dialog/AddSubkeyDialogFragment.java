/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.spongycastle.bcpg.PublicKeyAlgorithmTags;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.util.Choice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class AddSubkeyDialogFragment extends DialogFragment {

    public interface OnAlgorithmSelectedListener {
        public void onAlgorithmSelected(SaveKeyringParcel.SubkeyAdd newSubkey);
    }

    private static final String ARG_WILL_BE_MASTER_KEY = "will_be_master_key";

    private OnAlgorithmSelectedListener mAlgorithmSelectedListener;

    private CheckBox mNoExpiryCheckBox;
    private TableRow mExpiryRow;
    private DatePicker mExpiryDatePicker;
    private Spinner mAlgorithmSpinner;
    private Spinner mKeySizeSpinner;
    private TextView mCustomKeyTextView;
    private EditText mCustomKeyEditText;
    private TextView mCustomKeyInfoTextView;
    private CheckBox mFlagCertify;
    private CheckBox mFlagSign;
    private CheckBox mFlagEncrypt;
    private CheckBox mFlagAuthenticate;

    private boolean mWillBeMasterKey;

    public void setOnAlgorithmSelectedListener(OnAlgorithmSelectedListener listener) {
        mAlgorithmSelectedListener = listener;
    }

    public static AddSubkeyDialogFragment newInstance(boolean willBeMasterKey) {
        AddSubkeyDialogFragment frag = new AddSubkeyDialogFragment();
        Bundle args = new Bundle();

        args.putBoolean(ARG_WILL_BE_MASTER_KEY, willBeMasterKey);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity context = getActivity();
        final LayoutInflater mInflater;

        mWillBeMasterKey = getArguments().getBoolean(ARG_WILL_BE_MASTER_KEY);
        mInflater = context.getLayoutInflater();

        CustomAlertDialogBuilder dialog = new CustomAlertDialogBuilder(context);

        View view = mInflater.inflate(R.layout.add_subkey_dialog, null);
        dialog.setView(view);
        dialog.setTitle(R.string.title_add_subkey);

        mNoExpiryCheckBox = (CheckBox) view.findViewById(R.id.add_subkey_no_expiry);
        mExpiryRow = (TableRow) view.findViewById(R.id.add_subkey_expiry_row);
        mExpiryDatePicker = (DatePicker) view.findViewById(R.id.add_subkey_expiry_date_picker);
        mAlgorithmSpinner = (Spinner) view.findViewById(R.id.add_subkey_algorithm);
        mKeySizeSpinner = (Spinner) view.findViewById(R.id.add_subkey_size);
        mCustomKeyTextView = (TextView) view.findViewById(R.id.add_subkey_custom_key_size_label);
        mCustomKeyEditText = (EditText) view.findViewById(R.id.add_subkey_custom_key_size_input);
        mCustomKeyInfoTextView = (TextView) view.findViewById(R.id.add_subkey_custom_key_size_info);
        mFlagCertify = (CheckBox) view.findViewById(R.id.add_subkey_flag_certify);
        mFlagSign = (CheckBox) view.findViewById(R.id.add_subkey_flag_sign);
        mFlagEncrypt = (CheckBox) view.findViewById(R.id.add_subkey_flag_encrypt);
        mFlagAuthenticate = (CheckBox) view.findViewById(R.id.add_subkey_flag_authenticate);

        mNoExpiryCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mExpiryRow.setVisibility(View.GONE);
                } else {
                    mExpiryRow.setVisibility(View.VISIBLE);
                }
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            // date picker works based on default time zone
            Calendar minDateCal = Calendar.getInstance(TimeZone.getDefault());
            minDateCal.add(Calendar.DAY_OF_YEAR, 1); // at least one day after creation (today)
            mExpiryDatePicker.setMinDate(minDateCal.getTime().getTime());
        }

        ArrayList<Choice> choices = new ArrayList<Choice>();
        choices.add(new Choice(PublicKeyAlgorithmTags.DSA, getResources().getString(
                R.string.dsa)));
        if (!mWillBeMasterKey) {
            choices.add(new Choice(PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT, getResources().getString(
                    R.string.elgamal)));
        }
        choices.add(new Choice(PublicKeyAlgorithmTags.RSA_GENERAL, getResources().getString(
                R.string.rsa)));
        choices.add(new Choice(PublicKeyAlgorithmTags.ECDH, getResources().getString(
                R.string.ecdh)));
        choices.add(new Choice(PublicKeyAlgorithmTags.ECDSA, getResources().getString(
                R.string.ecdsa)));
        ArrayAdapter<Choice> adapter = new ArrayAdapter<Choice>(context,
                android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAlgorithmSpinner.setAdapter(adapter);
        // make RSA the default
        for (int i = 0; i < choices.size(); ++i) {
            if (choices.get(i).getId() == PublicKeyAlgorithmTags.RSA_GENERAL) {
                mAlgorithmSpinner.setSelection(i);
                break;
            }
        }

        // dynamic ArrayAdapter must be created (instead of ArrayAdapter.getFromResource), because it's content may change
        ArrayAdapter<CharSequence> keySizeAdapter = new ArrayAdapter<CharSequence>(context, android.R.layout.simple_spinner_item,
                new ArrayList<CharSequence>(Arrays.asList(getResources().getStringArray(R.array.rsa_key_size_spinner_values))));
        keySizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mKeySizeSpinner.setAdapter(keySizeAdapter);
        mKeySizeSpinner.setSelection(1); // Default to 4096 for the key length


        dialog.setCancelable(true);

        // onClickListener are set in onStart() to override default dismiss behaviour
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.setNegativeButton(android.R.string.cancel, null);

        final AlertDialog alertDialog = dialog.show();

        mCustomKeyEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                setOkButtonAvailability(alertDialog);
            }
        });

        mKeySizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setCustomKeyVisibility();
                setOkButtonAvailability(alertDialog);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mAlgorithmSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateUiForAlgorithm(((Choice) parent.getSelectedItem()).getId());

                setCustomKeyVisibility();
                setOkButtonAvailability(alertDialog);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return alertDialog;
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            Button negativeButton = d.getButton(Dialog.BUTTON_NEGATIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mFlagCertify.isChecked() && !mFlagSign.isChecked()
                            && !mFlagEncrypt.isChecked() && !mFlagAuthenticate.isChecked()) {
                        Toast.makeText(getActivity(), R.string.edit_key_select_flag, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // dismiss only if at least one flag is selected
                    dismiss();

                    Choice newKeyAlgorithmChoice = (Choice) mAlgorithmSpinner.getSelectedItem();
                    int newKeySize = getProperKeyLength(newKeyAlgorithmChoice.getId(), getSelectedKeyLength());

                    int flags = 0;
                    if (mFlagCertify.isChecked()) {
                        flags |= KeyFlags.CERTIFY_OTHER;
                    }
                    if (mFlagSign.isChecked()) {
                        flags |= KeyFlags.SIGN_DATA;
                    }
                    if (mFlagEncrypt.isChecked()) {
                        flags |= KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE;
                    }
                    if (mFlagAuthenticate.isChecked()) {
                        flags |= KeyFlags.AUTHENTICATION;
                    }

                    long expiry;
                    if (mNoExpiryCheckBox.isChecked()) {
                        expiry = 0L;
                    } else {
                        Calendar selectedCal = Calendar.getInstance(TimeZone.getDefault());
                        //noinspection ResourceType
                        selectedCal.set(mExpiryDatePicker.getYear(),
                                mExpiryDatePicker.getMonth(), mExpiryDatePicker.getDayOfMonth());
                        // date picker uses default time zone, we need to convert to UTC
                        selectedCal.setTimeZone(TimeZone.getTimeZone("UTC"));

                        expiry = selectedCal.getTime().getTime() / 1000;
                    }

                    SaveKeyringParcel.SubkeyAdd newSubkey = new SaveKeyringParcel.SubkeyAdd(
                            newKeyAlgorithmChoice.getId(),
                            newKeySize,
                            flags,
                            expiry
                    );
                    mAlgorithmSelectedListener.onAlgorithmSelected(newSubkey);
                }
            });
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }

    private int getSelectedKeyLength() {
        final String selectedItemString = (String) mKeySizeSpinner.getSelectedItem();
        final String customLengthString = getResources().getString(R.string.key_size_custom);
        final boolean customSelected = customLengthString.equals(selectedItemString);
        String keyLengthString = customSelected ? mCustomKeyEditText.getText().toString() : selectedItemString;
        int keySize;
        try {
            keySize = Integer.parseInt(keyLengthString);
        } catch (NumberFormatException e) {
            keySize = 0;
        }
        return keySize;
    }

    /**
     * <h3>RSA</h3>
     * <p>for RSA algorithm, key length must be greater than 1024 (according to
     * <a href="https://github.com/open-keychain/open-keychain/issues/102">#102</a>). Possibility to generate keys bigger
     * than 8192 bits is currently disabled, because it's almost impossible to generate them on a mobile device (check
     * <a href="http://www.javamex.com/tutorials/cryptography/rsa_key_length.shtml">RSA key length plot</a> and
     * <a href="http://www.keylength.com/">Cryptographic Key Length Recommendation</a>). Also, key length must be a
     * multiplicity of 8.</p>
     * <h3>ElGamal</h3>
     * <p>For ElGamal algorithm, supported key lengths are 1536, 2048, 3072, 4096 or 8192 bits.</p>
     * <h3>DSA</h3>
     * <p>For DSA algorithm key length must be between 512 and 1024. Also, it must me dividable by 64.</p>
     *
     * @return correct key length, according to SpongyCastle specification. Returns <code>-1</code>, if key length is
     * inappropriate.
     */
    private int getProperKeyLength(int algorithmId, int currentKeyLength) {
        final int[] elGamalSupportedLengths = {1536, 2048, 3072, 4096, 8192};
        int properKeyLength = -1;
        switch (algorithmId) {
            case PublicKeyAlgorithmTags.RSA_GENERAL:
                if (currentKeyLength > 1024 && currentKeyLength <= 16384) {
                    properKeyLength = currentKeyLength + ((8 - (currentKeyLength % 8)) % 8);
                }
                break;
            case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT:
                int[] elGammalKeyDiff = new int[elGamalSupportedLengths.length];
                for (int i = 0; i < elGamalSupportedLengths.length; i++) {
                    elGammalKeyDiff[i] = Math.abs(elGamalSupportedLengths[i] - currentKeyLength);
                }
                int minimalValue = Integer.MAX_VALUE;
                int minimalIndex = -1;
                for (int i = 0; i < elGammalKeyDiff.length; i++) {
                    if (elGammalKeyDiff[i] <= minimalValue) {
                        minimalValue = elGammalKeyDiff[i];
                        minimalIndex = i;
                    }
                }
                properKeyLength = elGamalSupportedLengths[minimalIndex];
                break;
            case PublicKeyAlgorithmTags.DSA:
                if (currentKeyLength >= 512 && currentKeyLength <= 1024) {
                    properKeyLength = currentKeyLength + ((64 - (currentKeyLength % 64)) % 64);
                }
                break;
        }
        return properKeyLength;
    }

    private void setOkButtonAvailability(AlertDialog alertDialog) {
        final Choice selectedAlgorithm = (Choice) mAlgorithmSpinner.getSelectedItem();
        final int selectedKeySize = getSelectedKeyLength(); //Integer.parseInt((String) mKeySizeSpinner.getSelectedItem());
        final int properKeyLength = getProperKeyLength(selectedAlgorithm.getId(), selectedKeySize);
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(properKeyLength > 0);
    }

    private void setCustomKeyVisibility() {
        final String selectedItemString = (String) mKeySizeSpinner.getSelectedItem();
        final String customLengthString = getResources().getString(R.string.key_size_custom);
        final boolean customSelected = customLengthString.equals(selectedItemString);
        final int visibility = customSelected ? View.VISIBLE : View.GONE;

        mCustomKeyEditText.setVisibility(visibility);
        mCustomKeyTextView.setVisibility(visibility);
        mCustomKeyInfoTextView.setVisibility(visibility);

        // hide keyboard after setting visibility to gone
        if (visibility == View.GONE) {
            InputMethodManager imm = (InputMethodManager)
                    getActivity().getSystemService(getActivity().INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mCustomKeyEditText.getWindowToken(), 0);
        }
    }

    private void updateUiForAlgorithm(int algorithmId) {
        final ArrayAdapter<CharSequence> keySizeAdapter = (ArrayAdapter<CharSequence>) mKeySizeSpinner.getAdapter();
        final Object selectedItem = mKeySizeSpinner.getSelectedItem();
        keySizeAdapter.clear();
        switch (algorithmId) {
            case PublicKeyAlgorithmTags.RSA_GENERAL:
                replaceArrayAdapterContent(keySizeAdapter, R.array.rsa_key_size_spinner_values);
                mCustomKeyInfoTextView.setText(getResources().getString(R.string.key_size_custom_info_rsa));
                // allowed flags:
                mFlagSign.setEnabled(true);
                mFlagEncrypt.setEnabled(true);
                mFlagAuthenticate.setEnabled(true);

                if (mWillBeMasterKey) {
                    mFlagCertify.setEnabled(true);

                    mFlagCertify.setChecked(true);
                    mFlagSign.setChecked(false);
                    mFlagEncrypt.setChecked(false);
                } else {
                    mFlagCertify.setEnabled(false);

                    mFlagCertify.setChecked(false);
                    mFlagSign.setChecked(true);
                    mFlagEncrypt.setChecked(true);
                }
                mFlagAuthenticate.setChecked(false);
                break;
            case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT:
                replaceArrayAdapterContent(keySizeAdapter, R.array.elgamal_key_size_spinner_values);
                mCustomKeyInfoTextView.setText(""); // ElGamal does not support custom key length
                // allowed flags:
                mFlagCertify.setChecked(false);
                mFlagCertify.setEnabled(false);
                mFlagSign.setChecked(false);
                mFlagSign.setEnabled(false);
                mFlagEncrypt.setChecked(true);
                mFlagEncrypt.setEnabled(true);
                mFlagAuthenticate.setChecked(false);
                mFlagAuthenticate.setEnabled(false);
                break;
            case PublicKeyAlgorithmTags.DSA:
                replaceArrayAdapterContent(keySizeAdapter, R.array.dsa_key_size_spinner_values);
                mCustomKeyInfoTextView.setText(getResources().getString(R.string.key_size_custom_info_dsa));
                // allowed flags:
                mFlagCertify.setChecked(false);
                mFlagCertify.setEnabled(false);
                mFlagSign.setChecked(true);
                mFlagSign.setEnabled(true);
                mFlagEncrypt.setChecked(false);
                mFlagEncrypt.setEnabled(false);
                mFlagAuthenticate.setChecked(false);
                mFlagAuthenticate.setEnabled(false);
                break;
        }
        keySizeAdapter.notifyDataSetChanged();

        // when switching algorithm, try to select same key length as before
        for (int i = 0; i < keySizeAdapter.getCount(); i++) {
            if (selectedItem.equals(keySizeAdapter.getItem(i))) {
                mKeySizeSpinner.setSelection(i);
                break;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void replaceArrayAdapterContent(ArrayAdapter<CharSequence> arrayAdapter, int stringArrayResourceId) {
        final String[] spinnerValuesStringArray = getResources().getStringArray(stringArrayResourceId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            arrayAdapter.addAll(spinnerValuesStringArray);
        } else {
            for (final String value : spinnerValuesStringArray) {
                arrayAdapter.add(value);
            }
        }
    }

}
