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


public class NewDeleteKeyDialogFragment extends DialogFragment {
    private static final String ARG_MESSENGER = "messenger";
    private static final String ARG_DELETE_KEY_RING_ROW_IDS = "delete_file";

    public static final int MESSAGE_OKAY = 1;
    private boolean isSingleSelection=false;

    private TextView mainMessage;
    private CheckBox checkDeleteSecret;
    private LinearLayout deleteSecretKeyView;
    private View inflateView;

    private Messenger mMessenger;

    /**
     * Creates new instance of this delete file dialog fragment
     */
    public static NewDeleteKeyDialogFragment newInstance(Messenger messenger, long[] keyRingRowIds
    ) {
        NewDeleteKeyDialogFragment frag = new NewDeleteKeyDialogFragment();
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
            isSingleSelection=true;

            long selectedRow = keyRingRowIds[0];
            long keyType;

            publicKeyRings = ProviderHelper.getPublicKeyRingsRowIds(activity);

            Log.i(Constants.TAG, "Single Key Selected ");

            if (publicKeyRings.contains(selectedRow)) {
                //TODO Should be a better method to do this other than getting all the KeyRings
                Log.i(Constants.TAG, "Is a public key");
                dataUri = KeychainContract.KeyRings.buildPublicKeyRingsUri(String.valueOf(selectedRow));
                keyType = Id.type.public_key;
            } else {
                Log.i(Constants.TAG, "Is private key");
                dataUri = KeychainContract.KeyRings.buildSecretKeyRingsUri(String.valueOf(selectedRow));
                keyType = Id.type.secret_key;
            }

            String userId = ProviderHelper.getUserId(activity, dataUri);
            //Hide the Checkbox and TextView since this is a single selection,user will be notified thru message
            deleteSecretKeyView.setVisibility(View.GONE);
            //Set message depending on which key it is.
            mainMessage.setText(getString(keyType == Id.type.secret_key ? R.string.secret_key_deletion_confirmation
                    : R.string.public_key_deletion_confirmation, userId));


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
                                KeychainContract.KeyRings._ID, // 0
                                KeychainContract.KeyRings.MASTER_KEY_ID, // 1
                                KeychainContract.UserIds.USER_ID,//2
                                KeychainContract.KeyRings.TYPE// 3
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

                        long rowId;
                        long masterKeyId;
                        String userId;
                        long keyType;
                        try {
                            while (cursor != null && cursor.moveToNext()) {
                                rowId = cursor.getLong(0);
                                masterKeyId = cursor.getLong(1);
                                userId = cursor.getString(2);
                                keyType = cursor.getLong(3);

                                Log.d(Constants.TAG, "rowId: " + rowId + ", masterKeyId: " + masterKeyId
                                        + ", userId: " + userId + ", keyType:" + (keyType == KeychainContract.KeyTypes.PUBLIC ? "Public" : "Private"));


                                if (keyType == KeychainContract.KeyTypes.SECRET) {
                                    if (checkDeleteSecret.isChecked() || isSingleSelection) {
                                        Log.i(Constants.TAG, "Deleting Secret Keys");
                                        //Only private key is deleted, should the entire key be deleted ?
                                        //If so, find out how and implement here
                                        ProviderHelper.deleteSecretKeyRing(activity, rowId);
                                        Log.i(Constants.TAG, "Secret Key Deleted");
                                    }
                                } else {
                                    Log.i(Constants.TAG, "Deleting Public Keys ");
                                    ProviderHelper.deletePublicKeyRing(activity, rowId);
                                    Log.i(Constants.TAG, "Public Key Deleted");
                                }

                            }

                        } finally {
                            if (cursor != null) {
                                cursor.close();
                            }

                        }

                        dismiss();

                        //TODO Actually check that selected items have been deleted and then send this
                        sendMessageToHandler(MESSAGE_OKAY, null);

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
