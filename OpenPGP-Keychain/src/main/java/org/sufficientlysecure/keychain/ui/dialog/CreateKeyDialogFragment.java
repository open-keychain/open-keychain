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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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

        final Spinner algorithm = (Spinner) view.findViewById(R.id.create_key_algorithm);
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
        algorithm.setAdapter(adapter);
        // make RSA the default
        for (int i = 0; i < choices.size(); ++i) {
            if (choices.get(i).getId() == Id.choice.algorithm.rsa) {
                algorithm.setSelection(i);
                break;
            }
        }

        final Spinner keySize = (Spinner) view.findViewById(R.id.create_key_size);
        ArrayAdapter<CharSequence> keySizeAdapter = ArrayAdapter.createFromResource(
                context, R.array.key_size_spinner_values,
                android.R.layout.simple_spinner_item);
        keySizeAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keySize.setAdapter(keySizeAdapter);
        keySize.setSelection(3); // Default to 4096 for the key length
        dialog.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        di.dismiss();
                        try {
                            int nKeyIndex = keySize.getSelectedItemPosition();
                            switch (nKeyIndex) {
                                case 0:
                                    mNewKeySize = 512;
                                    break;
                                case 1:
                                    mNewKeySize = 1024;
                                    break;
                                case 2:
                                    mNewKeySize = 2048;
                                    break;
                                case 3:
                                    mNewKeySize = 4096;
                                    break;
                            }
                        } catch (NumberFormatException e) {
                            mNewKeySize = 0;
                        }

                        mNewKeyAlgorithmChoice = (Choice) algorithm.getSelectedItem();
                        mAlgorithmSelectedListener.onAlgorithmSelected(mNewKeyAlgorithmChoice, mNewKeySize);
                    }
                });

        dialog.setCancelable(true);
        dialog.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int id) {
                        di.dismiss();
                    }
                });

        return dialog.create();
    }

}
