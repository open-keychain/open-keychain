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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.intents.OpenKeychainIntents;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.SingletonResult;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.util.Log;

import java.util.regex.Matcher;

public class DisplayTextActivity extends BaseActivity {

    // TODO make this only display text (maybe we need only the fragment?)

    /* Intents */
    public static final String EXTRA_METADATA = OpenKeychainIntents.DECRYPT_EXTRA_METADATA;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenDialogClose(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        }, false);

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
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        String type = intent.getType();

        if (extras == null) {
            extras = new Bundle();
        }

        if (savedInstanceState != null) {
            return;
        }

        Log.d(Constants.TAG, "ACTION_DECRYPT_TEXT");

        DecryptVerifyResult result = extras.getParcelable(EXTRA_METADATA);
        String plaintext = extras.getString(Intent.EXTRA_TEXT);

        if (plaintext != null) {
            loadFragment(plaintext, result);
        } else {
            Log.e(Constants.TAG, "EXTRA_TEXT does not contain PGP content!");
            Toast.makeText(this, R.string.error_invalid_data, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void returnInvalidResult() {
        SingletonResult result = new SingletonResult(
                SingletonResult.RESULT_ERROR, OperationResult.LogType.MSG_NO_VALID_ENC);
        Intent intent = new Intent();
        intent.putExtra(SingletonResult.EXTRA_RESULT, result);
        setResult(RESULT_OK, intent);
        finish();
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
