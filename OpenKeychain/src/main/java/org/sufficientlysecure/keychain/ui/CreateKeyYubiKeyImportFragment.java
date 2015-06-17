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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.KeychainService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.NfcListenerFragment;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.tor.OrbotHelper;


public class CreateKeyYubiKeyImportFragment extends Fragment implements NfcListenerFragment {

    private static final String ARG_FINGERPRINT = "fingerprint";
    public static final String ARG_AID = "aid";
    public static final String ARG_USER_ID = "user_ids";

    CreateKeyActivity mCreateKeyActivity;

    private byte[] mNfcFingerprints;
    private byte[] mNfcAid;
    private String mNfcUserId;
    private String mNfcFingerprint;
    private ImportKeysListFragment mListFragment;
    private TextView vSerNo;
    private TextView vUserId;

    public static Fragment createInstance(byte[] scannedFingerprints, byte[] nfcAid, String userId) {

        CreateKeyYubiKeyImportFragment frag = new CreateKeyYubiKeyImportFragment();

        Bundle args = new Bundle();
        args.putByteArray(ARG_FINGERPRINT, scannedFingerprints);
        args.putByteArray(ARG_AID, nfcAid);
        args.putString(ARG_USER_ID, userId);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();

        mNfcFingerprints = args.getByteArray(ARG_FINGERPRINT);
        mNfcAid = args.getByteArray(ARG_AID);
        mNfcUserId = args.getString(ARG_USER_ID);

        byte[] fp = new byte[20];
        ByteBuffer.wrap(fp).put(mNfcFingerprints, 0, 20);
        mNfcFingerprint = KeyFormattingUtils.convertFingerprintToHex(fp);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_yubikey_import_fragment, container, false);

        vSerNo = (TextView) view.findViewById(R.id.yubikey_serno);
        vUserId = (TextView) view.findViewById(R.id.yubikey_userid);

        {
            View mBackButton = view.findViewById(R.id.create_key_back_button);
            mBackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getFragmentManager().getBackStackEntryCount() == 0) {
                        getActivity().setResult(Activity.RESULT_CANCELED);
                        getActivity().finish();
                    } else {
                        mCreateKeyActivity.loadFragment(null, FragAction.TO_LEFT);
                    }
                }
            });

            View mNextButton = view.findViewById(R.id.create_key_next_button);
            mNextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    final Preferences.ProxyPrefs proxyPrefs = Preferences.getPreferences(getActivity()).getProxyPrefs();
                    Runnable ignoreTor = new Runnable() {
                        @Override
                        public void run() {
                            importKey(new ParcelableProxy(null, -1, null));
                        }
                    };

                    if (OrbotHelper.isOrbotInRequiredState(R.string.orbot_ignore_tor, ignoreTor, proxyPrefs,
                            getActivity())) {
                        importKey(proxyPrefs.parcelableProxy);
                    }
                }
            });
        }

        mListFragment = ImportKeysListFragment.newInstance(null, null,
                "0x" + mNfcFingerprint, true, null);

        view.findViewById(R.id.button_search).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final Preferences.ProxyPrefs proxyPrefs = Preferences.getPreferences(getActivity()).getProxyPrefs();
                Runnable ignoreTor = new Runnable() {
                    @Override
                    public void run() {
                        refreshSearch(new ParcelableProxy(null, -1, null));
                    }
                };

                if (OrbotHelper.isOrbotInRequiredState(R.string.orbot_ignore_tor, ignoreTor, proxyPrefs,
                        getActivity())) {
                    refreshSearch(proxyPrefs.parcelableProxy);
                }
            }
        });

        setData();

        getFragmentManager().beginTransaction()
                .replace(R.id.yubikey_import_fragment, mListFragment, "yubikey_import")
                .commit();

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle args) {
        super.onSaveInstanceState(args);

        args.putByteArray(ARG_FINGERPRINT, mNfcFingerprints);
        args.putByteArray(ARG_AID, mNfcAid);
        args.putString(ARG_USER_ID, mNfcUserId);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

    public void setData() {
        String serno = Hex.toHexString(mNfcAid, 10, 4);
        vSerNo.setText(getString(R.string.yubikey_serno, serno));

        if (!mNfcUserId.isEmpty()) {
            vUserId.setText(getString(R.string.yubikey_key_holder, mNfcUserId));
        } else {
            vUserId.setText(getString(R.string.yubikey_key_holder_not_set));
        }
    }

    public void refreshSearch(ParcelableProxy parcelableProxy) {
        // TODO: PHILIP verify proxy implementation in YubiKey parts
        mListFragment.loadNew(new ImportKeysListFragment.CloudLoaderState("0x" + mNfcFingerprint,
                Preferences.getPreferences(getActivity()).getCloudSearchPrefs()), parcelableProxy);
    }

    public void importKey(ParcelableProxy parcelableProxy) {

        // Message is received after decrypting is done in KeychainService
        ServiceProgressHandler saveHandler = new ServiceProgressHandler(getActivity()) {
            @Override
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    ImportKeyResult result =
                            returnData.getParcelable(DecryptVerifyResult.EXTRA_RESULT);

                    long[] masterKeyIds = result.getImportedMasterKeyIds();

                    // TODO handle masterKeyIds.length != 1...? sorta outlandish scenario

                    if (!result.success() || masterKeyIds.length == 0) {
                        result.createNotify(getActivity()).show();
                        return;
                    }

                    Intent intent = new Intent(getActivity(), ViewKeyActivity.class);
                    // use the imported masterKeyId, not the one from the yubikey, because
                    // that one might* just have been a subkey of the imported key
                    intent.setData(KeyRings.buildGenericKeyRingUri(masterKeyIds[0]));
                    intent.putExtra(ViewKeyActivity.EXTRA_DISPLAY_RESULT, result);
                    intent.putExtra(ViewKeyActivity.EXTRA_NFC_AID, mNfcAid);
                    intent.putExtra(ViewKeyActivity.EXTRA_NFC_USER_ID, mNfcUserId);
                    intent.putExtra(ViewKeyActivity.EXTRA_NFC_FINGERPRINTS, mNfcFingerprints);
                    startActivity(intent);
                    getActivity().finish();

                }

            }
        };

        // Send all information needed to service to decrypt in other thread
        Intent intent = new Intent(getActivity(), KeychainService.class);

        // fill values for this action
        Bundle data = new Bundle();

        intent.setAction(KeychainService.ACTION_IMPORT_KEYRING);

        ArrayList<ParcelableKeyRing> keyList = new ArrayList<>();
        keyList.add(new ParcelableKeyRing(mNfcFingerprint, null, null));
        data.putParcelableArrayList(KeychainService.IMPORT_KEY_LIST, keyList);

        {
            Preferences prefs = Preferences.getPreferences(getActivity());
            Preferences.CloudSearchPrefs cloudPrefs =
                    new Preferences.CloudSearchPrefs(true, true, prefs.getPreferredKeyserver());
            data.putString(KeychainService.IMPORT_KEY_SERVER, cloudPrefs.keyserver);
        }

        data.putParcelable(KeychainService.EXTRA_PARCELABLE_PROXY, parcelableProxy);

        intent.putExtra(KeychainService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainService.EXTRA_MESSENGER, messenger);

        saveHandler.showProgressDialog(
                getString(R.string.progress_importing),
                ProgressDialog.STYLE_HORIZONTAL, false
        );

        // start service with intent
        getActivity().startService(intent);

    }

    @Override
    public void onNfcPerform() throws IOException {

        mNfcFingerprints = mCreateKeyActivity.nfcGetFingerprints();
        mNfcAid = mCreateKeyActivity.nfcGetAid();
        mNfcUserId = mCreateKeyActivity.nfcGetUserId();

        byte[] fp = new byte[20];
        ByteBuffer.wrap(fp).put(mNfcFingerprints, 0, 20);
        mNfcFingerprint = KeyFormattingUtils.convertFingerprintToHex(fp);

        setData();

        Preferences.ProxyPrefs proxyPrefs = Preferences.getPreferences(getActivity()).getProxyPrefs();
        Runnable ignoreTor = new Runnable() {
            @Override
            public void run() {
                refreshSearch(new ParcelableProxy(null, -1, null));
            }
        };

        if (OrbotHelper.isOrbotInRequiredState(R.string.orbot_ignore_tor, ignoreTor, proxyPrefs,
                getActivity())) {
            refreshSearch(proxyPrefs.parcelableProxy);
        }

    }
}
