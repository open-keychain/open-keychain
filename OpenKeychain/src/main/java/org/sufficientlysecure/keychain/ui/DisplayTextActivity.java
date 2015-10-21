/*
 * Copyright (C) 2012-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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


import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import org.openintents.openpgp.OpenPgpMetadata;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;

public class DisplayTextActivity extends BaseActivity {

    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_METADATA = "metadata";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenDialogClose(Activity.RESULT_CANCELED, false);

        // Handle intent actions
        handleActions(savedInstanceState, getIntent());
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.decrypt_text_activity);
    }

    /**
     * Handles all actions with this intent
     */
    private void handleActions(Bundle savedInstanceState, Intent intent) {
        if (savedInstanceState != null) {
            return;
        }

        DecryptVerifyResult result = intent.getParcelableExtra(EXTRA_RESULT);
        OpenPgpMetadata metadata = intent.getParcelableExtra(EXTRA_METADATA);

        String plaintext;
        try {
            plaintext = FileHelper.readTextFromUri(this, intent.getData(), metadata.getCharset());
        } catch (IOException e) {
            Toast.makeText(this, R.string.error_preparing_data, Toast.LENGTH_LONG).show();
            return;
        }

        if (plaintext != null) {
            if (plaintext.length() > Constants.TEXT_LENGTH_LIMIT) {
                plaintext = plaintext.substring(0, Constants.TEXT_LENGTH_LIMIT);
                Notify.create(this, R.string.snack_text_too_long, Style.WARN).show();
            }

            loadFragment(plaintext, result);
        } else {
            Toast.makeText(this, R.string.error_invalid_data, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadFragment(String plaintext, DecryptVerifyResult result) {
        // Create an instance of the fragment
        Fragment frag = DisplayTextFragment.newInstance(plaintext, result);

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.decrypt_text_fragment_container, frag)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

}
