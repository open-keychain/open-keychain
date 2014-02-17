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

import org.sufficientlysecure.keychain.api.OpenKeychainIntents;

import java.io.UnsupportedEncodingException;

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

        // To prevent Android Studio from complaining...
        assert encrypt != null;
        assert encryptUri != null;
        assert decrypt != null;
        assert import_key != null;
        assert import_key_from_keyserver != null;
        assert import_key_from_qr_code != null;
        assert openpgp4fpr != null;

        encrypt.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent(OpenKeychainIntents.ENCRYPT);
                    intent.putExtra(OpenKeychainIntents.ENCRYPT_EXTRA_TEXT, "Hello world!");
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
                try {
                    Intent intent = new Intent(OpenKeychainIntents.DECRYPT);
                    intent.putExtra(OpenKeychainIntents.DECRYPT_EXTRA_TEXT, TEST_SIGNED_MESSAGE);
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
                    Intent intent = new Intent(OpenKeychainIntents.IMPORT_KEY);
                    byte[] pubkey = null;
                    try {
                        pubkey = TEST_PUBKEY.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    intent.putExtra(OpenKeychainIntents.IMPORT_KEY_EXTRA_KEY_BYTES, pubkey);
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
                    Intent intent = new Intent(OpenKeychainIntents.IMPORT_KEY_FROM_KEYSERVER);
                    intent.putExtra(OpenKeychainIntents.IMPORT_KEY_FROM_KEYSERVER_QUERY, "Richard Stallman");
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
                    Intent intent = new Intent(OpenKeychainIntents.IMPORT_KEY_FROM_QR_CODE);
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
                        Intent intent = new Intent(OpenKeychainIntents.ENCRYPT);
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

    private static final String TEST_SIGNED_MESSAGE = "-----BEGIN PGP SIGNED MESSAGE-----\n" +
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

    private static final String TEST_PUBKEY = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n" +
            "Version: GnuPG v1.4.12 (GNU/Linux)\n" +
            "\n" +
            "mQENBEuG/qABCACZeLHynGVXn7Ou6MKE2GzSTGPWGrA86uHwHPUbbTUR7tYTUWeA\n" +
            "Ur+l+lR3GRTbcQY4ColGUcDcTVlW/cp9jhHnbbSIS0uJvW+4yu3I5eSIIoI09PLY\n" +
            "KjT0U5l2z6t6daL7qWfZ1pQkCuCMe43eMLBPvyao1+zEd1zESbMz/bySZRlYMKAC\n" +
            "aD9pGnFHS+EOU+lQXxfzCpKEQcHmPrrBFh2Gr2JFWWjZArKh7B1lQLekD2KS8aFb\n" +
            "Lg1WGo5tK1sSk6MnMmqs1zNw1n15p5UDnJ7Qh8ecfMyDLy/ZyUjfFjy4BE0p+4mS\n" +
            "J5iDU0pTYK3BpdfujY6NE+S2Ca2J6QoNRN8XABEBAAG0MURvbWluaWsgU2Now7xy\n" +
            "bWFubiA8ZG9taW5pa0Bkb21pbmlrc2NodWVybWFubi5kZT6JAT8EEwECACkCGyMF\n" +
            "CQlmAYAGCwkIBwMCBBUCCAMEFgIDAQIeAQIXgAUCS4b/ZQIZAQAKCRBxjAcBAAEi\n" +
            "gjHWB/9w+D8DOxOGeUzNxfGn98C1nYVkt8zNcTnODBd8VsaPx1pKOXUu6IfqaTxa\n" +
            "qS4hsAmgV9l0xLA2CkRndZAangsl3ZwURh8UiX/uqJRA9c9py7O+8GxpARtwtOPQ\n" +
            "VxXx/O8vkXxFpYsFzpotN5XlGkLWWVySotKbTcSfaBifcS3oFT+d6VAZ/D68iTaH\n" +
            "YBRfwaganevuqUsrJQiCOX11d6Lnr5cDzvmR2yagsLZPUi3CI02bVZYH99uNAr8b\n" +
            "O7OkrgbcN7U5VlMuXdzj5fU43QpAzrT11JY9jmYsxbJ3t0Zgb5tnGXq9UBkPgGKC\n" +
            "T01QW7aKHBN5YeDtOY1d0DQMNLkKiQEcBBMBAgAGBQJL/m2NAAoJENjTIPvvLEnw\n" +
            "9TsIAIEV6UjXLSfxOggm7/0pG43P6OP1HvItUCg/wPfLexHGKJQAh2SuotUNNq7h\n" +
            "i67PajHBgS/iZNrT868sDWGN5FT2toFOsY3YMTwv1GpsZGTSB7my+ej0Lq5OxCkD\n" +
            "1rGsOLhetFzIqRuybmLRgAWxj9tg8ib2bkpI1S+67TRtrDkbKUdVrEvQp4rhItNl\n" +
            "L17mxdViOkmboNgdTxlfcjyRh96dkAMgzKL/LkwKsgFxYYAOOKGodg1pOGdeTku6\n" +
            "c0h5UmTY3DwLiY4FeAIzx3L8TYru3wx+if3j1MuVKRt3p51fX5/4dmyfWbrSRPO4\n" +
            "8fh7RM5JMWtcI9DKLoFIhydppl+IRgQTEQIABgUCTFF5iQAKCRBlHG1tQKlVAhQN\n" +
            "AKDTCAl9IVdBuuu7NEi3LOKKi/8ekgCeMG5UX1a5YrH7E7n8AqOk9efihuKJARwE\n" +
            "EwECAAYFAkxReagACgkQVhWX/ckAx3iKEgf+PinRsmf6sGSM7HMZigJPGQuw81jM\n" +
            "LPC0P3qc9wHpqCpoGcKzZO0wKKKdQc6uKwJUMLKyJeK7BwwJz+r75KWHz8zeUAoY\n" +
            "Iyqf5ukM26tADxr+oDpOJegHkvucAdKQjKDu04cXBkILRJ3lnytpqE6tiqS1v1Jx\n" +
            "SvgdtdC6WHHgpwJPRqtZz4KPllQ40SyrNhzgZ4V8qFNrt1jhO+6Z1rgyTEDwUwxM\n" +
            "VCUz8fIQX9Ic6il4XfwKKt4kA+kiQC8Chs6hSkUERyNZwNAkToUnhwfcULpWnDnz\n" +
            "q0mKBAhLe++KaigVwzXMFVGoy/YaYR35dxLkSlJEZC2CP962vO4nfkvrFohGBBAR\n" +
            "AgAGBQJMpHMVAAoJEOQ/9ZAjofikEHYAoJz13rWjPl7S1VCSLO48qPq6DEN3AKC7\n" +
            "HX/YUbDR3iskXSRDICKUwK8P54hKBBARAgAKBQJNnEjdAwUBeAAKCRDFl97MF3r9\n" +
            "fEVRAJ9AobzDC3824/Flhk4aomgwFuIJ7QCfbyXutcVsiFk3GJNa4jSThR9/4yuJ\n" +
            "AhwEEAECAAYFAlEFpa8ACgkQskE8Zt0sP+rkyA/+M2E53FXWzjlRttQQsfY9kH+/\n" +
            "nQ6x7HAEsa11rysB1seM4JJGXsvyp8e2KHgxAgwcOEbWt56NB5Vlx2qms11iEC5l\n" +
            "JrqTajWmF7sWcHxFaC8RVm+A0HHK0qWGga6T8E15izsKUM2esDJloWoYqA9Ddfdw\n" +
            "B0JXH3F8u+2OmWhgU08jIyDt2iM7HMCFssben3VlXribsCP0etOeHFITWyZXOCKZ\n" +
            "A/zotpLd9LdT1Z9fKFVlgdQbt9PNyxKraUuyG7TAYgU2tZMuDZ0y+FKM/MfsdWHL\n" +
            "9jBbzPcWYgh/GBkHAEpmuuX5KBVEJAQ+zCv+lBuHGZ0srLH/YZZNE8fSc99kC97i\n" +
            "IlU2Rj5IkiDySxPeEx3LWDzINV/vPejJ6XoWb6LuV/8PzRIWOdutaEdyjcFwQo0K\n" +
            "Ht9B3oETz5idGH200aYHJy3Ex3/vvX0xAyUL3naW2ipZH2CLivuD7H4LIdqC2ukh\n" +
            "BxKB0gufCGqc2YBB54jYU9fvCPGJJ7NYqymYKTBf0DKHe3zVCRkdxQYyLkWCFP3m\n" +
            "izpxSebbhdfdLbCSssRDA1e4nu8EASDuxDaaz43b+56KZ3Ca+/Q+zQopgaHNHuSy\n" +
            "7CIo9qO6okjCiLO7FkeeJxmD4GAGi9hSHGiW3hR/h3qv3XHjExeys7AFSVRyOXpo\n" +
            "DrGwnadlYBkAh8tMbYWJAhwEEAECAAYFAlEFzk4ACgkQ0DssUbNe6zwDuhAAjhwP\n" +
            "HGUwvS9YsF0+D8Vp8OSYh1Q0/S7iSSVLT1ekilpbOdCIXXETNz1Mphe/7LkRpSmn\n" +
            "Y6AjHQexvDHJB8VTGO06LJcpaNl+FRB5z7rmmpRSR6zl6mxuNo6UmM8UKxoFnXvk\n" +
            "Ta23uBo1Q3CylrX5B/6UrhGS+0WiJE/cbfAKWTvOQqD2s7j0AMeJgn/qcInziHik\n" +
            "W+0PWuHWjUCpy/BPNWDIsLn5ueYoXSmgUeGuWmI9VnyC6PdjWzxE+2QzYEpBANYQ\n" +
            "Yd09QaBvhQA8/p6FBV4Ofa1ZdMcaGCPA+QSA355a7+uRElzSHa3tN3vThhdwKOjO\n" +
            "d8l8baYq2RcXVELU3fjDwp6/L00ldSyV0HHlbBLZZ8RGpB76e7LEMqE14DzRkyyp\n" +
            "r0m1SkDzSYWcTIFuxGnZ1t2/FUe8Vt2WW8UwlKiHSo3VDQzLxvlGpkmV9uV4Z4FE\n" +
            "+LMFKSZfIa4qG0LUQeaJ8OZ8l1yZgZPkwtuGI2qKh7XFchfB79PIB2QUtMlAzoZf\n" +
            "CxPF8ifQ3hGtHaKI83LGaQpTHEkzPZYHzOEskSt5oGIPg90wq9c/bbN8nPuwmg9h\n" +
            "Lzsf5+V5ZZuJcSOQd8Rbx4Juj5NI72jvH0BY7XtVSWL6itJsqbvU8YzCbEcOMAQJ\n" +
            "jxuvTAQ3BnkOrETgH66TRjFPodTikZOG6hUbLaqJARwEEAECAAYFAlEGSowACgkQ\n" +
            "Zf77hz/aPZlvmQf8CJwn0S5l7fF1MMgFDs1gVYQLxNPDp+w+ijdiQy9AbHL0eeSA\n" +
            "5wFAPbMKNke7DiaGlXuJZkGjYE/gelVdiauno60R8sO/V8X0FXQNSb/XLaPVykzF\n" +
            "ajQBwcvCEGsyNIuYRIQpuKS9eROx+eZS7/Ez4bao6rOEIGWiormSjkUybkxnzXIR\n" +
            "i2fKUGrFaxSmRFmG3WiozudD5lbY/HD8d8ofZQrhbWGYRsG30VzZk0XY+CkofwYd\n" +
            "MEoooBc5N6vtskHLkl8Z91laNphC6pk4QQ9EOPfoxE9t9ZyT2wiF6YkysXMOUeJB\n" +
            "K6BD8Aim7HarCZU74C1permfnE43CKoSEk0HzokCHAQQAQIABgUCUQZgWwAKCRBi\n" +
            "udz/ZiTBorLyD/9J7Ub2sogWskFA8SPEw5SaCyAgxpNzb7ykJ6Tb1z8zq29be/zS\n" +
            "BsRWgcQWtOXPc55uhSY0Jwaw07ejT3/fQIDLZCWvXmPBgTh6OLdTJYZWNimBHDp5\n" +
            "8H35ZBdvEtha6zCGkA20c/F3dnrz56dPFeI8c4hHu0LBOzqZYgoRHFh85fAnHTHc\n" +
            "TfdvZ/obNeV0NhyKJZQgZepKdAN2Z9cXIHkfdjLeGuA6CEJlVBMk96BvozNqm/4X\n" +
            "KC+NGxbp5J+yARb80gT56/OGaB08WotRUcFRub6fc7gT7QmsXrx8YWYaBIWlhW/U\n" +
            "/yliJdZeMawKc7tgLkpf6qM1GerTgzkf44r3rXHl+ImlizdQhFhUGJFbkisiqYzU\n" +
            "caMmNkPWNg28e6dcUU7nfprt99IbSSdhPAxPd6B6bzUawwV/VnpNXIrH+048lgsh\n" +
            "FO45JceUfSadpK2p2UXVUP/TfYYY2xBwvoCjAsD4Q9YYheJsWh7i3xi9/0EzfSPB\n" +
            "1NpLdaujAO8CuTTZ4NlpTLOZFmksQSF4BSpcPC+8TUGwAKXIm2wwIUNm1sBwXNsZ\n" +
            "aOtAz3vQH3pKTzvXQEoCMCqoqkYS+uTGilwhmlSivfenB7DT3TX8DHnUzT1eNlh/\n" +
            "PfCmdufvGS4BodAYPgTfmq2B7bpohFOXzjwiIJ4AJfaYhdyQ/PqN9JLShokCHAQQ\n" +
            "AQIABgUCUQZr1QAKCRDIIhUzoaoywpfcD/wJveyGF8c24F6O5O2LX7yWmJ9LWs9t\n" +
            "9z7c0gMom/eQMyoFh2VWHnBhNfR635RtSWFasMJSEpPd0iyIz95eaXpsjdn408jC\n" +
            "8yGZD8N9EMkSbtGrcMExoyH+Tobx2Xs0mBDrG0TZdK07dW3q75asNFtU52isxC60\n" +
            "WTJKUJud5vfms0cnp+sRCewvwssOyaIYzqg9J4/GZIpY6d9Roi9u+7FIUnzQJN4d\n" +
            "zsDxBOsKZAVnyDaaaDMWXEV15BVwwFCap58e5HzL/NKK2uJMyMwBpPjZM4CYiWL+\n" +
            "DVNvedids4iuNezyYKJns+LSo7zl/Rv1IQKPhg6BRbxgKu+DZt7iYI0vEl7PJNIM\n" +
            "Ex77BZI/DUVZWXlE9g1bB4sP3iMBq1998VvWsTbA6CLRMYQvN5GWzlPyCICx2N9V\n" +
            "dRD8Y+ySBtXMSLTe/kMXvnpMvYl5Yree/mSRirqA5Z/sZtw+SpTdtXEgoPpuCouc\n" +
            "KKvxWkyDZX3Ehre/wDsOFN9XZfEp79SYym27rqMr6QPx6UQOPYd05soqM/OHBP89\n" +
            "OCQqcbKkdcmv+bvGHdrlMjSfQGf3sBHy2TdL0LzzDOiGhoMU4Q4QGkrVYZwzpLDv\n" +
            "InPbtO9GvAiJeZpgZyyvUHwGrmJDlpO3QPBMJj1AxOc06mJmSwg92ziIkK+jX8s9\n" +
            "xlxcKmFXxTFUVokBHAQTAQIABgUCUQbiuAAKCRCylLwGx1Uqeo/dCACjY9xvAup2\n" +
            "MCcs2nvHtKCy3NVmzm9Qsc5hferWJ8xPqUEi+OrpeyknWQWMQUlwCTRX/5I2HLh4\n" +
            "PI4ycieWGiNh1FLbAcTW+xqkjNfE1iAmD3h/c2wBqlsMIdTnPNKFD1zhRAjixn58\n" +
            "so1uW5+sTmPs6VVU9Ll6hcr+LzsUoS9t/sHOD53KYG3JKeCKLMzG95Ev8yJvxYZg\n" +
            "DC2NZZXEeQq8XM59gpBoGrTx0xmWIFio6w3XIYHlhwcvNormrpbaZDxq093qy2hx\n" +
            "AWVVSb+Rby/Vp4AsJyoQ6p0DzWbxj97o+rFV/Av0pgEuKnhpnpyB5mDvhwPGvWwp\n" +
            "xGi79eZYSN8qiQEcBBABAgAGBQJRCCrMAAoJEDVPpeI+qihik/EIAJXinMI3lDhV\n" +
            "KbhE6PYQLwfd8OBrV8fX4/vQbzosjCQBwiZFot1LO4aRAZgLcAwMoxDGQco2h+bf\n" +
            "UWskvhMGCC7rqvDkmoalGfQ+IwKnnHeZAghM1/Dd/C9ijl+2LQeeNlcaaWsMTjcV\n" +
            "q3cZtPInLJfJpci8nhDET4dHGl8tai80Oen30Wd19nDDaeL4qeF3E7YaPIwcg7jR\n" +
            "PF2fYQBIfh7+1Y4tbhAqyLRgFx1aB+nqgVbsLtRXEK4OTPeigN6cEawot4XRC+nR\n" +
            "Qtp2ZqDTzOF5KH9tG9+S5cQZsTUIFtWevBxrIg8kimIt6sOxt1wkeipb4QPBbkjS\n" +
            "+6zkLMhO8I+IRgQQEQIABgUCUQq1+AAKCRAEtb81V3CDSkjdAJ9Hq4iFYWxNRpJc\n" +
            "Hqv2AH1F4yWtjQCgvGQK6MsOuO9QBcqFJmVBHPUPWk2JAhwEEwEIAAYFAlEMbTQA\n" +
            "CgkQrpk18w7U8BBHfg//be7uVhY/hE04S4NrR6IG/k+9gMQHmH6OAylKWvfd8CuV\n" +
            "npd3ZmZGosWAxRaizaET8OPATP+Wojbuved0/dFL2cras/+GKWleuWI3qxFAAqSx\n" +
            "SGDilpladdQZbyHfrAHWFwpwEl51wsc9LkcAd9lywv0wbPzV4oFqEqbPM+wNW5jS\n" +
            "N1W5doj2A3MUw17ocRtk2XmzhF819w++t16Alweg0QrfEx5mwli/Z17FcYUC21Wc\n" +
            "04eDL8s/j3U3SYjBvzNIrtx2JiS/MdtewjvAWAeEoFusNcwbYn8J+2qiTrh3twR3\n" +
            "AU7xU1s2a3GxjjQ+J/HXr52Eujd4nk/V8Au+NYAWlCoR99ByVfzG7tsjaGPysHb7\n" +
            "KkGVIAlXM0brKwQvRhvGsN0+8mjfM+xl58AVV+w/K2MUTyWHyAMigvXtK0lBGcHb\n" +
            "YATLBD98dUTkeF49oHAFriw2fLqes4ayqqouiLj7RfNHDQ4X41PkxlaSq8GrXHig\n" +
            "jUKKod//taTViZU2JYL9ZIAGzDaV483pVZpQlBqeWBaaHTlk/fBrFfIxQ8gxrAX+\n" +
            "Izano++z5tFPgKPBFisDcLt6g5x53ADh3Ax94a0sR8aoBzeJYlvRLG2OLuzok059\n" +
            "CcvAG/lCYJTpz3LSsWdV2VqDk9LLqh+uzHmA1SVvYyxHz6IEYLGxNhEOPqC4JDSJ\n" +
            "ARwEEAECAAYFAlEZK/kACgkQ6kRcQQePqI3KNQf/cAik9KuU5q2LRzagJLpVqIGg\n" +
            "f7Yu57EQ5yENFbL8BJoVn/CMXsx8btDeouGkUXYVDtn5ThrGOHAs2OYEQSF3HSFp\n" +
            "xqUci5rVLBoYwq8WcbGihg/YA3T1m9T+hGx7uhvDQOUDxjggcwxuTaHGmbIoDHaw\n" +
            "BtlS2iznyku43ip0yazqrz79CPTJ7DrGe25q6ApVXhZeZ0Tmj2qa/ZB3nwqW5mov\n" +
            "0UqIVBoyO4WIP/rMGdK7e1HUu5vpsaeAJKBdU0FMADuDc+vVuQildwIejSxW6etZ\n" +
            "fHiKX05gZjN5KHfrjxaCr0Tv0xBmJ8QhFe7+4qZlfEEswUGk3gXUK5nkatEn+YhG\n" +
            "BBMRAgAGBQJReqVtAAoJECuuPPba/7AA2rgAnjbHP4UG4AU4DjSjIK+gAwN9Ekxz\n" +
            "AJ9/LSzNx/UzZYyw2qXMOdzXODTX67QqRG9taW5payBTY2jDvHJtYW5uIDxkLnNj\n" +
            "aHVlcm1hbm5AdHUtYnMuZGU+iQE8BBMBAgAmAhsjBQkJZgGABgsJCAcDAgQVAggD\n" +
            "BBYCAwECHgECF4AFAkuG/2UACgkQcYwHAQABIoLM2gf+Kzd0jobczAyFcvK8Tpu2\n" +
            "ica+3Cd9ZL9PmzCHKO5S7mAgzKGuUSl7/IUG3vuu+ijfVg6ujsSW9QjKptFZAU3C\n" +
            "/r0LL2+wUFjGscVCkE3ovFEZ+jOWUDUVQoKFN0h7ue1AkYtilYUNwHcKENxeqLAb\n" +
            "+nAl6U43eRUVe6IbHBtQcdszZ81C6R6Wm5qGCTRaZVWWhW4iRTxw4XMvZ5jeXO6U\n" +
            "2h7WOiqlAv0QJr7xARVp0k1qMGSKVMaoqvHj0oai4VeBBDMuYYjfHMo9Yo8beKPY\n" +
            "pmIAy96y7oE/Lb/WYkPcoZ+tmPVg8ZwvlZTTAbrEUlV1xBEjs8/3ldB/qn3Vf8q1\n" +
            "6YkBHAQTAQIABgUCS/5tjQAKCRDY0yD77yxJ8IIPB/9BcmtqFesUgLavyEGTCQu7\n" +
            "9DGDcn4oAQNnBxrIO4Am2jfnEwGt0b+9kIl06PG2zNMxhA8NIFxc6XGnfvqr3BkZ\n" +
            "gCN9dgNPSXQ4XazESylJk7F3h5yozAel2ZLy4lY04Sy63n/3J48coZaLSPLoUDq0\n" +
            "2gudqQBTG+sLs69PLTrwYdp4kZpJunmenpgcGSxqpaf0Dvo2Fvq4ftRre4pjaNzQ\n" +
            "9ZXsWJbC16boJd7Hbo/7oZNNMvZC2XU3PxhiEPhwGP6H/Sjv53MtGNp3/Hcjef1G\n" +
            "TFQsN79m4FHBB+VnRa7wieZXa3cQWy2RamxuVW15fiaZvAs3pKzvdDwSrTFuVWqv\n" +
            "iEYEExECAAYFAkxReYkACgkQZRxtbUCpVQJM8QCfWIZL9tcmOuVe1hHq6la4GBWI\n" +
            "QFEAoMdagHqMu/YZ+jeffnD0XzojV5Q+iQEcBBMBAgAGBQJMUXmoAAoJEFYVl/3J\n" +
            "AMd4cicIALpzq7i2P29c5C0a+cvJsVTGJWA3tQyi+BpCMtIwneqWH/ojsh0vM/KK\n" +
            "e6jrUAmQ8kQkLGHbMpDTlvVyhGw5kO6WSlIDKAx8TmzmMa/wuCBR8g8zi27fx06C\n" +
            "RQ7NcDJy4AmU2cKzK7rKpPkLTBHf3zNbUoISJW+icf2tfMBjsJ5tS6o54+f/zhnf\n" +
            "QM+S9IdRgfH2by59J11H+Oykiy0I77jMNXO04LbMfp/ZqJE1Cwa1piygNodBeWfm\n" +
            "mlB4WzCiplKJDqVCK6pQHjAnv7f5O3O1MH7w5FTjE/AYSeZ1AZtHbjv8QeXLbRuf\n" +
            "S3amF3w3yjZIpLSmp8DD3umJe2lpWaOIRgQQEQIABgUCTKRzFQAKCRDkP/WQI6H4\n" +
            "pIrtAJ9Ri2cWkWnJbvgYjCWF0mNCV2Zx4QCeJBeudrxAYqwTmU5clrFGb8kiuSiI\n" +
            "SgQQEQIACgUCTZxI4gMFAXgACgkQxZfezBd6/XwgQACbBOBJSdnAeuJHufGi7ETZ\n" +
            "cwRYGxcAn1B3NirdNJ8dJ6bVV1qEKoJZgpzUiQIcBBABAgAGBQJRBc5OAAoJENA7\n" +
            "LFGzXus8gCgP/jfftQRpM/PyGwW4O7w9lDf3EyWshqnoO4MGNEy1wX4TW48vpOZs\n" +
            "KMR1/e1r7hTIC0HXQIfUWGSWd2h+FJIVO36sGXqwgJnopOe1S/3W7MsWa8zfkZEz\n" +
            "fNXWmK8PUNIGc5hOFxfbAQk+4ZpRtu6nqnlZ4bqP9tDyZ3673jkbth2W9PqNifE7\n" +
            "wzKOYUIW/cWkyIh3HZLLKpLXu6o5p8P3nIP2IuznybPqNFMfhaFghYT7bWWpixLE\n" +
            "K2svy7tGKvhJAxfnGvtEYDzjhyh6L2Cmm0X+c/4HcLLlCdErE4tU0SjQSaf+HDce\n" +
            "+WmwpHL2q+EX8Yd2C2rM2vm5wMVZPiH5GL9Q1O+B+xkmmD7I2TEA3B7ZGw4pZpBz\n" +
            "X0U6QhRIM+ojMfFYkE03+S8kkhQdFjtPDEIJXAIkCZ/bROa1ByBuVdLm0TQAN1Hg\n" +
            "m8A39kylJ9kHXPiuQAYggbh0ynx8PYv3w+IxDg2lSnjz/pUrOBmGJ3Hw1MZU/9mj\n" +
            "riwmxOGg2LqAQ1uJN51pDFV9TGE6WoGi7cob3F1zLrzMZC75C+WpzWlur/gA7vIJ\n" +
            "Y9NSjND+FOIo8F/oQJO/PyfC1bEI5X/ofQ9yNTKBa9xHIxx+lgiCrDVlbD/pQQBT\n" +
            "9TyvhT2qjSdM9ipN19c8Mpc3pJrrs+RY96r4u4tZC/AO3+2nW7kWpmqiiQEcBBAB\n" +
            "AgAGBQJRBkqSAAoJEGX++4c/2j2ZTAQIAKXOo0o4XUzPrMKRBBj3iGTGFZ1ABZ+1\n" +
            "Zs99t+I9Ksy2LmPsQ96CwK2AzqfbcOlZ9+eMCzYhfX9alvJ7Ms5CTkKj9xo9wDcg\n" +
            "/fzqG+xVlt3oXeLMc1juW8nLLKhLBn4vELmjh4JuvkjEaMaGwZCbeAQKFyXtZQOq\n" +
            "YxcKnq8Fe7xW+rHdB9F1m4uCKRW2L9IglUDlOiflUTvCt/3blea936mzsDPhoQJO\n" +
            "O+zGCF0NXbvJ4lzQmgyWpvd81mbN/DQ055UQjG1DNNS6q7O/EqNRy5Jnv1/qSCAF\n" +
            "+afVQgrDvrvcAuQRUfw3i66HXNGEm++43C6K+0fkPteh8ESx+H1WSgeJAhwEEAEC\n" +
            "AAYFAlEGYFsACgkQYrnc/2YkwaIpgA/9HKKyfO5oJpV3bUXf0IZGTgrVISiVY90t\n" +
            "IbX0qkyGVFwEovp8eRJ0B6cluQiNypjKY+5xh0wYbexk2El53dNDkTfisfVM5ib7\n" +
            "a7aMAMQ6R99zFVtag/eXmAWzKcL8x3RdVyRFSttwrGwDlrv8VpQByYYSnUPWvzZs\n" +
            "YJQL+XgHqVLzK16/oZ5rzBvzbIH0oFm7HoGqKsRGDEL2hkNRhjuDlxrzijSqQfqY\n" +
            "qhMqQAjf6fenpVFFPXr8TY4RXRRcBFq1aP3Xp2Vq1ekwgzHTokryWEyZTdsVXoMU\n" +
            "Tmjk/sZ4R61N5YW3EEyGuG2E9wgZD8FmUElJTAduZPH16JcfO4KUXsNSXap8yKJ6\n" +
            "yZ47TvbNcwQq7IhxbwimhaR4pbBpcOfQEpdHA24csOBPJ5Ly02LpAs0ZuhmOvDLW\n" +
            "Yxxr9++Sm+5UBcAMav+N69f3lUnIc/MhDI0uYLe762z7cs5opIx8Fr64GZn30SY9\n" +
            "OMpce9UwsmhurO9T/0CKpKeZEynKUHcCWgsdsbDULhuCLr9WypzCy9wJm79bYwqE\n" +
            "sAJGqK5i1Qxdp0O7VJkPaR1FTdTWazW6phWCnlskpWKtRmu09v9vosqnzd6vSKqo\n" +
            "q/uL1i1lGvAyKdLSEBc8yrTUTrH82uFRZejcUgR2+f2BslZvPMtQlyQW35D9373A\n" +
            "MQxZYPg7vNmJAhwEEAECAAYFAlEGa9UACgkQyCIVM6GqMsI8jRAAxjj0Z/62i+Jd\n" +
            "Yy9iYuUXZyfLh5vexn89pesMgETaopNlv0OAT4rthpujmRCVLV/hN3XG94H/G5yW\n" +
            "trwzokBz3a7JDdPVSWsLWibANx1zzukG2FkKEl1gWJyoTQ69xet7jZK3p9xl/xEH\n" +
            "zS7t3rhniTqxViIpRIiHb5tSR/ESDIlR9tvoQwuoriI8TZd0tOkLS1myN+US39jf\n" +
            "Dbo1sla85bTEAQusTtpHTe7OztzON45saJvfRRIdHlZify3lv7+6Y7jOpFHTe6g5\n" +
            "1Qou5B9+mwZRb/2Pe4moWsQCKScZanJQDliyggY5s7a2gufEN2hTLzDniTc8FI0E\n" +
            "YZK+14DiI5uoPhhGJo3kKGcviye07l1VNvxsKxPwW0Xf/hYvTwgn1xIKN1rs1dTY\n" +
            "3wJpbGLZDdPfRDkqj4ZKAQTujjkqL1RQjdaBoFYmF3At6jV2dWCCK4Cppjv+rm6i\n" +
            "hNgvKtYpPrTX3m0FJ31x9G8UkGlqhxT0lQ3f5MVLC8K7rqOEUHCIdy4jBaNDEWV7\n" +
            "YJ/mU69yIb/xBGtVqrSDCMYh9sOy2zxaLQulUiSZRLRs1zr7npVvNf638DqErBAq\n" +
            "rTjzTNVzkEKcgp1Xpn97xl+pDtS9qm+4P0bp7RPSwIzM9kym69Gnwy6xk6v/Gizf\n" +
            "xZaRMdUyqXFuptsN5AAN5rn7ukZ3BAOJAhwEEAECAAYFAlEFpa8ACgkQskE8Zt0s\n" +
            "P+paKQ//QCkex3hC4v2xPOCnMtUtOZ/s+8ptjUaxBmcud985Kl1vuzpfqhRE5SpB\n" +
            "M3kgEWbqDmVhjvIDf2BwMxm1uLn3Ahl4fy3qZ0mOPlxTh1QRNINgPzf3Ch560jpy\n" +
            "rug21kUmr9QQRX4yFKe+4g1+NSuC/A7P2AzJKSgkvQM2orR9noNMLNMYO61mr8bN\n" +
            "cJgna/6G9PEwPunWkiU+ircp7gbDqZR0WDPIoj8WAHGHite7JA/tLD4t9gpNRSYw\n" +
            "hXqUWXObbB0a6sFSzgJt4QwEqOP6M/eggymohBlVjexA1Zh95mfJkNGnjhCkLXG4\n" +
            "qPMTq9Sk4cExv2Y5jSCEK/qDyz+IGRPGMIAdC8GFsLrQbWWcHPYWSAxGj5242gDg\n" +
            "DyYUl0KxMignGOY51eEL35a3Yha/B3L64+6fwStKbWx2X4L5+m26BUAJ9nNhdCmB\n" +
            "TMXB3uHhoHmstrI512md/M1voO76aq/20akGNcORTlKcfm2W805pSQfg1kfCQswP\n" +
            "Ja1j9/L1ELmUy+VaDHj2y8MRNIEo00Ax++ElHIM3/+eehyesmdCSLh11IPwxnWhw\n" +
            "GiJ+QPnqUqJe2e9LApff+Y+m4yPDUcZRnPfWDNRnfL4dEADR2P2ALF3YUS+OIDjh\n" +
            "/U9njqx2WdWGpI57H9D84EiayOrVE7r3FWJB3qtbYRU9ZrHxDfiJARwEEwECAAYF\n" +
            "AlEG4rgACgkQspS8BsdVKnrSDQgAiBoqUrh9dVmjo6EqEgJ+C+VsLdVP4t8DVWNV\n" +
            "Ufpc2lndtrJRpvdyqFN9Kc/7gBEyFuNCM5P5JRKfVoKSY0i9sTq3yWQjsv2iMsQ/\n" +
            "aDVaSzdmVvl4u7YtTJRGEgnIALL/X1Br9QmLcp/6Fju5p7f1mm5Sbwiqvi6G2cxP\n" +
            "GHm0ptHsfr96I4JjCAKfNbiZ8I7d9tPnejT6sSuuoB307E/Dr4J+hS2HWuevNm4D\n" +
            "KVrHvc/9+YTUIgkLSAqyAiOKUEsBIpDejyHfBCVM6x8S/BTBpLzJsIdXF4ip3ww9\n" +
            "GRPq1m9y0yuC/hvnnbNAP4cFUfLV9KWtOMvlhoFGHplW5GZYV4kBHAQQAQIABgUC\n" +
            "UQgq6AAKCRA1T6XiPqooYvSnB/9aJgpZ/LNn+QsXGQ0gz/D3aOT6P+coN/h2kfCU\n" +
            "p0TQ3djdOodrWJ4SQz3a4AmMmRkdcQa0XPKQqZJSir76IHoKnQep07oPSWD+JQyE\n" +
            "6Ix2BPM4Er5RqscQ3udbiwDatR57Hb5UIJChapiZqseu6Lx8uU/Swi9HjlFpKs3h\n" +
            "sDP6ocpD2LW2yLadtE5ivLTcLO3qPEzsecflq2XIi3zuaZRlPzkhnj4bnVWo3ET3\n" +
            "JfScv4OLTTMCWhF2zWSHjrwxBqMTdE/QrwaSMUvPdyaGjg8G9eDNRW3BylcDlWH7\n" +
            "SzDeFZGb2SjmwR9ie/mbiUjD05lIEQCk9NGQC4GTE+8c/qFoiEYEEBECAAYFAlEK\n" +
            "tfgACgkQBLW/NVdwg0qDGACeKq99OOyDJS1cCvAGJZeFRN6mmSkAoIDydwBZu23Z\n" +
            "ghKLi9JFSnQj80A6iQIcBBMBCAAGBQJRDG00AAoJEK6ZNfMO1PAQbeYP/1eyE285\n" +
            "TN3RO0OdaV7PyWkG9tpo+qiVMdEc77YP6DPkb46hDKcD5oTW5X9ySJIRP+SWFNUw\n" +
            "3kTeuVYnZz1VtnxZWW4ODeSk9czbvxN7+aRCDtS67mjVG+KFhC+o+iiwi7Ex51gY\n" +
            "BaFBTbowoIUBIAHcFz2nyBtY+8k2DRzcdiIAx+0CuRUWpWd3hqd5tK32SRAea366\n" +
            "kCcGBbBBMmpMRMvlkzXVvI4CC3BRoqhFQDgj7liJhSqQ831SAhR5FqxbioXTVVA+\n" +
            "h19Tzp+45YSjqyfE+VZe8PSg8P6hbLqUpqABZ+92e0HhR314U9XjPTpEEapaNMpm\n" +
            "34b8u+Ix5w2IL91fCJd7P5GAboYu+BoQagDP5NV4fXQOj5gTulhn6nIHX64+/nRK\n" +
            "F5IB+fcb0HZYFCQ2t7nMt2wM9QHmoPaGB9KhLrsre15raQURk0R2R9AEgh5kjdrY\n" +
            "sWkkhng4kAkO7zIOMZiti5TkMWiCXh0Uq0jGIHS5Bqg1MhLoEC9pcCNBcOVjIPFt\n" +
            "4jDBsxHAwp+x7Mmeo5ljFMoODAkcMq5JNhL1BI4kiSux5g32lU42aF6r1x79UPzE\n" +
            "9MvycTBaCQLGiRTHaUZyOeUrcwIK8+4TgvYHTrL9f0de/og16Qair+K7T+HDBQpM\n" +
            "p0evZHphbrnryKCUEKynIySP3IOTLAFevmLdiEYEExECAAYFAlF6pW0ACgkQK648\n" +
            "9tr/sACqOACcCAjaMFIUCWY4VPnZ6CjiMohki6oAnRz9LeE27s05FM3qF3r7yqTB\n" +
            "bLVetDREb21pbmlrIFNjaMO8cm1hbm4gPGQuc2NodWVybWFubkB0dS1icmF1bnNj\n" +
            "aHdlaWcuZGU+iQE+BBMBAgAoBQJMO4BcAhsjBQkJZgGABgsJCAcDAgYVCAIJCgsE\n" +
            "FgIDAQIeAQIXgAAKCRBxjAcBAAEigvbfB/9jjRtyvBDda1PbB5HMkS+5YZuy1mTj\n" +
            "WmMYMtza1p8L3uRhZLb09Ve2sQ0tSNJSnUcL4MEJapXSnwsz3l7Zh7aOo6GjUAO9\n" +
            "2LZzV/DWoCIei/caJhEiNV44HzdJUlN2+FBl5tMt9DFordfZIEm0jPWR8kTzF/l0\n" +
            "sGMxVUBo7JrdodTX/2nybPLnSpSIhTrSfA8sn2VJV1FrN50nXOOnGJCYOx0HoyFP\n" +
            "zX+QVoGO2S2lFl1dLcnrYKfNcMnkPZyxN9K+7/+4D6jKMCfn2hKBH2+in9D9yNWl\n" +
            "Dbb9fxYP3AW1ObyrvyKFe1pCEBDpifH5+n9W2gqbNS/w7Xoh1/Phn9vsiEYEExEC\n" +
            "AAYFAkxReYkACgkQZRxtbUCpVQKCkQCeKQ/i3XXlHunMU3blZu+vHoLO0XcAn3L+\n" +
            "erc3GGnUT+fUix8RmeY1oPiOiQEcBBMBAgAGBQJMUXmoAAoJEFYVl/3JAMd4ZisH\n" +
            "/0XuGH+G7cROn9u0cgjXSPScDdCTDVjaRwj1KYgZ3y63naqbvCe18gZ5sSsmsrBg\n" +
            "WSnI9ynpQmU4HFfqOnZFXoV8qXkkoSv6E43QUtsrKBJf77VYRRtmpNsQEs6MQ7l0\n" +
            "OPhWhnrEKWyeoa1PhMxN9vBXuqT/DvK9vQCCwAJ0i0mlLslnsw78tY6Dw3km0w7S\n" +
            "1AS7ZQ0R5Hv/VtxAwQEsQ0ON3sptVzy9Mv1mpyqT8VPcpVSoMs76MLvHv1FpdUJr\n" +
            "zxBwuapZjZcgH2L+QEzcgtUGIZKNfsw4w4T+S/fSzKQbhnROaLZG64cOAUuBAsxl\n" +
            "S1xpg9tupgk86g8Gu+GTKNuIRgQQEQIABgUCTKRzFQAKCRDkP/WQI6H4pP4gAJ9a\n" +
            "EpJPzGtsV1Hrp+L3J96kbX5cswCdH+IKmnveVUZBhWnDy2xCoW5X0BeISgQQEQIA\n" +
            "CgUCTZxI4gMFAXgACgkQxZfezBd6/Xz5YgCfRouhQNbaBelpk+pgwk8XbVi+C3sA\n" +
            "niQ3EIOLdXEDEsozpDcrKsd08rRAiQEcBBABAgAGBQJRBYnKAAoJENjTIPvvLEnw\n" +
            "CAcH/Rmciw+bRgCPbroPGkzQHTD8y9RWTEclBDv6mnJLNlzacKzfFhafvMnP/CuH\n" +
            "9gEVKf/nM1vCS9G98t5CksGrLsEXJoVRGeOG41bREafUc+n2dxEoHAW3yUuvfnVZ\n" +
            "zLEgNBk066v4wuNh6mt/vEUz+8k2kJ/1BRe+V3x6kFKKfN5ezszXs8UWMwROrLHA\n" +
            "ElYOZKeDEL6oLpykHXFokjLHMgOxnvwOuT3tOMuHo2kW2LyV5DyGlJbYx+pHbdaC\n" +
            "9dzXe+uPQ2YzKCuc1TgyMAjCDcG9OOiZEqTdFAY8Lr5eUdNG8Rozv9+rteSk2QaQ\n" +
            "FqCbKmpvV6u7cnO5dydego2t2O+JAhwEEAECAAYFAlEFzk4ACgkQ0DssUbNe6zwO\n" +
            "Tg//Qi20qePBfw+fsq77Pddt6s5kAulMzIK2vbUQYY+63MCnIbiiTC5464K1xwMz\n" +
            "56erQJSqltW5r7MxgLJdP2IISkG8PfRCBQqJWlsriHL/EuJ16AsLUCncWggHik1L\n" +
            "oaHegyplc35Ai3Nm70nxCVtmC/62k8EHlFuw7rJbhqg5s1hAKjl7HRryAHhzag/o\n" +
            "LwzIQxKiGg4jIRhhPS3Ye1NnJR1yv0JywovwgIbGYfvKqmNInym7au4o/DSKfigd\n" +
            "hA8t1LwmcGaXrTEyxTm2wj6hXu1BITzZhmhayrCZv3ZnEE3r99bdq/Qr9f1qrVPD\n" +
            "7W3OMve7MW2H2gpd48uVre29SV1RCl4qKnVGO7v6weppVudbnpYh/I+jfrpDC0QT\n" +
            "h8qPf8/4aec/j9tD8tXYMtBK/+J1xEYTO4o+j5Gg2u4Nv22xT0TUD53m9SPo2PXr\n" +
            "hIZLlE5t24Mj8lyK8f0nAspq9RZRoSaxdGzLzyrIVpXaaqo+3ldCA3JWPp+cAMay\n" +
            "dj7TTEJ+v3DlEmqsI1UQMTcsDXA+PaEqVryRxQ7rSu4HXKeszEJfAxPQslsSIQcy\n" +
            "deVqAhG06QYYgIgHGD8DNVvOOtp6IXj2vt2Ss77APVNMtIUualtb1R+tT+p/H3ti\n" +
            "bFn295UYYnCJOjG/3QnWGBBzrgwNqSbrdIUNEAf3w7ogUk2JARwEEAECAAYFAlEG\n" +
            "SpIACgkQZf77hz/aPZm5/wf/Z7uOa3Vg90aTBRa6UV22p7VK7kWYJW19MHNBNYQO\n" +
            "vDEPMPVkJz0GXyckOnYnPz+9fZvIeO3RzvYVc9YOYAYvmBlu4934R5ZGiCqjvy+M\n" +
            "KR9q6X0hXHZ/cioW2Li6zRIqdfdfomXHiK7IrK+yCLyJIIua8P5S0YzY6A0/Xfaj\n" +
            "xgO1QCA8O1wNaP7vcCxIN2a5fptlmOEsNe0okfX/2I/lKMF+//pJHGa8kYC9rnFo\n" +
            "Y5I4IcDuI/jXaJCattmUijAtvSaDMox5/MozEVv5lTbdet4cZyUQ6ZjgdrwjTs2e\n" +
            "nnvU6C4PDZWng/kbBxkp+ne+iaiKrT0iCUgBDIOWu+8VZokCHAQQAQIABgUCUQZg\n" +
            "WwAKCRBiudz/ZiTBoo65D/4vK0rAodk71PQvbWM2Z+p5+fWHmPrtg1v8jN3NTmWX\n" +
            "RG7+ujO5sX0gA3K4aY4X0zNRUROVMhJi8A9m9fU0ZlaZ3dXxbOGmEuhG8PMAcnwY\n" +
            "pTEYmHGOOcEOJ7dUE7zu9NIBKI+Hi1mzKLvQqLXbw9cRoAscHLK8M00hpmANSxb/\n" +
            "MWJS1+l2gqkWE8u6s1Jxih00a+ex6ealhKsgaxMpSd98FQzu8s3achTQYFy7zEGL\n" +
            "T5iEnXqspoEmrIrQoUL/yHJg6Sol5dofP/dWhMm7FewjrYZWykgo4yeGMPfIbALH\n" +
            "KlQu2p5i7NdTfwVcei20rtlk5R+ZqU/k520qcU2mwfgKu1Oma9cxPEbJ6Cn6tVHl\n" +
            "eelotjH6aCj8MratzZw+BO7u15st2j7BMFs5qPOqm98qCVJ/ujZbXgMvxuk/KloR\n" +
            "GRsPsr6r146GsbkcrtdWTvvSwiYcA2rRbdJkqqUkXc3Pr1pdKNkc51rnRnuaUp1P\n" +
            "EEyuBxSfiZdClpVf/yXiAZfPVf+db5mWhu32rvRq4GLQ5uXM/T/eX91YPWCcmOKn\n" +
            "wM+4RK0wmpcn7Iak8f+stJKnHF9QcInqHvb2JiHS7K/UOdjpzeQ3gr0xjoSyT5tq\n" +
            "Rhp13/PSr6tcgIWcghVTolmTtBj9BlAdf32+zfC/sE5fiuzQf+ckYHmyVIBjLAaH\n" +
            "lYkCHAQQAQIABgUCUQZr1QAKCRDIIhUzoaoywhGzD/9PW8BdkzJXyR6fCXi4z682\n" +
            "0/DvZkfYxHkOsaDBthjDwBnMaRZfNyP8QDQ9APequPSI43Kd3/RI+lof0NE2yXE2\n" +
            "j7W33K1RnSXTunrZ+knKL2vsU2t0mpoBX3D7QGF9IwMl31JuOPV/pPJ9gK6mVyD5\n" +
            "eq8fJgHkyI351OOnLFK7THDHF6lY2MeBSs8EsH8u0Qe4drb8AShOIEQxbG3NoCSp\n" +
            "SEPeAuPO8KoYSsUCDrJqHhK/UtLkORjVQpwv1T2hZSXe4kEoUn9rccpc+dY8mype\n" +
            "FZlq233hOfPRsYWX4z22JLK6XjuC9LmRN14ZjSQsYTbmHUKKn/yd5+JFeh9jaxQe\n" +
            "vKg7ZYeHOOl+9xNiMOCyeADvz15tqFSmeNtPMpzw/gUrMuootmrYVw6wsgG3rWQx\n" +
            "ljKMtR2Xq6/VEvE6RgVzE6Qp6ylFpQ332VuMCErrbUGwaimXbRQkX/C54U5pWdxg\n" +
            "O4OxNWanKawYNJXQ//gnNosr9EOQQudQ/Adkq5BnnC57XRzpGz7G3gwndBzI1nkp\n" +
            "lXJEpbh6+4WBxBulFbrv9VD2ot17uO9kQVWM7RLq4GI++x3pg1CQVdxo5yYMRcca\n" +
            "7gEGeBR/OzYKJNKyFxOcSbtMT54WGeptWU5IPSaR3corZyBcu0LJCzldXLgfF5jY\n" +
            "sP9hNqhK3hxgKelsI0ECd4kCHAQQAQIABgUCUQWlrwAKCRCyQTxm3Sw/6oi/D/9M\n" +
            "70bk62Gvqhe9X3bUvrrff1yieTa2UhTDqT3EG1cMRdLa1aGJsjbEBy2hr9u7vCWP\n" +
            "2NVYkPSIo2Q2+t9LfeT19Q+nzG11ynAZ+MM+pY63aHN8a/YrSEGLYbRM41Q8337/\n" +
            "SzppV737R9HYibj9pLo2m8q03DnjoacEfBO6RExoXoVuNn3J7rkaA52XNgWrj/MP\n" +
            "fcMVCJqUsBA69ZliFWNmizUeeM3yWvj6HSeDBxwz7l5pmj3Gq/50qw0vzYe6t05M\n" +
            "BGow8xqI2rstvr7wF/D2WZyABIIDEwlpE1kfBCB6Yifdq8go10dBJGH7KCETo7er\n" +
            "/a5NVV8ur4sgSJsOBrHYW0aHMJWvCp2mSooX4VOWhd91PJ1vUD6+3H8IB1NGWB5v\n" +
            "CVrqVTpYViZDhYcAbIEqF98vOwkjJga4w0BFUqNC5IwbWQ4VH5pDHuSviUyFIWii\n" +
            "ejAJLmZu5ycg3fHXJ51HDJlgyttP16W5NPJnPpOe7bKipcUcKER7YDOlPF/z2y7E\n" +
            "Af9lp3uPLx7iIN46iAAlbwSkMny52DNSxCaOsdvmuB5nIkfn762+1cURFvgACYh0\n" +
            "NeQawtn2tQGTYQjw6P21uJGXi8kmy12iFHGhi7vptVVZxNDT1GvcozyZ3bdOWN2T\n" +
            "/S/x3o+RO8kbdMgHjkOOHKNHYvfQpKAhAbVD5lCNCokBHAQTAQIABgUCUQbiuAAK\n" +
            "CRCylLwGx1UqeiraB/9yKTH4xcj0e13D8zRCyTcpQzoJwihllFVbtOYV07dcKi3d\n" +
            "SKoMPpW3W2yr3ADHFDTpHhNj55ZOqq985k9hrR2KccbFmvSAkqDJluBeK49AK7uF\n" +
            "4UW1kAHg2XqZB8ieiyITNsvNpZaB9a0dIGnuAoNJdE/b+Jwx8h9di5IPjVc9P0Sz\n" +
            "z6u1z+H4xlUc7rB0VW3xlLJEUvmflg2fqGZvJ/jE6F5/Sn3oPZ2Bevoz+F7gqsOW\n" +
            "LLjQbrulG/vLg5zXFqYNPU/2x34Z6bwEmmvWSYwY+bXlfYH71rEVzSzK3oA2KyyU\n" +
            "nCD4v6XbqdEj9Iaiqvz5wggs+pzRh7s4py9TjuhFiQEcBBABAgAGBQJRCCroAAoJ\n" +
            "EDVPpeI+qihix50H/3bfZkaaYo3OnmyQbj6KGcuptkBSdr77CfRgho3R0mOrFT63\n" +
            "1vUv8l3pUwCNxCWH1wm5v3QvYpCKs/G+J8fJvzJjInw+/CcEUtPJuO/A6WCsJYZ+\n" +
            "42O1Eu5IE6BpQhQwvq/v+ggJNdWZLNDipVBTVDtB6J8RBHk3ncUx6upTWVcQlvSv\n" +
            "kCwJ7hRMglM9V8jYBqhlR/oxDxbDaj9TXCkpQc6VuM8VLNKaA1Ih7tEvCtoW1+0d\n" +
            "ZQIqEn3DkWun1CtezBP/xR9BeA4tGselDnAZACUD9FxSza70FCoD2m/bUHFvsvgY\n" +
            "4cGNSjg+ZRgS3BikUgVKJAL/A3qhyF25ATSLFT6IRgQQEQIABgUCUQq1+AAKCRAE\n" +
            "tb81V3CDSkyLAJ9LEg8I/lyaUp0W6XUCfZ+yeFJk7QCggB2oBTyBin4VRDYg5aFW\n" +
            "2vDbKHSJAhwEEwEIAAYFAlEMbTQACgkQrpk18w7U8BDCuxAAiDD0h9v8UksJVjiz\n" +
            "RpAA6qiZyddjghzcO4GgAqxv3fdqBNZ5uYCtbYEeEHLWHmd/O2f98i4PRgHe6xlo\n" +
            "gC+7TQAG/O4YVNtnntCFmx0G4z6/1nZc+IbfYHSmk0tszo6zIO0NOzek5N8t8GzD\n" +
            "QknSixKh8z0hWmseUz0RJKagmxkbnDOLvfVAOIbJW3iKVauIeyxqE/5gNIWn+/vT\n" +
            "p87MxSpMMrgWHjMHuyaxdN93t+ea7XZ+iWQCd9HQ7RhylATUPsoeiwjUm0O16jqX\n" +
            "OGLFJM9PFKS4DIRMze4JRrdFlIxOQP4xjbu8VS4hGJK8Gi46QMhB8TLR3qOzpZyU\n" +
            "S2f+Kjt4RoYa8iwbWbfB8jCSGf+lgQPsNDVEdaRJQPqClKqkfldlt32E9GULx9ln\n" +
            "Ncyfb0CXt06Gt9dFXIP/tU0cgZm8KsmSEl11TofZ/UL/KLgIJjiw80ZqUSrFKARz\n" +
            "6UfxQwkbIWMu5BXU5t/8P/SQawpSbXugnejQ9Q7wNZ593SgH8VdXrGS5zNagGaIj\n" +
            "GJsj4LzCuYc2a12w1zuWeVIGCbJyoWzd6PYfIleHZo2ISRnAR6S24yTKPkMwiutT\n" +
            "VthVNeE33Yek6YQZ5Xdmgfy/q98IdV12U+sA1LQOABoJF+goBNHh1AlfCAuLbgmo\n" +
            "BYSjSGXQ7XjaiNUeexAV8f7TEhiIRgQTEQIABgUCUXqlbQAKCRArrjz22v+wABZn\n" +
            "AKCs+Z19eY/NmrSzPQsZ7SlHBNremACeJehgL8VdZkPMiW3xUbEki2ji62u0MURv\n" +
            "bWluaWsgU2Now7xybWFubiA8KzQ5MTcxNjU4MTQ1MkBjcnlwdG9jYWxsLm9yZz6J\n" +
            "AR8EMAECAAkFAlD393UCHQAACgkQcYwHAQABIoJawQf/RpeNorZbtnIZmNz8y2Ko\n" +
            "3xNKEGlf4XoFY7irtJo4ImO5Muftr+Y20rhIQYTf7tBNaFabj2nb223d7Apg84lR\n" +
            "MGSUA9+5V+C0yjALA1SttqRW2evd4NX9/N5WNrf4z+S3C2QfD0mL41eUiIgLgJhc\n" +
            "Hmll9wiZyJzr2t9GNkOk0iYJzkqDBhdxj2Zl3OcD3v6P6IUM+3RWzk5tFmt/YHvN\n" +
            "aXPWgND/8OVAdd470p/aK10qZ9v51ZxWN1OT/HVZrNh5rLdfroeNjFKtS/pl1wMT\n" +
            "ImtN03lhhyWR0a++Eowh6zEJKeDfg7C+2djqsB9C8nMDZbmQdNLFJNQRVSiK4i8E\n" +
            "4YkBPgQTAQIAKAUCTp8R3QIbIwUJCWYBgAYLCQgHAwIGFQgCCQoLBBYCAwECHgEC\n" +
            "F4AACgkQcYwHAQABIoI6KAf/THY5iMm+CH5dJOTAwuHUyuKduvSVFxq+1WX3rX21\n" +
            "x670fhHx9WarvE+CsgreUzfBVZxq1cq2oB72KyFsa45utKt761x4QEOM01CRQO21\n" +
            "hIgl+wed9CRgzO17OzZ/E/G47/P8pHxm8kXwictTWqZ4rlgfzOg7YcY5An05rFH2\n" +
            "J+fxUVfdjZ2u75XDE6CAHV1hMvrRwatnJQ33S1/yRCdhT3qad7U7wrbtiu7Y4KNi\n" +
            "gM4ur+kGqRSNWN6/4v7OHRgj0Pp6jbs2pXqccR9rAhlKhnatd6RKb1+LlYEyblSC\n" +
            "76PIZm26h8qhY8UKrj9a2ydmWDY2uquxbRLvjrT8suZZebQvRG9taW5payBTY2jD\n" +
            "vHJtYW5uIDxzY2h1ZXJtYW5uQGlici5jcy50dS1icy5kZT6JAT4EEwECACgFAlLq\n" +
            "hOACGyMFCQlmAYAGCwkIBwMCBhUIAgkKCwQWAgMBAh4BAheAAAoJEHGMBwEAASKC\n" +
            "nKcH/ik9LmnpEclsCDfzYqTEbVOSNZA30YcvwdlsHlzjRm+KjJC3D350147o5D60\n" +
            "xq0UBUxKeXJPPMofeRgzTiAjTdv2ilJEZe8bMM5b7gcmp92q4tAY3OxcGNprIswc\n" +
            "eb5hG4kY905WAy076iaQOD9z0Y+bXWQo0OHc07lc4+8ZLG9u+rGDjfF0x4UWgkAQ\n" +
            "d8Thth4lTzdZR/kLLBCdlOyb9sAKqfbxbfATDQEceex7dZF/uJRCvFojHMtDbhxe\n" +
            "xdfEjWbsJRQR0KKTHYS02zqhVu34elwuRSWf1OOR7ynh5nD5CCAAmVbi+x7y281i\n" +
            "YYTchi/s71CSs81OtFtaBfVNSeq5AQ0ES4b+oAEIANr825Ns9mewUTHNfZ3/xK7R\n" +
            "mp+nVLgOoyJDZF+Qum08RnFiECCiDTPlHIUuZt6jUu8vb/TKH5bdviFkC2MQPhm0\n" +
            "/5sbbbqbV6wMnXfMd/RTPkIeeheEumY/5n4oYYGuVTZ+0MBouPY/wXfxp6HkqtuI\n" +
            "qUZm8Bmy9AEScxiBURBu4MOr9/c9niLFlnpFLhEsSm17nS6/tdEJGdMRb3WNFn5+\n" +
            "bE8w9e8RqPlye9SFZHsjmv9jCZaW5fZkcdDTcDClPVvIBtUl6y/kkh0RfIwdU+T5\n" +
            "GRI8XekgI8WkvEqxTaQqn03C79zU3nhRuSgy8E492uaTmwpmAXC/m4Z6luTNPrEA\n" +
            "EQEAAYkBJQQYAQIADwUCS4b+oAIbDAUJCWYBgAAKCRBxjAcBAAEignQvB/915fHh\n" +
            "7di/yoyJfmufnj4fJ9Lt6OYyXvKetXpC+dLx7zH61KCeKosgWIN5HyY2Si1ZfGdO\n" +
            "JQ1L0d9Y+TsRVslU34uY7DuYLs4yGNwFdI4r6Y+PHIAE0Cd3xxf8xFr8oiinPMvm\n" +
            "SVDO2MbF0W/TnYwvyoN7Of0uAUdFY0sRupamPgNEz7dTZ+UoKgRFzfPh4zUb5Hav\n" +
            "loqJCE/BEJ4wkxYTaJfFdJq+3WAZdd0f1OZLLDcCCvbZHNYBvpPauoVq3LD8MHXz\n" +
            "hCRY9Rp2ZxX92PrFiSNpKheP30iZM8VInDfPGaApQU1y8R2uLL0I/7XWiFtpmR6e\n" +
            "k3wUxv46o0y15asU\n" +
            "=Bbew\n" +
            "-----END PGP PUBLIC KEY BLOCK-----\n";

}
