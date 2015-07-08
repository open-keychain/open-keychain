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


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserver;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.TlsHelper;
import org.sufficientlysecure.keychain.util.orbot.OrbotHelper;

import java.net.Proxy;

public class AddEditKeyserverDialogFragment extends DialogFragment implements OnEditorActionListener {
    private static final String ARG_MESSENGER = "arg_messenger";
    private static final String ARG_ACTION = "arg_dialog_action";
    private static final String ARG_POSITION = "arg_position";
    private static final String ARG_KEYSERVER = "arg_keyserver";

    public static final int MESSAGE_OKAY = 1;
    public static final int MESSAGE_VERIFICATION_FAILED = 2;

    public static final String MESSAGE_KEYSERVER = "new_keyserver";
    public static final String MESSAGE_VERIFIED = "verified";
    public static final String MESSAGE_FAILURE_REASON = "failure_reason";
    public static final String MESSAGE_KEYSERVER_DELETED = "keyserver_deleted";
    public static final String MESSAGE_DIALOG_ACTION = "message_dialog_action";
    public static final String MESSAGE_EDIT_POSITION = "keyserver_edited_position";

    private Messenger mMessenger;
    private DialogAction mDialogAction;
    private int mPosition;

    private EditText mKeyserverEditText;
    private CheckBox mVerifyKeyserverCheckBox;

    public enum DialogAction {
        ADD,
        EDIT
    }

    public enum FailureReason {
        INVALID_URL,
        CONNECTION_FAILED
    }

    public static AddEditKeyserverDialogFragment newInstance(Messenger messenger,
                                                             DialogAction action,
                                                             String keyserver,
                                                             int position) {
        AddEditKeyserverDialogFragment frag = new AddEditKeyserverDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_MESSENGER, messenger);
        args.putSerializable(ARG_ACTION, action);
        args.putString(ARG_KEYSERVER, keyserver);
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

        mKeyserverEditText = (EditText) view.findViewById(R.id.keyserver_url_edit_text);
        mVerifyKeyserverCheckBox = (CheckBox) view.findViewById(R.id.verify_keyserver_checkbox);

        switch (mDialogAction) {
            case ADD: {
                alert.setTitle(R.string.add_keyserver_dialog_title);
                break;
            }
            case EDIT: {
                alert.setTitle(R.string.edit_keyserver_dialog_title);
                mKeyserverEditText.setText(getArguments().getString(ARG_KEYSERVER));
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
                    // behaviour same for edit and add
                    final String keyserverUrl = mKeyserverEditText.getText().toString();
                    if (mVerifyKeyserverCheckBox.isChecked()) {
                        final Preferences.ProxyPrefs proxyPrefs = Preferences.getPreferences(getActivity())
                                .getProxyPrefs();
                        Runnable ignoreTor = new Runnable() {
                            @Override
                            public void run() {
                                verifyConnection(keyserverUrl, null);
                            }
                        };

                        if (OrbotHelper.putOrbotInRequiredState(R.string.orbot_ignore_tor, ignoreTor, proxyPrefs,
                                getActivity())) {
                            verifyConnection(keyserverUrl, proxyPrefs.parcelableProxy.getProxy());
                        }
                    } else {
                        dismiss();
                        // return unverified keyserver back to activity
                        keyserverEdited(keyserverUrl, false);
                    }
                }
            });
        }
    }

    public void keyserverEdited(String keyserver, boolean verified) {
        dismiss();
        Bundle data = new Bundle();
        data.putSerializable(MESSAGE_DIALOG_ACTION, mDialogAction);
        data.putString(MESSAGE_KEYSERVER, keyserver);
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

    public void verificationFailed(FailureReason reason) {
        Bundle data = new Bundle();
        data.putSerializable(MESSAGE_FAILURE_REASON, reason);

        sendMessageToHandler(MESSAGE_VERIFICATION_FAILED, data);
    }

    public void verifyConnection(String keyserver, final Proxy proxy) {

        new AsyncTask<String, Void, FailureReason>() {
            ProgressDialog mProgressDialog;
            String mKeyserver;

            @Override
            protected void onPreExecute() {
                mProgressDialog = new ProgressDialog(getActivity());
                mProgressDialog.setMessage(getString(R.string.progress_verifying_keyserver_url));
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
            }

            @Override
            protected FailureReason doInBackground(String... keyservers) {
                mKeyserver = keyservers[0];
                FailureReason reason = null;
                try {
                    // replace hkps/hkp scheme and reconstruct Uri
                    Uri keyserverUri = Uri.parse(mKeyserver);
                    String scheme = keyserverUri.getScheme();
                    String schemeSpecificPart = keyserverUri.getSchemeSpecificPart();
                    String fragment = keyserverUri.getFragment();
                    if (scheme == null) {
                        throw new MalformedURLException();
                    }
                    if ("hkps".equalsIgnoreCase(scheme)) {
                        scheme = "https";
                    } else if ("hkp".equalsIgnoreCase(scheme)) {
                        scheme = "http";
                    }
                    URI newKeyserver = new URI(scheme, schemeSpecificPart, fragment);

                    Log.d("Converted URL", newKeyserver.toString());

                    OkHttpClient client = HkpKeyserver.getClient(newKeyserver.toURL(), proxy);
                    TlsHelper.pinCertificateIfNecessary(client, newKeyserver.toURL());
                    client.newCall(new Request.Builder().url(newKeyserver.toURL()).build()).execute();
                } catch (TlsHelper.TlsHelperException e) {
                    reason = FailureReason.CONNECTION_FAILED;
                } catch (MalformedURLException | URISyntaxException e) {
                    Log.w(Constants.TAG, "Invalid keyserver URL entered by user.");
                    reason = FailureReason.INVALID_URL;
                } catch (IOException e) {
                    Log.w(Constants.TAG, "Could not connect to entered keyserver url");
                    reason = FailureReason.CONNECTION_FAILED;
                }
                return reason;
            }

            @Override
            protected void onPostExecute(FailureReason failureReason) {
                mProgressDialog.dismiss();
                if (failureReason == null) {
                    keyserverEdited(mKeyserver, true);
                } else {
                    verificationFailed(failureReason);
                }
            }
        }.execute(keyserver);
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
            Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w(Constants.TAG, "Messenger is null!", e);
        }
    }
}
