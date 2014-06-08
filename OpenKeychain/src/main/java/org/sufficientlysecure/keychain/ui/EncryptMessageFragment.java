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
import android.view.ViewGroup;
import android.widget.TextView;

import com.devspark.appmsg.AppMsg;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

public class EncryptMessageFragment extends Fragment {
    public static final String ARG_TEXT = "text";

    private TextView mMessage = null;
    private View mEncryptShare;
    private View mEncryptClipboard;

    private EncryptActivityInterface mEncryptInterface;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEncryptInterface = (EncryptActivityInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EncryptActivityInterface");
        }
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_message_fragment, container, false);

        mMessage = (TextView) view.findViewById(R.id.message);
        mEncryptClipboard = view.findViewById(R.id.action_encrypt_clipboard);
        mEncryptShare = view.findViewById(R.id.action_encrypt_share);
        mEncryptClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptClicked(true);
            }
        });
        mEncryptShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptClicked(false);
            }
        });

        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String text = getArguments().getString(ARG_TEXT);
        if (text != null) {
            mMessage.setText(text);
        }
    }

    /**
     * Fixes bad message characters for gmail
     *
     * @param message
     * @return
     */
    private String fixBadCharactersForGmail(String message) {
        // fix the message a bit, trailing spaces and newlines break stuff,
        // because GMail sends as HTML and such things fuck up the
        // signature,
        // TODO: things like "<" and ">" also fuck up the signature
        message = message.replaceAll(" +\n", "\n");
        message = message.replaceAll("\n\n+", "\n\n");
        message = message.replaceFirst("^\n+", "");
        // make sure there'll be exactly one newline at the end
        message = message.replaceFirst("\n*$", "\n");

        return message;
    }

    private void encryptClicked(final boolean toClipboard) {
        if (mEncryptInterface.isModeSymmetric()) {
            // symmetric encryption

            boolean gotPassphrase = (mEncryptInterface.getPassphrase() != null
                    && mEncryptInterface.getPassphrase().length() != 0);
            if (!gotPassphrase) {
                AppMsg.makeText(getActivity(), R.string.passphrase_must_not_be_empty, AppMsg.STYLE_ALERT)
                        .show();
                return;
            }

            if (!mEncryptInterface.getPassphrase().equals(mEncryptInterface.getPassphraseAgain())) {
                AppMsg.makeText(getActivity(), R.string.passphrases_do_not_match, AppMsg.STYLE_ALERT).show();
                return;
            }

        } else {
            // asymmetric encryption

            boolean gotEncryptionKeys = (mEncryptInterface.getEncryptionKeys() != null
                    && mEncryptInterface.getEncryptionKeys().length > 0);

            if (!gotEncryptionKeys && mEncryptInterface.getSignatureKey() == 0) {
                AppMsg.makeText(getActivity(), R.string.select_encryption_or_signature_key,
                        AppMsg.STYLE_ALERT).show();
                return;
            }

            if (mEncryptInterface.getSignatureKey() != 0 &&
                PassphraseCacheService.getCachedPassphrase(getActivity(),
                    mEncryptInterface.getSignatureKey()) == null) {
                PassphraseDialogFragment.show(getActivity(), mEncryptInterface.getSignatureKey(),
                    new Handler() {
                        @Override
                        public void handleMessage(Message message) {
                            if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                                encryptStart(toClipboard);
                            }
                        }
                    });

                return;
            }
        }

        encryptStart(toClipboard);
    }

    private void encryptStart(final boolean toClipboard) {
        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_ENCRYPT_SIGN);

        // fill values for this action
        Bundle data = new Bundle();

        data.putInt(KeychainIntentService.TARGET, KeychainIntentService.IO_BYTES);

        String message = mMessage.getText().toString();

        if (mEncryptInterface.isModeSymmetric()) {
            Log.d(Constants.TAG, "Symmetric encryption enabled!");
            String passphrase = mEncryptInterface.getPassphrase();
            if (passphrase.length() == 0) {
                passphrase = null;
            }
            data.putString(KeychainIntentService.ENCRYPT_SYMMETRIC_PASSPHRASE, passphrase);
        } else {
            data.putLong(KeychainIntentService.ENCRYPT_SIGNATURE_KEY_ID,
                mEncryptInterface.getSignatureKey());
            data.putLongArray(KeychainIntentService.ENCRYPT_ENCRYPTION_KEYS_IDS,
                mEncryptInterface.getEncryptionKeys());

            boolean signOnly = (mEncryptInterface.getEncryptionKeys() == null
                    || mEncryptInterface.getEncryptionKeys().length == 0);
            if (signOnly) {
                message = fixBadCharactersForGmail(message);
            }
        }

        data.putByteArray(KeychainIntentService.ENCRYPT_MESSAGE_BYTES, message.getBytes());

        data.putBoolean(KeychainIntentService.ENCRYPT_USE_ASCII_ARMOR, true);

        int compressionId = Preferences.getPreferences(getActivity()).getDefaultMessageCompression();
        data.putInt(KeychainIntentService.ENCRYPT_COMPRESSION_ID, compressionId);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after encrypting is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(getActivity(),
                getString(R.string.progress_encrypting), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle data = message.getData();

                    String output = new String(data.getByteArray(KeychainIntentService.RESULT_BYTES));
                    Log.d(Constants.TAG, "output: " + output);

                    if (toClipboard) {
                        ClipboardReflection.copyToClipboard(getActivity(), output);
                        AppMsg.makeText(getActivity(),
                                R.string.encrypt_sign_clipboard_successful, AppMsg.STYLE_INFO)
                                .show();
                    } else {
                        Intent sendIntent = new Intent(Intent.ACTION_SEND);

                        // Type is set to text/plain so that encrypted messages can
                        // be sent with Whatsapp, Hangouts, SMS etc...
                        sendIntent.setType("text/plain");

                        sendIntent.putExtra(Intent.EXTRA_TEXT, output);
                        startActivity(Intent.createChooser(sendIntent,
                                getString(R.string.title_share_with)));
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
