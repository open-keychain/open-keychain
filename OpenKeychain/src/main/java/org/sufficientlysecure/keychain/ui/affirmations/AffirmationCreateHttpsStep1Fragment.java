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

package org.sufficientlysecure.keychain.ui.affirmations;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.pgp.affirmation.resources.GenericHttpsResource;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.NfcActivity;
import org.sufficientlysecure.keychain.ui.PassphraseDialogActivity;

import java.util.Date;

public class AffirmationCreateHttpsStep1Fragment extends Fragment {

    public static final int REQUEST_CODE_PASSPHRASE = 0x00008001;
    public static final int REQUEST_CODE_NFC = 0x00008002;

    AffirmationWizard mAffirmationWizard;

    String mProofUri, mProofText;

    EditText mEditUri;

    // For NFC data
    protected String mSigningKeyPassphrase = null;
    protected Date mNfcTimestamp = null;
    protected byte[] mNfcHash = null;

    /**
     * Creates new instance of this fragment
     */
    public static AffirmationCreateHttpsStep1Fragment newInstance() {
        AffirmationCreateHttpsStep1Fragment frag = new AffirmationCreateHttpsStep1Fragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAffirmationWizard = (AffirmationWizard) getActivity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.affirmation_create_https_fragment_step1, container, false);

        view.findViewById(R.id.next_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                String uri = "https://" + mEditUri.getText();

                if (!checkUri(uri)) {
                    return;
                }

                mProofUri = uri;
                mProofText = GenericHttpsResource.generate(mAffirmationWizard.mFingerprint, null);

                generateResourceAndNext();
            }
        });

        mEditUri = (EditText) view.findViewById(R.id.affirmation_create_https_uri);

        mEditUri.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String uri = "https://" + editable;
                if (uri.length() > 0) {
                    if (checkUri(uri)) {
                        mEditUri.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.uid_mail_ok, 0);
                    } else {
                        mEditUri.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                R.drawable.uid_mail_bad, 0);
                    }
                } else {
                    // remove drawable if email is empty
                    mEditUri.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
            }
        });

        mEditUri.setText("mugenguild.com/pgpkey.txt");

        return view;
    }

    public void generateResourceAndNext () {

        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);
        intent.setAction(KeychainIntentService.ACTION_SIGN_ENCRYPT);
        intent.putExtra(KeychainIntentService.EXTRA_DATA, createEncryptBundle());

        // Message is received after encrypting is done in KeychainIntentService
        KeychainIntentServiceHandler serviceHandler = new KeychainIntentServiceHandler(
                mAffirmationWizard, getString(R.string.progress_encrypting),
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    SignEncryptResult pgpResult =
                            message.getData().getParcelable(SignEncryptResult.EXTRA_RESULT);

                    if (pgpResult.isPending()) {
                        if ((pgpResult.getResult() & SignEncryptResult.RESULT_PENDING_PASSPHRASE) ==
                                SignEncryptResult.RESULT_PENDING_PASSPHRASE) {
                            startPassphraseDialog(pgpResult.getKeyIdPassphraseNeeded());
                        } else if ((pgpResult.getResult() & SignEncryptResult.RESULT_PENDING_NFC) ==
                                SignEncryptResult.RESULT_PENDING_NFC) {

                            mNfcTimestamp = pgpResult.getNfcTimestamp();
                            startNfcSign(pgpResult.getNfcKeyId(), pgpResult.getNfcPassphrase(), pgpResult.getNfcHash(), pgpResult.getNfcAlgo());
                        } else {
                            throw new RuntimeException("Unhandled pending result!");
                        }
                        return;
                    }

                    if (pgpResult.success()) {
                        String proofText = new String(
                                message.getData().getByteArray(KeychainIntentService.RESULT_BYTES));

                        AffirmationCreateHttpsStep2Fragment frag =
                                AffirmationCreateHttpsStep2Fragment.newInstance(mProofUri, proofText);

                        mAffirmationWizard.loadFragment(null, frag, AffirmationWizard.FRAG_ACTION_TO_RIGHT);
                    } else {
                        pgpResult.createNotify(getActivity()).show();
                    }

                    // no matter the result, reset parameters
                    mSigningKeyPassphrase = null;
                    mNfcHash = null;
                    mNfcTimestamp = null;
                }
            }
        };
        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(serviceHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        serviceHandler.showProgressDialog(getActivity());

        // start service with intent
        getActivity().startService(intent);

    }

    protected Bundle createEncryptBundle() {
        // fill values for this action
        Bundle data = new Bundle();

        data.putInt(KeychainIntentService.SOURCE, KeychainIntentService.IO_BYTES);
        data.putInt(KeychainIntentService.TARGET, KeychainIntentService.IO_BYTES);
        data.putByteArray(KeychainIntentService.ENCRYPT_MESSAGE_BYTES, mProofText.getBytes());

        // Always use armor for messages
        data.putBoolean(KeychainIntentService.ENCRYPT_USE_ASCII_ARMOR, true);

        data.putLong(KeychainIntentService.ENCRYPT_SIGNATURE_MASTER_ID, mAffirmationWizard.mMasterKeyId);
        data.putString(KeychainIntentService.ENCRYPT_SIGNATURE_KEY_PASSPHRASE, mSigningKeyPassphrase);
        data.putSerializable(KeychainIntentService.ENCRYPT_SIGNATURE_NFC_TIMESTAMP, mNfcTimestamp);
        data.putByteArray(KeychainIntentService.ENCRYPT_SIGNATURE_NFC_HASH, mNfcHash);

        return data;
    }

    protected void startPassphraseDialog(long subkeyId) {
        Intent intent = new Intent(getActivity(), PassphraseDialogActivity.class);
        intent.putExtra(PassphraseDialogActivity.EXTRA_SUBKEY_ID, subkeyId);
        startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);
    }

    protected void startNfcSign(long keyId, String pin, byte[] hashToSign, int hashAlgo) {
        // build PendingIntent for Yubikey NFC operations
        Intent intent = new Intent(getActivity(), NfcActivity.class);
        intent.setAction(NfcActivity.ACTION_SIGN_HASH);

        // pass params through to activity that it can be returned again later to repeat pgp operation
        intent.putExtra(NfcActivity.EXTRA_DATA, new Intent()); // not used, only relevant to OpenPgpService
        intent.putExtra(NfcActivity.EXTRA_KEY_ID, keyId);
        intent.putExtra(NfcActivity.EXTRA_PIN, pin);
        intent.putExtra(NfcActivity.EXTRA_NFC_HASH_TO_SIGN, hashToSign);
        intent.putExtra(NfcActivity.EXTRA_NFC_HASH_ALGO, hashAlgo);

        startActivityForResult(intent, REQUEST_CODE_NFC);
    }

    private static boolean checkUri(String uri) {
        return Patterns.WEB_URL.matcher(uri).matches();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PASSPHRASE: {
                if (resultCode == AffirmationWizard.RESULT_OK && data != null) {
                    mSigningKeyPassphrase = data.getStringExtra(PassphraseDialogActivity.MESSAGE_DATA_PASSPHRASE);
                    generateResourceAndNext();
                    return;
                }
                break;
            }

            case REQUEST_CODE_NFC: {
                if (resultCode == AffirmationWizard.RESULT_OK && data != null) {
                    mNfcHash = data.getByteArrayExtra(OpenPgpApi.EXTRA_NFC_SIGNED_HASH);
                    generateResourceAndNext();
                    return;
                }
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
                break;
            }
        }
    }


}
