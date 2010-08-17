/*
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

package org.thialfihar.android.apg;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Vector;

import org.bouncycastle2.bcpg.HashAlgorithmTags;
import org.bouncycastle2.openpgp.PGPEncryptedData;
import org.thialfihar.android.apg.ui.widget.IntegerListPreference;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class PreferencesActivity extends PreferenceActivity {
    private ListPreference mLanguage = null;
    private IntegerListPreference mPassPhraseCacheTtl = null;
    private IntegerListPreference mEncryptionAlgorithm = null;
    private IntegerListPreference mHashAlgorithm = null;
    private IntegerListPreference mMessageCompression = null;
    private IntegerListPreference mFileCompression = null;
    private CheckBoxPreference mAsciiArmour = null;
    private CheckBoxPreference mForceV3Signatures = null;
    private PreferenceScreen mKeyServerPreference = null;
    private Preferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPreferences = Preferences.getPreferences(this);
        BaseActivity.setLanguage(this, mPreferences.getLanguage());
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.apg_preferences);

        mLanguage = (ListPreference) findPreference(Constants.pref.language);
        Vector<CharSequence> entryVector = new Vector<CharSequence>(Arrays.asList(mLanguage.getEntries()));
        Vector<CharSequence> entryValueVector = new Vector<CharSequence>(Arrays.asList(mLanguage.getEntryValues()));
        String supportedLanguages[] = getResources().getStringArray(R.array.supported_languages);
        HashSet<String> supportedLanguageSet = new HashSet<String>(Arrays.asList(supportedLanguages));
        for (int i = entryVector.size() - 1; i > -1; --i)
        {
            if (!supportedLanguageSet.contains(entryValueVector.get(i)))
            {
                entryVector.remove(i);
                entryValueVector.remove(i);
            }
        }
        CharSequence dummy[] = new CharSequence[0];
        mLanguage.setEntries(entryVector.toArray(dummy));
        mLanguage.setEntryValues(entryValueVector.toArray(dummy));
        mLanguage.setValue(mPreferences.getLanguage());
        mLanguage.setSummary(mLanguage.getEntry());
        mLanguage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                mLanguage.setValue(newValue.toString());
                mLanguage.setSummary(mLanguage.getEntry());
                mPreferences.setLanguage(newValue.toString());
                return false;
            }
        });

        mPassPhraseCacheTtl = (IntegerListPreference) findPreference(Constants.pref.pass_phrase_cache_ttl);
        mPassPhraseCacheTtl.setValue("" + mPreferences.getPassPhraseCacheTtl());
        mPassPhraseCacheTtl.setSummary(mPassPhraseCacheTtl.getEntry());
        mPassPhraseCacheTtl.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                mPassPhraseCacheTtl.setValue(newValue.toString());
                mPassPhraseCacheTtl.setSummary(mPassPhraseCacheTtl.getEntry());
                mPreferences.setPassPhraseCacheTtl(Integer.parseInt(newValue.toString()));
                BaseActivity.startCacheService(PreferencesActivity.this, mPreferences);
                return false;
            }
        });

        mEncryptionAlgorithm = (IntegerListPreference) findPreference(Constants.pref.default_encryption_algorithm);
        int valueIds[] = {
                PGPEncryptedData.AES_128, PGPEncryptedData.AES_192, PGPEncryptedData.AES_256,
                PGPEncryptedData.BLOWFISH, PGPEncryptedData.TWOFISH, PGPEncryptedData.CAST5,
                PGPEncryptedData.DES, PGPEncryptedData.TRIPLE_DES, PGPEncryptedData.IDEA,
        };
        String entries[] = {
                "AES-128", "AES-192", "AES-256",
                "Blowfish", "Twofish", "CAST5",
                "DES", "Triple DES", "IDEA",
        };
        String values[] = new String[valueIds.length];
        for (int i = 0; i < values.length; ++i) {
            values[i] = "" + valueIds[i];
        }
        mEncryptionAlgorithm.setEntries(entries);
        mEncryptionAlgorithm.setEntryValues(values);
        mEncryptionAlgorithm.setValue("" + mPreferences.getDefaultEncryptionAlgorithm());
        mEncryptionAlgorithm.setSummary(mEncryptionAlgorithm.getEntry());
        mEncryptionAlgorithm.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                mEncryptionAlgorithm.setValue(newValue.toString());
                mEncryptionAlgorithm.setSummary(mEncryptionAlgorithm.getEntry());
                mPreferences.setDefaultEncryptionAlgorithm(Integer.parseInt(newValue.toString()));
                return false;
            }
        });

        mHashAlgorithm = (IntegerListPreference) findPreference(Constants.pref.default_hash_algorithm);
        valueIds = new int[] {
                HashAlgorithmTags.MD5, HashAlgorithmTags.RIPEMD160, HashAlgorithmTags.SHA1,
                HashAlgorithmTags.SHA224, HashAlgorithmTags.SHA256, HashAlgorithmTags.SHA384,
                HashAlgorithmTags.SHA512,
        };
        entries = new String[] {
                "MD5", "RIPEMD-160", "SHA-1",
                "SHA-224", "SHA-256", "SHA-384",
                "SHA-512",
        };
        values = new String[valueIds.length];
        for (int i = 0; i < values.length; ++i) {
            values[i] = "" + valueIds[i];
        }
        mHashAlgorithm.setEntries(entries);
        mHashAlgorithm.setEntryValues(values);
        mHashAlgorithm.setValue("" + mPreferences.getDefaultHashAlgorithm());
        mHashAlgorithm.setSummary(mHashAlgorithm.getEntry());
        mHashAlgorithm.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                mHashAlgorithm.setValue(newValue.toString());
                mHashAlgorithm.setSummary(mHashAlgorithm.getEntry());
                mPreferences.setDefaultHashAlgorithm(Integer.parseInt(newValue.toString()));
                return false;
            }
        });

        mMessageCompression = (IntegerListPreference) findPreference(Constants.pref.default_message_compression);
        valueIds = new int[] {
                Id.choice.compression.none,
                Id.choice.compression.zip,
                Id.choice.compression.zlib,
                Id.choice.compression.bzip2,
        };
        entries = new String[] {
                getString(R.string.choice_none) + " (" + getString(R.string.fast) + ")",
                "ZIP (" + getString(R.string.fast) + ")",
                "ZLIB (" + getString(R.string.fast) + ")",
                "BZIP2 (" + getString(R.string.very_slow) + ")",
        };
        values = new String[valueIds.length];
        for (int i = 0; i < values.length; ++i) {
            values[i] = "" + valueIds[i];
        }
        mMessageCompression.setEntries(entries);
        mMessageCompression.setEntryValues(values);
        mMessageCompression.setValue("" + mPreferences.getDefaultMessageCompression());
        mMessageCompression.setSummary(mMessageCompression.getEntry());
        mMessageCompression.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                mMessageCompression.setValue(newValue.toString());
                mMessageCompression.setSummary(mMessageCompression.getEntry());
                mPreferences.setDefaultMessageCompression(Integer.parseInt(newValue.toString()));
                return false;
            }
        });

        mFileCompression = (IntegerListPreference) findPreference(Constants.pref.default_file_compression);
        mFileCompression.setEntries(entries);
        mFileCompression.setEntryValues(values);
        mFileCompression.setValue("" + mPreferences.getDefaultFileCompression());
        mFileCompression.setSummary(mFileCompression.getEntry());
        mFileCompression.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                mFileCompression.setValue(newValue.toString());
                mFileCompression.setSummary(mFileCompression.getEntry());
                mPreferences.setDefaultFileCompression(Integer.parseInt(newValue.toString()));
                return false;
            }
        });

        mAsciiArmour = (CheckBoxPreference) findPreference(Constants.pref.default_ascii_armour);
        mAsciiArmour.setChecked(mPreferences.getDefaultAsciiArmour());
        mAsciiArmour.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                mAsciiArmour.setChecked((Boolean)newValue);
                mPreferences.setDefaultAsciiArmour((Boolean)newValue);
                return false;
            }
        });

        mForceV3Signatures = (CheckBoxPreference) findPreference(Constants.pref.force_v3_signatures);
        mForceV3Signatures.setChecked(mPreferences.getForceV3Signatures());
        mForceV3Signatures.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                mForceV3Signatures.setChecked((Boolean)newValue);
                mPreferences.setForceV3Signatures((Boolean)newValue);
                return false;
            }
        });

        mKeyServerPreference = (PreferenceScreen) findPreference(Constants.pref.key_servers);
        String servers[] = mPreferences.getKeyServers();
        mKeyServerPreference.setSummary(getResources().getString(R.string.nKeyServers, servers.length));
        mKeyServerPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(PreferencesActivity.this,
                                           KeyServerPreferenceActivity.class);
                intent.putExtra(Apg.EXTRA_KEY_SERVERS, mPreferences.getKeyServers());
                startActivityForResult(intent, Id.request.key_server_preference);
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Id.request.key_server_preference: {
                if (resultCode == RESULT_CANCELED || data == null) {
                    return;
                }
                String servers[] = data.getStringArrayExtra(Apg.EXTRA_KEY_SERVERS);
                mPreferences.setKeyServers(servers);
                mKeyServerPreference.setSummary(getResources().getString(R.string.nKeyServers, servers.length));
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
                break;
            }
        }
    }
}

