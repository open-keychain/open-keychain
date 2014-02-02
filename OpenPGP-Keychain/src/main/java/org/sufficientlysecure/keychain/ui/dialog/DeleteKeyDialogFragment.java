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
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

public class DeleteKeyDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_DELETE_KEY_RING_ROW_IDS = "delete_file";
    private static final String ARG_KEY_TYPE = "key_type";

    public static final int MESSAGE_OKAY = 1;

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
            // TODO: better way to do this?
            String userId = activity.getString(R.string.user_id_no_name);

            if (keyType == Id.type.public_key) {
                PGPPublicKeyRing keyRing = ProviderHelper.getPGPPublicKeyRingByRowId(activity,
                        keyRingRowIds[0]);
                userId = PgpKeyHelper.getMainUserIdSafe(activity,
                        PgpKeyHelper.getMasterKey(keyRing));
            } else {
                PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByRowId(activity,
                        keyRingRowIds[0]);
                userId = PgpKeyHelper.getMainUserIdSafe(activity,
                        PgpKeyHelper.getMasterKey(keyRing));
            }

            builder.setMessage(getString(
                    keyType == Id.type.public_key ? R.string.key_deletion_confirmation
                            : R.string.secret_key_deletion_confirmation, userId));
        } else {
            builder.setMessage(R.string.key_deletion_confirmation_multi);
        }

        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (keyType == Id.type.public_key) {
                    for (long keyRowId : keyRingRowIds) {
                        ProviderHelper.deletePublicKeyRing(activity, keyRowId);
                    }
                } else {
                    for (long keyRowId : keyRingRowIds) {
                        ProviderHelper.deleteSecretKeyRing(activity, keyRowId);
                    }
                }

                dismiss();

                sendMessageToHandler(MESSAGE_OKAY);
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