/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class DeleteKeyDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_DELETE_MASTER_KEY_IDS = "delete_master_key_ids";

    public static final int MESSAGE_OKAY = 1;
    public static final int MESSAGE_ERROR = 0;

    private boolean mIsSingleSelection = false;

    private TextView mMainMessage;
    private CheckBox mCheckDeleteSecret;
    private LinearLayout mDeleteSecretKeyView;
    private View mInflateView;

    private Messenger mMessenger;

    /**
     * Creates new instance of this delete file dialog fragment
     */
    public static DeleteKeyDialogFragment newInstance(Messenger messenger,
                                                      long[] masterKeyIds) {
        DeleteKeyDialogFragment frag = new DeleteKeyDialogFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_MESSENGER, messenger);
        args.putLongArray(ARG_DELETE_MASTER_KEY_IDS, masterKeyIds);
        //We don't need the key type

        frag.setArguments(args);

        return frag;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final FragmentActivity activity = getActivity();
        mMessenger = getArguments().getParcelable(ARG_MESSENGER);

        final long[] masterKeyIds = getArguments().getLongArray(ARG_DELETE_MASTER_KEY_IDS);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        //Setup custom View to display in AlertDialog
        LayoutInflater inflater = activity.getLayoutInflater();
        mInflateView = inflater.inflate(R.layout.view_key_delete_fragment, null);
        builder.setView(mInflateView);

        mDeleteSecretKeyView = (LinearLayout) mInflateView.findViewById(R.id.deleteSecretKeyView);
        mMainMessage = (TextView) mInflateView.findViewById(R.id.mainMessage);
        mCheckDeleteSecret = (CheckBox) mInflateView.findViewById(R.id.checkDeleteSecret);

        builder.setTitle(R.string.warning);

        // If only a single key has been selected
        if (masterKeyIds.length == 1) {
            mIsSingleSelection = true;

            long masterKeyId = masterKeyIds[0];

            HashMap<String, Object> data = ProviderHelper.getUnifiedData(activity, masterKeyId, new String[]{
                    KeychainContract.UserIds.USER_ID,
                    KeychainDatabase.Tables.KEY_RINGS_SECRET + "." + KeychainContract.KeyRings.MASTER_KEY_ID
            });
            String userId = (String) data.get(KeychainContract.UserIds.USER_ID);
            boolean hasSecret = data.get(
                    KeychainDatabase.Tables.KEY_RINGS_SECRET + "." + KeychainContract.KeyRings.MASTER_KEY_ID
            ) instanceof Long;

            // Hide the Checkbox and TextView since this is a single selection,user will be notified through message
            mDeleteSecretKeyView.setVisibility(View.GONE);
            // Set message depending on which key it is.
            mMainMessage.setText(getString(
                    hasSecret ? R.string.secret_key_deletion_confirmation
                              : R.string.public_key_deletetion_confirmation,
                    userId));
        } else {
            mDeleteSecretKeyView.setVisibility(View.VISIBLE);
            mMainMessage.setText(R.string.key_deletion_confirmation_multi);
        }


        builder.setIcon(R.drawable.ic_dialog_alert_holo_light);
        builder.setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                boolean success = false;
                for(long masterKeyId : masterKeyIds) {
                    int count = activity.getContentResolver().delete(
                            KeychainContract.KeyRings.buildPublicKeyRingUri(Long.toString(masterKeyId)), null, null
                        );
                    if(count > 0)
                    success = true;
                }

                dismiss();

                if (success) {
                    sendMessageToHandler(MESSAGE_OKAY, null);
                } else {
                    sendMessageToHandler(MESSAGE_ERROR, null);
                }
            }

        }
        );
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        return builder.create();
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
