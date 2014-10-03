/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.View;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.service.results.OperationResult;
import org.sufficientlysecure.keychain.ui.util.SubtleAttentionSeeker;

import java.util.regex.Matcher;

public class DecryptActivity extends DrawerActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.decrypt_activity);

        activateDrawerNavigation(savedInstanceState);

        View actionFile = findViewById(R.id.decrypt_files);
        View actionFromClipboard = findViewById(R.id.decrypt_from_clipboard);

        actionFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent filesDecrypt = new Intent(DecryptActivity.this, DecryptFilesActivity.class);
                filesDecrypt.setAction(DecryptFilesActivity.ACTION_DECRYPT_DATA_OPEN);
                startActivity(filesDecrypt);
            }
        });

        actionFromClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent clipboardDecrypt = new Intent(DecryptActivity.this, DecryptTextActivity.class);
                clipboardDecrypt.setAction(DecryptTextActivity.ACTION_DECRYPT_FROM_CLIPBOARD);
                startActivityForResult(clipboardDecrypt, 0);
            }
        });
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    @Override
    protected void onResume() {
        super.onResume();

        // This is an eye candy ice cream sandwich feature, nvm on versions below
        if (Build.VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {

            // get text from clipboard
            final CharSequence clipboardText =
                    ClipboardReflection.getClipboardText(DecryptActivity.this);

            // if it's null, nothing to do here /o/
            if (clipboardText == null) {
                return;
            }

            new AsyncTask<String, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(String... clipboardText) {

                    // see if it looks like a pgp thing
                    Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(clipboardText[0]);
                    boolean animate = matcher.matches();

                    // see if it looks like another pgp thing
                    if (!animate) {
                        matcher = PgpHelper.PGP_CLEARTEXT_SIGNATURE.matcher(clipboardText[0]);
                        animate = matcher.matches();
                    }
                    return animate;
                }

                @Override
                protected void onPostExecute(Boolean animate) {
                    super.onPostExecute(animate);

                    // if so, animate the clipboard icon just a bit~
                    if (animate) {
                        SubtleAttentionSeeker.tada(findViewById(R.id.clipboard_icon), 1.5f).start();
                    }
                }
            }.execute(clipboardText.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if a result has been returned, display a notify
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(this).show();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
