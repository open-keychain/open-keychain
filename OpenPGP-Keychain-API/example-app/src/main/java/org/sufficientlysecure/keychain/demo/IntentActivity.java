/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.demo;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import org.sufficientlysecure.keychain.api.KeychainIntents;

public class IntentActivity extends PreferenceActivity {

    private static final int SELECT_PHOTO = 100;

    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load preferences from xml
        addPreferencesFromResource(R.xml.intent_preference);

        // find preferences
        Preference encrypt = (Preference) findPreference("ENCRYPT");
        Preference encryptUri = (Preference) findPreference("ENCRYPT_URI");
        Preference decrypt = (Preference) findPreference("DECRYPT");
        Preference import_key = (Preference) findPreference("IMPORT_KEY");
        Preference import_key_from_keyserver = (Preference) findPreference("IMPORT_KEY_FROM_KEYSERVER");
        Preference import_key_from_qr_code = (Preference) findPreference("IMPORT_KEY_FROM_QR_CODE");
        Preference openpgp4fpr = (Preference) findPreference("openpgp4fpr");

        encrypt.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent(KeychainIntents.ENCRYPT);
                    intent.putExtra(KeychainIntents.ENCRYPT_EXTRA_TEXT, "Hello world!");
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(IntentActivity.this, "Activity not found!", Toast.LENGTH_LONG).show();
                }

                return false;
            }
        });

        encryptUri.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);

                return false;
            }
        });

        decrypt.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String text = "-----BEGIN PGP SIGNED MESSAGE-----\n" +
                        "Hash: SHA1\n" +
                        "\n" +
                        "Hello world!\n" +
                        "-----BEGIN PGP SIGNATURE-----\n" +
                        "Version: GnuPG v1.4.12 (GNU/Linux)\n" +
                        "Comment: Using GnuPG with Thunderbird - http://www.enigmail.net/\n" +
                        "\n" +
                        "iQEcBAEBAgAGBQJS/7vTAAoJEHGMBwEAASKCkGYH/2jBLzamVyqd61jrjMQM0jUv\n" +
                        "MkDcPUxPrYH3wZOO0HcgdBQEo66GZEC2ATmo8izJUMk35Q5jas99k0ac9pXhPUPE\n" +
                        "5qDXdQS10S07R6J0SeDYFWDSyrSiDTCZpFkVu3JGP/3S0SkMYXPzfYlh8Ciuxu7i\n" +
                        "FR5dmIiz3VQaBgTBSCBFEomNFM5ypynBJqKIzIty8v0NbV72Rtg6Xg76YqWQ/6MC\n" +
                        "/MlT3y3++HhfpEmLf5WLEXljbuZ4SfCybgYXG9gBzhJu3+gmBoSicdYTZDHSxBBR\n" +
                        "BwI+ueLbhgRz+gU+WJFE7xNw35xKtBp1C4PR0iKI8rZCSHLjsRVzor7iwDaR51M=\n" +
                        "=3Ydc\n" +
                        "-----END PGP SIGNATURE-----";
                try {
                    Intent intent = new Intent(KeychainIntents.DECRYPT);
                    intent.putExtra(KeychainIntents.DECRYPT_EXTRA_TEXT, text);
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(IntentActivity.this, "Activity not found!", Toast.LENGTH_LONG).show();
                }

                return false;
            }
        });

        import_key.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent(KeychainIntents.IMPORT_KEY);
//                    intent.putExtra(KeychainIntents.IMPORT_KEY_EXTRA_KEY_BYTES, TODO);
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(IntentActivity.this, "Activity not found!", Toast.LENGTH_LONG).show();
                }

                return false;
            }
        });

        import_key_from_keyserver.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent(KeychainIntents.IMPORT_KEY_FROM_KEYSERVER);
                    intent.putExtra(KeychainIntents.IMPORT_KEY_FROM_KEYSERVER_QUERY, "Richard Stallman");
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(IntentActivity.this, "Activity not found!", Toast.LENGTH_LONG).show();
                }

                return false;
            }
        });

        import_key_from_qr_code.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent(KeychainIntents.IMPORT_KEY_FROM_QR_CODE);
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(IntentActivity.this, "Activity not found!", Toast.LENGTH_LONG).show();
                }

                return false;
            }
        });

        openpgp4fpr.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("openpgp4fpr:73EE2314F65FA92EC2390D3A718C070100012282"));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(IntentActivity.this, "Activity not found!", Toast.LENGTH_LONG).show();
                }

                return false;
            }
        });


    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();

                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(
                            selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();

                    // TODO: after fixing DECRYPT, we could use Uri selectedImage directly
                    Log.d(Constants.TAG, "filePath: " + filePath);

                    try {
                        Intent intent = new Intent(KeychainIntents.ENCRYPT);
                        Uri dataUri = Uri.parse("file://" + filePath);
                        Log.d(Constants.TAG, "Uri: " + dataUri);
                        intent.setData(dataUri);
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(IntentActivity.this, "Activity not found!", Toast.LENGTH_LONG).show();
                    }
                }
        }
    }

}
