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

import java.util.ArrayList;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.ui.base.CachingCryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.orbot.OrbotHelper;

public abstract class DecryptFragment
        extends CachingCryptoOperationFragment<PgpDecryptVerifyInputParcel, DecryptVerifyResult>
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int LOADER_ID_UNIFIED = 0;
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

    private LinearLayout mContentLayout;
    private LinearLayout mErrorOverlayLayout;

    private OpenPgpSignatureResult mSignatureResult;
    private DecryptVerifyResult mDecryptVerifyResult;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // NOTE: These views are inside the activity!
        mResultLayout = (LinearLayout) getActivity().findViewById(R.id.result_main_layout);
        mResultLayout.setVisibility(View.GONE);
        mEncryptionIcon = (ImageView) getActivity().findViewById(R.id.result_encryption_icon);
        mEncryptionText = (TextView) getActivity().findViewById(R.id.result_encryption_text);
        mSignatureIcon = (ImageView) getActivity().findViewById(R.id.result_signature_icon);
        mSignatureText = (TextView) getActivity().findViewById(R.id.result_signature_text);
        mSignatureLayout = getActivity().findViewById(R.id.result_signature_layout);
        mSignatureName = (TextView) getActivity().findViewById(R.id.result_signature_name);
        mSignatureEmail = (TextView) getActivity().findViewById(R.id.result_signature_email);
        mSignatureAction = (TextView) getActivity().findViewById(R.id.result_signature_action);

        // Overlay
        mContentLayout = (LinearLayout) view.findViewById(R.id.decrypt_content);
        mErrorOverlayLayout = (LinearLayout) view.findViewById(R.id.decrypt_error_overlay);
        Button vErrorOverlayButton = (Button) view.findViewById(R.id.decrypt_error_overlay_button);
        vErrorOverlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mErrorOverlayLayout.setVisibility(View.GONE);
                mContentLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
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

    private void lookupUnknownKey(long unknownKeyId, ParcelableProxy parcelableProxy) {

        // Message is received after importing is done in KeychainService
        ServiceProgressHandler serviceHandler = new ServiceProgressHandler(getActivity()) {
            @Override
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    if (returnData == null) {
                        return;
                    }

                    final ImportKeyResult result =
                            returnData.getParcelable(OperationResult.EXTRA_RESULT);

                    result.createNotify(getActivity()).show();

                    getLoaderManager().restartLoader(LOADER_ID_UNIFIED, null, DecryptFragment.this);
                }
            }
        };

        // fill values for this action
        Bundle data = new Bundle();

        // search config
        {
            Preferences prefs = Preferences.getPreferences(getActivity());
            Preferences.CloudSearchPrefs cloudPrefs =
                    new Preferences.CloudSearchPrefs(true, true, prefs.getPreferredKeyserver());
            data.putString(KeychainService.IMPORT_KEY_SERVER, cloudPrefs.keyserver);
        }

        {
            ParcelableKeyRing keyEntry = new ParcelableKeyRing(null,
                    KeyFormattingUtils.convertKeyIdToHex(unknownKeyId), null);
            ArrayList<ParcelableKeyRing> selectedEntries = new ArrayList<>();
            selectedEntries.add(keyEntry);

            data.putParcelableArrayList(KeychainService.IMPORT_KEY_LIST, selectedEntries);
        }

        data.putParcelable(KeychainService.EXTRA_PARCELABLE_PROXY, parcelableProxy);

        // Send all information needed to service to query keys in other thread
        Intent intent = new Intent(getActivity(), KeychainService.class);
        intent.setAction(KeychainService.ACTION_IMPORT_KEYRING);
        intent.putExtra(KeychainService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(serviceHandler);
        intent.putExtra(KeychainService.EXTRA_MESSENGER, messenger);

        getActivity().startService(intent);
    }

    private void showKey(long keyId) {
        try {

            Intent viewKeyIntent = new Intent(getActivity(), ViewKeyActivity.class);
            long masterKeyId = new ProviderHelper(getActivity()).getCachedPublicKeyRing(
                    KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(keyId)
            ).getMasterKeyId();
            viewKeyIntent.setData(KeyRings.buildGenericKeyRingUri(masterKeyId));
            startActivity(viewKeyIntent);

        } catch (PgpKeyNotFoundException e) {
            Notify.create(getActivity(), R.string.error_key_not_found, Style.ERROR);
        }
    }

    /**
     * @return returns false if signature is invalid, key is revoked or expired.
     */
    protected void loadVerifyResult(DecryptVerifyResult decryptVerifyResult) {

        mDecryptVerifyResult = decryptVerifyResult;
        mSignatureResult = decryptVerifyResult.getSignatureResult();

        mResultLayout.setVisibility(View.VISIBLE);

        // unsigned data
        if (mSignatureResult == null) {

            setSignatureLayoutVisibility(View.GONE);

            mSignatureText.setText(R.string.decrypt_result_no_signature);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.NOT_SIGNED);
            mEncryptionText.setText(R.string.decrypt_result_encrypted);
            KeyFormattingUtils.setStatusImage(getActivity(), mEncryptionIcon, mEncryptionText, State.ENCRYPTED);

            getLoaderManager().destroyLoader(LOADER_ID_UNIFIED);

            mErrorOverlayLayout.setVisibility(View.GONE);
            mContentLayout.setVisibility(View.VISIBLE);

            onVerifyLoaded(true);

            return;
        }

        if (mSignatureResult.isSignatureOnly()) {
            mEncryptionText.setText(R.string.decrypt_result_not_encrypted);
            KeyFormattingUtils.setStatusImage(getActivity(), mEncryptionIcon, mEncryptionText, State.NOT_ENCRYPTED);
        } else {
            mEncryptionText.setText(R.string.decrypt_result_encrypted);
            KeyFormattingUtils.setStatusImage(getActivity(), mEncryptionIcon, mEncryptionText, State.ENCRYPTED);
        }

        getLoaderManager().restartLoader(LOADER_ID_UNIFIED, null, this);
    }

    private void setSignatureLayoutVisibility(int visibility) {
        mSignatureLayout.setVisibility(visibility);
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

    // These are the rows that we will retrieve.
    static final String[] UNIFIED_PROJECTION = new String[]{
            KeychainContract.KeyRings._ID,
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.KeyRings.USER_ID,
            KeychainContract.KeyRings.VERIFIED,
            KeychainContract.KeyRings.HAS_ANY_SECRET,
    };

    @SuppressWarnings("unused")
    static final int INDEX_MASTER_KEY_ID = 1;
    static final int INDEX_USER_ID = 2;
    static final int INDEX_VERIFIED = 3;
    static final int INDEX_HAS_ANY_SECRET = 4;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != LOADER_ID_UNIFIED) {
            return null;
        }

        Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(
                mSignatureResult.getKeyId());
        return new CursorLoader(getActivity(), baseUri, UNIFIED_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (loader.getId() != LOADER_ID_UNIFIED) {
            return;
        }

        // If the key is unknown, show it as such
        if (data.getCount() == 0 || !data.moveToFirst()) {
            showUnknownKeyStatus();
            return;
        }

        long signatureKeyId = mSignatureResult.getKeyId();

        String userId = data.getString(INDEX_USER_ID);
        KeyRing.UserId userIdSplit = KeyRing.splitUserId(userId);
        if (userIdSplit.name != null) {
            mSignatureName.setText(userIdSplit.name);
        } else {
            mSignatureName.setText(R.string.user_id_no_name);
        }
        if (userIdSplit.email != null) {
            mSignatureEmail.setText(userIdSplit.email);
        } else {
            mSignatureEmail.setText(KeyFormattingUtils.beautifyKeyIdWithPrefix(
                    getActivity(), mSignatureResult.getKeyId()));
        }

        // NOTE: Don't use revoked and expired fields from database, they don't show
        // revoked/expired subkeys
        boolean isRevoked = mSignatureResult.getStatus() == OpenPgpSignatureResult.SIGNATURE_KEY_REVOKED;
        boolean isExpired = mSignatureResult.getStatus() == OpenPgpSignatureResult.SIGNATURE_KEY_EXPIRED;
        boolean isVerified = data.getInt(INDEX_VERIFIED) > 0;
        boolean isYours = data.getInt(INDEX_HAS_ANY_SECRET) != 0;

        if (isRevoked) {
            mSignatureText.setText(R.string.decrypt_result_signature_revoked_key);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.REVOKED);

            setSignatureLayoutVisibility(View.VISIBLE);
            setShowAction(signatureKeyId);

            mErrorOverlayLayout.setVisibility(View.VISIBLE);
            mContentLayout.setVisibility(View.GONE);

            onVerifyLoaded(false);

        } else if (isExpired) {
            mSignatureText.setText(R.string.decrypt_result_signature_expired_key);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.EXPIRED);

            setSignatureLayoutVisibility(View.VISIBLE);
            setShowAction(signatureKeyId);

            mErrorOverlayLayout.setVisibility(View.GONE);
            mContentLayout.setVisibility(View.VISIBLE);

            onVerifyLoaded(true);

        } else if (isYours) {

            mSignatureText.setText(R.string.decrypt_result_signature_secret);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.VERIFIED);

            setSignatureLayoutVisibility(View.VISIBLE);
            setShowAction(signatureKeyId);

            mErrorOverlayLayout.setVisibility(View.GONE);
            mContentLayout.setVisibility(View.VISIBLE);

            onVerifyLoaded(true);

        } else if (isVerified) {
            mSignatureText.setText(R.string.decrypt_result_signature_certified);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.VERIFIED);

            setSignatureLayoutVisibility(View.VISIBLE);
            setShowAction(signatureKeyId);

            mErrorOverlayLayout.setVisibility(View.GONE);
            mContentLayout.setVisibility(View.VISIBLE);

            onVerifyLoaded(true);

        } else {
            mSignatureText.setText(R.string.decrypt_result_signature_uncertified);
            KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.UNVERIFIED);

            setSignatureLayoutVisibility(View.VISIBLE);
            setShowAction(signatureKeyId);

            mErrorOverlayLayout.setVisibility(View.GONE);
            mContentLayout.setVisibility(View.VISIBLE);

            onVerifyLoaded(true);
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        if (loader.getId() != LOADER_ID_UNIFIED) {
            return;
        }

        setSignatureLayoutVisibility(View.GONE);
    }

    private void showUnknownKeyStatus() {

        final long signatureKeyId = mSignatureResult.getKeyId();

        int result = mSignatureResult.getStatus();
        if (result != OpenPgpSignatureResult.SIGNATURE_KEY_MISSING
                && result != OpenPgpSignatureResult.SIGNATURE_ERROR) {
            Log.e(Constants.TAG, "got missing status for non-missing key, shouldn't happen!");
        }

        String userId = mSignatureResult.getPrimaryUserId();
        KeyRing.UserId userIdSplit = KeyRing.splitUserId(userId);
        if (userIdSplit.name != null) {
            mSignatureName.setText(userIdSplit.name);
        } else {
            mSignatureName.setText(R.string.user_id_no_name);
        }
        if (userIdSplit.email != null) {
            mSignatureEmail.setText(userIdSplit.email);
        } else {
            mSignatureEmail.setText(KeyFormattingUtils.beautifyKeyIdWithPrefix(
                    getActivity(), mSignatureResult.getKeyId()));
        }

        switch (mSignatureResult.getStatus()) {

            case OpenPgpSignatureResult.SIGNATURE_KEY_MISSING: {
                mSignatureText.setText(R.string.decrypt_result_signature_missing_key);
                KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.UNKNOWN_KEY);

                setSignatureLayoutVisibility(View.VISIBLE);
                mSignatureAction.setText(R.string.decrypt_result_action_Lookup);
                mSignatureAction
                        .setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_file_download_grey_24dp, 0);
                mSignatureLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Preferences.ProxyPrefs proxyPrefs = Preferences.getPreferences(getActivity())
                                .getProxyPrefs();
                        Runnable ignoreTor = new Runnable() {
                            @Override
                            public void run() {
                                lookupUnknownKey(signatureKeyId, new ParcelableProxy(null, -1, null));
                            }
                        };

                        if (OrbotHelper.isOrbotInRequiredState(R.string.orbot_ignore_tor, ignoreTor, proxyPrefs,
                                getActivity())) {
                            lookupUnknownKey(signatureKeyId, proxyPrefs.parcelableProxy);
                        }
                    }
                });

                mErrorOverlayLayout.setVisibility(View.GONE);
                mContentLayout.setVisibility(View.VISIBLE);

                onVerifyLoaded(true);

                break;
            }

            case OpenPgpSignatureResult.SIGNATURE_ERROR: {
                mSignatureText.setText(R.string.decrypt_result_invalid_signature);
                KeyFormattingUtils.setStatusImage(getActivity(), mSignatureIcon, mSignatureText, State.INVALID);

                setSignatureLayoutVisibility(View.GONE);

                mErrorOverlayLayout.setVisibility(View.VISIBLE);
                mContentLayout.setVisibility(View.GONE);

                onVerifyLoaded(false);
                break;
            }

        }

    }

    protected abstract void onVerifyLoaded(boolean hideErrorOverlay);

}
