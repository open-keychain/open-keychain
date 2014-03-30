/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.devspark.appmsg.AppMsg;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ActionBarHelper;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.ui.adapter.PagerTabStripAdapter;
import org.sufficientlysecure.keychain.util.Log;

import java.util.regex.Matcher;

public class DecryptActivity extends DrawerActivity {

    /* Intents */
    // without permission
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
        // Pager
        mViewPager = (ViewPager) findViewById(R.id.decrypt_pager);
        mPagerTabStrip = (PagerTabStrip) findViewById(R.id.decrypt_pager_tab_strip);

        mTabsAdapter = new PagerTabStripAdapter(this);
        mViewPager.setAdapter(mTabsAdapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.decrypt_activity);

        // set actionbar without home button if called from another app
        ActionBarHelper.setBackButton(this);

        initView();

        setupDrawerNavigation(savedInstanceState);

        // Handle intent actions, maybe changes the bundles
        handleActions(getIntent());

        mTabsAdapter.addTab(DecryptMessageFragment.class, mMessageFragmentBundle, getString(R.string.label_message));
        mTabsAdapter.addTab(DecryptFileFragment.class, mFileFragmentBundle, getString(R.string.label_file));
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
                uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
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
            // get file path from uri
            String path = FileHelper.getPath(this, uri);

            if (path != null) {
                mFileFragmentBundle.putString(DecryptFileFragment.ARG_FILENAME, path);
                mSwitchToTab = PAGER_TAB_FILE;
            } else {
                Log.e(Constants.TAG,
                        "Direct binary data without actual file in filesystem is not supported. Please use the Remote Service API!");
                Toast.makeText(this, R.string.error_only_files_are_supported, Toast.LENGTH_LONG)
                        .show();
                // end activity
                finish();
            }
        } else {
            Log.e(Constants.TAG,
                    "Include the extra 'text' or an Uri with setData() in your Intent!");
        }
    }

}
