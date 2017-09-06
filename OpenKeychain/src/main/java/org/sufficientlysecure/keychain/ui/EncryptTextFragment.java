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


import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.PgpSecurityConstants.OpenKeychainCompressionAlgorithmTags;
import org.sufficientlysecure.keychain.pgp.PgpSignEncryptData;
import org.sufficientlysecure.keychain.pgp.SignEncryptParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.CachingCryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.ActionListener;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

public class EncryptTextFragment
        extends CachingCryptoOperationFragment<SignEncryptParcel, SignEncryptResult> {

    public static final String ARG_TEXT = "text";
    public static final String ARG_USE_COMPRESSION = "use_compression";
    public static final String ARG_RETURN_PROCESS_TEXT = "return_process_text";

    private boolean mShareAfterEncrypt;
    private boolean mReturnProcessTextAfterEncrypt;
    private boolean mUseCompression;
    private boolean mHiddenRecipients = false;

    private String mMessage = "";

    /**
     * Creates new instance of this fragment
     */
    public static EncryptTextFragment newInstance(String text, boolean returnProcessTextAfterEncrypt) {
        EncryptTextFragment frag = new EncryptTextFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TEXT, text);
        args.putBoolean(ARG_RETURN_PROCESS_TEXT, returnProcessTextAfterEncrypt);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if ( ! (context instanceof EncryptActivity) ) {
            throw new AssertionError(context + " must inherit from EncryptionActivity");
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
            mReturnProcessTextAfterEncrypt = getArguments().getBoolean(ARG_RETURN_PROCESS_TEXT, false);
        }

        Preferences prefs = Preferences.getPreferences(getActivity());

        Bundle args = savedInstanceState == null ? getArguments() : savedInstanceState;

        mUseCompression = args.getBoolean(ARG_USE_COMPRESSION, true);
        if (args.containsKey(ARG_USE_COMPRESSION)) {
            mUseCompression = args.getBoolean(ARG_USE_COMPRESSION, true);
        } else {
            mUseCompression = prefs.getTextUseCompression();
        }

        setHasOptionsMenu(true);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.encrypt_text_fragment, menu);

        menu.findItem(R.id.check_enable_compression).setChecked(mUseCompression);

        if (mReturnProcessTextAfterEncrypt) {
            menu.findItem(R.id.encrypt_paste).setVisible(true);
            menu.findItem(R.id.encrypt_copy).setVisible(false);
            menu.findItem(R.id.encrypt_share).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.check_enable_compression: {
                toggleEnableCompression(item, !item.isChecked());
                break;
            }
//            case R.id.check_hidden_recipients: {
//                mHiddenRecipients = item.isChecked();
//                notifyUpdate();
//                break;
//            }
            case R.id.encrypt_copy: {
                hideKeyboard();
                mShareAfterEncrypt = false;
                cryptoOperation(CryptoInputParcel.createCryptoInputParcel(new Date()));
                break;
            }
            case R.id.encrypt_share: {
                hideKeyboard();
                mShareAfterEncrypt = true;
                cryptoOperation(CryptoInputParcel.createCryptoInputParcel(new Date()));
                break;
            }
            case R.id.encrypt_paste: {
                hideKeyboard();
                cryptoOperation(CryptoInputParcel.createCryptoInputParcel(new Date()));
                break;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
        return true;
    }

    public void toggleEnableCompression(MenuItem item, final boolean compress) {

        mUseCompression = compress;
        item.setChecked(compress);

        Notify.create(getActivity(), compress
                        ? R.string.snack_compression_on
                        : R.string.snack_compression_off,
                Notify.LENGTH_LONG, Style.OK, new ActionListener() {
                    @Override
                    public void onAction() {
                        Preferences.getPreferences(getActivity()).setTextUseCompression(compress);
                        Notify.create(getActivity(), compress
                                        ? R.string.snack_compression_on
                                        : R.string.snack_compression_off,
                                Notify.LENGTH_SHORT, Style.OK, null, R.string.btn_saved)
                                .show(EncryptTextFragment.this, false);
                    }
                }, R.string.btn_save_default).show(this);

    }

    public SignEncryptParcel createOperationInput() {

        if (mMessage == null || mMessage.isEmpty()) {
            Notify.create(getActivity(), R.string.error_empty_text, Notify.Style.ERROR)
                    .show(this);
            return null;
        }

        // fill values for this action
        PgpSignEncryptData.Builder data = PgpSignEncryptData.builder();

        data.setCleartextSignature(true);

        if (!mUseCompression) {
            data.setCompressionAlgorithm(OpenKeychainCompressionAlgorithmTags.UNCOMPRESSED);
        }
        data.setHiddenRecipients(mHiddenRecipients);

        // Always use armor for messages
        data.setEnableAsciiArmorOutput(true);

        EncryptActivity modeInterface = (EncryptActivity) getActivity();
        EncryptModeFragment modeFragment = modeInterface.getModeFragment();

        if (modeFragment.isAsymmetric()) {
            long[] encryptionKeyIds = modeFragment.getAsymmetricEncryptionKeyIds();
            long signingKeyId = modeFragment.getAsymmetricSigningKeyId();

            boolean gotEncryptionKeys = (encryptionKeyIds != null
                    && encryptionKeyIds.length > 0);

            if (!gotEncryptionKeys && signingKeyId == Constants.key.none) {
                Notify.create(getActivity(), R.string.error_no_encryption_or_signature_key, Notify.Style.ERROR)
                        .show(this);
                return null;
            }

            data.setEncryptionMasterKeyIds(encryptionKeyIds);
            data.setSignatureMasterKeyId(signingKeyId);
            if (signingKeyId != Constants.key.none) {
                data.setAdditionalEncryptId(signingKeyId);
            }
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

        return SignEncryptParcel.createSignEncryptParcel(data.build(), mMessage.getBytes());
    }

    private void copyToClipboard(SignEncryptResult result) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        ClipboardManager clipMan = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipMan == null) {
            Notify.create(activity, R.string.error_clipboard_copy, Style.ERROR).show();
            return;
        }

        ClipData clip = ClipData.newPlainText(Constants.CLIPBOARD_LABEL, new String(result.getResultBytes()));
        clipMan.setPrimaryClip(clip);
        result.createNotify(activity).show();
    }

    private Intent createSendIntent(byte[] resultBytes) {
        Intent sendIntent;
        sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType(Constants.MIME_TYPE_TEXT);
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
            OpenPgpUtils.UserId userId = KeyRing.splitUserId(user);
            if (userId.email != null) {
                users.add(userId.email);
            }
        }
        // pass trough email addresses as extra for email applications
        sendIntent.putExtra(Intent.EXTRA_EMAIL, users.toArray(new String[users.size()]));

        return sendIntent;
    }

    @Override
    public void onQueuedOperationSuccess(SignEncryptResult result) {
        super.onQueuedOperationSuccess(result);

        hideKeyboard();

        if (mShareAfterEncrypt) {
            // Share encrypted message/file
            startActivity(Intent.createChooser(createSendIntent(result.getResultBytes()),
                    getString(R.string.title_share_message)));
        } else if (mReturnProcessTextAfterEncrypt) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(Intent.EXTRA_PROCESS_TEXT, new String(result.getResultBytes()));
            getActivity().setResult(Activity.RESULT_OK, resultIntent);
            getActivity().finish();
        } else {
            // Copy to clipboard
            copyToClipboard(result);
        }

    }

}
