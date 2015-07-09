package org.sufficientlysecure.keychain.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.DeleteResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.DeleteKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.util.Log;

import java.util.HashMap;

public class DeleteKeyDialogActivity extends FragmentActivity
        implements CryptoOperationHelper.Callback<DeleteKeyringParcel, DeleteResult> {
    public static final String EXTRA_DELETE_MASTER_KEY_IDS = "extra_delete_master_key_ids";

    private CryptoOperationHelper<DeleteKeyringParcel, DeleteResult> mDeleteOpHelper;

    private long[] mMasterKeyIds;
    private boolean mHasSecret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDeleteOpHelper = new CryptoOperationHelper<>(DeleteKeyDialogActivity.this,
                DeleteKeyDialogActivity.this, R.string.progress_deleting);
        mDeleteOpHelper.onRestoreInstanceState(savedInstanceState);

        mMasterKeyIds = getIntent().getLongArrayExtra(EXTRA_DELETE_MASTER_KEY_IDS);

        Handler deleteDialogHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == DeleteKeyDialogFragment.MESSAGE_PERFORM_DELETE) {
                    mHasSecret = msg.getData().getBoolean(DeleteKeyDialogFragment.MSG_HAS_SECRET);
                    mDeleteOpHelper.cryptoOperation();
                }
            }
        };

        Messenger messenger = new Messenger(deleteDialogHandler);

        DeleteKeyDialogFragment deleteKeyDialogFragment
                = DeleteKeyDialogFragment.newInstance(messenger, mMasterKeyIds);

        deleteKeyDialogFragment.show(getSupportFragmentManager(), "deleteKeyDialog");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mDeleteOpHelper.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mDeleteOpHelper.onSaveInstanceState(outState);
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
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onCryptoOperationError(DeleteResult result) {
        handleResult(result);
    }

    @Override
    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        return false;
    }

    public void handleResult(DeleteResult result) {
        Intent intent = new Intent();
        intent.putExtra(OperationResult.EXTRA_RESULT, result);
        setResult(RESULT_OK, intent);
        finish();
    }

    public static class DeleteKeyDialogFragment extends DialogFragment {

        public static final String MSG_HAS_SECRET = "msg_has_secret";

        private static final String ARG_MESSENGER = "messenger";
        private static final String ARG_DELETE_MASTER_KEY_IDS = "delete_master_key_ids";

        public static final int MESSAGE_PERFORM_DELETE = 1;

        private TextView mMainMessage;
        private View mInflateView;

        private Messenger mMessenger;
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
            mMessenger = getArguments().getParcelable(ARG_MESSENGER);

            final long[] masterKeyIds = getArguments().getLongArray(ARG_DELETE_MASTER_KEY_IDS);

            ContextThemeWrapper theme = new ContextThemeWrapper(activity,
                    R.style.Theme_AppCompat_Light_Dialog);

            CustomAlertDialogBuilder builder = new CustomAlertDialogBuilder(theme);

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
                                    KeychainContract.KeyRings.USER_ID,
                                    KeychainContract.KeyRings.HAS_ANY_SECRET
                            }, new int[]{
                                    ProviderHelper.FIELD_TYPE_STRING,
                                    ProviderHelper.FIELD_TYPE_INTEGER
                            }
                    );
                    String name;
                    KeyRing.UserId mainUserId = KeyRing.splitUserId((String) data.get(KeychainContract.KeyRings.USER_ID));
                    if (mainUserId.name != null) {
                        name = mainUserId.name;
                    } else {
                        name = getString(R.string.user_id_no_name);
                    }
                    hasSecret = ((Long) data.get(KeychainContract.KeyRings.HAS_ANY_SECRET)) == 1;

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

                    Bundle data = new Bundle();
                    data.putBoolean(MSG_HAS_SECRET, hasSecret);
                    Message msg = Message.obtain();
                    msg.setData(data);
                    msg.what = MESSAGE_PERFORM_DELETE;
                    try {
                        mMessenger.send(msg);
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "messenger error", e);
                    }
                }
            });

            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int id) {
                    getActivity().finish();
                }
            });

            return builder.show();
        }
    }

}
