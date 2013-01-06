/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.ui.dialog;

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import org.thialfihar.android.apg.util.Log;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

public class SetPassphraseDialogFragment extends DialogFragment implements OnEditorActionListener {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_TITLE = "title";

    public static final int MESSAGE_OKAY = 1;

    public static final String MESSAGE_NEW_PASSPHRASE = "new_passphrase";

    private Messenger mMessenger;
    private EditText mPassphraseEditText;
    private EditText mPassphraseAgainEditText;

    /**
     * Creates new instance of this dialog fragment
     * 
     * @param title
     *            title of dialog
     * @param messenger
     *            to communicate back after setting the passphrase
     * @return
     */
    public static SetPassphraseDialogFragment newInstance(Messenger messenger, int title) {
        SetPassphraseDialogFragment frag = new SetPassphraseDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TITLE, title);
        args.putParcelable(ARG_MESSENGER, messenger);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        int title = getArguments().getInt(ARG_TITLE);
        mMessenger = getArguments().getParcelable(ARG_MESSENGER);

        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setTitle(title);
        alert.setMessage(R.string.enterPassPhraseTwice);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.passphrase_repeat, null);
        alert.setView(view);

        mPassphraseEditText = (EditText) view.findViewById(R.id.passphrase_passphrase);
        mPassphraseAgainEditText = (EditText) view.findViewById(R.id.passphrase_passphrase_again);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                String passPhrase1 = mPassphraseEditText.getText().toString();
                String passPhrase2 = mPassphraseAgainEditText.getText().toString();
                if (!passPhrase1.equals(passPhrase2)) {
                    Toast.makeText(
                            activity,
                            getString(R.string.errorMessage,
                                    getString(R.string.passPhrasesDoNotMatch)), Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                if (passPhrase1.equals("")) {
                    Toast.makeText(
                            activity,
                            getString(R.string.errorMessage,
                                    getString(R.string.passPhraseMustNotBeEmpty)),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // return resulting data back to activity
                Bundle data = new Bundle();
                data.putString(MESSAGE_NEW_PASSPHRASE, passPhrase1);

                sendMessageToHandler(MESSAGE_OKAY, data);
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        return alert.create();
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);

        // request focus and open soft keyboard
        mPassphraseEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        mPassphraseAgainEditText.setOnEditorActionListener(this);
    }

    /**
     * Associate the "done" button on the soft keyboard with the okay button in the view
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            AlertDialog dialog = ((AlertDialog) getDialog());
            Button bt = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            bt.performClick();
            return true;
        }
        return false;
    }

    /**
     * Send message back to handler which is initialized in a activity
     * 
     * @param what
     *            Message integer you want to send
     */
    private void sendMessageToHandler(Integer what, Bundle data) {
        Message msg = Message.obtain();
        msg.what = what;
        if (data != null) {
            msg.setData(data);
        }

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w(Constants.TAG, "Messenger is null!", e);
        }
    }
}