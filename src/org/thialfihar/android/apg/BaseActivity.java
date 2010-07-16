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

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class BaseActivity extends Activity
                          implements Runnable, ProgressDialogUpdater,
                          AskForSecretKeyPassPhrase.PassPhraseCallbackInterface {

    private ProgressDialog mProgressDialog = null;
    private Thread mRunningThread = null;

    private long mSecretKeyId = 0;
    private String mDeleteFile = null;

    protected Preferences mPreferences;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handlerCallback(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = Preferences.getPreferences(this);

        Apg.initialize(this);

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dir = new File(Constants.path.app_dir);
            if (!dir.exists() && !dir.mkdirs()) {
                // ignore this for now, it's not crucial
                // that the directory doesn't exist at this point
            }
        }

        startCacheService(this, mPreferences);
    }

    public static void startCacheService(Activity activity, Preferences preferences) {
        Intent intent = new Intent(activity, Service.class);
        intent.putExtra(Service.EXTRA_TTL, preferences.getPassPhraseCacheTtl());
        activity.startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Id.menu.option.preferences, 0, R.string.menu_preferences)
                .setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, Id.menu.option.about, 1, R.string.menu_about)
                .setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Id.menu.option.about: {
                showDialog(Id.dialog.about);
                return true;
            }

            case Id.menu.option.preferences: {
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
            }

            case Id.menu.option.search: {
                startSearch("", false, null, false);
                return true;
            }

            default: {
                break;
            }
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        // in case it is a progress dialog
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        switch (id) {
            case Id.dialog.encrypting: {
                mProgressDialog.setMessage(this.getString(R.string.progress_initializing));
                return mProgressDialog;
            }

            case Id.dialog.decrypting: {
                mProgressDialog.setMessage(this.getString(R.string.progress_initializing));
                return mProgressDialog;
            }

            case Id.dialog.saving: {
                mProgressDialog.setMessage(this.getString(R.string.progress_saving));
                return mProgressDialog;
            }

            case Id.dialog.importing: {
                mProgressDialog.setMessage(this.getString(R.string.progress_importing));
                return mProgressDialog;
            }

            case Id.dialog.exporting: {
                mProgressDialog.setMessage(this.getString(R.string.progress_exporting));
                return mProgressDialog;
            }

            default: {
                break;
            }
        }
        mProgressDialog = null;

        switch (id) {
            case Id.dialog.about: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle("About " + Apg.getFullVersion(this));

                LayoutInflater inflater =
                        (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.info, null);
                TextView message = (TextView) layout.findViewById(R.id.message);
                message.setText("This is an attempt to bring OpenPGP to Android. " +
                        "It is far from complete, but more features are planned (see website).\n\n" +
                        "Feel free to send bug reports, suggestions, feature requests, feedback, " +
                        "photographs.\n\n" +
                        "mail: thi@thialfihar.org\n" +
                        "site: http://apg.thialfihar.org\n\n" +
                        "This software is provided \"as is\", without warranty of any kind.");
                alert.setView(layout);

                alert.setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                BaseActivity.this.removeDialog(Id.dialog.about);
                                            }
                                        });

                return alert.create();
            }

            case Id.dialog.pass_phrase: {
                return AskForSecretKeyPassPhrase.createDialog(this, getSecretKeyId(), this);
            }

            case Id.dialog.pass_phrases_do_not_match: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setIcon(android.R.drawable.ic_dialog_alert);
                alert.setTitle(R.string.error);
                alert.setMessage(R.string.passPhrasesDoNotMatch);

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
                alert.setTitle(R.string.error);
                alert.setMessage(R.string.passPhraseMustNotBeEmpty);

                alert.setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                removeDialog(Id.dialog.no_pass_phrase);
                                            }
                                        });
                alert.setCancelable(false);

                return alert.create();
            }

            case Id.dialog.delete_file: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setIcon(android.R.drawable.ic_dialog_alert);
                alert.setTitle(R.string.warning);
                alert.setMessage(this.getString(R.string.fileDeleteConfirmation, getDeleteFile()));

                alert.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                removeDialog(Id.dialog.delete_file);
                                File file = new File(getDeleteFile());
                                String msg = "";
                                if (file.delete()) {
                                    msg = BaseActivity.this.getString(
                                            R.string.fileDeleteSuccessful);
                                } else {
                                    msg = BaseActivity.this.getString(
                                            R.string.errorMessage,
                                            BaseActivity.this.getString(
                                                    R.string.error_fileDeleteFailed, file));
                                }
                                Toast.makeText(BaseActivity.this,
                                               msg, Toast.LENGTH_SHORT).show();
                            }
                        });
                alert.setNegativeButton(android.R.string.cancel,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                removeDialog(Id.dialog.delete_file);
                                            }
                                        });
                alert.setCancelable(true);

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
            case Id.request.secret_keys: {
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    setSecretKeyId(bundle.getLong(Apg.EXTRA_KEY_ID));
                } else {
                    setSecretKeyId(Id.key.none);
                }
                break;
            }

            default: {
                break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setProgress(int resourceId, int progress, int max) {
        setProgress(getString(resourceId), progress, max);
    }

    public void setProgress(int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(Apg.EXTRA_STATUS, Id.message.progress_update);
        data.putInt(Apg.EXTRA_PROGRESS, progress);
        data.putInt(Apg.EXTRA_MAX, max);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    public void setProgress(String message, int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(Apg.EXTRA_STATUS, Id.message.progress_update);
        data.putString(Apg.EXTRA_MESSAGE, message);
        data.putInt(Apg.EXTRA_PROGRESS, progress);
        data.putInt(Apg.EXTRA_MAX, max);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    public void handlerCallback(Message msg) {
        Bundle data = msg.getData();
        if (data == null) {
            return;
        }

        int type = data.getInt(Apg.EXTRA_STATUS);
        switch (type) {
            case Id.message.progress_update: {
                String message = data.getString(Apg.EXTRA_MESSAGE);
                if (mProgressDialog != null) {
                    if (message != null) {
                        mProgressDialog.setMessage(message);
                    }
                    mProgressDialog.setMax(data.getInt(Apg.EXTRA_MAX));
                    mProgressDialog.setProgress(data.getInt(Apg.EXTRA_PROGRESS));
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

    public void passPhraseCallback(long keyId, String passPhrase) {
        Apg.setCachedPassPhrase(keyId, passPhrase);
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

    protected void setDeleteFile(String deleteFile) {
        mDeleteFile = deleteFile;
    }

    protected String getDeleteFile() {
        return mDeleteFile;
    }
}
