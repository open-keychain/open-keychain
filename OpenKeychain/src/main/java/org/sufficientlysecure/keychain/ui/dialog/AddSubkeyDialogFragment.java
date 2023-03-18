/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.bouncycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Curve;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;
import org.sufficientlysecure.keychain.util.Choice;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class AddSubkeyDialogFragment extends DialogFragment {

    public interface OnAlgorithmSelectedListener {
        void onAlgorithmSelected(SaveKeyringParcel.SubkeyAdd newSubkey);
    }

    public enum SupportedKeyType {
        RSA_2048, RSA_3072, RSA_4096, ECC_P256, ECC_P521, EDDSA
    }

    private static final String ARG_WILL_BE_MASTER_KEY = "will_be_master_key";

    private OnAlgorithmSelectedListener mAlgorithmSelectedListener;

    private CheckBox mNoExpiryCheckBox;
    private TableRow mExpiryRow;
    private DatePicker mExpiryDatePicker;
    private Spinner mKeyTypeSpinner;
    private RadioGroup mUsageRadioGroup;
    private RadioButton mUsageNone;
    private RadioButton mUsageSign;
    private RadioButton mUsageEncrypt;
    private RadioButton mUsageSignAndEncrypt;
    private RadioButton mUsageAuthentication;

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


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity context = getActivity();
        final LayoutInflater mInflater;

        mWillBeMasterKey = getArguments().getBoolean(ARG_WILL_BE_MASTER_KEY);
        mInflater = context.getLayoutInflater();

        CustomAlertDialogBuilder dialog = new CustomAlertDialogBuilder(context);

        @SuppressLint("InflateParams")
        View view = mInflater.inflate(R.layout.add_subkey_dialog, null);
        dialog.setView(view);

        mNoExpiryCheckBox = view.findViewById(R.id.add_subkey_no_expiry);
        mExpiryRow = view.findViewById(R.id.add_subkey_expiry_row);
        mExpiryDatePicker = view.findViewById(R.id.add_subkey_expiry_date_picker);
        mKeyTypeSpinner = view.findViewById(R.id.add_subkey_type);
        mUsageRadioGroup = view.findViewById(R.id.add_subkey_usage_group);
        mUsageNone = view.findViewById(R.id.add_subkey_usage_none);
        mUsageSign = view.findViewById(R.id.add_subkey_usage_sign);
        mUsageEncrypt = view.findViewById(R.id.add_subkey_usage_encrypt);
        mUsageSignAndEncrypt = view.findViewById(R.id.add_subkey_usage_sign_and_encrypt);
        mUsageAuthentication = view.findViewById(R.id.add_subkey_usage_authentication);

        if(mWillBeMasterKey) {
            dialog.setTitle(R.string.title_change_master_key);
            mUsageNone.setVisibility(View.VISIBLE);
            mUsageNone.setChecked(true);
        } else {
            dialog.setTitle(R.string.title_add_subkey);
        }

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

        // date picker works based on default time zone
        Calendar minDateCal = Calendar.getInstance(TimeZone.getDefault());
        minDateCal.add(Calendar.DAY_OF_YEAR, 1); // at least one day after creation (today)
        mExpiryDatePicker.setMinDate(minDateCal.getTime().getTime());

        {
            ArrayList<Choice<SupportedKeyType>> choices = new ArrayList<>();
            choices.add(new Choice<>(SupportedKeyType.RSA_2048, getResources().getString(
                    R.string.rsa_2048), getResources().getString(R.string.rsa_2048_description_html)));
            choices.add(new Choice<>(SupportedKeyType.RSA_3072, getResources().getString(
                    R.string.rsa_3072), getResources().getString(R.string.rsa_3072_description_html)));
            choices.add(new Choice<>(SupportedKeyType.RSA_4096, getResources().getString(
                    R.string.rsa_4096), getResources().getString(R.string.rsa_4096_description_html)));
            choices.add(new Choice<>(SupportedKeyType.ECC_P256, getResources().getString(
                    R.string.ecc_p256), getResources().getString(R.string.ecc_p256_description_html)));
            choices.add(new Choice<>(SupportedKeyType.ECC_P521, getResources().getString(
                    R.string.ecc_p521), getResources().getString(R.string.ecc_p521_description_html)));
            choices.add(new Choice<>(SupportedKeyType.EDDSA, getResources().getString(
                    R.string.ecc_eddsa), getResources().getString(R.string.ecc_eddsa_description_html)));
            TwoLineArrayAdapter adapter = new TwoLineArrayAdapter(context,
                    android.R.layout.simple_spinner_item, choices);
            mKeyTypeSpinner.setAdapter(adapter);
            // make EDDSA the default
            for (int i = 0; i < choices.size(); ++i) {
                if (choices.get(i).getId() == SupportedKeyType.EDDSA) {
                    mKeyTypeSpinner.setSelection(i);
                    break;
                }
            }
        }

        dialog.setCancelable(true);

        // onClickListener are set in onStart() to override default dismiss behaviour
        dialog.setPositiveButton(android.R.string.ok, null);
        dialog.setNegativeButton(android.R.string.cancel, null);

        final AlertDialog alertDialog = dialog.show();

        mKeyTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // noinspection unchecked
                SupportedKeyType keyType = ((Choice<SupportedKeyType>) parent.getSelectedItem()).getId();

                // RadioGroup.getCheckedRadioButtonId() gives the wrong RadioButton checked
                // when programmatically unchecking children radio buttons. Clearing all is the only option.
                mUsageRadioGroup.clearCheck();

                if(mWillBeMasterKey) {
                    mUsageNone.setChecked(true);
                }

                boolean signAndEncryptAvailable = true;
                boolean encryptAvailable = true;
                switch (keyType) {
                    case ECC_P256:
                    case ECC_P521:
                    case EDDSA:
                        signAndEncryptAvailable = false;
                        if (mWillBeMasterKey) encryptAvailable = false;
                        break;
                }
                mUsageSignAndEncrypt.setEnabled(signAndEncryptAvailable);
                mUsageEncrypt.setEnabled(encryptAvailable);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
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
                    if (mUsageRadioGroup.getCheckedRadioButtonId() == -1) {
                        Toast.makeText(getActivity(), R.string.edit_key_select_usage, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // noinspection unchecked
                    SupportedKeyType keyType = ((Choice<SupportedKeyType>) mKeyTypeSpinner.getSelectedItem()).getId();
                    Curve curve = null;
                    Integer keySize = null;
                    Algorithm algorithm = null;

                    // set keysize & curve, for RSA & ECC respectively
                    switch (keyType) {
                        case RSA_2048: {
                            keySize = 2048;
                            break;
                        }
                        case RSA_3072: {
                            keySize = 3072;
                            break;
                        }
                        case RSA_4096: {
                            keySize = 4096;
                            break;
                        }
                        case ECC_P256: {
                            curve = Curve.NIST_P256;
                            break;
                        }
                        case ECC_P521: {
                            curve = Curve.NIST_P521;
                            break;
                        }
                        case EDDSA: {
                            curve = Curve.CV25519;
                            break;
                        }
                    }

                    // set algorithm
                    switch (keyType) {
                        case RSA_2048:
                        case RSA_3072:
                        case RSA_4096: {
                            algorithm = Algorithm.RSA;
                            break;
                        }

                        case ECC_P256:
                        case ECC_P521: {
                            if(mUsageEncrypt.isChecked()) {
                                algorithm = Algorithm.ECDH;
                            } else {
                                algorithm = Algorithm.ECDSA;
                            }
                            break;
                        }
                        case EDDSA: {
                            if(mUsageEncrypt.isChecked()) {
                                algorithm = Algorithm.ECDH;
                            } else {
                                algorithm = Algorithm.EDDSA;
                            }
                        }
                    }

                    // set flags
                    int flags = 0;
                    if (mWillBeMasterKey) {
                        flags |= KeyFlags.CERTIFY_OTHER;
                    }
                    if (mUsageSign.isChecked()) {
                        flags |= KeyFlags.SIGN_DATA;
                    } else if (mUsageEncrypt.isChecked()) {
                        flags |= KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE;
                    } else if (mUsageSignAndEncrypt.isChecked()) {
                        flags |= KeyFlags.SIGN_DATA | KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE;
                    } else if (mUsageAuthentication.isChecked()) {
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

                    SaveKeyringParcel.SubkeyAdd newSubkey = SubkeyAdd.createSubkeyAdd(
                            algorithm, keySize, curve, flags, expiry
                    );
                    mAlgorithmSelectedListener.onAlgorithmSelected(newSubkey);

                    // finally, dismiss the dialogue
                    dismiss();
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

    private class TwoLineArrayAdapter extends ArrayAdapter<Choice<SupportedKeyType>> {
        public TwoLineArrayAdapter(Context context, int resource, List<Choice<SupportedKeyType>> objects) {
            super(context, resource, objects);
        }


        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            // inflate view if not given one
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.two_line_spinner_dropdown_item, parent, false);
            }

            Choice c = this.getItem(position);

            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            text1.setText(c.getName());
            text2.setText(Html.fromHtml(c.getDescription()));

            return convertView;
        }
    }

}
