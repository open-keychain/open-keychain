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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;

public class FileDialog {

    public static interface OnClickListener {
        public void onCancelClick();
        public void onOkClick(String filename);
    }

    public static AlertDialog build(Context context, String title, String message,
                                    String defaultFile, OnClickListener onClickListener) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle(title);
        alert.setMessage(message);

        final EditText input = new EditText(context);
        input.setText(defaultFile);
        alert.setView(input);

        final OnClickListener clickListener = onClickListener;

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        clickListener.onOkClick(input.getText().toString());
                                    }
                                });

        alert.setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        clickListener.onCancelClick();
                                    }
                                });
        return alert.create();
    }
}
