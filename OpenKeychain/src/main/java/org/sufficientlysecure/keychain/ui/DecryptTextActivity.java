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
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.SingletonResult;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.util.Log;

import java.util.regex.Matcher;

public class DecryptTextActivity extends BaseActivity {

    /* Intents */
    public static final String ACTION_DECRYPT_TEXT = OpenKeychainIntents.DECRYPT_TEXT;
    public static final String EXTRA_TEXT = OpenKeychainIntents.DECRYPT_EXTRA_TEXT;

    // intern
    public static final String ACTION_DECRYPT_FROM_CLIPBOARD = Constants.INTENT_PREFIX + "DECRYPT_TEXT_FROM_CLIPBOARD";

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
     * Fixing broken PGP MESSAGE Strings coming from GMail/AOSP Mail
     */
    private String fixPgpMessage(String message) {
        // windows newline -> unix newline
        message = message.replaceAll("\r\n", "\n");
        // Mac OS before X newline -> unix newline
        message = message.replaceAll("\r", "\n");

        // remove whitespaces before newline
        message = message.replaceAll(" +\n", "\n");
        // only two consecutive newlines are allowed
        message = message.replaceAll("\n\n+", "\n\n");

        // replace non breakable spaces
        message = message.replaceAll("\\xa0", " ");

        return message;
    }

    /**
     * Fixing broken PGP SIGNED MESSAGE Strings coming from GMail/AOSP Mail
     */
    private String fixPgpCleartextSignature(CharSequence input) {
        if (!TextUtils.isEmpty(input)) {
            String text = input.toString();

            // windows newline -> unix newline
            text = text.replaceAll("\r\n", "\n");
            // Mac OS before X newline -> unix newline
            text = text.replaceAll("\r", "\n");

            return text;
        } else {
            return null;
        }
    }

    private String getPgpContent(CharSequence input) {
        // only decrypt if clipboard content is available and a pgp message or cleartext signature
        if (!TextUtils.isEmpty(input)) {
            Log.dEscaped(Constants.TAG, "input: " + input);

            Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(input);
            if (matcher.matches()) {
                String text = matcher.group(1);
                text = fixPgpMessage(text);

                Log.dEscaped(Constants.TAG, "input fixed: " + text);
                return text;
            } else {
                matcher = PgpHelper.PGP_CLEARTEXT_SIGNATURE.matcher(input);
                if (matcher.matches()) {
                    String text = matcher.group(1);
                    text = fixPgpCleartextSignature(text);

                    Log.dEscaped(Constants.TAG, "input fixed: " + text);
                    return text;
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
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

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Log.d(Constants.TAG, "ACTION_SEND");
            Log.logDebugBundle(extras, "SEND extras");

            // When sending to Keychain Decrypt via share menu
            if ("text/plain".equals(type)) {
                String sharedText = extras.getString(Intent.EXTRA_TEXT);
                sharedText = getPgpContent(sharedText);

                if (sharedText != null) {
                    loadFragment(sharedText);
                } else {
                    Log.e(Constants.TAG, "EXTRA_TEXT does not contain PGP content!");
                    Toast.makeText(this, R.string.error_invalid_data, Toast.LENGTH_LONG).show();
                    finish();
                }
            } else {
                Log.e(Constants.TAG, "ACTION_SEND received non-plaintext, this should not happen in this activity!");
                Toast.makeText(this, R.string.error_invalid_data, Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (ACTION_DECRYPT_TEXT.equals(action)) {
            Log.d(Constants.TAG, "ACTION_DECRYPT_TEXT");

            String extraText = extras.getString(EXTRA_TEXT);
            extraText = getPgpContent(extraText);

            if (extraText != null) {
                loadFragment(extraText);
            } else {
                Log.e(Constants.TAG, "EXTRA_TEXT does not contain PGP content!");
                Toast.makeText(this, R.string.error_invalid_data, Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (ACTION_DECRYPT_FROM_CLIPBOARD.equals(action)) {
            Log.d(Constants.TAG, "ACTION_DECRYPT_FROM_CLIPBOARD");

            CharSequence clipboardText = ClipboardReflection.getClipboardText(this);
            String text = getPgpContent(clipboardText);

            if (text != null) {
                loadFragment(text);
            } else {
                returnInvalidResult();
            }
        } else if (ACTION_DECRYPT_TEXT.equals(action)) {
            Log.e(Constants.TAG, "Include the extra 'text' in your Intent!");
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

    private void loadFragment(String ciphertext) {
        // Create an instance of the fragment
        Fragment frag = DecryptTextFragment.newInstance(ciphertext);

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.decrypt_text_fragment_container, frag)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

}
