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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;


/**
 * Signs the specified public key with the specified secret master key
 */
public class CertifyKeyActivity extends BaseActivity {

    public static final String EXTRA_RESULT = "operation_result";
    // For sending masterKeyIds to MultiUserIdsFragment to display list of keys
    public static final String EXTRA_KEY_IDS = MultiUserIdsFragment.EXTRA_KEY_IDS ;
    public static final String EXTRA_CERTIFY_KEY_ID = "certify_key_id";

    @Override
    protected void initLayout() {
        setContentView(R.layout.certify_key_activity);
    }

}
