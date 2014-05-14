/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.widget.Toast;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.ui.adapter.PagerTabStripAdapter;
import org.sufficientlysecure.keychain.util.Log;

public class EncryptActivity extends DrawerActivity implements
        EncryptSymmetricFragment.OnSymmetricKeySelection,
        EncryptAsymmetricFragment.OnAsymmetricKeySelection,
        EncryptActivityInterface {

    /* Intents */
    public static final String ACTION_ENCRYPT = Constants.INTENT_PREFIX + "ENCRYPT";

    /* EXTRA keys for input */
    public static final String EXTRA_TEXT = "text";

    // enables ASCII Armor for file encryption when uri is given
    public static final String EXTRA_ASCII_ARMOR = "ascii_armor";

    // preselect ids, for internal use
    public static final String EXTRA_SIGNATURE_KEY_ID = "signature_key_id";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = "encryption_key_ids";

    // view
    ViewPager mViewPagerMode;
    PagerTabStrip mPagerTabStripMode;
    PagerTabStripAdapter mTabsAdapterMode;
    ViewPager mViewPagerContent;
    PagerTabStrip mPagerTabStripContent;
    PagerTabStripAdapter mTabsAdapterContent;

    // tabs
    Bundle mAsymmetricFragmentBundle = new Bundle();
    Bundle mSymmetricFragmentBundle = new Bundle();
    Bundle mMessageFragmentBundle = new Bundle();
    Bundle mFileFragmentBundle = new Bundle();
    int mSwitchToMode = PAGER_MODE_ASYMMETRIC;
    int mSwitchToContent = PAGER_CONTENT_MESSAGE;

    private static final int PAGER_MODE_ASYMMETRIC = 0;
    private static final int PAGER_MODE_SYMMETRIC = 1;
    private static final int PAGER_CONTENT_MESSAGE = 0;
    private static final int PAGER_CONTENT_FILE = 1;

    // model used by message and file fragments
    private long mEncryptionKeyIds[] = null;
    private long mSigningKeyId = Constants.key.none;
    private String mPassphrase;
    private String mPassphraseAgain;

    @Override
    public void onSigningKeySelected(long signingKeyId) {
        mSigningKeyId = signingKeyId;
    }

    @Override
    public void onEncryptionKeysSelected(long[] encryptionKeyIds) {
        mEncryptionKeyIds = encryptionKeyIds;
    }

    @Override
    public void onPassphraseUpdate(String passphrase) {
        mPassphrase = passphrase;
    }

    @Override
    public void onPassphraseAgainUpdate(String passphrase) {
        mPassphraseAgain = passphrase;
    }

    @Override
    public boolean isModeSymmetric() {
        if (PAGER_MODE_SYMMETRIC == mViewPagerMode.getCurrentItem()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public long getSignatureKey() {
        return mSigningKeyId;
    }

    @Override
    public long[] getEncryptionKeys() {
        return mEncryptionKeyIds;
    }

    @Override
    public String getPassphrase() {
        return mPassphrase;
    }

    @Override
    public String getPassphraseAgain() {
        return mPassphraseAgain;
    }


    private void initView() {
        mViewPagerMode = (ViewPager) findViewById(R.id.encrypt_pager_mode);
        mPagerTabStripMode = (PagerTabStrip) findViewById(R.id.encrypt_pager_tab_strip_mode);
        mViewPagerContent = (ViewPager) findViewById(R.id.encrypt_pager_content);
        mPagerTabStripContent = (PagerTabStrip) findViewById(R.id.encrypt_pager_tab_strip_content);

        mTabsAdapterMode = new PagerTabStripAdapter(this);
        mViewPagerMode.setAdapter(mTabsAdapterMode);
        mTabsAdapterContent = new PagerTabStripAdapter(this);
        mViewPagerContent.setAdapter(mTabsAdapterContent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.encrypt_activity);

        initView();

        // if called with an intent action, do not init drawer navigation
        if (ACTION_ENCRYPT.equals(getIntent().getAction())) {
            // TODO: back button to key?
        } else {
            setupDrawerNavigation(savedInstanceState);
        }

        // Handle intent actions
        handleActions(getIntent());

        mTabsAdapterMode.addTab(EncryptAsymmetricFragment.class,
                mAsymmetricFragmentBundle, getString(R.string.label_asymmetric));
        mTabsAdapterMode.addTab(EncryptSymmetricFragment.class,
                mSymmetricFragmentBundle, getString(R.string.label_symmetric));
        mViewPagerMode.setCurrentItem(mSwitchToMode);

        mTabsAdapterContent.addTab(EncryptMessageFragment.class,
                mMessageFragmentBundle, getString(R.string.label_message));
        mTabsAdapterContent.addTab(EncryptFileFragment.class,
                mFileFragmentBundle, getString(R.string.label_file));
        mViewPagerContent.setCurrentItem(mSwitchToContent);
    }

    /**
     * Handles all actions with this intent
     *
     * @param intent
     */
    private void handleActions(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        String type = intent.getType();
        Uri uri = intent.getData();

        if (extras == null) {
            extras = new Bundle();
        }

        /*
         * Android's Action
         */
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            // When sending to APG Encrypt via share menu
            if ("text/plain".equals(type)) {
                // Plain text
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // handle like normal text encryption, override action and extras to later
                    // executeServiceMethod ACTION_ENCRYPT in main actions
                    extras.putString(EXTRA_TEXT, sharedText);
                    extras.putBoolean(EXTRA_ASCII_ARMOR, true);
                    action = ACTION_ENCRYPT;
                }
            } else {
                // Files via content provider, override uri and action
                uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                action = ACTION_ENCRYPT;
            }
        }

        if (extras.containsKey(EXTRA_ASCII_ARMOR)) {
            boolean requestAsciiArmor = extras.getBoolean(EXTRA_ASCII_ARMOR, true);
            mFileFragmentBundle.putBoolean(EncryptFileFragment.ARG_ASCII_ARMOR, requestAsciiArmor);
        }

        String textData = extras.getString(EXTRA_TEXT);

        long signatureKeyId = extras.getLong(EXTRA_SIGNATURE_KEY_ID);
        long[] encryptionKeyIds = extras.getLongArray(EXTRA_ENCRYPTION_KEY_IDS);

        // preselect keys given by intent
        mAsymmetricFragmentBundle.putLongArray(EncryptAsymmetricFragment.ARG_ENCRYPTION_KEY_IDS,
                encryptionKeyIds);
        mAsymmetricFragmentBundle.putLong(EncryptAsymmetricFragment.ARG_SIGNATURE_KEY_ID,
                signatureKeyId);
        mSwitchToMode = PAGER_MODE_ASYMMETRIC;

        /**
         * Main Actions
         */
        if (ACTION_ENCRYPT.equals(action) && textData != null) {
            // encrypt text based on given extra
            mMessageFragmentBundle.putString(EncryptMessageFragment.ARG_TEXT, textData);
            mSwitchToContent = PAGER_CONTENT_MESSAGE;
        } else if (ACTION_ENCRYPT.equals(action) && uri != null) {
            // encrypt file based on Uri

            // get file path from uri
            String path = FileHelper.getPath(this, uri);

            if (path != null) {
                mFileFragmentBundle.putString(EncryptFileFragment.ARG_FILENAME, path);
                mSwitchToContent = PAGER_CONTENT_FILE;
            } else {
                Log.e(Constants.TAG,
                        "Direct binary data without actual file in filesystem is not supported " +
                                "by Intents. Please use the Remote Service API!"
                );
                Toast.makeText(this, R.string.error_only_files_are_supported,
                        Toast.LENGTH_LONG).show();
                // end activity
                finish();
            }
        } else {
            Log.e(Constants.TAG,
                    "Include the extra 'text' or an Uri with setData() in your Intent!");
        }
    }

}
