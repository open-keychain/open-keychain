/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.devspark.appmsg.AppMsg;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyResult;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.util.Log;

import java.util.regex.Matcher;

public class DecryptMessageFragment extends DecryptFragment {
    public static final String ARG_CIPHERTEXT = "ciphertext";

    // view
    private EditText mMessage;
    private BootstrapButton mDecryptButton;
    private BootstrapButton mDecryptFromCLipboardButton;

    // model
    private String mCiphertext;

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decrypt_message_fragment, container, false);

        mMessage = (EditText) view.findViewById(R.id.message);
        mDecryptButton = (BootstrapButton) view.findViewById(R.id.action_decrypt);
        mDecryptFromCLipboardButton = (BootstrapButton) view.findViewById(R.id.action_decrypt_from_clipboard);
        mDecryptButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptClicked();
            }
        });
        mDecryptFromCLipboardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptFromClipboardClicked();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String ciphertext = getArguments().getString(ARG_CIPHERTEXT);
        if (ciphertext != null) {
            mCiphertext = ciphertext;
            decryptStart(null);
        }
    }

    private void decryptClicked() {
        mCiphertext = mMessage.getText().toString();
        decryptStart(null);
    }

    private void decryptFromClipboardClicked() {
        CharSequence clipboardText = ClipboardReflection.getClipboardText(getActivity());

        // only decrypt if clipboard content is available and a pgp message or cleartext signature
        if (clipboardText != null) {
            Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(clipboardText);
            if (!matcher.matches()) {
                matcher = PgpHelper.PGP_CLEARTEXT_SIGNATURE.matcher(clipboardText);
            }
            if (matcher.matches()) {
                mCiphertext = matcher.group(1);
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

    @Override
    protected void decryptStart(String passphrase) {
        Log.d(Constants.TAG, "decryptStart");

        // Send all information needed to service to decrypt in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        // fill values for this action
        Bundle data = new Bundle();

        intent.setAction(KeychainIntentService.ACTION_DECRYPT_VERIFY);

        // data
        data.putInt(KeychainIntentService.TARGET, KeychainIntentService.TARGET_BYTES);
        data.putByteArray(KeychainIntentService.DECRYPT_CIPHERTEXT_BYTES, mCiphertext.getBytes());
        data.putString(KeychainIntentService.DECRYPT_PASSPHRASE, passphrase);

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
                    } else if (PgpDecryptVerifyResult.SYMMETRIC_PASSHRASE_NEEDED ==
                                    decryptVerifyResult.getStatus()) {
                        showPassphraseDialog(Id.key.symmetric);
                    } else {
                        byte[] decryptedMessage = returnData
                                .getByteArray(KeychainIntentService.RESULT_DECRYPTED_BYTES);
                        mMessage.setText(new String(decryptedMessage));
                        mMessage.setHorizontallyScrolling(false);

                        // display signature result in activity
                        onResult(decryptVerifyResult);
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

}
