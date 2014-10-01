/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.api.OpenKeychainIntents;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.service.results.OperationResult;
import org.sufficientlysecure.keychain.service.results.SingletonResult;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.ui.util.Notify;

import java.util.regex.Matcher;

public class DecryptTextActivity extends ActionBarActivity {

    /* Intents */
    public static final String ACTION_DECRYPT_TEXT = OpenKeychainIntents.DECRYPT_TEXT;
    public static final String EXTRA_TEXT = OpenKeychainIntents.DECRYPT_EXTRA_TEXT;

    // intern
    public static final String ACTION_DECRYPT_FROM_CLIPBOARD = Constants.INTENT_PREFIX + "DECRYPT_TEXT_FROM_CLIPBOARD";

    DecryptTextFragment mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.decrypt_text_activity);

        // Handle intent actions
        handleActions(savedInstanceState, getIntent());
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
    private String fixPgpCleartextSignature(String message) {
        // windows newline -> unix newline
        message = message.replaceAll("\r\n", "\n");
        // Mac OS before X newline -> unix newline
        message = message.replaceAll("\r", "\n");

        return message;
    }

    private String getPgpContent(String input) {
        // only decrypt if clipboard content is available and a pgp message or cleartext signature
        if (input != null) {
            Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(input);
            if (matcher.matches()) {
                String message = matcher.group(1);
                message = fixPgpMessage(message);
                return message;
            } else {
                matcher = PgpHelper.PGP_CLEARTEXT_SIGNATURE.matcher(input);
                if (matcher.matches()) {
                    String message = matcher.group(1);
                    message = fixPgpCleartextSignature(message);
                    return message;
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
     *
     * @param intent
     */
    private void handleActions(Bundle savedInstanceState, Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        String type = intent.getType();

        if (extras == null) {
            extras = new Bundle();
        }

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Log.logDebugBundle(extras, "extras");

            // When sending to Keychain Decrypt via share menu
            if ("text/plain".equals(type)) {
                String sharedText = extras.getString(Intent.EXTRA_TEXT);
                Log.dEscaped(Constants.TAG, "sharedText incoming: " + sharedText);
                sharedText = getPgpContent(sharedText);
                Log.dEscaped(Constants.TAG, "sharedText fixed: " + sharedText);

                if (sharedText != null) {
                    loadFragment(savedInstanceState, sharedText);
                } else {
                    Notify.showNotify(this, R.string.error_invalid_data, Notify.Style.ERROR);
                }
            } else {
                Log.e(Constants.TAG, "ACTION_SEND received non-plaintext, this should not happen in this activity!");
            }
        } else if (ACTION_DECRYPT_TEXT.equals(action)) {
            Log.d(Constants.TAG, "ACTION_DECRYPT_TEXT textData not null, matching text...");

            String extraText = extras.getString(EXTRA_TEXT);
            extraText = getPgpContent(extraText);

            if (extraText != null) {
                loadFragment(savedInstanceState, extraText);
            } else {
                Notify.showNotify(this, R.string.error_invalid_data, Notify.Style.ERROR);
            }
        } else if (ACTION_DECRYPT_FROM_CLIPBOARD.equals(action)) {
            Log.d(Constants.TAG, "ACTION_DECRYPT_FROM_CLIPBOARD");

            String clipboardText = ClipboardReflection.getClipboardText(this).toString();
            clipboardText = getPgpContent(clipboardText);

            if (clipboardText != null) {
                loadFragment(savedInstanceState, clipboardText);
            } else {
                returnInvalidResult();
            }
        } else if (ACTION_DECRYPT_TEXT.equals(action)) {
            Log.e(Constants.TAG, "Include the extra 'text' in your Intent!");
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

    private void loadFragment(Bundle savedInstanceState, String ciphertext) {
        // However, if we're being restored from a previous state,
        // then we don't need to do anything and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }

        // Create an instance of the fragment
        mFragment = DecryptTextFragment.newInstance(ciphertext);

        // Add the fragment to the 'fragment_container' FrameLayout
        // NOTE: We use commitAllowingStateLoss() to prevent weird crashes!
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.decrypt_text_fragment_container, mFragment)
                .commitAllowingStateLoss();
        // do it immediately!
        getSupportFragmentManager().executePendingTransactions();
    }

}
