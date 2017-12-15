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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

public class EditSubkeyDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";

    public static final int MESSAGE_CHANGE_EXPIRY = 1;
    public static final int MESSAGE_REVOKE = 2;
    public static final int MESSAGE_STRIP = 3;
    public static final int SUBKEY_MENU_CHANGE_EXPIRY = 0;
    public static final int SUBKEY_MENU_REVOKE_SUBKEY = 1;
    public static final int SUBKEY_MENU_STRIP_SUBKEY = 2;

    private Messenger mMessenger;

    /**
     * Creates new instance of this dialog fragment
     */
    public static EditSubkeyDialogFragment newInstance(Messenger messenger) {
        EditSubkeyDialogFragment frag = new EditSubkeyDialogFragment();
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

        CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(getActivity());
        CharSequence[] array = getResources().getStringArray(R.array.edit_key_edit_subkey);

        builder.setTitle(R.string.edit_key_edit_subkey_title);
        builder.setItems(array, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case SUBKEY_MENU_CHANGE_EXPIRY:
                        sendMessageToHandler(MESSAGE_CHANGE_EXPIRY, null);
                        break;
                    case SUBKEY_MENU_REVOKE_SUBKEY:
                        sendMessageToHandler(MESSAGE_REVOKE, null);
                        break;
                    case SUBKEY_MENU_STRIP_SUBKEY:
                        showAlertDialog();
                        break;
                    default:
                        break;
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        return builder.show();
    }

    private void showAlertDialog() {
        CustomAlertDialogBuilder stripAlertDialog = new CustomAlertDialogBuilder(getActivity());
        stripAlertDialog.setTitle(R.string.title_alert_strip).
                setMessage(R.string.alert_strip).setCancelable(true);
        stripAlertDialog.setPositiveButton(R.string.strip, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                sendMessageToHandler(MESSAGE_STRIP, null);
            }
        });
        stripAlertDialog.setNegativeButton(R.string.btn_do_not_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });
        stripAlertDialog.show();
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
