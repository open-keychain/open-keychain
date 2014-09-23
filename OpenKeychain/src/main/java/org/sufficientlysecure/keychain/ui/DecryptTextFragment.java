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
import android.os.Message;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ShareHelper;

public class DecryptTextFragment extends DecryptFragment {
    public static final String ARG_CIPHERTEXT = "ciphertext";

    // view
    private TextView mText;
    private View mShareButton;
    private View mCopyButton;

    // model
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

        mText = (TextView) view.findViewById(R.id.decrypt_text_plaintext);
        mShareButton = view.findViewById(R.id.action_decrypt_share_plaintext);
        mCopyButton = view.findViewById(R.id.action_decrypt_copy_plaintext);
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(sendWithChooserExcludingEncrypt(mText.getText().toString()));
            }
        });
        mCopyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(mText.getText().toString());
            }
        });

        return view;
    }

    /**
     * Create Intent Chooser but exclude decrypt activites
     */
    private Intent sendWithChooserExcludingEncrypt(String text) {
        Intent prototype = createSendIntent(text);
        String title = getString(R.string.title_share_file);

        // we don't want to decrypt the decypted, no inception ;)
        String[] blacklist = new String[]{
                Constants.PACKAGE_NAME + ".ui.DecryptTextActivity",
                "org.thialfihar.android.apg.ui.DecryptActivity"
        };

        return new ShareHelper(getActivity()).createChooserExcluding(prototype, title, blacklist);
    }

    private Intent createSendIntent(String text) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        return sendIntent;
    }

    private void copyToClipboard(String text) {
        ClipboardReflection.copyToClipboard(getActivity(), text);
        Notify.showNotify(getActivity(), R.string.text_copied_to_clipboard, Notify.Style.INFO);
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

                    DecryptVerifyResult pgpResult =
                            returnData.getParcelable(DecryptVerifyResult.EXTRA_RESULT);

                    if (pgpResult.isPending()) {
                        if ((pgpResult.getResult() & DecryptVerifyResult.RESULT_PENDING_ASYM_PASSPHRASE) ==
                                DecryptVerifyResult.RESULT_PENDING_ASYM_PASSPHRASE) {
                            startPassphraseDialog(pgpResult.getKeyIdPassphraseNeeded());
                        } else if ((pgpResult.getResult() & DecryptVerifyResult.RESULT_PENDING_SYM_PASSPHRASE) ==
                                DecryptVerifyResult.RESULT_PENDING_SYM_PASSPHRASE) {
                            startPassphraseDialog(Constants.key.symmetric);
                        } else if ((pgpResult.getResult() & DecryptVerifyResult.RESULT_PENDING_NFC) ==
                                DecryptVerifyResult.RESULT_PENDING_NFC) {
                            // TODO
                        } else {
                            throw new RuntimeException("Unhandled pending result!");
                        }
                    } else if (pgpResult.success()) {

                        byte[] decryptedMessage = returnData
                                .getByteArray(KeychainIntentService.RESULT_DECRYPTED_BYTES);
                        mText.setText(new String(decryptedMessage));
                        mText.setHorizontallyScrolling(false);

                        pgpResult.createNotify(getActivity()).show();

                        // display signature result in activity
                        onResult(pgpResult);
                    } else {
                        pgpResult.createNotify(getActivity()).show();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_CODE_PASSPHRASE: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String passphrase = data.getStringExtra(PassphraseDialogActivity.MESSAGE_DATA_PASSPHRASE);
                    decryptStart(passphrase);
                } else {
                    getActivity().finish();
                }
                return;
            }

            case REQUEST_CODE_NFC: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // TODO
                } else {
                    getActivity().finish();
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

}
