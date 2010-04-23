/*
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
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

package org.thialfihar.android.apg;

import org.bouncycastle2.jce.provider.BouncyCastleProvider;
import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPSecretKey;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class AskForSecretKeyPassPhrase {
    public static interface PassPhraseCallbackInterface {
        void passPhraseCallback(String passPhrase);
    }

    public static Dialog createDialog(Activity context, long secretKeyId,
                                      PassPhraseCallbackInterface callback) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle(R.string.title_authentification);

        final PGPSecretKey secretKey;

        if (secretKeyId == 0) {
            secretKey = null;
            alert.setMessage("Pass phrase");
        } else {
            secretKey = Apg.getMasterKey(Apg.findSecretKeyRing(secretKeyId));
            if (secretKey == null) {
                return null;
            }
            String userId = Apg.getMainUserIdSafe(context, secretKey);
            alert.setMessage("Pass phrase for " + userId);
        }

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setTransformationMethod(new PasswordTransformationMethod());
        // 5dip padding
        int padding = (int) (10 * context.getResources().getDisplayMetrics().densityDpi / 160);
        LinearLayout layout = new LinearLayout(context);
        layout.setPadding(padding, 0, padding, 0);
        layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                                LayoutParams.WRAP_CONTENT));
        input.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                               LayoutParams.WRAP_CONTENT));
        layout.addView(input);
        alert.setView(layout);

        final PassPhraseCallbackInterface cb = callback;
        final Activity activity = context;
        alert.setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        activity.removeDialog(Id.dialog.pass_phrase);
                                        String passPhrase = "" + input.getText();
                                        if (secretKey != null) {
                                            try {
                                                secretKey.extractPrivateKey(passPhrase.toCharArray(),
                                                                            new BouncyCastleProvider());
                                            } catch (PGPException e) {
                                                Toast.makeText(activity,
                                                               R.string.wrong_pass_phrase,
                                                               Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                        }
                                        cb.passPhraseCallback(passPhrase);
                                    }
                                });

        alert.setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        activity.removeDialog(Id.dialog.pass_phrase);
                                    }
                                });

        return alert.create();
    }
}
