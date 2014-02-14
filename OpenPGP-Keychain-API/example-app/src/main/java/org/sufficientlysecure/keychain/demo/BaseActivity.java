/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.demo;

import org.sufficientlysecure.keychain.demo.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class BaseActivity extends PreferenceActivity {
    private Activity mActivity;

    private Preference mIntentDemo;
    private Preference mContentProviderDemo;
    private Preference mCryptoProvider;

    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = this;

        // load preferences from xml
        addPreferencesFromResource(R.xml.base_preference);

        // find preferences
        mIntentDemo = (Preference) findPreference("intent_demo");
        mContentProviderDemo = (Preference) findPreference("content_provider_demo");
        mCryptoProvider = (Preference) findPreference("openpgp_provider_demo");

        mIntentDemo.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // startActivity(new Intent(mActivity, IntentDemoActivity.class));
                Toast.makeText(BaseActivity.this, "Not implemented!", Toast.LENGTH_LONG).show();

                return false;
            }
        });

        mCryptoProvider.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(mActivity, OpenPgpProviderActivity.class));

                return false;
            }
        });

    }

}
