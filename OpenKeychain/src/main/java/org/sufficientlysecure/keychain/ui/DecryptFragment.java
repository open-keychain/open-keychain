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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.service.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

public abstract class DecryptFragment extends Fragment {
    private static final int RESULT_CODE_LOOKUP_KEY = 0x00007006;

    public static final int REQUEST_CODE_PASSPHRASE = 0x00008001;
    public static final int REQUEST_CODE_NFC_DECRYPT = 0x00008002;

    protected long mSignatureKeyId = 0;

    protected LinearLayout mResultLayout;

    protected ImageView mEncryptionIcon;
    protected TextView mEncryptionText;
    protected ImageView mSignatureIcon;
    protected TextView mSignatureText;

    protected View mSignatureLayout;
    protected View mSignatureDivider1;
    protected View mSignatureDivider2;
    protected TextView mSignatureName;
    protected TextView mSignatureEmail;
    protected TextView mSignatureAction;


    // State
    protected String mPassphrase;
    protected byte[] mNfcDecryptedSessionKey;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mResultLayout = (LinearLayout) getView().findViewById(R.id.result_main_layout);
        mResultLayout.setVisibility(View.GONE);

        mEncryptionIcon = (ImageView) getView().findViewById(R.id.result_encryption_icon);
        mEncryptionText = (TextView) getView().findViewById(R.id.result_encryption_text);
        mSignatureIcon = (ImageView) getView().findViewById(R.id.result_signature_icon);
        mSignatureText = (TextView) getView().findViewById(R.id.result_signature_text);
        mSignatureLayout = getView().findViewById(R.id.result_signature_layout);
        mSignatureDivider1 = getView().findViewById(R.id.result_signature_divider1);
        mSignatureDivider2 = getView().findViewById(R.id.result_signature_divider2);
        mSignatureName = (TextView) getView().findViewById(R.id.result_signature_name);
        mSignatureEmail = (TextView) getView().findViewById(R.id.result_signature_email);
        mSignatureAction = (TextView) getView().findViewById(R.id.result_signature_action);

    }

    private void lookupUnknownKey(long unknownKeyId) {
        Intent intent = new Intent(getActivity(), ImportKeysActivity.class);
        intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_KEYSERVER);
        intent.putExtra(ImportKeysActivity.EXTRA_KEY_ID, unknownKeyId);
        startActivityForResult(intent, RESULT_CODE_LOOKUP_KEY);
    }

    protected void startPassphraseDialog(long subkeyId) {
        Intent intent = new Intent(getActivity(), PassphraseDialogActivity.class);
        intent.putExtra(PassphraseDialogActivity.EXTRA_SUBKEY_ID, subkeyId);
        startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);
    }

    protected void startNfcDecrypt(long subKeyId, String pin, byte[] encryptedSessionKey) {
        // build PendingIntent for Yubikey NFC operations
        Intent intent = new Intent(getActivity(), NfcActivity.class);
        intent.setAction(NfcActivity.ACTION_DECRYPT_SESSION_KEY);
        intent.putExtra(NfcActivity.EXTRA_DATA, new Intent()); // not used, only relevant to OpenPgpService
        intent.putExtra(NfcActivity.EXTRA_KEY_ID, subKeyId);
        intent.putExtra(NfcActivity.EXTRA_PIN, pin);

        intent.putExtra(NfcActivity.EXTRA_NFC_ENC_SESSION_KEY, encryptedSessionKey);

        startActivityForResult(intent, REQUEST_CODE_NFC_DECRYPT);
    }

    protected void onResult(DecryptVerifyResult decryptVerifyResult) {
        final OpenPgpSignatureResult signatureResult = decryptVerifyResult.getSignatureResult();

        mSignatureKeyId = 0;
        mResultLayout.setVisibility(View.VISIBLE);
        if (signatureResult != null) {
            mSignatureKeyId = signatureResult.getKeyId();

            String userId = signatureResult.getPrimaryUserId();
            String[] userIdSplit = KeyRing.splitUserId(userId);
            if (userIdSplit[0] != null) {
                mSignatureName.setText(userIdSplit[0]);
            } else {
                mSignatureName.setText(R.string.user_id_no_name);
            }
            if (userIdSplit[1] != null) {
                mSignatureEmail.setText(userIdSplit[1]);
            } else {
                mSignatureEmail.setText(KeyFormattingUtils.beautifyKeyIdWithPrefix(getActivity(), mSignatureKeyId));
            }

            if (signatureResult.isSignatureOnly()) {
                mEncryptionText.setText(R.string.decrypt_result_not_encrypted);
                KeyFormattingUtils.setStatusImage(getActivity(), mEncryptionIcon, mEncryptionText, KeyFormattingUtils.STATE_NOT_ENCRYPTED);
            } else {
                mEncryptionText.setText(R.string.decrypt_result_encrypted);
                KeyFormattingUtils.setStatusImage(getActivity(), mEncryptionIcon, mEncryptionText, KeyFormattingUtils.STATE_ENCRYPTED);
            }

            switch (signatureResult.getStatus()) {
                case OpenPgpSignatureResult.SIGNATURE_SUCCESS_CERTIFIED: {
                    mSignatureText.setText(R.string.decrypt_result_signature_certified);
                    KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, KeyFormattingUtils.STATE_VERIFIED);

                    setSignatureLayoutVisibility(View.VISIBLE);
                    mSignatureAction.setText(R.string.decrypt_result_action_show);
                    mSignatureAction.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_action_accounts, 0);
                    mSignatureLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent viewKeyIntent = new Intent(getActivity(), ViewKeyActivity.class);
                            viewKeyIntent.setData(KeychainContract.KeyRings
                                    .buildGenericKeyRingUri(mSignatureKeyId));
                            startActivity(viewKeyIntent);
                        }
                    });
                    break;
                }

                case OpenPgpSignatureResult.SIGNATURE_SUCCESS_UNCERTIFIED: {
                    mSignatureText.setText(R.string.decrypt_result_signature_uncertified);
                    KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, KeyFormattingUtils.STATE_UNVERIFIED);

                    setSignatureLayoutVisibility(View.VISIBLE);
                    mSignatureAction.setText(R.string.decrypt_result_action_show);
                    mSignatureAction.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_action_accounts, 0);
                    mSignatureLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent viewKeyIntent = new Intent(getActivity(), ViewKeyActivity.class);
                            viewKeyIntent.setData(KeychainContract.KeyRings
                                    .buildGenericKeyRingUri(mSignatureKeyId));
                            startActivity(viewKeyIntent);
                        }
                    });
                    break;
                }

                case OpenPgpSignatureResult.SIGNATURE_KEY_MISSING: {
                    mSignatureText.setText(R.string.decrypt_result_signature_unknown_pub_key);
                    KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, KeyFormattingUtils.STATE_UNKNOWN_KEY);

                    setSignatureLayoutVisibility(View.VISIBLE);
                    mSignatureAction.setText(R.string.decrypt_result_action_Lookup);
                    mSignatureAction.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_action_download, 0);
                    mSignatureLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            lookupUnknownKey(mSignatureKeyId);
                        }
                    });
                    break;
                }

                // TODO: Maybe this should be part of the Result parcel, it is an error, not a valid status!
                case OpenPgpSignatureResult.SIGNATURE_ERROR: {
                    mSignatureText.setText(R.string.decrypt_result_invalid_signature);
                    KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, KeyFormattingUtils.STATE_INVALID);

                    setSignatureLayoutVisibility(View.GONE);
                    break;
                }
            }
        } else {
            setSignatureLayoutVisibility(View.GONE);

            mSignatureText.setText(R.string.decrypt_result_no_signature);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, KeyFormattingUtils.STATE_NOT_SIGNED);
            mEncryptionText.setText(R.string.decrypt_result_encrypted);
            KeyFormattingUtils.setStatusImage(getActivity(), mEncryptionIcon, mEncryptionText, KeyFormattingUtils.STATE_ENCRYPTED);
        }
    }

    private void setSignatureLayoutVisibility(int visibility) {
        mSignatureLayout.setVisibility(visibility);
        mSignatureDivider1.setVisibility(visibility);
        mSignatureDivider2.setVisibility(visibility);
    }

    /**
     * Should be overridden by MessageFragment and FileFragment to start actual decryption
     */
    protected abstract void decryptStart();

}
