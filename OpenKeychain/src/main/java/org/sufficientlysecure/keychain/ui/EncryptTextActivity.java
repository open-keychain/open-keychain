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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import org.apache.james.mime4j.util.MimeUtil;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.intents.OpenKeychainIntents;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;

public class EncryptTextActivity extends EncryptActivity {

    /* Intents */
    public static final String ACTION_ENCRYPT_TEXT = OpenKeychainIntents.ENCRYPT_TEXT;

    /* EXTRA keys for input */
    public static final String EXTRA_TEXT = OpenKeychainIntents.ENCRYPT_EXTRA_TEXT;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenDialogClose(Activity.RESULT_OK, false);

        Intent intent = getIntent();
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        String type = intent.getType();

        if (extras == null) {
            extras = new Bundle();
        }

        String textData = extras.getString(EXTRA_TEXT);
        boolean returnProcessText = false;

        // When sending to OpenKeychain Encrypt via share menu
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Log.logDebugBundle(extras, "extras");

            // When sending to OpenKeychain Encrypt via share menu
            if ( ! MimeUtil.isSameMimeType("text/plain", type)) {
                Toast.makeText(this, R.string.toast_wrong_mimetype, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            String sharedText;
            if (extras.containsKey(Intent.EXTRA_TEXT)) {
                sharedText = extras.getString(Intent.EXTRA_TEXT);
            } else  if (extras.containsKey(Intent.EXTRA_STREAM)) {
                try {
                    sharedText = FileHelper.readTextFromUri(this, extras.<Uri>getParcelable(Intent.EXTRA_STREAM), null);
                } catch (IOException e) {
                    Toast.makeText(this, R.string.error_preparing_data, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            } else {
                Toast.makeText(this, R.string.toast_no_text, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            if (sharedText != null) {
                if (sharedText.length() > Constants.TEXT_LENGTH_LIMIT) {
                    sharedText = sharedText.substring(0, Constants.TEXT_LENGTH_LIMIT);
                    Notify.create(this, R.string.snack_shared_text_too_long, Style.WARN).show();
                }
                // handle like normal text encryption, override action and extras to later
                // executeServiceMethod ACTION_ENCRYPT_TEXT in main actions
                textData = sharedText;
            }

        }

        // Android 6, PROCESS_TEXT Intent
        if (Intent.ACTION_PROCESS_TEXT.equals(action) && type != null) {

            String sharedText = null;
            if (extras.containsKey(Intent.EXTRA_PROCESS_TEXT)) {
                sharedText = extras.getString(Intent.EXTRA_PROCESS_TEXT);
                returnProcessText = true;
            } else  if (extras.containsKey(Intent.EXTRA_PROCESS_TEXT_READONLY)) {
                sharedText = extras.getString(Intent.EXTRA_PROCESS_TEXT_READONLY);
            }

            if (sharedText != null) {
                if (sharedText.length() > Constants.TEXT_LENGTH_LIMIT) {
                    sharedText = sharedText.substring(0, Constants.TEXT_LENGTH_LIMIT);
                    Notify.create(this, R.string.snack_shared_text_too_long, Style.WARN).show();
                }
                // handle like normal text encryption, override action and extras to later
                // executeServiceMethod ACTION_ENCRYPT_TEXT in main actions
                textData = sharedText;
            }
        }

        if (textData == null) {
            textData = "";
        }

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            EncryptTextFragment encryptFragment = EncryptTextFragment.newInstance(textData, returnProcessText);
            transaction.replace(R.id.encrypt_text_container, encryptFragment);
            transaction.commit();
        }

    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.encrypt_text_activity);
    }

}
