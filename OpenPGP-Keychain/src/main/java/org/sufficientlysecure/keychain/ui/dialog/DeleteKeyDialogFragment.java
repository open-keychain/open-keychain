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

import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;

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

import java.util.ArrayList;

public class DeleteKeyDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_DELETE_KEY_RING_ROW_IDS = "delete_file";
    private static final String ARG_KEY_TYPE = "key_type";

    public static final int MESSAGE_OKAY = 1;

    public static final String MESSAGE_NOT_DELETED = "not_deleted";

    private Messenger mMessenger;

    /**
     * Creates new instance of this delete file dialog fragment
     */
    public static DeleteKeyDialogFragment newInstance(Messenger messenger, long[] keyRingRowIds,
                                                      int keyType) {
        DeleteKeyDialogFragment frag = new DeleteKeyDialogFragment();
        Bundle args = new Bundle();

        args.putParcelable(ARG_MESSENGER, messenger);
        args.putLongArray(ARG_DELETE_KEY_RING_ROW_IDS, keyRingRowIds);
        args.putInt(ARG_KEY_TYPE, keyType);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        mMessenger = getArguments().getParcelable(ARG_MESSENGER);

        final long[] keyRingRowIds = getArguments().getLongArray(ARG_DELETE_KEY_RING_ROW_IDS);
        final int keyType = getArguments().getInt(ARG_KEY_TYPE);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.warning);

        if (keyRingRowIds.length == 1) {
            Uri dataUri;
            if (keyType == Id.type.public_key) {
                dataUri = KeychainContract.KeyRings.buildPublicKeyRingsUri(String.valueOf(keyRingRowIds[0]));
            } else {
                dataUri = KeychainContract.KeyRings.buildSecretKeyRingsUri(String.valueOf(keyRingRowIds[0]));
            }
            String userId = ProviderHelper.getUserId(activity, dataUri);

            builder.setMessage(getString(
                    keyType == Id.type.public_key ? R.string.key_deletion_confirmation
                            : R.string.secret_key_deletion_confirmation, userId));
        } else {
            builder.setMessage(R.string.key_deletion_confirmation_multi);
        }

        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                ArrayList<String> notDeleted = new ArrayList<String>();

                if (keyType == Id.type.public_key) {
                    Uri queryUri = KeychainContract.KeyRings.buildPublicKeyRingsUri();
                    String[] projection = new String[]{
                            KeychainContract.KeyRings._ID, // 0
                            KeychainContract.KeyRings.MASTER_KEY_ID, // 1
                            KeychainContract.UserIds.USER_ID // 2
                    };

                    // make selection with all entries where _ID is one of the given row ids
                    String selection = KeychainDatabase.Tables.KEY_RINGS + "." +
                            KeychainContract.KeyRings._ID + " IN(";
                    String selectionIDs = "";
                    for (int i = 0; i < keyRingRowIds.length; i++) {
                        selectionIDs += "'" + String.valueOf(keyRingRowIds[i]) + "'";
                        if (i+1 < keyRingRowIds.length)
                            selectionIDs += ",";
                    }
                    selection += selectionIDs + ")";

                    Cursor cursor = activity.getContentResolver().query(queryUri, projection,
                            selection, null, null);

                    long rowId;
                    long masterKeyId;
                    String userId;
                    try {
                        while (cursor != null && cursor.moveToNext()) {
                            rowId = cursor.getLong(0);
                            masterKeyId = cursor.getLong(1);
                            userId = cursor.getString(2);

                            Log.d(Constants.TAG, "rowId: " + rowId + ", masterKeyId: " + masterKeyId
                                    + ", userId: " + userId);

                            // check if a corresponding secret key exists...
                            Cursor secretCursor = activity.getContentResolver().query(
                                    KeychainContract.KeyRings.buildSecretKeyRingsByMasterKeyIdUri(String.valueOf(masterKeyId)),
                                    null, null, null, null
                            );
                            if (secretCursor != null && secretCursor.getCount() > 0) {
                                notDeleted.add(userId);
                            } else {
                                // it is okay to delete this key, no secret key found!
                                ProviderHelper.deletePublicKeyRing(activity, rowId);
                            }
                            if (secretCursor != null) {
                                secretCursor.close();
                            }
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                } else {
                    for (long keyRowId : keyRingRowIds) {
                        ProviderHelper.deleteSecretKeyRing(activity, keyRowId);
                    }
                }

                dismiss();

                if (notDeleted.size() > 0) {
                    Bundle data = new Bundle();
                    data.putStringArrayList(MESSAGE_NOT_DELETED, notDeleted);
                    sendMessageToHandler(MESSAGE_OKAY, data);
                } else {
                    sendMessageToHandler(MESSAGE_OKAY, null);
                }
            }
        });
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