/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.EncryptFilesFragment;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;


public class AddEditSmartPGPAuthorityDialogFragment extends DialogFragment implements OnEditorActionListener {
    private static final String IN_MESSENGER = "in_messenger";
    private static final String IN_ACTION = "in_dialog_action";
    private static final String IN_POSITION = "in_position";
    private static final String IN_ALIAS = "in_authority";
    private static final String IN_URI = "in_uri";

    public static final String OUT_ACTION = "out_action";
    public static final String OUT_ALIAS = "out_alias";
    public static final String OUT_POSITION = "out_position";
    public static final String OUT_URI = "out_uri";

    private Messenger mMessenger;
    private Action mAction;
    private int mPosition;
    private Uri mURI;

    private EditText mAuthorityAliasText;
    private TextInputLayout mAuthorityAliasTextLayout;
    private Button mAuthorityAdd;

    public enum Action {
        ADD,
        EDIT,
        DELETE
    }

    public static AddEditSmartPGPAuthorityDialogFragment newInstance(Messenger messenger,
                                                                     Action action,
                                                                     String alias,
                                                                     Uri uri,
                                                                     int position) {
        AddEditSmartPGPAuthorityDialogFragment frag = new AddEditSmartPGPAuthorityDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(IN_MESSENGER, messenger);
        args.putSerializable(IN_ACTION, action);
        args.putString(IN_ALIAS, alias);
        args.putInt(IN_POSITION, position);
        if (uri != null) {
            args.putString(IN_URI, uri.toString());
        }

        frag.setArguments(args);

        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        mMessenger = getArguments().getParcelable(IN_MESSENGER);
        mAction = (Action) getArguments().getSerializable(IN_ACTION);
        mPosition = getArguments().getInt(IN_POSITION);
        if (getArguments().getString(IN_URI) == null)
            mURI = null;
        else
            mURI = Uri.parse(getArguments().getString(IN_URI));

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.add_smartpgp_authority_dialog, null);
        alert.setView(view);

        mAuthorityAliasText = (EditText) view.findViewById(R.id.smartpgp_authority_alias_edit_text);
        mAuthorityAliasTextLayout = (TextInputLayout) view.findViewById(R.id.smartpgp_authority_alias_edit_text_layout);
        mAuthorityAdd = (Button) view.findViewById(R.id.smartpgp_authority_filename);

        mAuthorityAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileHelper.openDocument(AddEditSmartPGPAuthorityDialogFragment.this, null, "*/*", false,
                                        EncryptFilesFragment.REQUEST_CODE_INPUT);
            }
        });

        mAuthorityAliasText.setText(getArguments().getString(IN_ALIAS));

        switch (mAction) {
            case ADD:
                alert.setTitle(R.string.add_smartpgp_authority_dialog_title);
                break;
            case EDIT:
            case DELETE:
                alert.setTitle(R.string.show_smartpgp_authority_dialog_title);
                break;
        }

        // we don't want dialog to be dismissed on click for keyserver addition or edit,
        // thereby requiring the hack seen below and in onStart
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // we need to have an empty listener to prevent errors on some devices as mentioned
                // at http://stackoverflow.com/q/13746412/3000919
                // actual listener set in onStart for adding keyservers or editing them
                dismiss();
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        switch (mAction) {
            case EDIT:
            case DELETE:
                alert.setNeutralButton(R.string.label_smartpgp_authority_dialog_delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteAuthority();
                            }
                        });
                break;
        }

        // Hack to open keyboard.
        // This is the only method that I found to work across all Android versions
        // http://turbomanage.wordpress.com/2012/05/02/show-soft-keyboard-automatically-when-edittext-receives-focus/
        // Notes: * onCreateView can't be used because we want to add buttons to the dialog
        //        * opening in onActivityCreated does not work on Android 4.4
        mAuthorityAliasText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mAuthorityAliasText.post(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager imm = (InputMethodManager) getActivity()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(mAuthorityAliasText, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        });
        mAuthorityAliasText.requestFocus();

        mAuthorityAliasText.setImeActionLabel(getString(android.R.string.ok),
                                                EditorInfo.IME_ACTION_DONE);
        mAuthorityAliasText.setOnEditorActionListener(this);

        return alert.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case EncryptFilesFragment.REQUEST_CODE_INPUT:
                if (data != null) {
                    mURI = data.getData();
                } else {
                    mURI = null;
                }
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog addKeyserverDialog = (AlertDialog) getDialog();
        if (addKeyserverDialog != null) {
            Button positiveButton = addKeyserverDialog.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAuthorityAliasTextLayout.setErrorEnabled(false);

                    dismiss();
                    // return unverified keyserver back to activity
                    authorityEdited();
                }
            });
        }
    }

    public void authorityEdited() {
        dismiss();
        Bundle data = new Bundle();
        data.putSerializable(OUT_ACTION, mAction);
        data.putString(OUT_ALIAS,  mAuthorityAliasText.getText().toString());
        data.putInt(OUT_POSITION, mPosition);
        if (mURI != null) {
            data.putString(OUT_URI, mURI.toString());
        }

        sendMessageToHandler(data);
    }

    public void deleteAuthority() {
        dismiss();
        Bundle data = new Bundle();
        data.putSerializable(OUT_ACTION, Action.DELETE);
        data.putString(OUT_ALIAS, mAuthorityAliasText.getText().toString());
        data.putInt(OUT_POSITION, mPosition);

        sendMessageToHandler(data);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // hide keyboard on dismiss
        hideKeyboard();

        super.onDismiss(dialog);
    }

    private void hideKeyboard() {
        if (getActivity() == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        //check if no view has focus:
        View v = getActivity().getCurrentFocus();
        if (v == null)
            return;

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    /**
     * Associate the "done" button on the soft keyboard with the okay button in the view
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            AlertDialog dialog = ((AlertDialog) getDialog());
            Button bt = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            bt.performClick();
            return true;
        }
        return false;
    }

    /**
     * Send message back to handler which is initialized in a activity
     *
     */
    private void sendMessageToHandler(Bundle data) {
        Message msg = Message.obtain();
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
