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
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Notify;

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

        String textData = null;

        /*
         * Android's Action
         */
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            // When sending to Keychain Decrypt via share menu
            if ("text/plain".equals(type)) {
                // Plain text
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // handle like normal text decryption, override action and extras to later
                    // executeServiceMethod ACTION_DECRYPT_TEXT in main actions
                    textData = sharedText;
                }
            }
        }

        /**
         * Main Actions
         */
        textData = extras.getString(EXTRA_TEXT);
        if (ACTION_DECRYPT_TEXT.equals(action) && textData != null) {
            Log.d(Constants.TAG, "textData not null, matching text ...");
            Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(textData);
            if (matcher.matches()) {
                Log.d(Constants.TAG, "PGP_MESSAGE matched");
                textData = matcher.group(1);
                // replace non breakable spaces
                textData = textData.replaceAll("\\xa0", " ");
            } else {
                matcher = PgpHelper.PGP_CLEARTEXT_SIGNATURE.matcher(textData);
                if (matcher.matches()) {
                    Log.d(Constants.TAG, "PGP_CLEARTEXT_SIGNATURE matched");
                    textData = matcher.group(1);
                    // replace non breakable spaces
                    textData = textData.replaceAll("\\xa0", " ");
                } else {
                    Notify.showNotify(this, R.string.error_invalid_data, Notify.Style.ERROR);
                    Log.d(Constants.TAG, "Nothing matched!");
                }
            }
        } else if (ACTION_DECRYPT_FROM_CLIPBOARD.equals(action)) {
            CharSequence clipboardText = ClipboardReflection.getClipboardText(this);

            // only decrypt if clipboard content is available and a pgp message or cleartext signature
            if (clipboardText != null) {
                Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(clipboardText);
                if (!matcher.matches()) {
                    matcher = PgpHelper.PGP_CLEARTEXT_SIGNATURE.matcher(clipboardText);
                }
                if (matcher.matches()) {
                    textData = matcher.group(1);
                } else {
                    Notify.showNotify(this, R.string.error_invalid_data, Notify.Style.ERROR);
                }
            } else {
                Notify.showNotify(this, R.string.error_invalid_data, Notify.Style.ERROR);
            }
        } else if (ACTION_DECRYPT_TEXT.equals(action)) {
            Log.e(Constants.TAG,
                    "Include the extra 'text' in your Intent!");
        }

        loadFragment(savedInstanceState, textData);
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
