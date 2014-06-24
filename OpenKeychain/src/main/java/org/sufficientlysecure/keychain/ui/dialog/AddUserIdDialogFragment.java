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

package org.sufficientlysecure.keychain.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

public class AddUserIdDialogFragment extends DialogFragment implements EditText.OnEditorActionListener {
    private static final String ARG_MESSENGER = "messenger";

    public static final int MESSAGE_OKAY = 1;

    public static final String MESSAGE_DATA_USER_ID = "user_id";

    private Messenger mMessenger;

    EditText mName;
    EditText mAddress;
    EditText mComment;

    /**
     * Creates new instance of this dialog fragment
     */
    public static AddUserIdDialogFragment newInstance(Messenger messenger) {
        AddUserIdDialogFragment frag = new AddUserIdDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSENGER, messenger);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mMessenger = getArguments().getParcelable(ARG_MESSENGER);

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.add_user_id_dialog, null);
        alert.setView(view);
        alert.setTitle("Add Identity");

        mName = (EditText) view.findViewById(R.id.name);
        mAddress = (EditText) view.findViewById(R.id.address);
        mComment = (EditText) view.findViewById(R.id.comment);

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                done();
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });


        return alert.show();
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);
        // Show soft keyboard automatically
        mName.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mComment.setOnEditorActionListener(this);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            done();
            return true;
        }
        return false;
    }

    private void done() {
        String name = mName.getText().toString();
        String email = mAddress.getText().toString();
        String comment = mComment.getText().toString();

        String userId = null;
        if (!TextUtils.isEmpty(name)) {
            userId = name;
            if (!TextUtils.isEmpty(comment)) {
                userId += " (" + comment + ")";
            }
            if (!TextUtils.isEmpty(email)) {
                userId += " <" + email + ">";
            }
        }
        Bundle data = new Bundle();
        data.putString(MESSAGE_DATA_USER_ID, userId);
        sendMessageToHandler(MESSAGE_OKAY, data);

        this.dismiss();
    }

    /**
     * Send message back to handler which is initialized in a activity
     *
     * @param what Message integer you want to send
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
