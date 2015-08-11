/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
package org.sufficientlysecure.keychain.ui.wizard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.ImportKeysListFragment;
import org.sufficientlysecure.keychain.ui.ViewKeyActivity;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class YubiKeyImportWizardFragment extends WizardFragment implements
        CreateKeyWizardActivity.NfcListenerFragment {
    private static final String ARG_FINGERPRINT = "fingerprint";
    public static final String ARG_AID = "aid";
    public static final String ARG_USER_ID = "user_ids";

    private byte[] mNfcFingerprints;
    private byte[] mNfcAid;
    private String mNfcUserId;
    private String mNfcFingerprint;
    private ImportKeysListFragment mListFragment;
    private TextView mYubiKeySerNo;
    private TextView mYubiKeyUserId;

    // for CryptoOperationFragment key import
    private String mKeyserver;
    private ArrayList<ParcelableKeyRing> mKeyList;

    public static YubiKeyImportWizardFragment newInstance(byte[] scannedFingerprints, byte[] nfcAid,
                                                          String userId) {
        YubiKeyImportWizardFragment yubiKeyImportWizardFragment = new YubiKeyImportWizardFragment();
        Bundle args = new Bundle();
        args.putByteArray(ARG_FINGERPRINT, scannedFingerprints);
        args.putByteArray(ARG_AID, nfcAid);
        args.putString(ARG_USER_ID, userId);
        yubiKeyImportWizardFragment.setArguments(args);
        return new YubiKeyImportWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();

        if (savedInstanceState != null) {
            updateNFCData(args.getByteArray(ARG_FINGERPRINT),
                    args.getByteArray(ARG_AID),
                    args.getString(ARG_USER_ID), true);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_yubi_import, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mYubiKeyUserId = (TextView) view.findViewById(R.id.yubikey_userid);
        mYubiKeySerNo = (TextView) view.findViewById(R.id.yubikey_serno);

        view.findViewById(R.id.button_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshSearch();
            }
        });

        mListFragment = ImportKeysListFragment.newInstance(null, null,
                "0x" + mNfcFingerprint, true, null);

        view.findViewById(R.id.button_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshSearch();
            }
        });

        getFragmentManager().beginTransaction()
                .replace(R.id.yubikey_import_fragment, mListFragment, "yubikey_import")
                .commit();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray(ARG_FINGERPRINT, mNfcFingerprints);
        outState.putByteArray(ARG_AID, mNfcAid);
        outState.putString(ARG_USER_ID, mNfcUserId);
    }

    @Override
    public boolean onBackClicked() {
        final Activity activity = getActivity();
        if (activity.getFragmentManager().getBackStackEntryCount() == 0) {
            activity.setResult(Activity.RESULT_CANCELED);
            activity.finish();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean onNextClicked() {
        importKey();
        return false;
    }

    public void refreshSearch() {
        mListFragment.loadNew(ImportKeysListFragment.newCloudLoaderStateInstance("0x" + mNfcFingerprint,
                        Preferences.getPreferences(getActivity()).getCloudSearchPrefs()));
    }

    public void importKey() {

        ArrayList<ParcelableKeyRing> keyList = new ArrayList<>();
        keyList.add(new ParcelableKeyRing(mNfcFingerprint,
                null, null));
        mKeyList = keyList;
        {
            Preferences prefs = Preferences.getPreferences(getActivity());
            Preferences.CloudSearchPrefs cloudPrefs =
                    new Preferences.CloudSearchPrefs(true, true, prefs.getPreferredKeyserver());

            mKeyserver = cloudPrefs.keyserver;
        }

        super.setProgressMessageResource(R.string.progress_importing);

        super.cryptoOperation();

    }

    public void updateUserID(CharSequence userID) {
        mYubiKeyUserId.setText(userID);
    }

    public void updateSerialNumber(CharSequence serialNumber) {
        mYubiKeySerNo.setText(serialNumber);
    }

    @Override
    public void onNfcError(Exception exception) {

    }

    @Override
    public void onNfcPreExecute() throws IOException {

    }

    @Override
    public void doNfcInBackground() throws IOException {
        updateNFCData(mWizardFragmentListener.nfcGetFingerprints(), mWizardFragmentListener.nfcGetAid(),
                mWizardFragmentListener.nfcGetUserId(), false);
    }

    @Override
    public void onNfcPostExecute() throws IOException {
        setData();
        refreshSearch();
    }

    @Override
    public void onNfcTagDiscovery(Intent intent) {

    }

    @Override
    public ImportKeyringParcel createOperationInput() {
        return new ImportKeyringParcel(mKeyList, mKeyserver);
    }

    @Override
    public void onQueuedOperationSuccess(ImportKeyResult result) {
        long[] masterKeyIds = result.getImportedMasterKeyIds();
        if (masterKeyIds.length == 0) {
            super.onCryptoOperationError(result);
            return;
        }

        // null-protected from Queueing*Fragment
        Activity activity = getActivity();

        Intent intent = new Intent(activity, ViewKeyActivity.class);
        // use the imported masterKeyId, not the one from the yubikey, because
        // that one might* just have been a subkey of the imported key
        intent.setData(KeychainContract.KeyRings.buildGenericKeyRingUri(masterKeyIds[0]));
        intent.putExtra(ViewKeyActivity.EXTRA_DISPLAY_RESULT, result);
        intent.putExtra(ViewKeyActivity.EXTRA_NFC_AID, mNfcAid);
        intent.putExtra(ViewKeyActivity.EXTRA_NFC_USER_ID, mNfcUserId);
        intent.putExtra(ViewKeyActivity.EXTRA_NFC_FINGERPRINTS, mNfcFingerprints);
        startActivity(intent);
        activity.finish();
    }

    /**
     * Updates the nfc data.
     *
     * @param nfcFingerprints
     * @param nfcAid
     * @param nfcUserId
     */
    public void updateNFCData(byte[] nfcFingerprints, byte[] nfcAid, String nfcUserId, boolean setData) {
        mNfcFingerprints = nfcFingerprints;
        mNfcAid = nfcAid;
        mNfcUserId = nfcUserId;
        mNfcFingerprint = KeyFormattingUtils.convertFingerprintToHex(mNfcFingerprints);

        byte[] fp = new byte[20];
        ByteBuffer.wrap(fp).put(mNfcFingerprints, 0, 20);
        mNfcFingerprint = KeyFormattingUtils.convertFingerprintToHex(fp);

        if (setData) {
            setData();
        }
    }

    /**
     * Updates Yubi Key display data,
     */
    public void setData() {
        final Activity activity = getActivity();
        String serialNumber = Hex.toHexString(mNfcAid, 10, 4);
        updateSerialNumber(activity.getString(R.string.yubikey_serno,
                serialNumber));

        if (!mNfcUserId.isEmpty()) {
            updateUserID(activity.getString(R.string.yubikey_key_holder,
                    mNfcUserId));
        } else {
            updateUserID(activity.getString(R.string.yubikey_key_holder_not_set));
        }
    }
}
