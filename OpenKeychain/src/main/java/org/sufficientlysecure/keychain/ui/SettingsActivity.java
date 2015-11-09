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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.ContactsContract;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.AppCompatPreferenceActivity;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.ui.widget.IntegerListPreference;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.orbot.OrbotHelper;

import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity {

    public static final int REQUEST_CODE_KEYSERVER_PREF = 0x00007005;

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
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //What to do on back clicked
                finish();
            }
        });
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        super.onBuildHeaders(target);
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    /**
     * This fragment shows the Cloud Search preferences
     */
    public static class CloudSearchPrefsFragment extends PreferenceFragment {

        private PreferenceScreen mKeyServerPreference = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.cloud_search_prefs);

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
            initializeSearchKeyserver(
                    (SwitchPreference) findPreference(Constants.Pref.SEARCH_KEYSERVER)
            );
            initializeSearchKeybase(
                    (SwitchPreference) findPreference(Constants.Pref.SEARCH_KEYBASE)
            );
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
    }

    /**
     * This fragment shows the PIN/password preferences
     */
    public static class PassphrasePrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.passphrase_preferences);

            initializePassphraseCacheSubs(
                    (CheckBoxPreference) findPreference(Constants.Pref.PASSPHRASE_CACHE_SUBS));

            initializePassphraseCacheTtl(
                    (IntegerListPreference) findPreference(Constants.Pref.PASSPHRASE_CACHE_TTL));

            initializeUseNumKeypadForYubiKeyPin(
                    (CheckBoxPreference) findPreference(Constants.Pref.USE_NUMKEYPAD_FOR_YUBIKEY_PIN));
        }
    }

    public static class ProxyPrefsFragment extends PreferenceFragment {

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
            private PreferenceActivity mActivity;
            private PreferenceFragment mFragment;

            public Initializer(PreferenceFragment fragment) {
                mFragment = fragment;
            }

            public Initializer(PreferenceActivity activity) {
                mActivity = activity;
            }

            public Preference automaticallyFindPreference(String key) {
                if (mFragment != null) {
                    return mFragment.findPreference(key);
                } else {
                    return mActivity.findPreference(key);
                }
            }

            public void initialize() {
                // makes android's preference framework write to our file instead of default
                // This allows us to use the "persistent" attribute to simplify code
                if (mFragment != null) {
                    Preferences.setPreferenceManagerFileAndMode(mFragment.getPreferenceManager());
                    // Load the preferences from an XML resource
                    mFragment.addPreferencesFromResource(R.xml.proxy_prefs);
                } else {
                    Preferences.setPreferenceManagerFileAndMode(mActivity.getPreferenceManager());
                    // Load the preferences from an XML resource
                    mActivity.addPreferencesFromResource(R.xml.proxy_prefs);
                }

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
                }
                else if (mUseNormalProxy.isChecked()) {
                    disableUseTorPrefs();
                } else {
                    disableNormalProxySettings();
                }
            }

            private void initializeUseTorPref() {
                mUseTor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        Activity activity = mFragment != null ? mFragment.getActivity() : mActivity;
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
                        Activity activity = mFragment != null ? mFragment.getActivity() : mActivity;
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
                        Activity activity = mFragment != null ? mFragment.getActivity() : mActivity;
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
     * This fragment shows the keyserver/contacts sync preferences
     */
    public static class SyncPrefsFragment extends PreferenceFragment {

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
            AccountManager manager = AccountManager.get(getActivity());
            final Account account = manager.getAccountsByType(Constants.ACCOUNT_TYPE)[0];
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
            boolean syncEnabled = ContentResolver.getSyncAutomatically(account, authority);
            syncCheckBox.setChecked(syncEnabled);
            setSummary(syncCheckBox, authority, syncEnabled);

            syncCheckBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean syncEnabled = (Boolean) newValue;
                    if (syncEnabled) {
                        ContentResolver.setSyncAutomatically(account, authority, true);
                    } else {
                        // disable syncs
                        ContentResolver.setSyncAutomatically(account, authority, false);
                        // cancel any ongoing/pending syncs
                        ContentResolver.cancelSync(account, authority);
                    }
                    setSummary(syncCheckBox, authority, syncEnabled);
                    return true;
                }
            });
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
    public static class ExperimentalPrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.experimental_preferences);

            initializeExperimentalEnableWordConfirm(
                    (SwitchPreference) findPreference(Constants.Pref.EXPERIMENTAL_ENABLE_WORD_CONFIRM));

            initializeExperimentalEnableLinkedIdentities(
                    (SwitchPreference) findPreference(Constants.Pref.EXPERIMENTAL_ENABLE_LINKED_IDENTITIES));

            initializeExperimentalEnableKeybase(
                    (SwitchPreference) findPreference(Constants.Pref.EXPERIMENTAL_ENABLE_KEYBASE));

            initializeTheme((ListPreference) findPreference(Constants.Pref.THEME));

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

    private static void initializePassphraseCacheSubs(final CheckBoxPreference mPassphraseCacheSubs) {
        mPassphraseCacheSubs.setChecked(sPreferences.getPassphraseCacheSubs());
        mPassphraseCacheSubs.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mPassphraseCacheSubs.setChecked((Boolean) newValue);
                sPreferences.setPassphraseCacheSubs((Boolean) newValue);
                return false;
            }
        });
    }

    private static void initializePassphraseCacheTtl(final IntegerListPreference mPassphraseCacheTtl) {
        mPassphraseCacheTtl.setValue("" + sPreferences.getPassphraseCacheTtl());
        mPassphraseCacheTtl.setSummary(mPassphraseCacheTtl.getEntry());
        mPassphraseCacheTtl
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        mPassphraseCacheTtl.setValue(newValue.toString());
                        mPassphraseCacheTtl.setSummary(mPassphraseCacheTtl.getEntry());
                        sPreferences.setPassphraseCacheTtl(Integer.parseInt(newValue.toString()));
                        return false;
                    }
                });
    }

    private static void initializeTheme(final ListPreference mTheme) {
        mTheme.setValue(sPreferences.getTheme());
        mTheme.setSummary(mTheme.getEntry() + "\n"
                + mTheme.getContext().getString(R.string.label_experimental_settings_theme_summary));
        mTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mTheme.setValue((String) newValue);
                mTheme.setSummary(mTheme.getEntry() + "\n"
                        + mTheme.getContext().getString(R.string.label_experimental_settings_theme_summary));
                sPreferences.setTheme((String) newValue);

                ((SettingsActivity) mTheme.getContext()).recreate();

                return false;
            }
        });
    }

    private static void initializeSearchKeyserver(final SwitchPreference mSearchKeyserver) {
        Preferences.CloudSearchPrefs prefs = sPreferences.getCloudSearchPrefs();
        mSearchKeyserver.setChecked(prefs.searchKeyserver);
        mSearchKeyserver.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mSearchKeyserver.setChecked((Boolean) newValue);
                sPreferences.setSearchKeyserver((Boolean) newValue);
                return false;
            }
        });
    }

    private static void initializeSearchKeybase(final SwitchPreference mSearchKeybase) {
        Preferences.CloudSearchPrefs prefs = sPreferences.getCloudSearchPrefs();
        mSearchKeybase.setChecked(prefs.searchKeybase);
        mSearchKeybase.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mSearchKeybase.setChecked((Boolean) newValue);
                sPreferences.setSearchKeybase((Boolean) newValue);
                return false;
            }
        });
    }

    public static String keyserverSummary(Context context) {
        String[] servers = sPreferences.getKeyServers();
        String serverSummary = context.getResources().getQuantityString(
                R.plurals.n_keyservers, servers.length, servers.length);
        return serverSummary + "; " + context.getString(R.string.label_preferred) + ": " + sPreferences
                .getPreferredKeyserver();
    }

    private static void initializeUseNumKeypadForYubiKeyPin(final CheckBoxPreference mUseNumKeypadForYubiKeyPin) {
        mUseNumKeypadForYubiKeyPin.setChecked(sPreferences.useNumKeypadForYubiKeyPin());
        mUseNumKeypadForYubiKeyPin.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mUseNumKeypadForYubiKeyPin.setChecked((Boolean) newValue);
                sPreferences.setUseNumKeypadForYubiKeyPin((Boolean) newValue);
                return false;
            }
        });
    }

    private static void initializeExperimentalEnableWordConfirm(final SwitchPreference mExperimentalEnableWordConfirm) {
        mExperimentalEnableWordConfirm.setChecked(sPreferences.getExperimentalEnableWordConfirm());
        mExperimentalEnableWordConfirm.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mExperimentalEnableWordConfirm.setChecked((Boolean) newValue);
                sPreferences.setExperimentalEnableWordConfirm((Boolean) newValue);
                return false;
            }
        });
    }

    private static void initializeExperimentalEnableLinkedIdentities(final SwitchPreference mExperimentalEnableLinkedIdentities) {
        mExperimentalEnableLinkedIdentities.setChecked(sPreferences.getExperimentalEnableLinkedIdentities());
        mExperimentalEnableLinkedIdentities.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mExperimentalEnableLinkedIdentities.setChecked((Boolean) newValue);
                sPreferences.setExperimentalEnableLinkedIdentities((Boolean) newValue);
                return false;
            }
        });
    }

    private static void initializeExperimentalEnableKeybase(final SwitchPreference mExperimentalKeybase) {
        mExperimentalKeybase.setChecked(sPreferences.getExperimentalEnableKeybase());
        mExperimentalKeybase.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mExperimentalKeybase.setChecked((Boolean) newValue);
                sPreferences.setExperimentalEnableKeybase((Boolean) newValue);
                return false;
            }
        });
    }
}
