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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Choice;

import java.util.ArrayList;

public class CreateKeyDialogFragment extends DialogFragment {

    public interface OnAlgorithmSelectedListener {
        public void onAlgorithmSelected(Choice algorithmChoice, int keySize);
    }

    private static final String ARG_EDITOR_CHILD_COUNT = "child_count";

    private int mNewKeySize;
    private Choice mNewKeyAlgorithmChoice;
    private OnAlgorithmSelectedListener mAlgorithmSelectedListener;
    private Spinner mAlgorithmSpinner;
    private Spinner mKeySizeSpinner;
    private TextView mCustomKeyTextView;
    private EditText mCustomKeyEditText;

    public void setOnAlgorithmSelectedListener(OnAlgorithmSelectedListener listener) {
        mAlgorithmSelectedListener = listener;
    }

    public static CreateKeyDialogFragment newInstance(int mEditorChildCount) {
        CreateKeyDialogFragment frag = new CreateKeyDialogFragment();
        Bundle args = new Bundle();

        args.putInt(ARG_EDITOR_CHILD_COUNT, mEditorChildCount);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity context = getActivity();
        final LayoutInflater mInflater;

        final int childCount = getArguments().getInt(ARG_EDITOR_CHILD_COUNT);
        mInflater = context.getLayoutInflater();

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);

        View view = mInflater.inflate(R.layout.create_key_dialog, null);
        dialog.setView(view);
        dialog.setTitle(R.string.title_create_key);

        boolean wouldBeMasterKey = (childCount == 0);

        mAlgorithmSpinner = (Spinner) view.findViewById(R.id.create_key_algorithm);
        ArrayList<Choice> choices = new ArrayList<Choice>();
        choices.add(new Choice(Id.choice.algorithm.dsa, getResources().getString(
                R.string.dsa)));
        if (!wouldBeMasterKey) {
            choices.add(new Choice(Id.choice.algorithm.elgamal, getResources().getString(
                    R.string.elgamal)));
        }

        choices.add(new Choice(Id.choice.algorithm.rsa, getResources().getString(
                R.string.rsa)));

        ArrayAdapter<Choice> adapter = new ArrayAdapter<Choice>(context,
                android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAlgorithmSpinner.setAdapter(adapter);
        // make RSA the default
        for (int i = 0; i < choices.size(); ++i) {
            if (choices.get(i).getId() == Id.choice.algorithm.rsa) {
                mAlgorithmSpinner.setSelection(i);
                break;
            }
        }

        mKeySizeSpinner = (Spinner) view.findViewById(R.id.create_key_size);
        ArrayAdapter<CharSequence> keySizeAdapter = ArrayAdapter.createFromResource(
                context, R.array.key_size_spinner_values,
                android.R.layout.simple_spinner_item);
        keySizeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mKeySizeSpinner.setAdapter(keySizeAdapter);
        mKeySizeSpinner.setSelection(3); // Default to 4096 for the key length

        mCustomKeyTextView = (TextView) view.findViewById(R.id.custom_key_size_label);
        mCustomKeyEditText = (EditText) view.findViewById(R.id.custom_key_size_input);

        final AdapterView.OnItemSelectedListener customKeySelectedLisener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final String selectedItemString = (String) parent.getSelectedItem();
                final String customLengthString = getResources().getString(R.string.key_size_custom);
                final boolean customSelected = customLengthString.equals(selectedItemString);
                final int visibility = customSelected ? View.VISIBLE : View.GONE;
                mCustomKeyEditText.setVisibility(visibility);
                mCustomKeyTextView.setVisibility(visibility);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        dialog.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        di.dismiss();
                        mNewKeyAlgorithmChoice = (Choice) mAlgorithmSpinner.getSelectedItem();
                        mNewKeySize = getProperKeyLength(mNewKeyAlgorithmChoice.getId(), getSelectedKeyLength());
                        mAlgorithmSelectedListener.onAlgorithmSelected(mNewKeyAlgorithmChoice, mNewKeySize);
                    }
                }
        );

        dialog.setCancelable(true);
        dialog.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        di.dismiss();
                    }
                });

        final AlertDialog alertDialog = dialog.create();

        final AdapterView.OnItemSelectedListener weakRsaListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mKeySizeSpinner == parent) {
                    customKeySelectedLisener.onItemSelected(parent, view, position, id);
                }
                setOkButtonAvailability(alertDialog);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

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

        mKeySizeSpinner.setOnItemSelectedListener(weakRsaListener);
        mAlgorithmSpinner.setOnItemSelectedListener(weakRsaListener);

        return alertDialog;
    }

    private int getSelectedKeyLength() {
        final String selectedItemString = (String) mKeySizeSpinner.getSelectedItem();
        final String customLengthString = getResources().getString(R.string.key_size_custom);
        final boolean customSelected = customLengthString.equals(selectedItemString);
        String keyLengthString = customSelected ? mCustomKeyEditText.getText().toString() : selectedItemString;
        int keySize = 0;
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
            case Id.choice.algorithm.rsa:
                if (currentKeyLength > 1024 && currentKeyLength <= 8192) {
                    properKeyLength = currentKeyLength + ((8 - (currentKeyLength % 8)) % 8);
                }
                break;
            case Id.choice.algorithm.elgamal:
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
            case Id.choice.algorithm.dsa:
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

}
