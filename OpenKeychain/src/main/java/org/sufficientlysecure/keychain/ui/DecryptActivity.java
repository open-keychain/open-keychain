/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpAsciiArmorReformatter;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;


public class DecryptActivity extends BaseActivity {

    /* Intents */
    public static final String ACTION_DECRYPT_FROM_CLIPBOARD = "DECRYPT_DATA_CLIPBOARD";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenDialogClose(Activity.RESULT_CANCELED, false);

        // Handle intent actions
        handleActions(savedInstanceState, getIntent());
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.decrypt_files_activity);
    }

    /**
     * Handles all actions with this intent
     */
    private void handleActions(Bundle savedInstanceState, Intent intent) {

        // No need to initialize fragments if we are just being restored
        if (savedInstanceState != null) {
            return;
        }

        ArrayList<Uri> uris = new ArrayList<>();

        String action = intent.getAction();

        if (action == null) {
            Toast.makeText(this, "Error: No action specified!", Toast.LENGTH_LONG).show();
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        // depending on the data source, we may or may not be able to delete the original file
        boolean canDelete = false;

        try {

            switch (action) {
                case Intent.ACTION_SEND: {
                    // When sending to Keychain Decrypt via share menu
                    // Binary via content provider (could also be files)
                    // override uri to get stream from send
                    if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                        uris.add(intent.<Uri>getParcelableExtra(Intent.EXTRA_STREAM));
                    } else if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                        Uri uri = readToTempFile(text);
                        if (uri != null) {
                            uris.add(uri);
                        }
                    }

                    break;
                }

                case Intent.ACTION_SEND_MULTIPLE: {
                    if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                        uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    } else if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                        for (String text : intent.getStringArrayListExtra(Intent.EXTRA_TEXT)) {
                            Uri uri = readToTempFile(text);
                            if (uri != null) {
                                uris.add(uri);
                            }
                        }
                    }

                    break;
                }

                case ACTION_DECRYPT_FROM_CLIPBOARD: {
                    ClipboardManager clipMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipMan == null) {
                        break;
                    }

                    ClipData clip = clipMan.getPrimaryClip();
                    if (clip == null) {
                        break;
                    }

                    // check if data is available as uri
                    Uri uri = null;
                    for (int i = 0; i < clip.getItemCount(); i++) {
                        ClipData.Item item = clip.getItemAt(i);
                        Uri itemUri = item.getUri();
                        if (itemUri != null) {
                            uri = itemUri;
                            break;
                        }
                    }

                    // otherwise, coerce to text (almost always possible) and work from there
                    if (uri == null) {
                        String text = clip.getItemAt(0).coerceToText(this).toString();
                        uri = readToTempFile(text);
                    }
                    if (uri != null) {
                        uris.add(uri);
                    }

                    break;
                }

                // for everything else, just work on the intent data
                case Intent.ACTION_VIEW:
                    canDelete = true;
                case Constants.DECRYPT_DATA:
                default:
                    Uri uri = intent.getData();
                    if (uri != null) {

                        if ("com.android.email.attachmentprovider".equals(uri.getHost())) {
                            Toast.makeText(this, R.string.error_reading_aosp, Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        uris.add(uri);
                    }

            }

        } catch (IOException e) {
            Toast.makeText(this, R.string.error_reading_text, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Definitely need a data uri with the decrypt_data intent
        if (uris.isEmpty()) {
            Toast.makeText(this, "No data to decrypt!", Toast.LENGTH_LONG).show();
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        displayListFragment(uris, canDelete);

    }

    @Nullable
    public Uri readToTempFile(String text) throws IOException {
        Uri tempFile = TemporaryFileProvider.createFile(this);
        OutputStream outStream = getContentResolver().openOutputStream(tempFile);
        if (outStream == null) {
            return null;
        }

        // clean up ascii armored message, fixing newlines and stuff
        String cleanedText = PgpAsciiArmorReformatter.getPgpMessageContent(text);
        if (cleanedText == null) {
            return null;
        }

        // if cleanup didn't work, just try the raw data
        outStream.write(cleanedText.getBytes());
        outStream.close();
        return tempFile;
    }

    public void displayListFragment(ArrayList<Uri> inputUris, boolean canDelete) {

        DecryptListFragment frag = DecryptListFragment.newInstance(inputUris, canDelete);

        FragmentManager fragMan = getSupportFragmentManager();

        FragmentTransaction trans = fragMan.beginTransaction();
        trans.replace(R.id.decrypt_files_fragment_container, frag);

        // if there already is a fragment, allow going back to that. otherwise, we're top level!
        if (fragMan.getFragments() != null && !fragMan.getFragments().isEmpty()) {
            trans.addToBackStack("list");
        }

        trans.commit();

    }

}
