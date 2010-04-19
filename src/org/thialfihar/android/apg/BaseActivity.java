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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BaseActivity extends Activity
                          implements Runnable, ProgressDialogUpdater,
                          AskForSecretKeyPassPhrase.PassPhraseCallbackInterface {

    private ProgressDialog mProgressDialog = null;
    private Thread mRunningThread = null;

    private long mSecretKeyId = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handlerCallback(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Apg.initialize(this);
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        // in case it is a progress dialog
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        switch (id) {
            case Id.dialog.encrypting: {
                mProgressDialog.setMessage("initializing...");
                return mProgressDialog;
            }

            case Id.dialog.decrypting: {
                mProgressDialog.setMessage("initializing...");
                return mProgressDialog;
            }

            case Id.dialog.saving: {
                mProgressDialog.setMessage("saving...");
                return mProgressDialog;
            }

            case Id.dialog.importing: {
                mProgressDialog.setMessage("importing...");
                return mProgressDialog;
            }

            case Id.dialog.exporting: {
                mProgressDialog.setMessage("exporting...");
                return mProgressDialog;
            }

            default: {
                break;
            }
        }
        mProgressDialog = null;

        switch (id) {
            case Id.dialog.pass_phrase: {
                return AskForSecretKeyPassPhrase.createDialog(this, getSecretKeyId(), this);
            }

            case Id.dialog.pass_phrases_do_not_match: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setIcon(android.R.drawable.ic_dialog_alert);
                alert.setTitle("Error");
                alert.setMessage("The pass phrases didn't match.");

                alert.setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                removeDialog(Id.dialog.pass_phrases_do_not_match);
                                            }
                                        });
                alert.setCancelable(false);

                return alert.create();
            }

            case Id.dialog.no_pass_phrase: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setIcon(android.R.drawable.ic_dialog_alert);
                alert.setTitle("Error");
                alert.setMessage("Empty pass phrases are not supported.");

                alert.setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                removeDialog(Id.dialog.no_pass_phrase);
                                            }
                                        });
                alert.setCancelable(false);

                return alert.create();
            }


            default: {
                break;
            }
        }

        return super.onCreateDialog(id);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            default: {
                break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void setProgress(int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt("type", Id.message.progress_update);
        data.putInt("progress", progress);
        data.putInt("max", max);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    @Override
    public void setProgress(String message, int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt("type", Id.message.progress_update);
        data.putString("message", message);
        data.putInt("progress", progress);
        data.putInt("max", max);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    public void handlerCallback(Message msg) {
        Bundle data = msg.getData();
        if (data == null) {
            return;
        }

        int type = data.getInt("type");
        switch (type) {
            case Id.message.progress_update: {
                String message = data.getString("message");
                if (mProgressDialog != null) {
                    if (message != null) {
                        mProgressDialog.setMessage(message);
                    }
                    mProgressDialog.setMax(data.getInt("max"));
                    mProgressDialog.setProgress(data.getInt("progress"));
                }
                break;
            }

            case Id.message.import_done: // intentionall no break
            case Id.message.export_done: // intentionall no break
            case Id.message.done: {
                mProgressDialog = null;
                doneCallback(msg);
                break;
            }
        }
    }

    public void doneCallback(Message msg) {

    }

    public void passPhraseCallback(String passPhrase) {
        Log.e("oink", "setting pass phrase to " + passPhrase);
        Apg.setPassPhrase(passPhrase);
    }

    public void sendMessage(Message msg) {
        mHandler.sendMessage(msg);
    }

    public void startThread() {
        mRunningThread = new Thread(this);
        mRunningThread.start();
    }

    public void run() {

    }

    public void setSecretKeyId(long id) {
        mSecretKeyId = id;
    }

    public long getSecretKeyId() {
        return mSecretKeyId;
    }
}
