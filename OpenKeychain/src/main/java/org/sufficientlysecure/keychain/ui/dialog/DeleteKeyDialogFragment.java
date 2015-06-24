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
import org.sufficientlysecure.keychain.operations.results.DeleteResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.DeleteKeyringParcel;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.util.HashMap;

public class DeleteKeyDialogFragment extends DialogFragment
        implements CryptoOperationHelper.Callback<DeleteKeyringParcel, DeleteResult> {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_DELETE_MASTER_KEY_IDS = "delete_master_key_ids";

    public static final int MESSAGE_OKAY = 1;
    public static final int MESSAGE_ERROR = 0;

    private TextView mMainMessage;
    private View mInflateView;

    private Messenger mMessenger;

    // for CryptoOperationHelper.Callback
    private long[] mMasterKeyIds;
    private boolean mHasSecret;
    private CryptoOperationHelper<DeleteKeyringParcel, DeleteResult> mDeleteOpHelper;

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mDeleteOpHelper != null) {
            mDeleteOpHelper.handleActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        mMessenger = getArguments().getParcelable(ARG_MESSENGER);

        final long[] masterKeyIds = getArguments().getLongArray(ARG_DELETE_MASTER_KEY_IDS);

        CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(activity);

        // Setup custom View to display in AlertDialog
        LayoutInflater inflater = activity.getLayoutInflater();
        mInflateView = inflater.inflate(R.layout.view_key_delete_fragment, null);
        builder.setView(mInflateView);

        mMainMessage = (TextView) mInflateView.findViewById(R.id.mainMessage);

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
                String name;
                KeyRing.UserId mainUserId = KeyRing.splitUserId((String) data.get(KeyRings.USER_ID));
                if (mainUserId.name != null) {
                    name = mainUserId.name;
                } else {
                    name = getString(R.string.user_id_no_name);
                }
                hasSecret = ((Long) data.get(KeyRings.HAS_ANY_SECRET)) == 1;

                if (hasSecret) {
                    // show title only for secret key deletions,
                    // see http://www.google.com/design/spec/components/dialogs.html#dialogs-behavior
                    builder.setTitle(getString(R.string.title_delete_secret_key, name));
                    mMainMessage.setText(getString(R.string.secret_key_deletion_confirmation, name));
                } else {
                    mMainMessage.setText(getString(R.string.public_key_deletetion_confirmation, name));
                }
            } catch (ProviderHelper.NotFoundException e) {
                dismiss();
                return null;
            }
        } else {
            mMainMessage.setText(R.string.key_deletion_confirmation_multi);
            hasSecret = false;
        }

        builder.setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                mMasterKeyIds = masterKeyIds;
                mHasSecret = hasSecret;

                mDeleteOpHelper = new CryptoOperationHelper<>
                        (DeleteKeyDialogFragment.this, DeleteKeyDialogFragment.this,
                                R.string.progress_deleting);
                mDeleteOpHelper.cryptoOperation();
                // do NOT dismiss here, it'll give
                // OperationHelper a null fragmentManager
                // dismiss();
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

    @Override
    public DeleteKeyringParcel createOperationInput() {
        return new DeleteKeyringParcel(mMasterKeyIds, mHasSecret);
    }

    @Override
    public void onCryptoOperationSuccess(DeleteResult result) {
        handleResult(result);
    }

    @Override
    public void onCryptoOperationCancelled() {

    }

    @Override
    public void onCryptoOperationError(DeleteResult result) {
        handleResult(result);
    }

    public void handleResult(DeleteResult result) {
        try {
            Bundle data = new Bundle();
            data.putParcelable(OperationResult.EXTRA_RESULT, result);
            Message msg = Message.obtain();
            msg.arg1 = ServiceProgressHandler.MessageStatus.OKAY.ordinal();
            msg.setData(data);
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(Constants.TAG, "messenger error", e);
        }
        dismiss();
    }
}
