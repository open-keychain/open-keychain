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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class FileDialog {
    private static EditText mFilename;
    private static ImageButton mBrowse;
    private static CheckBox mCheckBox;
    private static Activity mActivity;
    private static String mFileManagerTitle;
    private static String mFileManagerButton;
    private static int mRequestCode;

    public static interface OnClickListener {
        public void onCancelClick();
        public void onOkClick(String filename, boolean checkbox);
    }

    public static AlertDialog build(Activity activity, String title, String message,
                                    String defaultFile, OnClickListener onClickListener,
                                    String fileManagerTitle, String fileManagerButton,
                                    String checkboxText,
                                    int requestCode) {
        // TODO: fileManagerTitle and fileManagerButton are deprecated, no use for them right now,
        // but maybe the Intent now used will someday support them again, so leaving them in
        LayoutInflater inflater =
            (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setTitle(title);
        alert.setMessage(message);

        View view = inflater.inflate(R.layout.file_dialog, null);

        mActivity = activity;
        mFilename = (EditText) view.findViewById(R.id.input);
        mFilename.setText(defaultFile);
        mBrowse = (ImageButton) view.findViewById(R.id.btn_browse);
        mBrowse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openFile();
            }
        });
        mFileManagerTitle = fileManagerTitle;
        mFileManagerButton = fileManagerButton;
        mRequestCode = requestCode;
        mCheckBox = (CheckBox) view.findViewById(R.id.checkbox);
        if (checkboxText == null) {
            mCheckBox.setEnabled(false);
            mCheckBox.setVisibility(View.GONE);
        } else {
            mCheckBox.setEnabled(true);
            mCheckBox.setVisibility(View.VISIBLE);
            mCheckBox.setText(checkboxText);
        }

        alert.setView(view);

        final OnClickListener clickListener = onClickListener;

        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        boolean checked = false;
                                        if (mCheckBox.isEnabled()) {
                                            checked = mCheckBox.isChecked();
                                        }
                                        clickListener.onOkClick(mFilename.getText().toString(),
                                                                checked);
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

    public static void setFilename(String filename) {
        if (mFilename != null) {
            mFilename.setText(filename);
        }
    }

    /**
     * Opens the file manager to select a file to open.
     */
    private static void openFile() {
        String filename = mFilename.getText().toString();

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setData(Uri.parse("file://" + filename));
        intent.setType("*/*");

        try {
            mActivity.startActivityForResult(intent, mRequestCode);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(mActivity, R.string.noFilemanagerInstalled, Toast.LENGTH_SHORT).show();
        }
    }
}
