/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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


import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.KeychainApplication;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.AppCompatPreferenceActivity;
import org.sufficientlysecure.keychain.service.ContactSyncAdapterService;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverAddress;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.network.orbot.OrbotHelper;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {

    public static final int REQUEST_CODE_KEYSERVER_PREF = 0x00007005;
    public static final int REQUEST_CODE_SMARTPGP_AUTHORITIES_PREF = 0x00007006;
    private static final int REQUEST_PERMISSION_READ_CONTACTS = 13;

    private static Preferences sPreferences;
    private ThemeChanger mThemeChanger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sPreferences = Preferences.getPreferences(this);
        mThemeChanger = new ThemeChanger(this);
        mThemeChanger.setThemes(R.style.Theme_Keychain_Light, R.style.Theme_Keychain_Dark);
        mThemeChanger.changeTheme();
        super.onCreate(savedInstanceState);

        setupToolbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BaseActivity.onResumeChecks(this);

        if (mThemeChanger.changeTheme()) {
            Intent intent = getIntent();
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    /**
     * Hack to get Toolbar in PreferenceActivity. See http://stackoverflow.com/a/26614696
     */
    private void setupToolbar() {
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        LinearLayout content = (LinearLayout) root.getChildAt(0);
        LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.preference_toolbar, null);

        root.removeAllViews();
        toolbarContainer.addView(content);
        root.addView(toolbarContainer);

        Toolbar toolbar = (Toolbar) toolbarContainer.findViewById(R.id.toolbar);

        toolbar.setTitle(R.string.title_preferences);
        // noinspection deprecation, TODO use alternative in API level 21
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //What to do on back clicked
                finish();
            }
        });
    }

    public static abstract class PresetPreferenceFragment extends PreferenceFragment {
        @Override
        public void addPreferencesFromResource(int preferencesResId) {
            // so that preferences are written to our preference file, not the default
            Preferences.setPreferenceManagerFileAndMode(this.getPreferenceManager());
            super.addPreferencesFromResource(preferencesResId);
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        super.onBuildHeaders(target);
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    /**
     * This fragment shows the Cloud Search preferences
     */
    public static class CloudSearchPrefsFragment extends PresetPreferenceFragment {

        private PreferenceScreen mKeyServerPreference = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.cloud_search_preferences);

            mKeyServerPreference = (PreferenceScreen) findPreference(Constants.Pref.KEY_SERVERS);
            mKeyServerPreference.setSummary(keyserverSummary(getActivity()));

            mKeyServerPreference
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        public boolean onPreferenceClick(Preference preference) {
                            Intent intent = new Intent(getActivity(),
                                    SettingsKeyServerActivity.class);
                            intent.putExtra(SettingsKeyServerActivity.EXTRA_KEY_SERVERS,
                                    sPreferences.getKeyServers());
                            startActivityForResult(intent, REQUEST_CODE_KEYSERVER_PREF);
                            return false;
                        }
                    });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQUEST_CODE_KEYSERVER_PREF: {
                    // update preference, in case it changed
                    mKeyServerPreference.setSummary(keyserverSummary(getActivity()));
                    break;
                }

                default: {
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
                }
            }
        }

        public static String keyserverSummary(Context context) {
            ArrayList<HkpKeyserverAddress> servers = sPreferences.getKeyServers();
            String serverSummary = context.getResources().getQuantityString(
                    R.plurals.n_keyservers, servers.size(), servers.size());
            return serverSummary + "; " + context.getString(R.string.label_preferred) + ": " + sPreferences
                    .getPreferredKeyserver().getUrl();
        }
    }

    /**
     * This fragment shows the PIN/password preferences
     */
    public static class PassphrasePrefsFragment extends PresetPreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.passphrase_preferences);
        }
    }

    public static class ProxyPrefsFragment extends PresetPreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            new Initializer(this).initialize();

        }

        public static class Initializer {
            private SwitchPreference mUseTor;
            private SwitchPreference mUseNormalProxy;
            private EditTextPreference mProxyHost;
            private EditTextPreference mProxyPort;
            private ListPreference mProxyType;
            private PresetPreferenceFragment mFragment;

            public Initializer(PresetPreferenceFragment fragment) {
                mFragment = fragment;
            }

            public Preference automaticallyFindPreference(String key) {
                return mFragment.findPreference(key);
            }

            public void initialize() {
                mFragment.addPreferencesFromResource(R.xml.proxy_preferences);

                mUseTor = (SwitchPreference) automaticallyFindPreference(Constants.Pref.USE_TOR_PROXY);
                mUseNormalProxy = (SwitchPreference) automaticallyFindPreference(Constants.Pref.USE_NORMAL_PROXY);
                mProxyHost = (EditTextPreference) automaticallyFindPreference(Constants.Pref.PROXY_HOST);
                mProxyPort = (EditTextPreference) automaticallyFindPreference(Constants.Pref.PROXY_PORT);
                mProxyType = (ListPreference) automaticallyFindPreference(Constants.Pref.PROXY_TYPE);
                initializeUseTorPref();
                initializeUseNormalProxyPref();
                initializeEditTextPreferences();
                initializeProxyTypePreference();

                if (mUseTor.isChecked()) {
                    disableNormalProxyPrefs();
                } else if (mUseNormalProxy.isChecked()) {
                    disableUseTorPrefs();
                } else {
                    disableNormalProxySettings();
                }
            }

            private void initializeUseTorPref() {
                mUseTor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        Activity activity = mFragment.getActivity();
                        if ((Boolean) newValue) {
                            boolean installed = OrbotHelper.isOrbotInstalled(activity);
                            if (!installed) {
                                Log.d(Constants.TAG, "Prompting to install Tor");
                                OrbotHelper.getPreferenceInstallDialogFragment().show(activity.getFragmentManager(),
                                        "installDialog");
                                // don't let the user check the box until he's installed orbot
                                return false;
                            } else {
                                disableNormalProxyPrefs();
                                // let the enable tor box be checked
                                return true;
                            }
                        } else {
                            // we're unchecking Tor, so enable other proxy
                            enableNormalProxyCheckbox();
                            return true;
                        }
                    }
                });
            }

            private void initializeUseNormalProxyPref() {
                mUseNormalProxy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if ((Boolean) newValue) {
                            disableUseTorPrefs();
                            enableNormalProxySettings();
                        } else {
                            enableUseTorPrefs();
                            disableNormalProxySettings();
                        }
                        return true;
                    }
                });
            }

            private void initializeEditTextPreferences() {
                mProxyHost.setSummary(mProxyHost.getText());
                mProxyPort.setSummary(mProxyPort.getText());

                mProxyHost.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        Activity activity = mFragment.getActivity();
                        if (TextUtils.isEmpty((String) newValue)) {
                            Notify.create(
                                    activity,
                                    R.string.pref_proxy_host_err_invalid,
                                    Notify.Style.ERROR
                            ).show();
                            return false;
                        } else {
                            mProxyHost.setSummary((CharSequence) newValue);
                            return true;
                        }
                    }
                });

                mProxyPort.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        Activity activity = mFragment.getActivity();
                        try {
                            int port = Integer.parseInt((String) newValue);
                            if (port < 0 || port > 65535) {
                                Notify.create(
                                        activity,
                                        R.string.pref_proxy_port_err_invalid,
                                        Notify.Style.ERROR
                                ).show();
                                return false;
                            }
                            // no issues, save port
                            mProxyPort.setSummary("" + port);
                            return true;
                        } catch (NumberFormatException e) {
                            Notify.create(
                                    activity,
                                    R.string.pref_proxy_port_err_invalid,
                                    Notify.Style.ERROR
                            ).show();
                            return false;
                        }
                    }
                });
            }

            private void initializeProxyTypePreference() {
                mProxyType.setSummary(mProxyType.getEntry());

                mProxyType.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        CharSequence entry = mProxyType.getEntries()[mProxyType.findIndexOfValue((String) newValue)];
                        mProxyType.setSummary(entry);
                        return true;
                    }
                });
            }

            private void disableNormalProxyPrefs() {
                mUseNormalProxy.setChecked(false);
                mUseNormalProxy.setEnabled(false);
                disableNormalProxySettings();
            }

            private void enableNormalProxyCheckbox() {
                mUseNormalProxy.setEnabled(true);
            }

            private void enableNormalProxySettings() {
                mProxyHost.setEnabled(true);
                mProxyPort.setEnabled(true);
                mProxyType.setEnabled(true);
            }

            private void disableNormalProxySettings() {
                mProxyHost.setEnabled(false);
                mProxyPort.setEnabled(false);
                mProxyType.setEnabled(false);
            }

            private void disableUseTorPrefs() {
                mUseTor.setChecked(false);
                mUseTor.setEnabled(false);
            }

            private void enableUseTorPrefs() {
                mUseTor.setEnabled(true);
            }
        }
    }

    /**
     * This fragment shows the keyserver/wifi-only-sync/contacts sync preferences
     */
    public static class SyncPrefsFragment extends PresetPreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.sync_preferences);
        }

        @Override
        public void onResume() {
            super.onResume();
            // this needs to be done in onResume since the user can change sync values from Android
            // settings and we need to reflect that change when the user navigates back
            final Account account = KeychainApplication.createAccountIfNecessary(getActivity());
            // for keyserver sync
            initializeSyncCheckBox(
                    (SwitchPreference) findPreference(Constants.Pref.SYNC_KEYSERVER),
                    account,
                    Constants.PROVIDER_AUTHORITY
            );
            // for contacts sync
            initializeSyncCheckBox(
                    (SwitchPreference) findPreference(Constants.Pref.SYNC_CONTACTS),
                    account,
                    ContactsContract.AUTHORITY
            );
        }

        private void initializeSyncCheckBox(final SwitchPreference syncCheckBox,
                                            final Account account,
                                            final String authority) {
            // account is null if it could not be created for some reason
            boolean syncEnabled =
                    account != null
                            && ContentResolver.getSyncAutomatically(account, authority)
                            && checkContactsPermission(authority);
            syncCheckBox.setChecked(syncEnabled);
            setSummary(syncCheckBox, authority, syncEnabled);

            syncCheckBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @TargetApi(Build.VERSION_CODES.M)
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean syncEnabled = (Boolean) newValue;
                    if (syncEnabled) {
                        if (checkContactsPermission(authority)) {
                            ContentResolver.setSyncAutomatically(account, authority, true);
                            setSummary(syncCheckBox, authority, true);
                            return true;
                        } else {
                            requestPermissions(
                                    new String[]{Manifest.permission.READ_CONTACTS},
                                    REQUEST_PERMISSION_READ_CONTACTS);
                            // don't update preference
                            return false;
                        }
                    } else {
                        if (account == null) {
                            // if account could not be created for some reason,
                            // we can't have our sync
                            return false;
                        }
                        // disable syncs
                        ContentResolver.setSyncAutomatically(account, authority, false);
                        // immediately delete any linked contacts
                        ContactSyncAdapterService.deleteIfSyncDisabled(getActivity());
                        // cancel any ongoing/pending syncs
                        ContentResolver.cancelSync(account, authority);
                        setSummary(syncCheckBox, authority, false);
                        return true;
                    }
                }
            });
        }

        private boolean checkContactsPermission(String authority) {
            if (!ContactsContract.AUTHORITY.equals(authority)) {
                // provides convenience of not using separate checks for keyserver and contact sync
                // in initializeSyncCheckBox
                return true;
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return true;
            }

            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            }

            return false;
        }

        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {

            if (requestCode != REQUEST_PERMISSION_READ_CONTACTS) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
            }

            boolean permissionWasGranted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (permissionWasGranted) {
                // permission granted -> enable contact linking
                AccountManager manager = AccountManager.get(getActivity());
                final Account account = manager.getAccountsByType(Constants.ACCOUNT_TYPE)[0];
                SwitchPreference pref = (SwitchPreference) findPreference(Constants.Pref.SYNC_CONTACTS);
                ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
                setSummary(pref, ContactsContract.AUTHORITY, true);
                pref.setChecked(true);
            }
        }

        private void setSummary(SwitchPreference syncCheckBox, String authority,
                                boolean checked) {
            switch (authority) {
                case Constants.PROVIDER_AUTHORITY: {
                    if (checked) {
                        syncCheckBox.setSummary(R.string.label_sync_settings_keyserver_summary_on);
                    } else {
                        syncCheckBox.setSummary(R.string.label_sync_settings_keyserver_summary_off);
                    }
                    break;
                }
                case ContactsContract.AUTHORITY: {
                    if (checked) {
                        syncCheckBox.setSummary(R.string.label_sync_settings_contacts_summary_on);
                    } else {
                        syncCheckBox.setSummary(R.string.label_sync_settings_contacts_summary_off);
                    }
                    break;
                }
            }
        }
    }

    /**
     * This fragment shows experimental features
     */
    public static class ExperimentalPrefsFragment extends PresetPreferenceFragment {

        private PreferenceScreen mSmartPGPAuthoritiesPreference = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.experimental_preferences);

            initializeTheme((ListPreference) findPreference(Constants.Pref.THEME));

            mSmartPGPAuthoritiesPreference = (PreferenceScreen) findPreference(Constants.Pref.EXPERIMENTAL_SMARTPGP_AUTHORITIES);

            final KeyStore ks = SettingsSmartPGPAuthoritiesActivity.readKeystore(getActivity());
            int size = 0;
            try {
                if (ks != null) {
                    size = ks.size();
                }
            } catch (KeyStoreException e) {}

            mSmartPGPAuthoritiesPreference.setSummary(getActivity().getResources().getQuantityString(R.plurals.n_authorities, size, size));

            mSmartPGPAuthoritiesPreference
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        public boolean onPreferenceClick(Preference preference) {
                            Intent intent = new Intent(getActivity(),
                                    SettingsSmartPGPAuthoritiesActivity.class);
                            startActivityForResult(intent, REQUEST_CODE_SMARTPGP_AUTHORITIES_PREF);
                            return false;
                        }
                    });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQUEST_CODE_SMARTPGP_AUTHORITIES_PREF: {
                    // update preference, in case it changed
                    final KeyStore ks = SettingsSmartPGPAuthoritiesActivity.readKeystore(getActivity());
                    int size = 0;
                    try {
                        if (ks != null) {
                            size = ks.size();
                        }
                    } catch (KeyStoreException e) {}

                    mSmartPGPAuthoritiesPreference.setSummary(getActivity().getResources().getQuantityString(R.plurals.n_authorities, size, size));
                    break;
                }

                default: {
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
                }
            }
        }

        private static void initializeTheme(final ListPreference themePref) {
            themePref.setSummary(themePref.getEntry() + "\n"
                    + themePref.getContext().getString(R.string.label_experimental_settings_theme_summary));
            themePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    themePref.setSummary(newValue + "\n"
                            + themePref.getContext().getString(R.string.label_experimental_settings_theme_summary));

                    ((SettingsActivity) themePref.getContext()).recreate();

                    return true;
                }
            });
        }
    }

    protected boolean isValidFragment(String fragmentName) {
        return PassphrasePrefsFragment.class.getName().equals(fragmentName)
                || CloudSearchPrefsFragment.class.getName().equals(fragmentName)
                || ProxyPrefsFragment.class.getName().equals(fragmentName)
                || SyncPrefsFragment.class.getName().equals(fragmentName)
                || ExperimentalPrefsFragment.class.getName().equals(fragmentName)
                || super.isValidFragment(fragmentName);
    }
}
