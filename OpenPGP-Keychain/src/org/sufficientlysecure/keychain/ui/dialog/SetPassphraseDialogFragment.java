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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;

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
        alert.setMessage(R.string.enter_passphrase_twice);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.passphrase_repeat_dialogf, null);
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
                            getString(R.string.error_message,
                                    getString(R.string.passphrases_do_not_match)), Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                if (passPhrase1.equals("")) {
                    Toast.makeText(
                            activity,
                            getString(R.string.error_message,
                                    getString(R.string.passphrase_must_not_be_empty)),
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