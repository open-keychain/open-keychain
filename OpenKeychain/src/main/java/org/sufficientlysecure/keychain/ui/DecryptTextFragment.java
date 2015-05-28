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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentService.IOType;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.ShareHelper;

import java.io.UnsupportedEncodingException;

public class DecryptTextFragment extends DecryptFragment {
    public static final String ARG_CIPHERTEXT = "ciphertext";
    public static final String ARG_SHOW_MENU = "show_menu";

    // view
    private TextView mText;

    // model
    private String mCiphertext;
    private boolean mShowMenuOptions;

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

        return view;
    }

    /**
     * Create Intent Chooser but exclude decrypt activites
     */
    private Intent sendWithChooserExcludingDecrypt(String text) {
        Intent prototype = createSendIntent(text);
        String title = getString(R.string.title_share_message);

        // we don't want to decrypt the decrypted, no inception ;)
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
        Notify.create(getActivity(), R.string.text_copied_to_clipboard, Notify.Style.OK).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        Bundle args = savedInstanceState == null ? getArguments() : savedInstanceState;
        mCiphertext = args.getString(ARG_CIPHERTEXT);
        mShowMenuOptions = args.getBoolean(ARG_SHOW_MENU, false);

        if (savedInstanceState == null) {
            cryptoOperation(new CryptoInputParcel());
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_CIPHERTEXT, mCiphertext);
        outState.putBoolean(ARG_SHOW_MENU, mShowMenuOptions);
        // no need to save the decrypted text, it's in the textview

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mShowMenuOptions) {
            inflater.inflate(R.menu.decrypt_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.decrypt_share: {
                startActivity(sendWithChooserExcludingDecrypt(mText.getText().toString()));
                break;
            }
            case R.id.decrypt_copy: {
                copyToClipboard(mText.getText().toString());
                break;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }

        return true;
    }

    @Override
    protected void cryptoOperation(CryptoInputParcel cryptoInput) {
        // Send all information needed to service to decrypt in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        // fill values for this action
        Bundle data = new Bundle();

        intent.setAction(KeychainIntentService.ACTION_DECRYPT_VERIFY);

        // data
        data.putParcelable(KeychainIntentService.EXTRA_CRYPTO_INPUT, cryptoInput);
        data.putInt(KeychainIntentService.TARGET, IOType.BYTES.ordinal());
        data.putByteArray(KeychainIntentService.DECRYPT_CIPHERTEXT_BYTES, mCiphertext.getBytes());
        data.putParcelable(KeychainIntentService.EXTRA_CRYPTO_INPUT, cryptoInput);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after encrypting is done in KeychainIntentService
        ServiceProgressHandler saveHandler = new ServiceProgressHandler(
                getActivity(),
                getString(R.string.progress_decrypting),
                ProgressDialog.STYLE_HORIZONTAL,
                ProgressDialogFragment.ServiceType.KEYCHAIN_INTENT) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                // handle pending messages
                if (handlePendingMessage(message)) {
                    return;
                }

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    DecryptVerifyResult pgpResult =
                            returnData.getParcelable(DecryptVerifyResult.EXTRA_RESULT);

                    if (pgpResult.success()) {
                        byte[] decryptedMessage = returnData
                                .getByteArray(KeychainIntentService.RESULT_DECRYPTED_BYTES);
                        String displayMessage;
                        if (pgpResult.getCharset() != null) {
                            try {
                                displayMessage = new String(decryptedMessage, pgpResult.getCharset());
                            } catch (UnsupportedEncodingException e) {
                                // if we can't decode properly, just fall back to utf-8
                                displayMessage = new String(decryptedMessage);
                            }
                        } else {
                            displayMessage = new String(decryptedMessage);
                        }
                        mText.setText(displayMessage);

                        // display signature result in activity
                        loadVerifyResult(pgpResult);
                    } else {
                        // TODO: show also invalid layout with different text?
                    }
                    pgpResult.createNotify(getActivity()).show(DecryptTextFragment.this);
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
    protected void onVerifyLoaded(boolean hideErrorOverlay) {
        mShowMenuOptions = hideErrorOverlay;
        getActivity().supportInvalidateOptionsMenu();
    }
}
