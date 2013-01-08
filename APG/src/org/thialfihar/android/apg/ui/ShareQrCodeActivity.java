/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.thialfihar.android.apg.ui;

import java.util.ArrayList;

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.provider.ProviderHelper;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.zxing.integration.android.IntentIntegrator;

import android.content.Intent;
import android.os.Bundle;

public class ShareQrCodeActivity extends SherlockFragmentActivity {
    public static final String ACTION_SHARE_WITH_QR_CODE = Constants.INTENT_PREFIX
            + "SHARE_WITH_QR_CODE";

    public static final String EXTRA_MASTER_KEY_ID = "masterKeyId";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleActions(getIntent());
    }

    protected void handleActions(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        if (ACTION_SHARE_WITH_QR_CODE.equals(action)) {
            long masterKeyId = getIntent().getExtras().getLong(EXTRA_MASTER_KEY_ID);

            // get public keyring as ascii armored string
            ArrayList<String> keyringArmored = ProviderHelper.getPublicKeyRingsAsArmoredString(
                    this, new long[] { masterKeyId });
            // close this activity
            finish();

            // use barcode scanner integration library
            new IntentIntegrator(this).shareText(keyringArmored.get(0));
        }
    }

}
