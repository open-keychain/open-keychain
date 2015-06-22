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

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.PgpConstants;
import org.sufficientlysecure.keychain.pgp.SignEncryptParcel;
import org.sufficientlysecure.keychain.ui.base.CachingCryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.ActionListener;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.ShareHelper;

import java.util.HashSet;
import java.util.Set;

public class EncryptTextFragment
        extends CachingCryptoOperationFragment<SignEncryptParcel, SignEncryptResult> {

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
                mShareAfterEncrypt = false;
                cryptoOperation();
                break;
            }
            case R.id.encrypt_share: {
                mShareAfterEncrypt = true;
                cryptoOperation();
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

    protected SignEncryptParcel createOperationInput() {

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

            if (!gotEncryptionKeys && signingKeyId == Constants.key.none) {
                Notify.create(getActivity(), R.string.error_no_encryption_or_signature_key, Notify.Style.ERROR)
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

    @Override
    protected void onCryptoOperationSuccess(SignEncryptResult result) {

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

}
