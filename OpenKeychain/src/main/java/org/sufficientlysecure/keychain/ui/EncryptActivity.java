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
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;

import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.ui.adapter.PagerTabStripAdapter;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;

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
    //PagerTabStrip mPagerTabStripMode;
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
    private String mEncryptionUserIds[] = null;
    private long mSigningKeyId = Constants.key.none;
    private String mPassphrase;
    private String mPassphraseAgain;
    private int mCurrentMode = PAGER_MODE_ASYMMETRIC;
    private boolean mUseArmor;
    private boolean mDeleteAfterEncrypt = false;

    @Override
    public void onSigningKeySelected(long signingKeyId) {
        mSigningKeyId = signingKeyId;
    }

    @Override
    public void onEncryptionKeysSelected(long[] encryptionKeyIds) {
        mEncryptionKeyIds = encryptionKeyIds;
    }

    @Override
    public void onEncryptionUserSelected(String[] encryptionUserIds) {
        mEncryptionUserIds = encryptionUserIds;
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
        return PAGER_MODE_SYMMETRIC == mCurrentMode;
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
    public String[] getEncryptionUsers() {
        return mEncryptionUserIds;
    }

    @Override
    public String getPassphrase() {
        return mPassphrase;
    }

    @Override
    public String getPassphraseAgain() {
        return mPassphraseAgain;
    }

    @Override
    public boolean isUseArmor() {
        return mUseArmor;
    }

    @Override
    public boolean isDeleteAfterEncrypt() {
        return mDeleteAfterEncrypt;
    }

    private void initView() {
        mViewPagerMode = (ViewPager) findViewById(R.id.encrypt_pager_mode);
        //mPagerTabStripMode = (PagerTabStrip) findViewById(R.id.encrypt_pager_tab_strip_mode);
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

        mUseArmor = Preferences.getPreferences(this).getDefaultAsciiArmor();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.encrypt_activity, menu);
        menu.findItem(R.id.check_use_armor).setChecked(mUseArmor);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.isCheckable()) {
            item.setChecked(!item.isChecked());
        }
        switch (item.getItemId()) {
            case R.id.check_use_symmetric:
                mSwitchToMode = item.isChecked() ? PAGER_MODE_SYMMETRIC : PAGER_MODE_ASYMMETRIC;
                mViewPagerMode.setCurrentItem(mSwitchToMode);
                break;
            case R.id.check_use_armor:
                mUseArmor = item.isChecked();
                break;
            case R.id.check_delete_after_encrypt:
                mDeleteAfterEncrypt = item.isChecked();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
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
        ArrayList<Uri> uris = new ArrayList<Uri>();

        if (extras == null) {
            extras = new Bundle();
        }

        if (intent.getData() != null) {
            uris.add(intent.getData());
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
                uris.clear();
                uris.add(intent.<Uri>getParcelableExtra(Intent.EXTRA_STREAM));
                action = ACTION_ENCRYPT;
            }
        }

        if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            action = ACTION_ENCRYPT;
        }

        if (extras.containsKey(EXTRA_ASCII_ARMOR)) {
            mUseArmor = extras.getBoolean(EXTRA_ASCII_ARMOR, true);
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
        } else if (ACTION_ENCRYPT.equals(action) && uris != null && !uris.isEmpty()) {
            // encrypt file based on Uri
            mFileFragmentBundle.putParcelableArrayList(EncryptFileFragment.ARG_URIS, uris);
            mSwitchToContent = PAGER_CONTENT_FILE;
        } else {
            Log.e(Constants.TAG,
                    "Include the extra 'text' or an Uri with setData() in your Intent!");
        }
    }

}
