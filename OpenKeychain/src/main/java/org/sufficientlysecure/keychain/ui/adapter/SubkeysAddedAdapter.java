/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui.adapter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ContactHelper;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.dialog.CreateKeyDialogFragment;
import org.sufficientlysecure.keychain.util.Choice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

public class SubkeysAddedAdapter extends ArrayAdapter<SaveKeyringParcel.SubkeyAdd> {
    private LayoutInflater mInflater;
    private Activity mActivity;

    public interface OnAlgorithmSelectedListener {
        public void onAlgorithmSelected(Choice algorithmChoice, int keySize);
    }

    // hold a private reference to the underlying data List
    private List<SaveKeyringParcel.SubkeyAdd> mData;

    public SubkeysAddedAdapter(Activity activity, List<SaveKeyringParcel.SubkeyAdd> data) {
        super(activity, -1, data);
        mActivity = activity;
        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mData = data;
    }

    static class ViewHolder {
        public OnAlgorithmSelectedListener mAlgorithmSelectedListener;
        public Spinner mAlgorithmSpinner;
        public Spinner mKeySizeSpinner;
        public TextView mCustomKeyTextView;
        public EditText mCustomKeyEditText;
        public TextView mCustomKeyInfoTextView;
        public ImageButton vDelete;
        // also hold a reference to the model item
        public SaveKeyringParcel.SubkeyAdd mModel;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            // Not recycled, inflate a new view
            convertView = mInflater.inflate(R.layout.edit_key_subkey_added_item, null);
            final ViewHolder holder = new ViewHolder();
            holder.mAlgorithmSpinner = (Spinner) convertView.findViewById(R.id.create_key_algorithm);
            holder.mKeySizeSpinner = (Spinner) convertView.findViewById(R.id.create_key_size);
            holder.mCustomKeyTextView = (TextView) convertView.findViewById(R.id.custom_key_size_label);
            holder.mCustomKeyEditText = (EditText) convertView.findViewById(R.id.custom_key_size_input);
            holder.mCustomKeyInfoTextView = (TextView) convertView.findViewById(R.id.custom_key_size_info);
            holder.vDelete = (ImageButton) convertView.findViewById(R.id.subkey_added_item_delete);
            convertView.setTag(holder);

            holder.mAlgorithmSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Choice newKeyAlgorithmChoice = (Choice) holder.mAlgorithmSpinner.getSelectedItem();
                    // update referenced model item
                    holder.mModel.mAlgorithm = newKeyAlgorithmChoice.getId();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            holder.mKeySizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Choice newKeyAlgorithmChoice = (Choice) holder.mAlgorithmSpinner.getSelectedItem();
                    int newKeySize = getProperKeyLength(newKeyAlgorithmChoice.getId(),
                            getSelectedKeyLength(holder.mKeySizeSpinner, holder.mCustomKeyEditText));
                    // update referenced model item
                    holder.mModel.mKeysize = newKeySize;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            holder.vDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // remove reference model item from adapter (data and notify about change)
                    SubkeysAddedAdapter.this.remove(holder.mModel);
                }
            });

        }
        final ViewHolder holder = (ViewHolder) convertView.getTag();

        // save reference to model item
        holder.mModel = getItem(position);

        // TODO
        boolean wouldBeMasterKey = false;
//        boolean wouldBeMasterKey = (childCount == 0);

        ArrayList<Choice> choices = new ArrayList<Choice>();
        choices.add(new Choice(Constants.choice.algorithm.dsa, mActivity.getResources().getString(
                R.string.dsa)));
        if (!wouldBeMasterKey) {
            choices.add(new Choice(Constants.choice.algorithm.elgamal, mActivity.getResources().getString(
                    R.string.elgamal)));
        }

        choices.add(new Choice(Constants.choice.algorithm.rsa, mActivity.getResources().getString(
                R.string.rsa)));

        ArrayAdapter<Choice> adapter = new ArrayAdapter<Choice>(mActivity,
                android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.mAlgorithmSpinner.setAdapter(adapter);
        // make RSA the default
        for (int i = 0; i < choices.size(); ++i) {
            if (choices.get(i).getId() == Constants.choice.algorithm.rsa) {
                holder.mAlgorithmSpinner.setSelection(i);
                break;
            }
        }

        // dynamic ArrayAdapter must be created (instead of ArrayAdapter.getFromResource), because it's content may change
        ArrayAdapter<CharSequence> keySizeAdapter = new ArrayAdapter<CharSequence>(mActivity, android.R.layout.simple_spinner_item,
                new ArrayList<CharSequence>(Arrays.asList(mActivity.getResources().getStringArray(R.array.rsa_key_size_spinner_values))));
        keySizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.mKeySizeSpinner.setAdapter(keySizeAdapter);
        holder.mKeySizeSpinner.setSelection(1); // Default to 4096 for the key length

        holder.mCustomKeyEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
//                setOkButtonAvailability(alertDialog);
            }
        });

        holder.mKeySizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setCustomKeyVisibility(holder.mKeySizeSpinner, holder.mCustomKeyEditText,
                        holder.mCustomKeyTextView, holder.mCustomKeyInfoTextView);
//                setOkButtonAvailability(alertDialog);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        holder.mAlgorithmSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setKeyLengthSpinnerValuesForAlgorithm(((Choice) parent.getSelectedItem()).getId(),
                        holder.mKeySizeSpinner, holder.mCustomKeyInfoTextView);

                setCustomKeyVisibility(holder.mKeySizeSpinner, holder.mCustomKeyEditText,
                        holder.mCustomKeyTextView, holder.mCustomKeyInfoTextView);
//                setOkButtonAvailability(alertDialog);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
//
//        holder.vAddress.setText(holder.mModel.address);
//        holder.vAddress.setThreshold(1); // Start working from first character
//        holder.vAddress.setAdapter(mAutoCompleteEmailAdapter);
//
//        holder.vName.setText(holder.mModel.name);
//        holder.vName.setThreshold(1); // Start working from first character
//        holder.vName.setAdapter(mAutoCompleteNameAdapter);
//
//        holder.vComment.setText(holder.mModel.comment);

        return convertView;
    }


    private int getSelectedKeyLength(Spinner keySizeSpinner, EditText customKeyEditText) {
        final String selectedItemString = (String) keySizeSpinner.getSelectedItem();
        final String customLengthString = mActivity.getResources().getString(R.string.key_size_custom);
        final boolean customSelected = customLengthString.equals(selectedItemString);
        String keyLengthString = customSelected ? customKeyEditText.getText().toString() : selectedItemString;
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
            case Constants.choice.algorithm.rsa:
                if (currentKeyLength > 1024 && currentKeyLength <= 8192) {
                    properKeyLength = currentKeyLength + ((8 - (currentKeyLength % 8)) % 8);
                }
                break;
            case Constants.choice.algorithm.elgamal:
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
            case Constants.choice.algorithm.dsa:
                if (currentKeyLength >= 512 && currentKeyLength <= 1024) {
                    properKeyLength = currentKeyLength + ((64 - (currentKeyLength % 64)) % 64);
                }
                break;
        }
        return properKeyLength;
    }

    // TODO: make this an error message on the field
//    private boolean setOkButtonAvailability(AlertDialog alertDialog) {
//        final Choice selectedAlgorithm = (Choice) mAlgorithmSpinner.getSelectedItem();
//        final int selectedKeySize = getSelectedKeyLength(); //Integer.parseInt((String) mKeySizeSpinner.getSelectedItem());
//        final int properKeyLength = getProperKeyLength(selectedAlgorithm.getId(), selectedKeySize);
//        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(properKeyLength > 0);
//    }

    private void setCustomKeyVisibility(Spinner keySizeSpinner, EditText customkeyedittext, TextView customKeyTextView, TextView customKeyInfoTextView) {
        final String selectedItemString = (String) keySizeSpinner.getSelectedItem();
        final String customLengthString = mActivity.getResources().getString(R.string.key_size_custom);
        final boolean customSelected = customLengthString.equals(selectedItemString);
        final int visibility = customSelected ? View.VISIBLE : View.GONE;

        customkeyedittext.setVisibility(visibility);
        customKeyTextView.setVisibility(visibility);
        customKeyInfoTextView.setVisibility(visibility);

        // hide keyboard after setting visibility to gone
        if (visibility == View.GONE) {
            InputMethodManager imm = (InputMethodManager)
                    mActivity.getSystemService(mActivity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(customkeyedittext.getWindowToken(), 0);
        }
    }

    private void setKeyLengthSpinnerValuesForAlgorithm(int algorithmId, Spinner keySizeSpinner, TextView customKeyInfoTextView) {
        final ArrayAdapter<CharSequence> keySizeAdapter = (ArrayAdapter<CharSequence>) keySizeSpinner.getAdapter();
        final Object selectedItem = keySizeSpinner.getSelectedItem();
        keySizeAdapter.clear();
        switch (algorithmId) {
            case Constants.choice.algorithm.rsa:
                replaceArrayAdapterContent(keySizeAdapter, R.array.rsa_key_size_spinner_values);
                customKeyInfoTextView.setText(mActivity.getResources().getString(R.string.key_size_custom_info_rsa));
                break;
            case Constants.choice.algorithm.elgamal:
                replaceArrayAdapterContent(keySizeAdapter, R.array.elgamal_key_size_spinner_values);
                customKeyInfoTextView.setText(""); // ElGamal does not support custom key length
                break;
            case Constants.choice.algorithm.dsa:
                replaceArrayAdapterContent(keySizeAdapter, R.array.dsa_key_size_spinner_values);
                customKeyInfoTextView.setText(mActivity.getResources().getString(R.string.key_size_custom_info_dsa));
                break;
        }
        keySizeAdapter.notifyDataSetChanged();

        // when switching algorithm, try to select same key length as before
        for (int i = 0; i < keySizeAdapter.getCount(); i++) {
            if (selectedItem.equals(keySizeAdapter.getItem(i))) {
                keySizeSpinner.setSelection(i);
                break;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void replaceArrayAdapterContent(ArrayAdapter<CharSequence> arrayAdapter, int stringArrayResourceId) {
        final String[] spinnerValuesStringArray = mActivity.getResources().getStringArray(stringArrayResourceId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            arrayAdapter.addAll(spinnerValuesStringArray);
        } else {
            for (final String value : spinnerValuesStringArray) {
                arrayAdapter.add(value);
            }
        }
    }

}
