/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.devspark.appmsg.AppMsg;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyResult;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import java.util.regex.Matcher;

public class DecryptMessageFragment extends Fragment {
    DecryptSignatureResultDisplay mSignatureResultDisplay;

    private EditText mMessage;
    private BootstrapButton mDecryptButton;
    private BootstrapButton mDecryptFromCLipboardButton;

    public static final String EXTRA_CIPHERTEXT = "ciphertext";

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decrypt_message_fragment, container, false);

        mMessage = (EditText) view.findViewById(R.id.message);
        mDecryptButton = (BootstrapButton) view.findViewById(R.id.action_decrypt);
        mDecryptFromCLipboardButton = (BootstrapButton) view.findViewById(R.id.action_decrypt_from_clipboard);

        String ciphertext = getArguments().getString(EXTRA_CIPHERTEXT);
        if (ciphertext != null) {
            mMessage.setText(ciphertext);
        }

        mDecryptButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptAction();
            }
        });

        mDecryptFromCLipboardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptFromClipboard();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mSignatureResultDisplay = (DecryptSignatureResultDisplay) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement DecryptSignatureResultDisplay");
        }
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(Constants.TAG, "onActivityCreated tag: " + getTag());


    }

    private void decryptFromClipboard() {
        CharSequence clipboardText = ClipboardReflection.getClipboardText(getActivity());

        if (clipboardText != null) {
            Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(clipboardText);
            if (!matcher.matches()) {
                matcher = PgpHelper.PGP_CLEARTEXT_SIGNATURE.matcher(clipboardText);
            }
            if (matcher.matches()) {
                String data = matcher.group(1);
                mMessage.setText(data);
                decryptStart(null);
            } else {
                AppMsg.makeText(getActivity(), R.string.error_invalid_data, AppMsg.STYLE_INFO)
                        .show();
            }
        } else {
            AppMsg.makeText(getActivity(), R.string.error_invalid_data, AppMsg.STYLE_INFO)
                    .show();
        }
    }

    private void decryptAction() {
        decryptStart(null);
    }

    private void decryptStart(String passphrase) {
        Log.d(Constants.TAG, "decryptStart");

        // Send all information needed to service to decrypt in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        // fill values for this action
        Bundle data = new Bundle();

        intent.setAction(KeychainIntentService.ACTION_DECRYPT_VERIFY);

        // data
        data.putInt(KeychainIntentService.TARGET, KeychainIntentService.TARGET_BYTES);
        String message = mMessage.getText().toString();
        data.putByteArray(KeychainIntentService.DECRYPT_CIPHERTEXT_BYTES, message.getBytes());
        data.putString(KeychainIntentService.DECRYPT_PASSPHRASE, passphrase);

        // TODO
        data.putBoolean(KeychainIntentService.DECRYPT_ASSUME_SYMMETRIC, false);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after encrypting is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(getActivity(),
                getString(R.string.progress_decrypting), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle returnData = message.getData();


                    PgpDecryptVerifyResult decryptVerifyResult =
                            returnData.getParcelable(KeychainIntentService.RESULT_DECRYPT_VERIFY_RESULT);

                    if (PgpDecryptVerifyResult.KEY_PASSHRASE_NEEDED == decryptVerifyResult.getStatus()) {
                        showPassphraseDialog(decryptVerifyResult.getKeyIdPassphraseNeeded());
                    } else {

                        AppMsg.makeText(getActivity(), R.string.decryption_successful,
                                AppMsg.STYLE_INFO).show();

                        byte[] decryptedMessage = returnData
                                .getByteArray(KeychainIntentService.RESULT_DECRYPTED_BYTES);
                        mMessage.setText(new String(decryptedMessage));
                        mMessage.setHorizontallyScrolling(false);


                        OpenPgpSignatureResult signatureResult = decryptVerifyResult.getSignatureResult();

                        // display signature result in activity
                        mSignatureResultDisplay.onSignatureResult(signatureResult);
                    }

                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(getActivity());

        // start service with intent
        getActivity().startService(intent);
    }

    private void showPassphraseDialog(long keyId) {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    String passphrase =
                            message.getData().getString(PassphraseDialogFragment.MESSAGE_DATA_PASSPHRASE);
                    decryptStart(passphrase);
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(getActivity(),
                    messenger, keyId);

            passphraseDialog.show(getActivity().getSupportFragmentManager(), "passphraseDialog");
        } catch (PgpGeneralException e) {
            Log.d(Constants.TAG, "No passphrase for this secret key, encrypt directly!");
            // send message to handler to start encryption directly
            returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
        }
    }


}
