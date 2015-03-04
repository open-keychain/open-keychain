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

import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.sufficientlysecure.keychain.R;

/**
 * Signs the specified public key with the specified secret master key
 */
public class CertifyKeyActivity extends BaseActivity {

    public static final String EXTRA_RESULT = "operation_result";
    public static final String EXTRA_KEY_IDS = "extra_key_ids";
    public static final String EXTRA_CERTIFY_KEY_ID = "certify_key_id";

    @Override
    protected void initLayout() {
        setContentView(R.layout.certify_key_activity);
        changeToolbarColor();
    }

    /**
     * Changes the color of our ToolBar.
     *
     * Currently Set to ORANGE
     */
    private void changeToolbarColor() {
        RelativeLayout mToolBarInclude = (RelativeLayout) findViewById(R.id.toolbar_include);

        // Changes the color of the Status Bar strip
        ImageView mStatusBar = (ImageView) mToolBarInclude.findViewById(R.id.status_bar);
        mStatusBar.setBackgroundResource(getResources().getColor(R.color.android_orange_dark));

        // Changes the color of our Tool Bar
        Toolbar toolbar = (Toolbar) mToolBarInclude.findViewById(R.id.toolbar);
        toolbar.setBackgroundResource(getResources().getColor(R.color.android_orange_light));
    }
}
