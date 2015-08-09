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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.CreateKeyWizardActivity;
import org.sufficientlysecure.keychain.ui.ImportKeysListFragment;
import org.sufficientlysecure.keychain.ui.ViewKeyActivity;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.orbot.OrbotHelper;

import java.io.IOException;
import java.util.ArrayList;

public class YubiKeyImportWizardFragment extends WizardFragment implements
        CreateKeyWizardActivity.NfcListenerFragment,
        YubiKeyImportWizardFragmentViewModel.OnViewModelEventBind {
    private static final String ARG_FINGERPRINT = "fingerprint";
    public static final String ARG_AID = "aid";
    public static final String ARG_USER_ID = "user_ids";

    private ImportKeysListFragment mListFragment;
    private YubiKeyImportWizardFragmentViewModel mYubiKeyImportWizardFragmentViewModel;
    private TextView mYubiKeySerno;
    private TextView mYubiKeyUserid;

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
        mYubiKeyImportWizardFragmentViewModel = new YubiKeyImportWizardFragmentViewModel(this);
        mYubiKeyImportWizardFragmentViewModel.restoreViewModelState(savedInstanceState);
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

        mYubiKeyUserid = (TextView) view.findViewById(R.id.yubikey_userid);
        mYubiKeySerno = (TextView) view.findViewById(R.id.yubikey_serno);

        view.findViewById(R.id.button_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshSearch();
            }
        });

        mListFragment = ImportKeysListFragment.newInstance(null, null,
                "0x" + mYubiKeyImportWizardFragmentViewModel.getNfcFingerprint(), true, null);

        view.findViewById(R.id.button_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSearchClicked();
            }
        });

        getFragmentManager().beginTransaction()
                .replace(R.id.yubikey_import_fragment, mListFragment, "yubikey_import")
                .commit();


        mYubiKeyImportWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mYubiKeyImportWizardFragmentViewModel.saveViewModelState(outState);
    }

    public void onSearchClicked() {
        final Preferences.ProxyPrefs proxyPrefs = Preferences.getPreferences(getActivity()).getProxyPrefs();
        Runnable ignoreTor = new Runnable() {
            @Override
            public void run() {
                refreshSearch();
            }
        };

        if (OrbotHelper.putOrbotInRequiredState(R.string.orbot_ignore_tor, ignoreTor, proxyPrefs,
                getActivity())) {
            refreshSearch();
        }
    }

    @Override
    public boolean onBackClicked() {
        return mYubiKeyImportWizardFragmentViewModel.onBackClicked();
    }

    @Override
    public boolean onNextClicked() {
        importKey();
        return false;
    }

    public void refreshSearch() {
        mListFragment.loadNew(ImportKeysListFragment.
                newCloudLoaderStateInstance("0x" + mYubiKeyImportWizardFragmentViewModel.getNfcFingerprint(),
                        Preferences.getPreferences(getActivity()).getCloudSearchPrefs()));
    }

    public void importKey() {

        ArrayList<ParcelableKeyRing> keyList = new ArrayList<>();
        keyList.add(new ParcelableKeyRing(mYubiKeyImportWizardFragmentViewModel.getNfcFingerprint(),
                null, null));
        mYubiKeyImportWizardFragmentViewModel.setKeyList(keyList);

        {
            Preferences prefs = Preferences.getPreferences(getActivity());
            Preferences.CloudSearchPrefs cloudPrefs =
                    new Preferences.CloudSearchPrefs(true, true, prefs.getPreferredKeyserver());

            mYubiKeyImportWizardFragmentViewModel.setKeyserver(cloudPrefs.keyserver);
        }

        super.setProgressMessageResource(R.string.progress_importing);

        super.cryptoOperation();

    }

    @Override
    public void updateUserID(CharSequence userID) {
        mYubiKeyUserid.setText(userID);
    }

    @Override
    public void updateSerialNumber(CharSequence serialNumber) {
        mYubiKeySerno.setText(serialNumber);
    }

    @Override
    public void doNfcInBackground() throws IOException {
        mYubiKeyImportWizardFragmentViewModel.updateNFCData(mWizardFragmentListener.nfcGetFingerprints(),
                mWizardFragmentListener.nfcGetAid(),
                mWizardFragmentListener.nfcGetUserId(), false);
    }

    @Override
    public void onNfcPostExecute() throws IOException {
        mYubiKeyImportWizardFragmentViewModel.setData();

        Preferences.ProxyPrefs proxyPrefs = Preferences.getPreferences(getActivity()).getProxyPrefs();
        Runnable ignoreTor = new Runnable() {
            @Override
            public void run() {
                refreshSearch();
            }
        };

        if (OrbotHelper.putOrbotInRequiredState(R.string.orbot_ignore_tor, ignoreTor, proxyPrefs,
                getActivity())) {
            refreshSearch();
        }
    }

    @Override
    public ImportKeyringParcel createOperationInput() {
        return mYubiKeyImportWizardFragmentViewModel.createOperationInput();
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
        intent.putExtra(ViewKeyActivity.EXTRA_NFC_AID, mYubiKeyImportWizardFragmentViewModel.getNfcAid());
        intent.putExtra(ViewKeyActivity.EXTRA_NFC_USER_ID, mYubiKeyImportWizardFragmentViewModel.getNfcUserId());
        intent.putExtra(ViewKeyActivity.EXTRA_NFC_FINGERPRINTS, mYubiKeyImportWizardFragmentViewModel.getNfcFingerprint());
        startActivity(intent);
        activity.finish();
    }
}
