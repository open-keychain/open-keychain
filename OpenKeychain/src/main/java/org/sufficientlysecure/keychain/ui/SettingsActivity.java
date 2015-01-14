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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.widget.IntegerListPreference;
import org.sufficientlysecure.keychain.util.Preferences;

import java.util.List;

public class SettingsActivity extends PreferenceActivity {

    public static final String ACTION_PREFS_CLOUD = "org.sufficientlysecure.keychain.ui.PREFS_CLOUD";
    public static final String ACTION_PREFS_ADV = "org.sufficientlysecure.keychain.ui.PREFS_ADV";

    public static final int REQUEST_CODE_KEYSERVER_PREF = 0x00007005;

    private PreferenceScreen mKeyServerPreference = null;
    private static Preferences sPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sPreferences = Preferences.getPreferences(this);
        super.onCreate(savedInstanceState);

        setupToolbar();

        String action = getIntent().getAction();

        if (action != null && action.equals(ACTION_PREFS_CLOUD)) {
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

        } else if (action != null && action.equals(ACTION_PREFS_ADV)) {
            addPreferencesFromResource(R.xml.adv_preferences);

            initializePassphraseCacheSubs(
                    (CheckBoxPreference) findPreference(Constants.Pref.PASSPHRASE_CACHE_SUBS));

            initializePassphraseCacheTtl(
                    (IntegerListPreference) findPreference(Constants.Pref.PASSPHRASE_CACHE_TTL));

            initializeEncryptionAlgorithm(
                    (IntegerListPreference) findPreference(Constants.Pref.DEFAULT_ENCRYPTION_ALGORITHM));

            initializeHashAlgorithm(
                    (IntegerListPreference) findPreference(Constants.Pref.DEFAULT_HASH_ALGORITHM));

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

            initializeMessageCompression(
                    (IntegerListPreference) findPreference(Constants.Pref.DEFAULT_MESSAGE_COMPRESSION),
                    entries, values);

            initializeFileCompression(
                    (IntegerListPreference) findPreference(Constants.Pref.DEFAULT_FILE_COMPRESSION),
                    entries, values);

            initializeAsciiArmor(
                    (CheckBoxPreference) findPreference(Constants.Pref.DEFAULT_ASCII_ARMOR));

            initializeWriteVersionHeader(
                    (CheckBoxPreference) findPreference(Constants.Pref.WRITE_VERSION_HEADER));

            initializeUseDefaultYubikeyPin(
                    (CheckBoxPreference) findPreference(Constants.Pref.USE_DEFAULT_YUBIKEY_PIN));

            initializeUseNumKeypadForYubikeyPin(
                    (CheckBoxPreference) findPreference(Constants.Pref.USE_NUMKEYPAD_FOR_YUBIKEY_PIN));

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

    /* Called only on Honeycomb and later */
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

            initializeEncryptionAlgorithm(
                    (IntegerListPreference) findPreference(Constants.Pref.DEFAULT_ENCRYPTION_ALGORITHM));

            initializeHashAlgorithm(
                    (IntegerListPreference) findPreference(Constants.Pref.DEFAULT_HASH_ALGORITHM));

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

            initializeMessageCompression(
                    (IntegerListPreference) findPreference(Constants.Pref.DEFAULT_MESSAGE_COMPRESSION),
                    entries, values);

            initializeFileCompression(
                    (IntegerListPreference) findPreference(Constants.Pref.DEFAULT_FILE_COMPRESSION),
                    entries, values);

            initializeAsciiArmor(
                    (CheckBoxPreference) findPreference(Constants.Pref.DEFAULT_ASCII_ARMOR));

            initializeWriteVersionHeader(
                    (CheckBoxPreference) findPreference(Constants.Pref.WRITE_VERSION_HEADER));

            initializeUseDefaultYubikeyPin(
                    (CheckBoxPreference) findPreference(Constants.Pref.USE_DEFAULT_YUBIKEY_PIN));

            initializeUseNumKeypadForYubikeyPin(
                    (CheckBoxPreference) findPreference(Constants.Pref.USE_NUMKEYPAD_FOR_YUBIKEY_PIN));
        }
    }

    protected boolean isValidFragment(String fragmentName) {
        return AdvancedPrefsFragment.class.getName().equals(fragmentName)
                || CloudSearchPrefsFragment.class.getName().equals(fragmentName)
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

    private static void initializeEncryptionAlgorithm(final IntegerListPreference mEncryptionAlgorithm) {
        int valueIds[] = {PGPEncryptedData.AES_128, PGPEncryptedData.AES_192,
                PGPEncryptedData.AES_256, PGPEncryptedData.BLOWFISH, PGPEncryptedData.TWOFISH,
                PGPEncryptedData.CAST5, PGPEncryptedData.DES, PGPEncryptedData.TRIPLE_DES,
                PGPEncryptedData.IDEA,};
        String entries[] = {"AES-128", "AES-192", "AES-256", "Blowfish", "Twofish", "CAST5",
                "DES", "Triple DES", "IDEA",};
        String values[] = new String[valueIds.length];
        for (int i = 0; i < values.length; ++i) {
            values[i] = "" + valueIds[i];
        }
        mEncryptionAlgorithm.setEntries(entries);
        mEncryptionAlgorithm.setEntryValues(values);
        mEncryptionAlgorithm.setValue("" + sPreferences.getDefaultEncryptionAlgorithm());
        mEncryptionAlgorithm.setSummary(mEncryptionAlgorithm.getEntry());
        mEncryptionAlgorithm
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        mEncryptionAlgorithm.setValue(newValue.toString());
                        mEncryptionAlgorithm.setSummary(mEncryptionAlgorithm.getEntry());
                        sPreferences.setDefaultEncryptionAlgorithm(Integer.parseInt(newValue
                                .toString()));
                        return false;
                    }
                });
    }

    private static void initializeHashAlgorithm(final IntegerListPreference mHashAlgorithm) {
        int[] valueIds = new int[]{HashAlgorithmTags.RIPEMD160,
                HashAlgorithmTags.SHA1, HashAlgorithmTags.SHA224, HashAlgorithmTags.SHA256,
                HashAlgorithmTags.SHA384, HashAlgorithmTags.SHA512,};
        String[] entries = new String[]{"RIPEMD-160", "SHA-1", "SHA-224", "SHA-256", "SHA-384",
                "SHA-512",};
        String[] values = new String[valueIds.length];
        for (int i = 0; i < values.length; ++i) {
            values[i] = "" + valueIds[i];
        }
        mHashAlgorithm.setEntries(entries);
        mHashAlgorithm.setEntryValues(values);
        mHashAlgorithm.setValue("" + sPreferences.getDefaultHashAlgorithm());
        mHashAlgorithm.setSummary(mHashAlgorithm.getEntry());
        mHashAlgorithm.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mHashAlgorithm.setValue(newValue.toString());
                mHashAlgorithm.setSummary(mHashAlgorithm.getEntry());
                sPreferences.setDefaultHashAlgorithm(Integer.parseInt(newValue.toString()));
                return false;
            }
        });
    }

    private static void initializeMessageCompression(final IntegerListPreference mMessageCompression,
                                                     String[] entries, String[] values) {
        mMessageCompression.setEntries(entries);
        mMessageCompression.setEntryValues(values);
        mMessageCompression.setValue("" + sPreferences.getDefaultMessageCompression());
        mMessageCompression.setSummary(mMessageCompression.getEntry());
        mMessageCompression
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        mMessageCompression.setValue(newValue.toString());
                        mMessageCompression.setSummary(mMessageCompression.getEntry());
                        sPreferences.setDefaultMessageCompression(Integer.parseInt(newValue
                                .toString()));
                        return false;
                    }
                });
    }

    private static void initializeFileCompression
            (final IntegerListPreference mFileCompression, String[] entries, String[] values) {
        mFileCompression.setEntries(entries);
        mFileCompression.setEntryValues(values);
        mFileCompression.setValue("" + sPreferences.getDefaultFileCompression());
        mFileCompression.setSummary(mFileCompression.getEntry());
        mFileCompression.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mFileCompression.setValue(newValue.toString());
                mFileCompression.setSummary(mFileCompression.getEntry());
                sPreferences.setDefaultFileCompression(Integer.parseInt(newValue.toString()));
                return false;
            }
        });
    }

    private static void initializeAsciiArmor(final CheckBoxPreference mAsciiArmor) {
        mAsciiArmor.setChecked(sPreferences.getDefaultAsciiArmor());
        mAsciiArmor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mAsciiArmor.setChecked((Boolean) newValue);
                sPreferences.setDefaultAsciiArmor((Boolean) newValue);
                return false;
            }
        });
    }

    private static void initializeWriteVersionHeader(final CheckBoxPreference mWriteVersionHeader) {
        mWriteVersionHeader.setChecked(sPreferences.getWriteVersionHeader());
        mWriteVersionHeader.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mWriteVersionHeader.setChecked((Boolean) newValue);
                sPreferences.setWriteVersionHeader((Boolean) newValue);
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
        return serverSummary + "; " + context.getString(R.string.label_preferred) + ": " + sPreferences.getPreferredKeyserver();
    }

    private static void initializeUseDefaultYubikeyPin(final CheckBoxPreference mUseDefaultYubikeyPin) {
        mUseDefaultYubikeyPin.setChecked(sPreferences.useDefaultYubikeyPin());
        mUseDefaultYubikeyPin.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mUseDefaultYubikeyPin.setChecked((Boolean) newValue);
                sPreferences.setUseDefaultYubikeyPin((Boolean) newValue);
                return false;
            }
        });
    }

    private static void initializeUseNumKeypadForYubikeyPin(final CheckBoxPreference mUseNumKeypadForYubikeyPin) {
        mUseNumKeypadForYubikeyPin.setChecked(sPreferences.useNumKeypadForYubikeyPin());
        mUseNumKeypadForYubikeyPin.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mUseNumKeypadForYubikeyPin.setChecked((Boolean) newValue);
                sPreferences.setUseNumKeypadForYubikeyPin((Boolean) newValue);
                return false;
            }
        });
    }
}
