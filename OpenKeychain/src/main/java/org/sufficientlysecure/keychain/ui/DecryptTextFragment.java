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
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.util.Log;

public class DecryptTextFragment extends DecryptFragment {
    public static final String ARG_CIPHERTEXT = "ciphertext";

//    // view
    private TextView mMessage;
//    private View mDecryptButton;
//    private View mDecryptFromCLipboardButton;
//
//    // model
    private String mCiphertext;

    /**
     * Creates new instance of this fragment
     */
    public static DecryptTextFragment newInstance(String ciphertext) {
        DecryptTextFragment frag = new DecryptTextFragment();

        Bundle args = new Bundle();
        args.putString(ARG_CIPHERTEXT, ciphertext);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decrypt_text_fragment, container, false);

        mMessage = (TextView) view.findViewById(R.id.decrypt_text_plaintext);
//        mDecryptButton = view.findViewById(R.id.action_decrypt);
//        mDecryptFromCLipboardButton = view.findViewById(R.id.action_decrypt_from_clipboard);
//        mDecryptButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                decryptClicked();
//            }
//        });
//        mDecryptFromCLipboardButton.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                decryptFromClipboardClicked();
//            }
//        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String ciphertext = getArguments().getString(ARG_CIPHERTEXT);
        if (ciphertext != null) {
            mCiphertext = ciphertext;
            decryptStart(null);
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
        data.putInt(KeychainIntentService.TARGET, KeychainIntentService.IO_BYTES);
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

                    DecryptVerifyResult result =
                            returnData.getParcelable(KeychainIntentService.RESULT_DECRYPT_VERIFY_RESULT);

                    if (result.isPending()) {
                        switch (result.getResult()) {
                            case DecryptVerifyResult.RESULT_PENDING_ASYM_PASSPHRASE:
                                showPassphraseDialog(result.getKeyIdPassphraseNeeded());
                                return;
                            case DecryptVerifyResult.RESULT_PENDING_SYM_PASSPHRASE:
                                showPassphraseDialog(Constants.key.symmetric);
                                return;
                        }
                        // error, we can't work with this!
                        result.createNotify(getActivity()).show();
                        return;
                    }

                    byte[] decryptedMessage = returnData
                            .getByteArray(KeychainIntentService.RESULT_DECRYPTED_BYTES);
                    mMessage.setText(new String(decryptedMessage));
                    mMessage.setHorizontallyScrolling(false);

                    result.createNotify(getActivity()).show();

                    // display signature result in activity
                    onResult(result);
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
