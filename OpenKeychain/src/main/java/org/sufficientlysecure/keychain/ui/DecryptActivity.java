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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.ui.adapter.PagerTabStripAdapter;
import org.sufficientlysecure.keychain.util.Log;

import java.util.regex.Matcher;

public class DecryptActivity extends DrawerActivity {

    /* Intents */
    public static final String ACTION_DECRYPT = Constants.INTENT_PREFIX + "DECRYPT";

    /* EXTRA keys for input */
    public static final String EXTRA_TEXT = "text";

    ViewPager mViewPager;
    PagerTabStrip mPagerTabStrip;
    PagerTabStripAdapter mTabsAdapter;

    Bundle mMessageFragmentBundle = new Bundle();
    Bundle mFileFragmentBundle = new Bundle();
    int mSwitchToTab = PAGER_TAB_MESSAGE;

    private static final int PAGER_TAB_MESSAGE = 0;
    private static final int PAGER_TAB_FILE = 1;

    private void initView() {
        mViewPager = (ViewPager) findViewById(R.id.decrypt_pager);
        mPagerTabStrip = (PagerTabStrip) findViewById(R.id.decrypt_pager_tab_strip);

        mTabsAdapter = new PagerTabStripAdapter(this);
        mViewPager.setAdapter(mTabsAdapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.decrypt_activity);

        initView();

        setupDrawerNavigation(savedInstanceState);

        // Handle intent actions, maybe changes the bundles
        handleActions(getIntent());

        mTabsAdapter.addTab(DecryptMessageFragment.class,
            mMessageFragmentBundle, getString(R.string.label_message));
        mTabsAdapter.addTab(DecryptFileFragment.class,
            mFileFragmentBundle, getString(R.string.label_file));
        mViewPager.setCurrentItem(mSwitchToTab);
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
            // When sending to Keychain Decrypt via share menu
            if ("text/plain".equals(type)) {
                // Plain text
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // handle like normal text decryption, override action and extras to later
                    // executeServiceMethod ACTION_DECRYPT in main actions
                    extras.putString(EXTRA_TEXT, sharedText);
                    action = ACTION_DECRYPT;
                }
            } else {
                // Binary via content provider (could also be files)
                // override uri to get stream from send
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                action = ACTION_DECRYPT;
            }
        } else if (Intent.ACTION_VIEW.equals(action)) {
            // Android's Action when opening file associated to Keychain (see AndroidManifest.xml)

            // override action
            action = ACTION_DECRYPT;
        }

        String textData = extras.getString(EXTRA_TEXT);

        /**
         * Main Actions
         */
        if (ACTION_DECRYPT.equals(action) && textData != null) {
            Log.d(Constants.TAG, "textData not null, matching text ...");
            Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(textData);
            if (matcher.matches()) {
                Log.d(Constants.TAG, "PGP_MESSAGE matched");
                textData = matcher.group(1);
                // replace non breakable spaces
                textData = textData.replaceAll("\\xa0", " ");

                mMessageFragmentBundle.putString(DecryptMessageFragment.ARG_CIPHERTEXT, textData);
                mSwitchToTab = PAGER_TAB_MESSAGE;
            } else {
                matcher = PgpHelper.PGP_CLEARTEXT_SIGNATURE.matcher(textData);
                if (matcher.matches()) {
                    Log.d(Constants.TAG, "PGP_CLEARTEXT_SIGNATURE matched");
                    textData = matcher.group(1);
                    // replace non breakable spaces
                    textData = textData.replaceAll("\\xa0", " ");

                    mMessageFragmentBundle.putString(DecryptMessageFragment.ARG_CIPHERTEXT, textData);
                    mSwitchToTab = PAGER_TAB_MESSAGE;
                } else {
                    Log.d(Constants.TAG, "Nothing matched!");
                }
            }
        } else if (ACTION_DECRYPT.equals(action) && uri != null) {
            mFileFragmentBundle.putParcelable(DecryptFileFragment.ARG_URI, uri);
            mSwitchToTab = PAGER_TAB_FILE;
        } else if (ACTION_DECRYPT.equals(action)) {
            Log.e(Constants.TAG,
                    "Include the extra 'text' or an Uri with setData() in your Intent!");
        }
    }

}
