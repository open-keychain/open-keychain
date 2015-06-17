/*
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.widget.IntegerListPreference;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.tor.OrbotHelper;

import java.util.List;

public class SettingsActivity extends PreferenceActivity {

    public static final String ACTION_PREFS_CLOUD = "org.sufficientlysecure.keychain.ui.PREFS_CLOUD";
    public static final String ACTION_PREFS_ADV = "org.sufficientlysecure.keychain.ui.PREFS_ADV";
    public static final String ACTION_PREFS_PROXY = "org.sufficientlysecure.keychain.ui.PREFS_PROXY";

    public static final int REQUEST_CODE_KEYSERVER_PREF = 0x00007005;

    private PreferenceScreen mKeyServerPreference = null;
    private static Preferences sPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sPreferences = Preferences.getPreferences(this);
        super.onCreate(savedInstanceState);

        setupToolbar();

        String action = getIntent().getAction();
        if (action == null) return;

        switch (action) {
            case ACTION_PREFS_CLOUD: {
                addPreferencesFromResource(R.xml.cloud_search_prefs);

                mKeyServerPreference = (PreferenceScreen) findPreference(Constants.Pref.KEY_SERVERS);
                mKeyServerPreference.setSummary(keyserverSummary(this));
                mKeyServerPreference
                        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            public boolean onPreferenceClick(Preference preference) {
                                Intent intent = new Intent(SettingsActivity.this,
                                        SettingsKeyServerActivity.class);
                                intent.putExtra(SettingsKeyServerActivity.EXTRA_KEY_SERVERS,
                                        sPreferences.getKeyServers());
                                startActivityForResult(intent, REQUEST_CODE_KEYSERVER_PREF);
                                return false;
                            }
                        });
                initializeSearchKeyserver(
                        (CheckBoxPreference) findPreference(Constants.Pref.SEARCH_KEYSERVER)
                );
                initializeSearchKeybase(
                        (CheckBoxPreference) findPreference(Constants.Pref.SEARCH_KEYBASE)
                );

                break;
            }

            case ACTION_PREFS_ADV: {
                addPreferencesFromResource(R.xml.adv_preferences);

                initializePassphraseCacheSubs(
                        (CheckBoxPreference) findPreference(Constants.Pref.PASSPHRASE_CACHE_SUBS));

                initializePassphraseCacheTtl(
                        (IntegerListPreference) findPreference(Constants.Pref.PASSPHRASE_CACHE_TTL));

                int[] valueIds = new int[]{
                        CompressionAlgorithmTags.UNCOMPRESSED,
                        CompressionAlgorithmTags.ZIP,
                        CompressionAlgorithmTags.ZLIB,
                        CompressionAlgorithmTags.BZIP2,
                };
                String[] entries = new String[]{
                        getString(R.string.choice_none) + " (" + getString(R.string.compression_fast) + ")",
                        "ZIP (" + getString(R.string.compression_fast) + ")",
                        "ZLIB (" + getString(R.string.compression_fast) + ")",
                        "BZIP2 (" + getString(R.string.compression_very_slow) + ")",};
                String[] values = new String[valueIds.length];
                for (int i = 0; i < values.length; ++i) {
                    values[i] = "" + valueIds[i];
                }

                initializeUseDefaultYubiKeyPin(
                        (CheckBoxPreference) findPreference(Constants.Pref.USE_DEFAULT_YUBIKEY_PIN));

                initializeUseNumKeypadForYubiKeyPin(
                        (CheckBoxPreference) findPreference(Constants.Pref.USE_NUMKEYPAD_FOR_YUBIKEY_PIN));

                break;
            }

            case ACTION_PREFS_PROXY: {
                new ProxyPrefsFragment.Initializer(this).initialize();

                break;
            }
        }
    }

    /**
     * Hack to get Toolbar in PreferenceActivity. See http://stackoverflow.com/a/26614696
     */
    private void setupToolbar() {
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        LinearLayout content = (LinearLayout) root.getChildAt(0);
        LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.preference_toolbar_activity, null);

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_KEYSERVER_PREF: {
                if (resultCode == RESULT_CANCELED || data == null) {
                    return;
                }
                String servers[] = data
                        .getStringArrayExtra(SettingsKeyServerActivity.EXTRA_KEY_SERVERS);
                sPreferences.setKeyServers(servers);
                mKeyServerPreference.setSummary(keyserverSummary(this));
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
                break;
            }
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        super.onBuildHeaders(target);
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    /**
     * This fragment shows the Cloud Search preferences in android 3.0+
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
                    (CheckBoxPreference) findPreference(Constants.Pref.SEARCH_KEYSERVER)
            );
            initializeSearchKeybase(
                    (CheckBoxPreference) findPreference(Constants.Pref.SEARCH_KEYBASE)
            );
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQUEST_CODE_KEYSERVER_PREF: {
                    if (resultCode == RESULT_CANCELED || data == null) {
                        return;
                    }
                    String servers[] = data
                            .getStringArrayExtra(SettingsKeyServerActivity.EXTRA_KEY_SERVERS);
                    sPreferences.setKeyServers(servers);
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
     * This fragment shows the advanced preferences in android 3.0+
     */
    public static class AdvancedPrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.adv_preferences);

            initializePassphraseCacheSubs(
                    (CheckBoxPreference) findPreference(Constants.Pref.PASSPHRASE_CACHE_SUBS));

            initializePassphraseCacheTtl(
                    (IntegerListPreference) findPreference(Constants.Pref.PASSPHRASE_CACHE_TTL));

            int[] valueIds = new int[]{
                    CompressionAlgorithmTags.UNCOMPRESSED,
                    CompressionAlgorithmTags.ZIP,
                    CompressionAlgorithmTags.ZLIB,
                    CompressionAlgorithmTags.BZIP2,
            };

            String[] entries = new String[]{
                    getString(R.string.choice_none) + " (" + getString(R.string.compression_fast) + ")",
                    "ZIP (" + getString(R.string.compression_fast) + ")",
                    "ZLIB (" + getString(R.string.compression_fast) + ")",
                    "BZIP2 (" + getString(R.string.compression_very_slow) + ")",
            };

            String[] values = new String[valueIds.length];
            for (int i = 0; i < values.length; ++i) {
                values[i] = "" + valueIds[i];
            }

            initializeUseDefaultYubiKeyPin(
                    (CheckBoxPreference) findPreference(Constants.Pref.USE_DEFAULT_YUBIKEY_PIN));

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
            private CheckBoxPreference mUseTor;
            private CheckBoxPreference mUseNormalProxy;
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

                mUseTor = (CheckBoxPreference) automaticallyFindPreference(Constants.Pref.USE_TOR_PROXY);
                mUseNormalProxy = (CheckBoxPreference) automaticallyFindPreference(Constants.Pref.USE_NORMAL_PROXY);
                mProxyHost = (EditTextPreference) automaticallyFindPreference(Constants.Pref.PROXY_HOST);
                mProxyPort = (EditTextPreference) automaticallyFindPreference(Constants.Pref.PROXY_PORT);
                mProxyType = (ListPreference) automaticallyFindPreference(Constants.Pref.PROXY_TYPE);
                initializeUseTorPref();
                initializeUseNormalProxyPref();
                initializeEditTextPreferences();
                initializeProxyTypePreference();

                if (mUseTor.isChecked()) disableNormalProxyPrefs();
                else if (mUseNormalProxy.isChecked()) disableUseTorPrefs();
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
                            enableNormalProxyPrefs();
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
                        } else {
                            enableUseTorPrefs();
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
                mProxyHost.setEnabled(false);
                mProxyPort.setEnabled(false);
                mProxyType.setEnabled(false);
            }

            private void enableNormalProxyPrefs() {
                mUseNormalProxy.setEnabled(true);
                mProxyHost.setEnabled(true);
                mProxyPort.setEnabled(true);
                mProxyType.setEnabled(true);
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

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected boolean isValidFragment(String fragmentName) {
        return AdvancedPrefsFragment.class.getName().equals(fragmentName)
                || CloudSearchPrefsFragment.class.getName().equals(fragmentName)
                || ProxyPrefsFragment.class.getName().equals(fragmentName)
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

    private static void initializeSearchKeyserver(final CheckBoxPreference mSearchKeyserver) {
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

    private static void initializeSearchKeybase(final CheckBoxPreference mSearchKeybase) {
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

    private static void initializeUseDefaultYubiKeyPin(final CheckBoxPreference mUseDefaultYubiKeyPin) {
        mUseDefaultYubiKeyPin.setChecked(sPreferences.useDefaultYubiKeyPin());
        mUseDefaultYubiKeyPin.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mUseDefaultYubiKeyPin.setChecked((Boolean) newValue);
                sPreferences.setUseDefaultYubiKeyPin((Boolean) newValue);
                return false;
            }
        });
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
}
