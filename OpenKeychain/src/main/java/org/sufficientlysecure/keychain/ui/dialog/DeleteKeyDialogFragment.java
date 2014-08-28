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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.util.Log;

import java.util.HashMap;

public class DeleteKeyDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_DELETE_MASTER_KEY_IDS = "delete_master_key_ids";

    public static final int MESSAGE_OKAY = 1;
    public static final int MESSAGE_ERROR = 0;

    private TextView mMainMessage;
    private View mInflateView;

    /**
     * Creates new instance of this delete file dialog fragment
     */
    public static DeleteKeyDialogFragment newInstance(Messenger messenger, long[] masterKeyIds) {
        DeleteKeyDialogFragment frag = new DeleteKeyDialogFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_MESSENGER, messenger);
        args.putLongArray(ARG_DELETE_MASTER_KEY_IDS, masterKeyIds);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        final Messenger messenger = getArguments().getParcelable(ARG_MESSENGER);

        final long[] masterKeyIds = getArguments().getLongArray(ARG_DELETE_MASTER_KEY_IDS);

        CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(activity);

        // Setup custom View to display in AlertDialog
        LayoutInflater inflater = activity.getLayoutInflater();
        mInflateView = inflater.inflate(R.layout.view_key_delete_fragment, null);
        builder.setView(mInflateView);

        mMainMessage = (TextView) mInflateView.findViewById(R.id.mainMessage);

        builder.setTitle(R.string.warning);

        final boolean hasSecret;

        // If only a single key has been selected
        if (masterKeyIds.length == 1) {
            long masterKeyId = masterKeyIds[0];

            try {
                HashMap<String, Object> data = new ProviderHelper(activity).getUnifiedData(
                        masterKeyId, new String[]{
                                KeyRings.USER_ID,
                                KeyRings.HAS_ANY_SECRET
                        }, new int[]{
                                ProviderHelper.FIELD_TYPE_STRING,
                                ProviderHelper.FIELD_TYPE_INTEGER
                        }
                );
                String userId = (String) data.get(KeyRings.USER_ID);
                hasSecret = ((Long) data.get(KeyRings.HAS_ANY_SECRET)) == 1;

                // Set message depending on which key it is.
                mMainMessage.setText(getString(
                        hasSecret ? R.string.secret_key_deletion_confirmation
                                : R.string.public_key_deletetion_confirmation,
                        userId
                ));
            } catch (ProviderHelper.NotFoundException e) {
                dismiss();
                return null;
            }
        } else {
            mMainMessage.setText(R.string.key_deletion_confirmation_multi);
            hasSecret = false;
        }

        builder.setIcon(R.drawable.ic_dialog_alert_holo_light);
        builder.setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // Send all information needed to service to import key in other thread
                Intent intent = new Intent(getActivity(), KeychainIntentService.class);

                intent.setAction(KeychainIntentService.ACTION_DELETE);

                // Message is received after importing is done in KeychainIntentService
                KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(
                        getActivity(),
                        getString(R.string.progress_deleting),
                        ProgressDialog.STYLE_HORIZONTAL) {
                    public void handleMessage(Message message) {
                        // handle messages by standard KeychainIntentServiceHandler first
                        super.handleMessage(message);
                        try {
                            Message msg = Message.obtain();
                            msg.copyFrom(message);
                            messenger.send(msg);
                        } catch (RemoteException e) {
                            Log.e(Constants.TAG, "messenger error", e);
                        }
                    }
                };

                // fill values for this action
                Bundle data = new Bundle();
                data.putLongArray(KeychainIntentService.DELETE_KEY_LIST, masterKeyIds);
                data.putBoolean(KeychainIntentService.DELETE_IS_SECRET, hasSecret);
                intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

                // Create a new Messenger for the communication back
                Messenger messenger = new Messenger(saveHandler);
                intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

                // show progress dialog
                saveHandler.showProgressDialog(getActivity());

                // start service with intent
                getActivity().startService(intent);

                dismiss();
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

}
