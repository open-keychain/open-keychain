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
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;

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

    private void showKey(long keyId) {
        Intent viewKeyIntent = new Intent(getActivity(), ViewKeyActivity.class);
        viewKeyIntent.setData(KeychainContract.KeyRings
                .buildGenericKeyRingUri(keyId));
        startActivity(viewKeyIntent);
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

    /**
     *
     * @return returns false if signature is invalid, key is revoked or expired.
     */
    protected boolean onResult(DecryptVerifyResult decryptVerifyResult) {
        final OpenPgpSignatureResult signatureResult = decryptVerifyResult.getSignatureResult();

        boolean valid = false;

        mSignatureKeyId = 0;
        mResultLayout.setVisibility(View.VISIBLE);
        if (signatureResult != null) {
            mSignatureKeyId = signatureResult.getKeyId();

            String userId = signatureResult.getPrimaryUserId();
            KeyRing.UserId userIdSplit = KeyRing.splitUserId(userId);
            if (userIdSplit.name != null) {
                mSignatureName.setText(userIdSplit.name);
            } else {
                mSignatureName.setText(R.string.user_id_no_name);
            }
            if (userIdSplit.email != null) {
                mSignatureEmail.setText(userIdSplit.email);
            } else {
                mSignatureEmail.setText(KeyFormattingUtils.beautifyKeyIdWithPrefix(getActivity(), mSignatureKeyId));
            }

            if (signatureResult.isSignatureOnly()) {
                mEncryptionText.setText(R.string.decrypt_result_not_encrypted);
                KeyFormattingUtils.setStatusImage(getActivity(), mEncryptionIcon, mEncryptionText, State.NOT_ENCRYPTED);
            } else {
                mEncryptionText.setText(R.string.decrypt_result_encrypted);
                KeyFormattingUtils.setStatusImage(getActivity(), mEncryptionIcon, mEncryptionText, State.ENCRYPTED);
            }

            switch (signatureResult.getStatus()) {
                case OpenPgpSignatureResult.SIGNATURE_SUCCESS_CERTIFIED: {
                    mSignatureText.setText(R.string.decrypt_result_signature_certified);
                    KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.VERIFIED);

                    setSignatureLayoutVisibility(View.VISIBLE);
                    setShowAction(mSignatureKeyId);

                    valid = true;
                    break;
                }

                case OpenPgpSignatureResult.SIGNATURE_SUCCESS_UNCERTIFIED: {
                    mSignatureText.setText(R.string.decrypt_result_signature_uncertified);
                    KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.UNVERIFIED);

                    setSignatureLayoutVisibility(View.VISIBLE);
                    setShowAction(mSignatureKeyId);

                    valid = true;
                    break;
                }

                case OpenPgpSignatureResult.SIGNATURE_KEY_MISSING: {
                    mSignatureText.setText(R.string.decrypt_result_signature_missing_key);
                    KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.UNKNOWN_KEY);

                    setSignatureLayoutVisibility(View.VISIBLE);
                    mSignatureAction.setText(R.string.decrypt_result_action_Lookup);
                    mSignatureAction.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_file_download_grey_24dp, 0);
                    mSignatureLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            lookupUnknownKey(mSignatureKeyId);
                        }
                    });

                    valid = true;
                    break;
                }

                case OpenPgpSignatureResult.SIGNATURE_KEY_EXPIRED: {
                    mSignatureText.setText(R.string.decrypt_result_signature_expired_key);
                    KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.EXPIRED);

                    setSignatureLayoutVisibility(View.VISIBLE);
                    setShowAction(mSignatureKeyId);

                    valid = false;
                    break;
                }

                case OpenPgpSignatureResult.SIGNATURE_KEY_REVOKED: {
                    mSignatureText.setText(R.string.decrypt_result_signature_revoked_key);
                    KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.REVOKED);

                    setSignatureLayoutVisibility(View.VISIBLE);
                    setShowAction(mSignatureKeyId);

                    valid = false;
                    break;
                }

                case OpenPgpSignatureResult.SIGNATURE_ERROR: {
                    mSignatureText.setText(R.string.decrypt_result_invalid_signature);
                    KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.INVALID);

                    setSignatureLayoutVisibility(View.GONE);

                    valid = false;
                    break;
                }
            }
        } else {
            setSignatureLayoutVisibility(View.GONE);

            mSignatureText.setText(R.string.decrypt_result_no_signature);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.NOT_SIGNED);
            mEncryptionText.setText(R.string.decrypt_result_encrypted);
            KeyFormattingUtils.setStatusImage(getActivity(), mEncryptionIcon, mEncryptionText, State.ENCRYPTED);

            valid = true;
        }

        return valid;
    }

    private void setSignatureLayoutVisibility(int visibility) {
        mSignatureLayout.setVisibility(visibility);
        mSignatureDivider1.setVisibility(visibility);
        mSignatureDivider2.setVisibility(visibility);
    }

    private void setShowAction(final long signatureKeyId) {
        mSignatureAction.setText(R.string.decrypt_result_action_show);
        mSignatureAction.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_vpn_key_grey_24dp, 0);
        mSignatureLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKey(signatureKeyId);
            }
        });
    }

    /**
     * Should be overridden by MessageFragment and FileFragment to start actual decryption
     */
    protected abstract void decryptStart();

}
