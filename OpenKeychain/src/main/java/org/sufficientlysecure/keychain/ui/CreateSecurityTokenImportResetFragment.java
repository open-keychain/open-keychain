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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.SecurityTokenListenerFragment;
import org.sufficientlysecure.keychain.ui.base.QueueingCryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Preferences;


public class CreateSecurityTokenImportResetFragment
        extends QueueingCryptoOperationFragment<ImportKeyringParcel, ImportKeyResult>
        implements SecurityTokenListenerFragment {

    private static final int REQUEST_CODE_RESET = 0x00005001;

    private static final String ARG_FINGERPRINTS = "fingerprint";
    public static final String ARG_AID = "aid";
    public static final String ARG_USER_ID = "user_ids";

    CreateKeyActivity mCreateKeyActivity;

    private byte[] mTokenFingerprints;
    private byte[] mTokenAid;
    private String mTokenUserId;
    private String mTokenFingerprint;
    private ImportKeysListFragment mListFragment;
    private TextView vSerNo;
    private TextView vUserId;
    private TextView mNextButton;
    private RadioButton mRadioImport;
    private RadioButton mRadioReset;
    private View mResetWarning;

    // for CryptoOperationFragment key import
    private String mKeyserver;
    private ArrayList<ParcelableKeyRing> mKeyList;

    public static Fragment newInstance(byte[] scannedFingerprints, byte[] nfcAid, String userId) {

        CreateSecurityTokenImportResetFragment frag = new CreateSecurityTokenImportResetFragment();

        Bundle args = new Bundle();
        args.putByteArray(ARG_FINGERPRINTS, scannedFingerprints);
        args.putByteArray(ARG_AID, nfcAid);
        args.putString(ARG_USER_ID, userId);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();

        mTokenFingerprints = args.getByteArray(ARG_FINGERPRINTS);
        mTokenAid = args.getByteArray(ARG_AID);
        mTokenUserId = args.getString(ARG_USER_ID);

        byte[] fp = new byte[20];
        ByteBuffer.wrap(fp).put(mTokenFingerprints, 0, 20);
        mTokenFingerprint = KeyFormattingUtils.convertFingerprintToHex(fp);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_security_token_import_reset_fragment, container, false);

        vSerNo = (TextView) view.findViewById(R.id.token_serno);
        vUserId = (TextView) view.findViewById(R.id.token_userid);
        mNextButton = (TextView) view.findViewById(R.id.create_key_next_button);
        mRadioImport = (RadioButton) view.findViewById(R.id.token_decision_import);
        mRadioReset = (RadioButton) view.findViewById(R.id.token_decision_reset);
        mResetWarning = view.findViewById(R.id.token_import_reset_warning);

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

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRadioReset.isChecked()) {
                    resetCard();
                } else {
                    importKey();
                }
            }
        });

        mListFragment = ImportKeysListFragment.newInstance(null, null,
                "0x" + mTokenFingerprint, true, null);

        mRadioImport.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mNextButton.setText(R.string.btn_import);
                    mNextButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_key_plus_grey600_24dp, 0);
                    mNextButton.setVisibility(View.VISIBLE);
                    mResetWarning.setVisibility(View.GONE);

                    getFragmentManager().beginTransaction()
                            .replace(R.id.security_token_import_fragment, mListFragment, "token_import")
                            .commit();

                    getFragmentManager().executePendingTransactions();
                    refreshSearch();
                }
            }
        });
        mRadioReset.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mNextButton.setText(R.string.btn_reset);
                    mNextButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_close_grey_24dp, 0);
                    mNextButton.setVisibility(View.VISIBLE);
                    mResetWarning.setVisibility(View.VISIBLE);

                    getFragmentManager().beginTransaction()
                            .remove(mListFragment)
                            .commit();
                }
            }
        });

        setData();


        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle args) {
        super.onSaveInstanceState(args);

        args.putByteArray(ARG_FINGERPRINTS, mTokenFingerprints);
        args.putByteArray(ARG_AID, mTokenAid);
        args.putString(ARG_USER_ID, mTokenUserId);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

    public void setData() {
        String serno = Hex.toHexString(mTokenAid, 10, 4);
        vSerNo.setText(getString(R.string.security_token_serial_no, serno));

        if (!mTokenUserId.isEmpty()) {
            vUserId.setText(getString(R.string.security_token_key_holder, mTokenUserId));
        } else {
            vUserId.setText(getString(R.string.security_token_key_holder_not_set));
        }
    }

    public void refreshSearch() {
        mListFragment.loadNew(new ImportKeysListFragment.CloudLoaderState("0x" + mTokenFingerprint,
                Preferences.getPreferences(getActivity()).getCloudSearchPrefs()));
    }

    public void importKey() {

        ArrayList<ParcelableKeyRing> keyList = new ArrayList<>();
        keyList.add(new ParcelableKeyRing(mTokenFingerprint, null));
        mKeyList = keyList;

        mKeyserver = Preferences.getPreferences(getActivity()).getPreferredKeyserver();

        super.setProgressMessageResource(R.string.progress_importing);

        super.cryptoOperation();

    }

    public void resetCard() {
        Intent intent = new Intent(getActivity(), SecurityTokenOperationActivity.class);
        RequiredInputParcel resetP = RequiredInputParcel.createSecurityTokenReset();
        intent.putExtra(SecurityTokenOperationActivity.EXTRA_REQUIRED_INPUT, resetP);
        intent.putExtra(SecurityTokenOperationActivity.EXTRA_CRYPTO_INPUT, new CryptoInputParcel());
        startActivityForResult(intent, REQUEST_CODE_RESET);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_RESET && resultCode == Activity.RESULT_OK) {
            mCreateKeyActivity.loadFragment(null, FragAction.TO_LEFT);
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void doSecurityTokenInBackground() throws IOException {

        mTokenFingerprints = mCreateKeyActivity.getSecurityTokenHelper().getFingerprints();
        mTokenAid = mCreateKeyActivity.getSecurityTokenHelper().getAid();
        mTokenUserId = mCreateKeyActivity.getSecurityTokenHelper().getUserId();

        byte[] fp = new byte[20];
        ByteBuffer.wrap(fp).put(mTokenFingerprints, 0, 20);
        mTokenFingerprint = KeyFormattingUtils.convertFingerprintToHex(fp);
    }

    @Override
    public void onSecurityTokenPostExecute() {

        setData();

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

        Intent viewKeyIntent = new Intent(activity, ViewKeyActivity.class);
        // use the imported masterKeyId, not the one from the token, because
        // that one might* just have been a subkey of the imported key
        viewKeyIntent.setData(KeyRings.buildGenericKeyRingUri(masterKeyIds[0]));
        viewKeyIntent.putExtra(ViewKeyActivity.EXTRA_DISPLAY_RESULT, result);
        viewKeyIntent.putExtra(ViewKeyActivity.EXTRA_SECURITY_TOKEN_AID, mTokenAid);
        viewKeyIntent.putExtra(ViewKeyActivity.EXTRA_SECURITY_TOKEN_USER_ID, mTokenUserId);
        viewKeyIntent.putExtra(ViewKeyActivity.EXTRA_SECURITY_TOKEN_FINGERPRINTS, mTokenFingerprints);

        if (activity instanceof CreateKeyActivity) {
            ((CreateKeyActivity) activity).finishWithFirstTimeHandling(viewKeyIntent);
        } else {
            activity.startActivity(viewKeyIntent);
            activity.finish();
        }
    }
}
