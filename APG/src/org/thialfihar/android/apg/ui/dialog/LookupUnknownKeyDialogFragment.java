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

import org.thialfihar.android.apg.helper.PGPHelper;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;

import org.thialfihar.android.apg.ui.KeyServerQueryActivity;
import org.thialfihar.android.apg.util.Log;

public class LookupUnknownKeyDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_UNKNOWN_KEY_ID = "unknown_key_id";

    public static final int MESSAGE_OKAY = 1;
    public static final int MESSAGE_CANCEL = 2;

    private Messenger mMessenger;

    /**
     * Creates new instance of this dialog fragment
     * 
     * @param messenger
     * @param unknownKeyId
     * @return
     */
    public static LookupUnknownKeyDialogFragment newInstance(Messenger messenger, long unknownKeyId) {
        LookupUnknownKeyDialogFragment frag = new LookupUnknownKeyDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_UNKNOWN_KEY_ID, unknownKeyId);
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

        final long unknownKeyId = getArguments().getLong(ARG_UNKNOWN_KEY_ID);
        mMessenger = getArguments().getParcelable(ARG_MESSENGER);

        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setIcon(android.R.drawable.ic_dialog_alert);
        alert.setTitle(R.string.title_unknownSignatureKey);
        alert.setMessage(getString(R.string.lookupUnknownKey,
                PGPHelper.getSmallFingerPrint(unknownKeyId)));

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                sendMessageToHandler(MESSAGE_OKAY);

                Intent intent = new Intent(activity, KeyServerQueryActivity.class);
                intent.setAction(KeyServerQueryActivity.ACTION_LOOK_UP_KEY_ID);
                intent.putExtra(KeyServerQueryActivity.EXTRA_KEY_ID, unknownKeyId);
                startActivityForResult(intent, Id.request.look_up_key_id);
            }
        });
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();

                sendMessageToHandler(MESSAGE_CANCEL);
            }
        });
        alert.setCancelable(true);
        alert.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                sendMessageToHandler(MESSAGE_CANCEL);
            }
        });

        return alert.create();
    }

    /**
     * Send message back to handler which is initialized in a activity
     * 
     * @param what
     *            Message integer you want to send
     */
    private void sendMessageToHandler(Integer what) {
        Message msg = Message.obtain();
        msg.what = what;

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w(Constants.TAG, "Messenger is null!", e);
        }
    }
}