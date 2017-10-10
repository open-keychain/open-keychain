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
import java.util.ArrayList;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.TaskStackBuilder;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.securitytoken.KeyFormat;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo;
import org.sufficientlysecure.keychain.ui.base.BaseSecurityTokenActivity;
import org.sufficientlysecure.keychain.ui.token.ManageSecurityTokenFragment;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

public class CreateKeyActivity extends BaseSecurityTokenActivity {

    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_FIRST_TIME = "first_time";
    public static final String EXTRA_ADDITIONAL_EMAILS = "additional_emails";
    public static final String EXTRA_PASSPHRASE = "passphrase";
    public static final String EXTRA_CREATE_SECURITY_TOKEN = "create_yubi_key";
    public static final String EXTRA_SECURITY_TOKEN_PIN = "yubi_key_pin";
    public static final String EXTRA_SECURITY_TOKEN_ADMIN_PIN = "yubi_key_admin_pin";

    public static final String EXTRA_SECURITY_TOKEN_INFO = "token_info";

    public static final String FRAGMENT_TAG = "currentFragment";

    String mName;
    String mEmail;
    ArrayList<String> mAdditionalEmails;
    Passphrase mPassphrase;
    boolean mFirstTime;
    boolean mCreateSecurityToken;
    Passphrase mSecurityTokenPin;
    Passphrase mSecurityTokenAdminPin;
    KeyFormat mSecurityTokenSign;
    KeyFormat mSecurityTokenDec;
    KeyFormat mSecurityTokenAuth;

    Fragment mCurrentFragment;

    SecurityTokenInfo tokenInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // React on NDEF_DISCOVERED from Manifest
        // NOTE: ACTION_NDEF_DISCOVERED and not ACTION_TAG_DISCOVERED like in BaseNfcActivity
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {

            mNfcTagDispatcher.interceptIntent(getIntent());

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
            mCreateSecurityToken = savedInstanceState.getBoolean(EXTRA_CREATE_SECURITY_TOKEN);
            mSecurityTokenPin = savedInstanceState.getParcelable(EXTRA_SECURITY_TOKEN_PIN);
            mSecurityTokenAdminPin = savedInstanceState.getParcelable(EXTRA_SECURITY_TOKEN_ADMIN_PIN);

            mCurrentFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        } else {

            Intent intent = getIntent();
            // Initialize members with default values for a new instance
            mName = intent.getStringExtra(EXTRA_NAME);
            mEmail = intent.getStringExtra(EXTRA_EMAIL);
            mFirstTime = intent.getBooleanExtra(EXTRA_FIRST_TIME, false);
            mCreateSecurityToken = intent.getBooleanExtra(EXTRA_CREATE_SECURITY_TOKEN, false);

            if (intent.hasExtra(EXTRA_SECURITY_TOKEN_INFO)) {
                SecurityTokenInfo tokenInfo = intent.getParcelableExtra(EXTRA_SECURITY_TOKEN_INFO);

                Fragment frag = ManageSecurityTokenFragment.newInstance(tokenInfo);
                loadFragment(frag, FragAction.START);
                setTitle(R.string.title_manage_my_keys);

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
    protected void doSecurityTokenInBackground() throws IOException {
        if (mCurrentFragment instanceof SecurityTokenListenerFragment) {
            ((SecurityTokenListenerFragment) mCurrentFragment).doSecurityTokenInBackground();
            return;
        }

        tokenInfo = mSecurityTokenHelper.getTokenInfo();
    }

    @Override
    protected void onSecurityTokenPostExecute() {
        handleTokenInfo(tokenInfo);
    }

    public void handleTokenInfo(SecurityTokenInfo tokenInfo) {
        if (mCurrentFragment instanceof SecurityTokenListenerFragment) {
            ((SecurityTokenListenerFragment) mCurrentFragment).onSecurityTokenPostExecute();
            return;
        }

        // We don't want get back to wait activity mainly because it looks weird with otg token
        if (mCurrentFragment instanceof CreateSecurityTokenWaitFragment) {
            // hack from http://stackoverflow.com/a/11253987
            CreateSecurityTokenWaitFragment.sDisableFragmentAnimations = true;
            getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            CreateSecurityTokenWaitFragment.sDisableFragmentAnimations = false;
        }

        Fragment frag = ManageSecurityTokenFragment.newInstance(tokenInfo);
        if (mCurrentFragment instanceof ManageSecurityTokenFragment) {
            loadFragment(frag, FragAction.REPLACE);
        } else {
            loadFragment(frag, FragAction.TO_RIGHT);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(EXTRA_NAME, mName);
        outState.putString(EXTRA_EMAIL, mEmail);
        outState.putStringArrayList(EXTRA_ADDITIONAL_EMAILS, mAdditionalEmails);
        outState.putParcelable(EXTRA_PASSPHRASE, mPassphrase);
        outState.putBoolean(EXTRA_FIRST_TIME, mFirstTime);
        outState.putBoolean(EXTRA_CREATE_SECURITY_TOKEN, mCreateSecurityToken);
        outState.putParcelable(EXTRA_SECURITY_TOKEN_PIN, mSecurityTokenPin);
        outState.putParcelable(EXTRA_SECURITY_TOKEN_ADMIN_PIN, mSecurityTokenAdminPin);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.create_key_activity);
    }

    public void startCreateKeyForSecurityToken(SecurityTokenInfo tokenInfo) {
        mCreateSecurityToken = true;
        this.tokenInfo = tokenInfo;

        CreateKeyNameFragment frag = CreateKeyNameFragment.newInstance();
        loadFragment(frag, FragAction.TO_RIGHT);
    }

    public enum FragAction {
        START,
        TO_RIGHT,
        TO_LEFT,
        REPLACE
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
            case REPLACE:
                transaction.replace(R.id.create_key_fragment_container, fragment, FRAGMENT_TAG)
                        .commit();
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

    interface SecurityTokenListenerFragment {
        void doSecurityTokenInBackground() throws IOException;
        void onSecurityTokenPostExecute();
    }

    @Override
    public void finish() {
        finishWithFirstTimeHandling(null);
    }

    public void finishWithFirstTimeHandling(@Nullable Intent intentToLaunch) {
        if (mFirstTime) {
            Preferences prefs = Preferences.getPreferences(this);
            prefs.setFirstTime(false);

            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
            Intent mainActivityIntent = new Intent(this, MainActivity.class);
            taskStackBuilder.addNextIntent(mainActivityIntent);
            if (intentToLaunch != null) {
                taskStackBuilder.addNextIntent(intentToLaunch);
            }
            taskStackBuilder.startActivities();
        } else if (intentToLaunch != null) {
            startActivity(intentToLaunch);
        }

        super.finish();
    }
}
