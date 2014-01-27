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

package org.sufficientlysecure.keychain.ui;

import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.ui.widget.IntegerListPreference;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

public class PreferencesActivity extends SherlockPreferenceActivity {
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
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);

        addPreferencesFromResource(R.xml.preferences);

        mPassPhraseCacheTtl = (IntegerListPreference) findPreference(Constants.pref.PASS_PHRASE_CACHE_TTL);
        mPassPhraseCacheTtl.setValue("" + mPreferences.getPassPhraseCacheTtl());
        mPassPhraseCacheTtl.setSummary(mPassPhraseCacheTtl.getEntry());
        mPassPhraseCacheTtl
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        mPassPhraseCacheTtl.setValue(newValue.toString());
                        mPassPhraseCacheTtl.setSummary(mPassPhraseCacheTtl.getEntry());
                        mPreferences.setPassPhraseCacheTtl(Integer.parseInt(newValue.toString()));
                        return false;
                    }
                });

        mEncryptionAlgorithm = (IntegerListPreference) findPreference(Constants.pref.DEFAULT_ENCRYPTION_ALGORITHM);
        int valueIds[] = { PGPEncryptedData.AES_128, PGPEncryptedData.AES_192,
                PGPEncryptedData.AES_256, PGPEncryptedData.BLOWFISH, PGPEncryptedData.TWOFISH,
                PGPEncryptedData.CAST5, PGPEncryptedData.DES, PGPEncryptedData.TRIPLE_DES,
                PGPEncryptedData.IDEA, };
        String entries[] = { "AES-128", "AES-192", "AES-256", "Blowfish", "Twofish", "CAST5",
                "DES", "Triple DES", "IDEA", };
        String values[] = new String[valueIds.length];
        for (int i = 0; i < values.length; ++i) {
            values[i] = "" + valueIds[i];
        }
        mEncryptionAlgorithm.setEntries(entries);
        mEncryptionAlgorithm.setEntryValues(values);
        mEncryptionAlgorithm.setValue("" + mPreferences.getDefaultEncryptionAlgorithm());
        mEncryptionAlgorithm.setSummary(mEncryptionAlgorithm.getEntry());
        mEncryptionAlgorithm
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        mEncryptionAlgorithm.setValue(newValue.toString());
                        mEncryptionAlgorithm.setSummary(mEncryptionAlgorithm.getEntry());
                        mPreferences.setDefaultEncryptionAlgorithm(Integer.parseInt(newValue
                                .toString()));
                        return false;
                    }
                });

        mHashAlgorithm = (IntegerListPreference) findPreference(Constants.pref.DEFAULT_HASH_ALGORITHM);
        valueIds = new int[] { HashAlgorithmTags.MD5, HashAlgorithmTags.RIPEMD160,
                HashAlgorithmTags.SHA1, HashAlgorithmTags.SHA224, HashAlgorithmTags.SHA256,
                HashAlgorithmTags.SHA384, HashAlgorithmTags.SHA512, };
        entries = new String[] { "MD5", "RIPEMD-160", "SHA-1", "SHA-224", "SHA-256", "SHA-384",
                "SHA-512", };
        values = new String[valueIds.length];
        for (int i = 0; i < values.length; ++i) {
            values[i] = "" + valueIds[i];
        }
        mHashAlgorithm.setEntries(entries);
        mHashAlgorithm.setEntryValues(values);
        mHashAlgorithm.setValue("" + mPreferences.getDefaultHashAlgorithm());
        mHashAlgorithm.setSummary(mHashAlgorithm.getEntry());
        mHashAlgorithm.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mHashAlgorithm.setValue(newValue.toString());
                mHashAlgorithm.setSummary(mHashAlgorithm.getEntry());
                mPreferences.setDefaultHashAlgorithm(Integer.parseInt(newValue.toString()));
                return false;
            }
        });

        mMessageCompression = (IntegerListPreference) findPreference(Constants.pref.DEFAULT_MESSAGE_COMPRESSION);
        valueIds = new int[] { Id.choice.compression.none, Id.choice.compression.zip,
                Id.choice.compression.zlib, Id.choice.compression.bzip2, };
        entries = new String[] {
                getString(R.string.choice_none) + " (" + getString(R.string.compression_fast) + ")",
                "ZIP (" + getString(R.string.compression_fast) + ")",
                "ZLIB (" + getString(R.string.compression_fast) + ")",
                "BZIP2 (" + getString(R.string.compression_very_slow) + ")", };
        values = new String[valueIds.length];
        for (int i = 0; i < values.length; ++i) {
            values[i] = "" + valueIds[i];
        }
        mMessageCompression.setEntries(entries);
        mMessageCompression.setEntryValues(values);
        mMessageCompression.setValue("" + mPreferences.getDefaultMessageCompression());
        mMessageCompression.setSummary(mMessageCompression.getEntry());
        mMessageCompression
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        mMessageCompression.setValue(newValue.toString());
                        mMessageCompression.setSummary(mMessageCompression.getEntry());
                        mPreferences.setDefaultMessageCompression(Integer.parseInt(newValue
                                .toString()));
                        return false;
                    }
                });

        mFileCompression = (IntegerListPreference) findPreference(Constants.pref.DEFAULT_FILE_COMPRESSION);
        mFileCompression.setEntries(entries);
        mFileCompression.setEntryValues(values);
        mFileCompression.setValue("" + mPreferences.getDefaultFileCompression());
        mFileCompression.setSummary(mFileCompression.getEntry());
        mFileCompression.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mFileCompression.setValue(newValue.toString());
                mFileCompression.setSummary(mFileCompression.getEntry());
                mPreferences.setDefaultFileCompression(Integer.parseInt(newValue.toString()));
                return false;
            }
        });

        mAsciiArmour = (CheckBoxPreference) findPreference(Constants.pref.DEFAULT_ASCII_ARMOUR);
        mAsciiArmour.setChecked(mPreferences.getDefaultAsciiArmour());
        mAsciiArmour.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                mAsciiArmour.setChecked((Boolean) newValue);
                mPreferences.setDefaultAsciiArmour((Boolean) newValue);
                return false;
            }
        });

        mForceV3Signatures = (CheckBoxPreference) findPreference(Constants.pref.FORCE_V3_SIGNATURES);
        mForceV3Signatures.setChecked(mPreferences.getForceV3Signatures());
        mForceV3Signatures
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        mForceV3Signatures.setChecked((Boolean) newValue);
                        mPreferences.setForceV3Signatures((Boolean) newValue);
                        return false;
                    }
                });

        mKeyServerPreference = (PreferenceScreen) findPreference(Constants.pref.KEY_SERVERS);
        String servers[] = mPreferences.getKeyServers();
        mKeyServerPreference.setSummary(getResources().getQuantityString(R.plurals.n_key_servers,
                servers.length, servers.length));
        mKeyServerPreference
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(PreferencesActivity.this,
                                PreferencesKeyServerActivity.class);
                        intent.putExtra(PreferencesKeyServerActivity.EXTRA_KEY_SERVERS,
                                mPreferences.getKeyServers());
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
            String servers[] = data
                    .getStringArrayExtra(PreferencesKeyServerActivity.EXTRA_KEY_SERVERS);
            mPreferences.setKeyServers(servers);
            mKeyServerPreference.setSummary(getResources().getQuantityString(
                    R.plurals.n_key_servers, servers.length, servers.length));
            break;
        }

        default: {
            super.onActivityResult(requestCode, resultCode, data);
            break;
        }
        }
    }
}
