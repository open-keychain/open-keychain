/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Adithya Abraham Philip <adithyaphilip@gmail.com>
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

package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.DeleteResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.RevokeResult;
import org.sufficientlysecure.keychain.service.DeleteKeyringParcel;
import org.sufficientlysecure.keychain.service.RevokeKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

public class RevokeDeleteDialogActivity extends FragmentActivity {

    public static final String EXTRA_MASTER_KEY_ID = "extra_master_key_id";
    public static final String EXTRA_KEYSERVER = "extra_keyserver";

    private final int REVOKE_OP_ID = 1;
    private final int DELETE_OP_ID = 2;
    private CryptoOperationHelper<RevokeKeyringParcel, RevokeResult> mRevokeOpHelper;
    private CryptoOperationHelper<DeleteKeyringParcel, DeleteResult> mDeleteOpHelper;

    private long mMasterKeyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRevokeOpHelper = new CryptoOperationHelper<>(this,
                getRevocationCallback(), R.string.progress_revoking_uploading, REVOKE_OP_ID);
        mRevokeOpHelper.onRestoreInstanceState(savedInstanceState);

        mDeleteOpHelper = new CryptoOperationHelper<>(this,
                getDeletionCallback(), R.string.progress_deleting, DELETE_OP_ID);
        mDeleteOpHelper.onRestoreInstanceState(savedInstanceState);

        mMasterKeyId = getIntent().getLongExtra(EXTRA_MASTER_KEY_ID, -1);

        RevokeDeleteDialogFragment fragment = RevokeDeleteDialogFragment.newInstance();
        fragment.show(getSupportFragmentManager(), "deleteRevokeDialog");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mDeleteOpHelper.handleActivityResult(requestCode, resultCode, data);
        mRevokeOpHelper.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mRevokeOpHelper.onSaveInstanceState(outState);
        mDeleteOpHelper.onSaveInstanceState(outState);
    }

    private void returnResult(OperationResult result) {
        Intent intent = new Intent();
        intent.putExtra(OperationResult.EXTRA_RESULT, result);
        setResult(RESULT_OK, intent);
        finish();
    }

    private CryptoOperationHelper.Callback<RevokeKeyringParcel, RevokeResult> getRevocationCallback() {

        CryptoOperationHelper.Callback<RevokeKeyringParcel, RevokeResult> callback
                = new CryptoOperationHelper.Callback<RevokeKeyringParcel, RevokeResult>() {
            @Override
            public RevokeKeyringParcel createOperationInput() {
                return new RevokeKeyringParcel(mMasterKeyId, true,
                        getIntent().getStringExtra(EXTRA_KEYSERVER));
            }

            @Override
            public void onCryptoOperationSuccess(RevokeResult result) {
                returnResult(result);
            }

            @Override
            public void onCryptoOperationCancelled() {
                setResult(RESULT_CANCELED);
                finish();
            }

            @Override
            public void onCryptoOperationError(RevokeResult result) {
                returnResult(result);
            }

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };

        return callback;
    }

    private CryptoOperationHelper.Callback<DeleteKeyringParcel, DeleteResult> getDeletionCallback() {

        CryptoOperationHelper.Callback<DeleteKeyringParcel, DeleteResult> callback
                = new CryptoOperationHelper.Callback<DeleteKeyringParcel, DeleteResult>() {
            @Override
            public DeleteKeyringParcel createOperationInput() {
                return new DeleteKeyringParcel(new long[]{mMasterKeyId}, true);
            }

            @Override
            public void onCryptoOperationSuccess(DeleteResult result) {
                returnResult(result);
            }

            @Override
            public void onCryptoOperationCancelled() {
                setResult(RESULT_CANCELED);
                finish();
            }

            @Override
            public void onCryptoOperationError(DeleteResult result) {
                returnResult(result);
            }

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };

        return callback;
    }

    private void startRevocationOperation() {
        mRevokeOpHelper.cryptoOperation();
    }

    private void startDeletionOperation() {
        mDeleteOpHelper.cryptoOperation();
    }

    public static class RevokeDeleteDialogFragment extends DialogFragment {

        public static RevokeDeleteDialogFragment newInstance() {
            RevokeDeleteDialogFragment frag = new RevokeDeleteDialogFragment();
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            final String CHOICE_REVOKE = getString(R.string.del_rev_dialog_choice_rev_upload);
            final String CHOICE_DELETE = getString(R.string.del_rev_dialog_choice_delete);

            // if the dialog is displayed from the application class, design is missing
            // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
            ContextThemeWrapper theme = new ContextThemeWrapper(activity,
                    R.style.Theme_AppCompat_Light_Dialog);

            CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);
            alert.setTitle(R.string.del_rev_dialog_title);

            LayoutInflater inflater = LayoutInflater.from(theme);
            View view = inflater.inflate(R.layout.del_rev_dialog, null);
            alert.setView(view);

            final Spinner spinner = (Spinner) view.findViewById(R.id.spinner);

            alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    activity.setResult(RESULT_CANCELED);
                    activity.finish();
                }
            });

            alert.setPositiveButton(R.string.del_rev_dialog_btn_revoke,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            String choice = spinner.getSelectedItem().toString();
                            if (choice.equals(CHOICE_REVOKE)) {
                                ((RevokeDeleteDialogActivity) activity)
                                        .startRevocationOperation();
                            } else if (choice.equals(CHOICE_DELETE)) {
                                ((RevokeDeleteDialogActivity) activity)
                                        .startDeletionOperation();
                            } else {
                                throw new AssertionError(
                                        "Unsupported delete type in RevokeDeleteDialogFragment");
                            }
                        }
                    });

            final AlertDialog alertDialog = alert.show();

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                    String choice = parent.getItemAtPosition(pos).toString();

                    if (choice.equals(CHOICE_REVOKE)) {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setText(R.string.del_rev_dialog_btn_revoke);
                    } else if (choice.equals(CHOICE_DELETE)) {
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setText(R.string.del_rev_dialog_btn_delete);
                    } else {
                        throw new AssertionError(
                                "Unsupported delete type in RevokeDeleteDialogFragment");
                    }
                }

                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            return alertDialog;
        }
    }
}
