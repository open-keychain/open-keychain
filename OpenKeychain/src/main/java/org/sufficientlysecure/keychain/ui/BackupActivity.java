/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2011 Senecaso
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


import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;


public class BackupActivity extends BaseActivity {

    public static final String EXTRA_SECRET = "export_secret";

    @Override
    protected void initLayout() {
        setContentView(R.layout.backup_activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            BackupCodeDisplayFragment frag = BackupCodeDisplayFragment.newInstance();

            FragmentManager fragMan = getSupportFragmentManager();
            fragMan.beginTransaction()
                .setCustomAnimations(0, 0)
                .replace(R.id.content_frame, frag)
                .commit();
        }

    }
}
