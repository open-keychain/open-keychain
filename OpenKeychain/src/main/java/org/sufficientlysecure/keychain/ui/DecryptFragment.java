/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.util.ArrayList;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverAddress;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.livedata.GenericLiveData;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing.VerificationStatus;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.keyview.ViewKeyActivity;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


public abstract class DecryptFragment extends Fragment {
    public static final String ARG_DECRYPT_VERIFY_RESULT = "decrypt_verify_result";

    protected LinearLayout mResultLayout;
    protected ImageView mEncryptionIcon;
    protected TextView mEncryptionText;
    protected ImageView mSignatureIcon;
    protected TextView mSignatureText;
    protected View mSignatureLayout;
    protected TextView mSignatureName;
    protected TextView mSignatureEmail;
    protected TextView mSignatureAction;

    private OpenPgpSignatureResult mSignatureResult;
    private DecryptVerifyResult mDecryptVerifyResult;
    private ViewAnimator mOverlayAnimator;

    private CryptoOperationHelper<ImportKeyringParcel, ImportKeyResult> mImportOpHelper;
    private LiveData<UnifiedKeyInfo> unifiedKeyInfoLiveData;

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Activity activity = requireActivity();
        // NOTE: These views are inside the activity!
        mResultLayout = activity.findViewById(R.id.result_main_layout);
        mEncryptionIcon = activity.findViewById(R.id.result_encryption_icon);
        mEncryptionText = activity.findViewById(R.id.result_encryption_text);
        mSignatureIcon = activity.findViewById(R.id.result_signature_icon);
        mSignatureText = activity.findViewById(R.id.result_signature_text);
        mSignatureLayout = activity.findViewById(R.id.result_signature_layout);
        mSignatureName = activity.findViewById(R.id.result_signature_name);
        mSignatureEmail = activity.findViewById(R.id.result_signature_email);
        mSignatureAction = activity.findViewById(R.id.result_signature_action);
        mResultLayout.setVisibility(View.GONE);

        // Overlay
        mOverlayAnimator = (ViewAnimator) view;
        Button vErrorOverlayButton = view.findViewById(R.id.decrypt_error_overlay_button);
        vErrorOverlayButton.setOnClickListener(v -> mOverlayAnimator.setDisplayedChild(0));
    }

    private void showErrorOverlay(boolean overlay) {
        int child = overlay ? 1 : 0;
        if (mOverlayAnimator.getDisplayedChild() != child) {
            mOverlayAnimator.setDisplayedChild(child);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_DECRYPT_VERIFY_RESULT, mDecryptVerifyResult);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null) {
            return;
        }

        DecryptVerifyResult result = savedInstanceState.getParcelable(ARG_DECRYPT_VERIFY_RESULT);
        if (result != null) {
            loadVerifyResult(result);
        }
    }

    private void lookupUnknownKey(long unknownKeyId) {

        final ArrayList<ParcelableKeyRing> keyList;
        final HkpKeyserverAddress keyserver;

        // search config
        keyserver = Preferences.getPreferences(getActivity()).getPreferredKeyserver();

        {
            ParcelableKeyRing keyEntry = ParcelableKeyRing.createFromReference(null,
                    KeyFormattingUtils.convertKeyIdToHex(unknownKeyId), null, null);
            ArrayList<ParcelableKeyRing> selectedEntries = new ArrayList<>();
            selectedEntries.add(keyEntry);

            keyList = selectedEntries;
        }

        CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> callback
                = new CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult>() {

            @Override
            public ImportKeyringParcel createOperationInput() {
                return ImportKeyringParcel.createImportKeyringParcel(keyList, keyserver);
            }

            @Override
            public void onCryptoOperationSuccess(ImportKeyResult result) {
                result.createNotify(getActivity()).show();

                loadSignerKeyData();
            }

            @Override
            public void onCryptoOperationCancelled() {
                // do nothing
            }

            @Override
            public void onCryptoOperationError(ImportKeyResult result) {
                result.createNotify(getActivity()).show();
            }

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };

        mImportOpHelper = new CryptoOperationHelper<>(1, this, callback, R.string.progress_importing);

        mImportOpHelper.cryptoOperation();
    }

    private void showKey(long keyId) {
        KeyRepository keyRepository = KeyRepository.create(requireContext());
        Long masterKeyId = keyRepository.getMasterKeyIdBySubkeyId(keyId);
        if (masterKeyId == null) {
            Notify.create(getActivity(), R.string.error_key_not_found, Style.ERROR).show();
            return;
        }
        Intent viewKeyIntent = ViewKeyActivity.getViewKeyActivityIntent(requireActivity(), masterKeyId);
        startActivity(viewKeyIntent);
    }

    protected void loadVerifyResult(DecryptVerifyResult decryptVerifyResult) {

        mDecryptVerifyResult = decryptVerifyResult;
        mSignatureResult = decryptVerifyResult.getSignatureResult();
        OpenPgpDecryptionResult decryptionResult = decryptVerifyResult.getDecryptionResult();

        mResultLayout.setVisibility(View.VISIBLE);

        switch (decryptionResult.getResult()) {
            case OpenPgpDecryptionResult.RESULT_ENCRYPTED: {
                mEncryptionText.setText(R.string.decrypt_result_encrypted);
                KeyFormattingUtils.setStatusImage(getActivity(), mEncryptionIcon, mEncryptionText, State.ENCRYPTED);
                break;
            }

            case OpenPgpDecryptionResult.RESULT_INSECURE: {
                mEncryptionText.setText(R.string.decrypt_result_insecure);
                KeyFormattingUtils.setStatusImage(getActivity(), mEncryptionIcon, mEncryptionText, State.INSECURE);
                break;
            }

            default:
            case OpenPgpDecryptionResult.RESULT_NOT_ENCRYPTED: {
                mEncryptionText.setText(R.string.decrypt_result_not_encrypted);
                KeyFormattingUtils.setStatusImage(getActivity(), mEncryptionIcon, mEncryptionText, State.NOT_ENCRYPTED);
                break;
            }
        }

        if (mSignatureResult.getResult() == OpenPgpSignatureResult.RESULT_NO_SIGNATURE) {
            // no signature

            setSignatureLayoutVisibility(View.GONE);

            mSignatureText.setText(R.string.decrypt_result_no_signature);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.NOT_SIGNED);

            loadSignerKeyData();
            showErrorOverlay(false);

            onVerifyLoaded(true);
        } else {
            // signature present
            loadSignerKeyData();
        }
    }

    private void setSignatureLayoutVisibility(int visibility) {
        mSignatureLayout.setVisibility(visibility);
    }

    private void setShowAction(final long signatureKeyId) {
        mSignatureAction.setText(R.string.decrypt_result_action_show);
        mSignatureAction.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_vpn_key_grey_24dp, 0);
        mSignatureLayout.setOnClickListener(v -> showKey(signatureKeyId));
    }

    public void loadSignerKeyData() {
        if (unifiedKeyInfoLiveData != null) {
            unifiedKeyInfoLiveData.removeObservers(this);
            unifiedKeyInfoLiveData = null;
        }

        if (mSignatureResult == null || mSignatureResult.getResult() == OpenPgpSignatureResult.RESULT_NO_SIGNATURE) {
            setSignatureLayoutVisibility(View.GONE);
            return;
        }

        unifiedKeyInfoLiveData = new GenericLiveData<>(requireContext(), () -> {
            KeyRepository keyRepository = KeyRepository.create(requireContext());
            Long masterKeyId = keyRepository.getMasterKeyIdBySubkeyId(mSignatureResult.getKeyId());
            return keyRepository.getUnifiedKeyInfo(masterKeyId);
        });
        unifiedKeyInfoLiveData.observe(this, this::onLoadSignerKeyData);
    }

    public void onLoadSignerKeyData(UnifiedKeyInfo unifiedKeyInfo) {
        if (unifiedKeyInfo == null) {
            showUnknownKeyStatus();
            return;
        }

        long signatureKeyId = mSignatureResult.getKeyId();

        if (unifiedKeyInfo.name() != null) {
            mSignatureName.setText(unifiedKeyInfo.name());
        } else {
            mSignatureName.setText(R.string.user_id_no_name);
        }
        if (unifiedKeyInfo.email() != null) {
            mSignatureEmail.setText(unifiedKeyInfo.email());
        } else {
            mSignatureEmail.setText(KeyFormattingUtils.beautifyKeyIdWithPrefix(
                    mSignatureResult.getKeyId()));
        }

        // NOTE: Don't use revoked and expired fields from database, they don't show
        // revoked/expired subkeys
        boolean isRevoked = mSignatureResult.getResult() == OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED;
        boolean isExpired = mSignatureResult.getResult() == OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED;
        boolean isInsecure = mSignatureResult.getResult() == OpenPgpSignatureResult.RESULT_INVALID_KEY_INSECURE;
        boolean isVerified = unifiedKeyInfo.verified() == VerificationStatus.VERIFIED_SECRET;
        boolean isYours = unifiedKeyInfo.has_any_secret();

        if (isRevoked) {
            mSignatureText.setText(R.string.decrypt_result_signature_revoked_key);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.REVOKED);

            setSignatureLayoutVisibility(View.VISIBLE);
            setShowAction(signatureKeyId);

            onVerifyLoaded(true);

        } else if (isExpired) {
            mSignatureText.setText(R.string.decrypt_result_signature_expired_key);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.EXPIRED);

            setSignatureLayoutVisibility(View.VISIBLE);
            setShowAction(signatureKeyId);

            showErrorOverlay(false);

            onVerifyLoaded(true);

        } else if (isInsecure) {
            mSignatureText.setText(R.string.decrypt_result_insecure_cryptography);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.INSECURE);

            setSignatureLayoutVisibility(View.VISIBLE);
            setShowAction(signatureKeyId);

            showErrorOverlay(false);

            onVerifyLoaded(true);

        } else if (isYours) {

            mSignatureText.setText(R.string.decrypt_result_signature_secret);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.VERIFIED);

            setSignatureLayoutVisibility(View.VISIBLE);
            setShowAction(signatureKeyId);

            showErrorOverlay(false);

            onVerifyLoaded(true);

        } else if (isVerified) {
            mSignatureText.setText(R.string.decrypt_result_signature_certified);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.VERIFIED);

            setSignatureLayoutVisibility(View.VISIBLE);
            setShowAction(signatureKeyId);

            showErrorOverlay(false);

            onVerifyLoaded(true);

        } else {
            mSignatureText.setText(R.string.decrypt_result_signature_uncertified);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.UNVERIFIED);

            setSignatureLayoutVisibility(View.VISIBLE);
            setShowAction(signatureKeyId);

            showErrorOverlay(false);

            onVerifyLoaded(true);
        }

    }

    private void showUnknownKeyStatus() {

        final long signatureKeyId = mSignatureResult.getKeyId();

        int result = mSignatureResult.getResult();
        if (result != OpenPgpSignatureResult.RESULT_KEY_MISSING
                && result != OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE) {
            Timber.e("got missing status for non-missing key, shouldn't happen!");
        }

        String userId = mSignatureResult.getPrimaryUserId();
        OpenPgpUtils.UserId userIdSplit = KeyRing.splitUserId(userId);
        if (userIdSplit.name != null) {
            mSignatureName.setText(userIdSplit.name);
        } else {
            mSignatureName.setText(R.string.user_id_no_name);
        }
        if (userIdSplit.email != null) {
            mSignatureEmail.setText(userIdSplit.email);
        } else {
            mSignatureEmail.setText(KeyFormattingUtils.beautifyKeyIdWithPrefix(
                    mSignatureResult.getKeyId()));
        }

        switch (mSignatureResult.getResult()) {

            case OpenPgpSignatureResult.RESULT_KEY_MISSING: {
                mSignatureText.setText(R.string.decrypt_result_signature_missing_key);
                KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.UNKNOWN_KEY);

                setSignatureLayoutVisibility(View.VISIBLE);
                mSignatureAction.setText(R.string.decrypt_result_action_Lookup);
                mSignatureAction
                        .setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_file_download_grey_24dp, 0);
                mSignatureLayout.setOnClickListener(v -> lookupUnknownKey(signatureKeyId));

                showErrorOverlay(false);

                onVerifyLoaded(true);

                break;
            }

            case OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE: {
                mSignatureText.setText(R.string.decrypt_result_invalid_signature);
                KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.INVALID);

                setSignatureLayoutVisibility(View.GONE);

                showErrorOverlay(true);

                onVerifyLoaded(false);
                break;
            }

        }

    }

    protected abstract void onVerifyLoaded(boolean hideErrorOverlay);

    public void startDisplayLogActivity() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        Intent intent = new Intent(activity, LogDisplayActivity.class);
        intent.putExtra(LogDisplayFragment.EXTRA_RESULT, mDecryptVerifyResult);
        activity.startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mImportOpHelper != null) {
            mImportOpHelper.handleActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
