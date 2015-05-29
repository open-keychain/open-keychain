/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.PgpConstants;
import org.sufficientlysecure.keychain.pgp.SignEncryptParcel;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.CachingCryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ShareHelper;

import java.util.HashSet;
import java.util.Set;

public class EncryptTextFragment extends CachingCryptoOperationFragment<SignEncryptParcel> {

    public static final String ARG_TEXT = "text";
    public static final String ARG_USE_COMPRESSION = "use_compression";

    private boolean mShareAfterEncrypt;
    private boolean mUseCompression;
    private boolean mHiddenRecipients = false;

    private String mMessage = "";

    /**
     * Creates new instance of this fragment
     */
    public static EncryptTextFragment newInstance(String text) {
        EncryptTextFragment frag = new EncryptTextFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TEXT, text);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if ( ! (activity instanceof EncryptActivity) ) {
            throw new AssertionError(activity + " must inherit from EncryptionActivity");
        }
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_text_fragment, container, false);

        TextView textView = (TextView) view.findViewById(R.id.encrypt_text_text);
        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mMessage = s.toString();
            }
        });

        // set initial text
        if (mMessage != null) {
            textView.setText(mMessage);
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_USE_COMPRESSION, mUseCompression);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mMessage = getArguments().getString(ARG_TEXT);
        }

        Bundle args = savedInstanceState == null ? getArguments() : savedInstanceState;
        mUseCompression = args.getBoolean(ARG_USE_COMPRESSION, true);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.encrypt_text_fragment, menu);

        menu.findItem(R.id.check_enable_compression).setChecked(mUseCompression);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.isCheckable()) {
            item.setChecked(!item.isChecked());
        }
        switch (item.getItemId()) {
            case R.id.check_enable_compression: {
                mUseCompression = item.isChecked();
                break;
            }
//            case R.id.check_hidden_recipients: {
//                mHiddenRecipients = item.isChecked();
//                notifyUpdate();
//                break;
//            }
            case R.id.encrypt_copy: {
                cryptoOperation(false);
                break;
            }
            case R.id.encrypt_share: {
                cryptoOperation(true);
                break;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
        return true;
    }

    protected void onEncryptSuccess(SignEncryptResult result) {
        if (mShareAfterEncrypt) {
            // Share encrypted message/file
            startActivity(sendWithChooserExcludingEncrypt(result.getResultBytes()));
        } else {
            // Copy to clipboard
            copyToClipboard(result.getResultBytes());
            result.createNotify(getActivity()).show();
            // Notify.create(EncryptTextActivity.this,
            // R.string.encrypt_sign_clipboard_successful, Notify.Style.OK)
            // .show(getSupportFragmentManager().findFragmentById(R.id.encrypt_text_fragment));
        }
    }

    protected SignEncryptParcel createEncryptBundle() {

        if (mMessage == null || mMessage.isEmpty()) {
            Notify.create(getActivity(), R.string.error_empty_text, Notify.Style.ERROR)
                    .show(this);
            return null;
        }

        // fill values for this action
        SignEncryptParcel data = new SignEncryptParcel();

        data.setBytes(mMessage.getBytes());
        data.setCleartextSignature(true);

        if (mUseCompression) {
            data.setCompressionId(PgpConstants.sPreferredCompressionAlgorithms.get(0));
        } else {
            data.setCompressionId(CompressionAlgorithmTags.UNCOMPRESSED);
        }
        data.setHiddenRecipients(mHiddenRecipients);
        data.setSymmetricEncryptionAlgorithm(
                PgpConstants.OpenKeychainSymmetricKeyAlgorithmTags.USE_PREFERRED);
        data.setSignatureHashAlgorithm(
                PgpConstants.OpenKeychainSymmetricKeyAlgorithmTags.USE_PREFERRED);

        // Always use armor for messages
        data.setEnableAsciiArmorOutput(true);

        EncryptActivity modeInterface = (EncryptActivity) getActivity();
        EncryptModeFragment modeFragment = modeInterface.getModeFragment();

        if (modeFragment.isAsymmetric()) {
            long[] encryptionKeyIds = modeFragment.getAsymmetricEncryptionKeyIds();
            long signingKeyId = modeFragment.getAsymmetricSigningKeyId();

            boolean gotEncryptionKeys = (encryptionKeyIds != null
                    && encryptionKeyIds.length > 0);

            if (!gotEncryptionKeys && signingKeyId == 0L) {
                Notify.create(getActivity(), R.string.select_encryption_or_signature_key, Notify.Style.ERROR)
                        .show(this);
                return null;
            }

            data.setEncryptionMasterKeyIds(encryptionKeyIds);
            data.setSignatureMasterKeyId(signingKeyId);
        } else {
            Passphrase passphrase = modeFragment.getSymmetricPassphrase();
            if (passphrase == null) {
                Notify.create(getActivity(), R.string.passphrases_do_not_match, Notify.Style.ERROR)
                        .show(this);
                return null;
            }
            if (passphrase.isEmpty()) {
                Notify.create(getActivity(), R.string.passphrase_must_not_be_empty, Notify.Style.ERROR)
                        .show(this);
                return null;
            }
            data.setSymmetricPassphrase(passphrase);
        }
        return data;
    }

    private void copyToClipboard(byte[] resultBytes) {
        ClipboardReflection.copyToClipboard(getActivity(), new String(resultBytes));
    }

    /**
     * Create Intent Chooser but exclude OK's EncryptActivity.
     */
    private Intent sendWithChooserExcludingEncrypt(byte[] resultBytes) {
        Intent prototype = createSendIntent(resultBytes);
        String title = getString(R.string.title_share_message);

        // we don't want to encrypt the encrypted, no inception ;)
        String[] blacklist = new String[]{
                Constants.PACKAGE_NAME + ".ui.EncryptTextActivity",
                "org.thialfihar.android.apg.ui.EncryptActivity"
        };

        return new ShareHelper(getActivity()).createChooserExcluding(prototype, title, blacklist);
    }

    private Intent createSendIntent(byte[] resultBytes) {
        Intent sendIntent;
        sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType(Constants.ENCRYPTED_TEXT_MIME);
        sendIntent.putExtra(Intent.EXTRA_TEXT, new String(resultBytes));

        EncryptActivity modeInterface = (EncryptActivity) getActivity();
        EncryptModeFragment modeFragment = modeInterface.getModeFragment();
        if (!modeFragment.isAsymmetric()) {
            return sendIntent;
        }

        String[] encryptionUserIds = modeFragment.getAsymmetricEncryptionUserIds();
        if (encryptionUserIds == null) {
            return sendIntent;
        }

        Set<String> users = new HashSet<>();
        for (String user : encryptionUserIds) {
            KeyRing.UserId userId = KeyRing.splitUserId(user);
            if (userId.email != null) {
                users.add(userId.email);
            }
        }
        // pass trough email addresses as extra for email applications
        sendIntent.putExtra(Intent.EXTRA_EMAIL, users.toArray(new String[users.size()]));

        return sendIntent;
    }

    public void cryptoOperation(boolean share) {
        mShareAfterEncrypt = share;
        cryptoOperation();
    }

    @Override
    protected void cryptoOperation(CryptoInputParcel cryptoInput, SignEncryptParcel actionsParcel) {

        if (actionsParcel == null) {

            actionsParcel = createEncryptBundle();
            // this is null if invalid, just return in that case
            if (actionsParcel == null) {
                // Notify was created by inputIsValid.
                return;
            }

            cacheActionsParcel(actionsParcel);
        }

        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);
        intent.setAction(KeychainIntentService.ACTION_SIGN_ENCRYPT);

        Bundle data = new Bundle();
        data.putParcelable(KeychainIntentService.SIGN_ENCRYPT_PARCEL, actionsParcel);
        data.putParcelable(KeychainIntentService.EXTRA_CRYPTO_INPUT, cryptoInput);
        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after encrypting is done in KeychainIntentService
        ServiceProgressHandler serviceHandler = new ServiceProgressHandler(
                getActivity(),
                getString(R.string.progress_encrypting),
                ProgressDialog.STYLE_HORIZONTAL,
                ProgressDialogFragment.ServiceType.KEYCHAIN_INTENT) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (handlePendingMessage(message)) {
                    return;
                }

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    SignEncryptResult result =
                            message.getData().getParcelable(SignEncryptResult.EXTRA_RESULT);

                    if (result.success()) {
                        onEncryptSuccess(result);
                    } else {
                        result.createNotify(getActivity()).show();
                    }
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

}
