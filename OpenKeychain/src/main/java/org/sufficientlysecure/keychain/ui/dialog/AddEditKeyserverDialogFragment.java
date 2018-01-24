/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverAddress;
import org.sufficientlysecure.keychain.network.OkHttpClientFactory;
import org.sufficientlysecure.keychain.network.TlsCertificatePinning;
import org.sufficientlysecure.keychain.network.orbot.OrbotHelper;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


public class AddEditKeyserverDialogFragment extends DialogFragment implements OnEditorActionListener {
    private static final String ARG_MESSENGER = "arg_messenger";
    private static final String ARG_ACTION = "arg_dialog_action";
    private static final String ARG_POSITION = "arg_position";
    private static final String ARG_KEYSERVER = "arg_keyserver";

    public static final int MESSAGE_OKAY = 1;

    public static final String MESSAGE_KEYSERVER = "new_keyserver";
    public static final String MESSAGE_VERIFIED = "verified";
    public static final String MESSAGE_KEYSERVER_DELETED = "keyserver_deleted";
    public static final String MESSAGE_DIALOG_ACTION = "message_dialog_action";
    public static final String MESSAGE_EDIT_POSITION = "keyserver_edited_position";

    private Messenger mMessenger;
    private DialogAction mDialogAction;
    private int mPosition;

    private EditText mKeyserverEditText;
    private TextInputLayout mKeyserverEditTextLayout;
    private EditText mKeyserverEditOnionText;
    private TextInputLayout mKeyserverEditOnionTextLayout;
    private CheckBox mVerifyKeyserverCheckBox;
    private CheckBox mOnlyTrustedKeyserverCheckBox;

    public enum DialogAction {
        ADD,
        EDIT
    }

    public enum VerifyReturn {
        INVALID_URL,
        CONNECTION_FAILED,
        NO_PINNED_CERTIFICATE,
        GOOD
    }

    public static AddEditKeyserverDialogFragment newInstance(Messenger messenger,
                                                             DialogAction action,
                                                             HkpKeyserverAddress keyserver,
                                                             int position) {
        AddEditKeyserverDialogFragment frag = new AddEditKeyserverDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSENGER, messenger);
        args.putSerializable(ARG_ACTION, action);
        args.putParcelable(ARG_KEYSERVER, keyserver);
        args.putInt(ARG_POSITION, position);

        frag.setArguments(args);

        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        mMessenger = getArguments().getParcelable(ARG_MESSENGER);
        mDialogAction = (DialogAction) getArguments().getSerializable(ARG_ACTION);
        mPosition = getArguments().getInt(ARG_POSITION);

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.add_keyserver_dialog, null);
        alert.setView(view);

        mKeyserverEditText = view.findViewById(R.id.keyserver_url_edit_text);
        mKeyserverEditTextLayout = view.findViewById(R.id.keyserver_url_edit_text_layout);
        mKeyserverEditOnionText = view.findViewById(R.id.keyserver_onion_edit_text);
        mKeyserverEditOnionTextLayout = view.findViewById(R.id.keyserver_onion_edit_text_layout);
        mVerifyKeyserverCheckBox = view.findViewById(R.id.verify_connection_checkbox);
        mOnlyTrustedKeyserverCheckBox = view.findViewById(R.id.only_trusted_keyserver_checkbox);
        mVerifyKeyserverCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mOnlyTrustedKeyserverCheckBox.setEnabled(isChecked);
            }
        });

        switch (mDialogAction) {
            case ADD: {
                alert.setTitle(R.string.add_keyserver_dialog_title);
                break;
            }
            case EDIT: {
                alert.setTitle(R.string.edit_keyserver_dialog_title);
                HkpKeyserverAddress keyserver = getArguments().getParcelable(ARG_KEYSERVER);
                mKeyserverEditText.setText(keyserver.getUrl());
                mKeyserverEditOnionText.setText(keyserver.getOnion());
                break;
            }
        }

        // we don't want dialog to be dismissed on click for keyserver addition or edit,
        // thereby requiring the hack seen below and in onStart
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // we need to have an empty listener to prevent errors on some devices as mentioned
                // at http://stackoverflow.com/q/13746412/3000919
                // actual listener set in onStart for adding keyservers or editing them
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        switch (mDialogAction) {
            case EDIT: {
                alert.setNeutralButton(R.string.label_keyserver_dialog_delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteKeyserver(mPosition);
                            }
                        });
                break;
            }
            case ADD: {
                // do nothing
                break;
            }
        }

        // Hack to open keyboard.
        // This is the only method that I found to work across all Android versions
        // http://turbomanage.wordpress.com/2012/05/02/show-soft-keyboard-automatically-when-edittext-receives-focus/
        // Notes: * onCreateView can't be used because we want to add buttons to the dialog
        //        * opening in onActivityCreated does not work on Android 4.4
        mKeyserverEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                mKeyserverEditText.post(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager imm = (InputMethodManager) getActivity()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(mKeyserverEditText, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
            }
        });
        mKeyserverEditText.requestFocus();

        mKeyserverEditText.setImeActionLabel(getString(android.R.string.ok),
                EditorInfo.IME_ACTION_DONE);
        mKeyserverEditText.setOnEditorActionListener(this);

        return alert.show();
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
                    mKeyserverEditTextLayout.setErrorEnabled(false);
                    mKeyserverEditOnionTextLayout.setErrorEnabled(false);

                    // behaviour same for edit and add
                    String keyserverUrl = mKeyserverEditText.getText().toString();
                    String keyserverOnion = mKeyserverEditOnionText.getText() == null ? null
                            : mKeyserverEditOnionText.getText().toString();
                    final HkpKeyserverAddress keyserver =
                            HkpKeyserverAddress.createWithOnionProxy(keyserverUrl, keyserverOnion);
                    if (mVerifyKeyserverCheckBox.isChecked()) {
                        final ParcelableProxy proxy = Preferences.getPreferences(getActivity())
                                .getParcelableProxy();
                        OrbotHelper.DialogActions dialogActions = new OrbotHelper.DialogActions() {
                            @Override
                            public void onOrbotStarted() {
                                verifyConnection(
                                        keyserver,
                                        proxy,
                                        mOnlyTrustedKeyserverCheckBox.isChecked()
                                );
                            }

                            @Override
                            public void onNeutralButton() {
                                verifyConnection(
                                        keyserver,
                                        null,
                                        mOnlyTrustedKeyserverCheckBox.isChecked()
                                );
                            }

                            @Override
                            public void onCancel() {
                                // do nothing
                            }
                        };

                        if (OrbotHelper.putOrbotInRequiredState(dialogActions, getActivity())) {
                            verifyConnection(
                                    keyserver,
                                    proxy,
                                    mOnlyTrustedKeyserverCheckBox.isChecked()
                            );
                        }
                    } else {
                        dismiss();
                        // return unverified keyserver back to activity
                        keyserverEdited(keyserver, false);
                    }
                }
            });
        }
    }

    public void keyserverEdited(HkpKeyserverAddress keyserver, boolean verified) {
        dismiss();
        Bundle data = new Bundle();
        data.putSerializable(MESSAGE_DIALOG_ACTION, mDialogAction);
        data.putParcelable(MESSAGE_KEYSERVER, keyserver);
        data.putBoolean(MESSAGE_VERIFIED, verified);

        if (mDialogAction == DialogAction.EDIT) {
            data.putInt(MESSAGE_EDIT_POSITION, mPosition);
        }

        sendMessageToHandler(MESSAGE_OKAY, data);
    }

    public void deleteKeyserver(int position) {
        dismiss();
        Bundle data = new Bundle();
        data.putSerializable(MESSAGE_DIALOG_ACTION, DialogAction.EDIT);
        data.putInt(MESSAGE_EDIT_POSITION, position);
        data.putBoolean(MESSAGE_KEYSERVER_DELETED, true);

        sendMessageToHandler(MESSAGE_OKAY, data);
    }

    public void verificationFailed(VerifyReturn verifyReturn) {
        switch (verifyReturn) {
            case CONNECTION_FAILED: {
                mKeyserverEditTextLayout.setError(
                        getString(R.string.add_keyserver_connection_failed));
                break;
            }
            case INVALID_URL: {
                mKeyserverEditTextLayout.setError(
                        getString(R.string.add_keyserver_invalid_url));
                break;
            }
            case NO_PINNED_CERTIFICATE: {
                mKeyserverEditTextLayout.setError(
                        getString(R.string.add_keyserver_keyserver_not_trusted));
                break;
            }
        }

    }

    public void verifyConnection(HkpKeyserverAddress keyserver, final ParcelableProxy proxy, final boolean onlyTrustedKeyserver) {

        new AsyncTask<HkpKeyserverAddress, Void, VerifyReturn>() {
            ProgressDialog mProgressDialog;
            HkpKeyserverAddress mKeyserver;

            @Override
            protected void onPreExecute() {
                mProgressDialog = new ProgressDialog(getActivity());
                mProgressDialog.setMessage(getString(R.string.progress_verifying_keyserver_connection));
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
            }

            @Override
            protected VerifyReturn doInBackground(HkpKeyserverAddress... keyservers) {
                mKeyserver = keyservers[0];

                return verifyKeyserver(mKeyserver, proxy, onlyTrustedKeyserver);
            }

            @Override
            protected void onPostExecute(VerifyReturn verifyReturn) {
                mProgressDialog.dismiss();
                if (verifyReturn == VerifyReturn.GOOD) {
                    keyserverEdited(mKeyserver, true);
                } else {
                    verificationFailed(verifyReturn);
                }
            }
        }.execute(keyserver);
    }

    private VerifyReturn verifyKeyserver(HkpKeyserverAddress keyserver, final ParcelableProxy proxy, final boolean onlyTrustedKeyserver) {
        VerifyReturn reason = VerifyReturn.GOOD;
        try {
            URI keyserverUriHttp = keyserver.getUrlURI();

            // check TLS pinning only for non-Tor keyservers
            TlsCertificatePinning tlsCertificatePinning = new TlsCertificatePinning(keyserverUriHttp.toURL());
            boolean isPinAvailable = tlsCertificatePinning.isPinAvailable();
            if (onlyTrustedKeyserver && !isPinAvailable) {
                Timber.w("No pinned certificate for this host in OpenKeychain's assets.");
                reason = VerifyReturn.NO_PINNED_CERTIFICATE;
                return reason;
            }

            OkHttpClient client = OkHttpClientFactory.getClientPinnedIfAvailable(
                    keyserverUriHttp.toURL(), proxy.getProxy());
            client.newCall(new Request.Builder().url(keyserverUriHttp.toURL()).build()).execute();

            // try out onion keyserver if Tor is enabled
            if (proxy.isTorEnabled()) {
                URI keyserverUriOnion = keyserver.getOnionURI();

                OkHttpClient clientTor = OkHttpClientFactory.getClientPinnedIfAvailable(
                        keyserverUriOnion.toURL(), proxy.getProxy());
                clientTor.newCall(new Request.Builder().url(keyserverUriOnion.toURL()).build()).execute();
            }
        } catch (MalformedURLException | URISyntaxException e) {
            Timber.w("Invalid keyserver URL entered by user.");
            reason = VerifyReturn.INVALID_URL;
        } catch (IOException e) {
            Timber.w("Could not connect to entered keyserver url");
            reason = VerifyReturn.CONNECTION_FAILED;
        }

        return reason;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        // hide keyboard on dismiss
        hideKeyboard();
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
            Timber.w(e, "Exception sending message, Is handler present?");
        } catch (NullPointerException e) {
            Timber.w(e, "Messenger is null!");
        }
    }
}
