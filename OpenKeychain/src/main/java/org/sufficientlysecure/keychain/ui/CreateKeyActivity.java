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
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.base.BaseNfcActivity;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.IOException;
import java.util.ArrayList;

public class CreateKeyActivity extends BaseNfcActivity {

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_FIRST_TIME = "first_time";
    public static final String EXTRA_ADDITIONAL_EMAILS = "additional_emails";
    public static final String EXTRA_PASSPHRASE = "passphrase";
    public static final String EXTRA_CREATE_YUBI_KEY = "create_yubi_key";
    public static final String EXTRA_YUBI_KEY_PIN = "yubi_key_pin";
    public static final String EXTRA_YUBI_KEY_ADMIN_PIN = "yubi_key_admin_pin";

    public static final String EXTRA_NFC_USER_ID = "nfc_user_id";
    public static final String EXTRA_NFC_AID = "nfc_aid";
    public static final String EXTRA_NFC_FINGERPRINTS = "nfc_fingerprints";

    public static final String FRAGMENT_TAG = "currentFragment";

    String mName;
    String mEmail;
    ArrayList<String> mAdditionalEmails;
    Passphrase mPassphrase;
    boolean mFirstTime;
    boolean mCreateYubiKey;
    Passphrase mYubiKeyPin;
    Passphrase mYubiKeyAdminPin;

    Fragment mCurrentFragment;


    byte[] mScannedFingerprints;
    byte[] mNfcAid;
    String mNfcUserId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // React on NDEF_DISCOVERED from Manifest
        // NOTE: ACTION_NDEF_DISCOVERED and not ACTION_TAG_DISCOVERED like in BaseNfcActivity
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {

            handleIntentInBackground(getIntent());

            setTitle(R.string.title_manage_my_keys);

            // done
            return;
        }

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            mName = savedInstanceState.getString(EXTRA_NAME);
            mEmail = savedInstanceState.getString(EXTRA_EMAIL);
            mAdditionalEmails = savedInstanceState.getStringArrayList(EXTRA_ADDITIONAL_EMAILS);
            mPassphrase = savedInstanceState.getParcelable(EXTRA_PASSPHRASE);
            mFirstTime = savedInstanceState.getBoolean(EXTRA_FIRST_TIME);
            mCreateYubiKey = savedInstanceState.getBoolean(EXTRA_CREATE_YUBI_KEY);
            mYubiKeyPin = savedInstanceState.getParcelable(EXTRA_YUBI_KEY_PIN);
            mYubiKeyAdminPin = savedInstanceState.getParcelable(EXTRA_YUBI_KEY_ADMIN_PIN);

            mCurrentFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        } else {

            Intent intent = getIntent();
            // Initialize members with default values for a new instance
            mName = intent.getStringExtra(EXTRA_NAME);
            mEmail = intent.getStringExtra(EXTRA_EMAIL);
            mFirstTime = intent.getBooleanExtra(EXTRA_FIRST_TIME, false);
            mCreateYubiKey = intent.getBooleanExtra(EXTRA_CREATE_YUBI_KEY, false);

            if (intent.hasExtra(EXTRA_NFC_FINGERPRINTS)) {
                byte[] nfcFingerprints = intent.getByteArrayExtra(EXTRA_NFC_FINGERPRINTS);
                String nfcUserId = intent.getStringExtra(EXTRA_NFC_USER_ID);
                byte[] nfcAid = intent.getByteArrayExtra(EXTRA_NFC_AID);

                if (containsKeys(nfcFingerprints)) {
                    Fragment frag = CreateYubiKeyImportFragment.newInstance(
                            nfcFingerprints, nfcAid, nfcUserId);
                    loadFragment(frag, FragAction.START);

                    setTitle(R.string.title_import_keys);
                } else {
//                    Fragment frag = CreateYubiKeyBlankFragment.newInstance();
//                    loadFragment(frag, FragAction.START);
//                    setTitle(R.string.title_manage_my_keys);
                    Notify.create(this,
                            "YubiKey key creation is currently not supported. Please follow our FAQ.",
                            Notify.Style.ERROR
                    ).show();
                }

                // done
                return;
            }

            // normal key creation
            CreateKeyStartFragment frag = CreateKeyStartFragment.newInstance();
            loadFragment(frag, FragAction.START);
        }

        if (mFirstTime) {
            setTitle(R.string.app_name);
            mToolbar.setNavigationIcon(null);
            mToolbar.setNavigationOnClickListener(null);
        } else {
            setTitle(R.string.title_manage_my_keys);
        }
    }

    @Override
    protected void doNfcInBackground() throws IOException {
        if (mCurrentFragment instanceof NfcListenerFragment) {
            ((NfcListenerFragment) mCurrentFragment).doNfcInBackground();
            return;
        }

        mScannedFingerprints = nfcGetFingerprints();
        mNfcAid = nfcGetAid();
        mNfcUserId = nfcGetUserId();
    }

    @Override
    protected void onNfcPostExecute() throws IOException {
        if (mCurrentFragment instanceof NfcListenerFragment) {
            ((NfcListenerFragment) mCurrentFragment).onNfcPostExecute();
            return;
        }

        if (containsKeys(mScannedFingerprints)) {
            try {
                long masterKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(mScannedFingerprints);
                CachedPublicKeyRing ring = new ProviderHelper(this).getCachedPublicKeyRing(masterKeyId);
                ring.getMasterKeyId();

                Intent intent = new Intent(this, ViewKeyActivity.class);
                intent.setData(KeyRings.buildGenericKeyRingUri(masterKeyId));
                intent.putExtra(ViewKeyActivity.EXTRA_NFC_AID, mNfcAid);
                intent.putExtra(ViewKeyActivity.EXTRA_NFC_USER_ID, mNfcUserId);
                intent.putExtra(ViewKeyActivity.EXTRA_NFC_FINGERPRINTS, mScannedFingerprints);
                startActivity(intent);
                finish();

            } catch (PgpKeyNotFoundException e) {
                Fragment frag = CreateYubiKeyImportFragment.newInstance(
                        mScannedFingerprints, mNfcAid, mNfcUserId);
                loadFragment(frag, FragAction.TO_RIGHT);
            }
        } else {
//            Fragment frag = CreateYubiKeyBlankFragment.newInstance();
//            loadFragment(frag, FragAction.TO_RIGHT);
            Notify.create(this,
                    "YubiKey key creation is currently not supported. Please follow our FAQ.",
                    Notify.Style.ERROR
            ).show();
        }
    }

    private boolean containsKeys(byte[] scannedFingerprints) {
        // If all fingerprint bytes are 0, the card contains no keys.
        boolean cardContainsKeys = false;
        for (byte b : scannedFingerprints) {
            if (b != 0) {
                cardContainsKeys = true;
                break;
            }
        }
        return cardContainsKeys;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(EXTRA_NAME, mName);
        outState.putString(EXTRA_EMAIL, mEmail);
        outState.putStringArrayList(EXTRA_ADDITIONAL_EMAILS, mAdditionalEmails);
        outState.putParcelable(EXTRA_PASSPHRASE, mPassphrase);
        outState.putBoolean(EXTRA_FIRST_TIME, mFirstTime);
        outState.putBoolean(EXTRA_CREATE_YUBI_KEY, mCreateYubiKey);
        outState.putParcelable(EXTRA_YUBI_KEY_PIN, mYubiKeyPin);
        outState.putParcelable(EXTRA_YUBI_KEY_ADMIN_PIN, mYubiKeyAdminPin);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.create_key_activity);
    }

    public enum FragAction {
        START,
        TO_RIGHT,
        TO_LEFT
    }

    public void loadFragment(Fragment fragment, FragAction action) {
        mCurrentFragment = fragment;

        // Add the fragment to the 'fragment_container' FrameLayout
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        switch (action) {
            case START:
                transaction.setCustomAnimations(0, 0);
                transaction.replace(R.id.create_key_fragment_container, fragment, FRAGMENT_TAG)
                        .commit();
                break;
            case TO_LEFT:
                getSupportFragmentManager().popBackStackImmediate();
                break;
            case TO_RIGHT:
                transaction.setCustomAnimations(R.anim.frag_slide_in_from_right, R.anim.frag_slide_out_to_left,
                        R.anim.frag_slide_in_from_left, R.anim.frag_slide_out_to_right);
                transaction.addToBackStack(null);
                transaction.replace(R.id.create_key_fragment_container, fragment, FRAGMENT_TAG)
                        .commit();
                break;

        }

        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();

    }

    interface NfcListenerFragment {
        public void doNfcInBackground() throws IOException;
        public void onNfcPostExecute() throws IOException;
    }

}
