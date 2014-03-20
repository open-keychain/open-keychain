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

public class DeleteKeyDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_DELETE_KEY_RING_ROW_IDS = "delete_file";

    public static final int MESSAGE_OKAY = 1;
    public static final int MESSAGE_ERROR = 0;

    private boolean isSingleSelection = false;

    private TextView mainMessage;
    private CheckBox checkDeleteSecret;
    private LinearLayout deleteSecretKeyView;
    private View inflateView;

    private Messenger mMessenger;

    /**
     * Creates new instance of this delete file dialog fragment
     */
    public static DeleteKeyDialogFragment newInstance(Messenger messenger, long[] keyRingRowIds
    ) {
        DeleteKeyDialogFragment frag = new DeleteKeyDialogFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_MESSENGER, messenger);
        args.putLongArray(ARG_DELETE_KEY_RING_ROW_IDS, keyRingRowIds);
        //We don't need the key type

        frag.setArguments(args);

        return frag;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final FragmentActivity activity = getActivity();
        mMessenger = getArguments().getParcelable(ARG_MESSENGER);

        final long[] keyRingRowIds = getArguments().getLongArray(ARG_DELETE_KEY_RING_ROW_IDS);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        //Setup custom View to display in AlertDialog
        LayoutInflater inflater = activity.getLayoutInflater();
        inflateView = inflater.inflate(R.layout.view_key_delete_fragment, null);
        builder.setView(inflateView);

        deleteSecretKeyView = (LinearLayout) inflateView.findViewById(R.id.deleteSecretKeyView);
        mainMessage = (TextView) inflateView.findViewById(R.id.mainMessage);
        checkDeleteSecret = (CheckBox) inflateView.findViewById(R.id.checkDeleteSecret);

        builder.setTitle(R.string.warning);

        //If only a single key has been selected
        if (keyRingRowIds.length == 1) {
            Uri dataUri;
            ArrayList<Long> publicKeyRings; //Any one will do
            isSingleSelection = true;

            long selectedRow = keyRingRowIds[0];
            long keyType;
            publicKeyRings = ProviderHelper.getPublicKeyRingsRowIds(activity);

            if (publicKeyRings.contains(selectedRow)) {
                //TODO Should be a better method to do this other than getting all the KeyRings
                dataUri = KeychainContract.KeyRings.buildPublicKeyRingsUri(String.valueOf(selectedRow));
                keyType = Id.type.public_key;
            } else {
                dataUri = KeychainContract.KeyRings.buildSecretKeyRingsUri(String.valueOf(selectedRow));
                keyType = Id.type.secret_key;
            }

            String userId = ProviderHelper.getUserId(activity, dataUri);
            //Hide the Checkbox and TextView since this is a single selection,user will be notified thru message
            deleteSecretKeyView.setVisibility(View.GONE);
            //Set message depending on which key it is.
            mainMessage.setText(getString(keyType == Id.type.secret_key ? R.string.secret_key_deletion_confirmation
                    : R.string.public_key_deletetion_confirmation, userId));


        } else {
            deleteSecretKeyView.setVisibility(View.VISIBLE);
            mainMessage.setText(R.string.key_deletion_confirmation_multi);
        }


        builder.setIcon(R.drawable.ic_dialog_alert_holo_light);
        builder.setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri queryUri = KeychainContract.KeyRings.buildUnifiedKeyRingsUri();
                        String[] projection = new String[]{
                                KeychainContract.KeyRings.MASTER_KEY_ID, // 0
                                KeychainContract.KeyRings.TYPE// 1
                        };

                        // make selection with all entries where _ID is one of the given row ids
                        String selection = KeychainDatabase.Tables.KEY_RINGS + "." +
                                KeychainContract.KeyRings._ID + " IN(";
                        String selectionIDs = "";
                        for (int i = 0; i < keyRingRowIds.length; i++) {
                            selectionIDs += "'" + String.valueOf(keyRingRowIds[i]) + "'";
                            if (i + 1 < keyRingRowIds.length)
                                selectionIDs += ",";
                        }
                        selection += selectionIDs + ")";

                        Cursor cursor = activity.getContentResolver().query(queryUri, projection,
                                selection, null, null);


                        long masterKeyId;
                        long keyType;
                        boolean isSuccessfullyDeleted;
                        try {
                            isSuccessfullyDeleted = false;
                            while (cursor != null && cursor.moveToNext()) {
                                masterKeyId = cursor.getLong(0);
                                keyType = cursor.getLong(1);

                                Log.d(Constants.TAG, "masterKeyId: " + masterKeyId
                                        + ", keyType:" + (keyType == KeychainContract.KeyTypes.PUBLIC ? "Public" : "Private"));


                                if (keyType == KeychainContract.KeyTypes.SECRET) {
                                    if (checkDeleteSecret.isChecked() || isSingleSelection) {
                                        ProviderHelper.deleteUnifiedKeyRing(activity, String.valueOf(masterKeyId), true);
                                    }
                                } else {
                                    ProviderHelper.deleteUnifiedKeyRing(activity, String.valueOf(masterKeyId), false);
                                }
                            }

                            //Check if the selected rows have actually been deleted
                            cursor = activity.getContentResolver().query(queryUri, projection, selection, null, null);
                            if (cursor == null || cursor.getCount() == 0 || !checkDeleteSecret.isChecked()) {
                                isSuccessfullyDeleted = true;
                            }

                        } finally {
                            if (cursor != null) {
                                cursor.close();
                            }

                        }

                        dismiss();

                        if (isSuccessfullyDeleted) {
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