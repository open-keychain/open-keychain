/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.thialfihar.android.apg.ui;

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.compatibility.DialogFragmentWorkaround;
import org.thialfihar.android.apg.service.ApgIntentService;
import org.thialfihar.android.apg.service.ApgIntentServiceHandler;
import org.thialfihar.android.apg.ui.dialog.DeleteFileDialogFragment;
import org.thialfihar.android.apg.ui.dialog.DeleteKeyDialogFragment;
import org.thialfihar.android.apg.ui.dialog.FileDialogFragment;
import org.thialfihar.android.apg.util.Log;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class KeyListActivity extends SherlockFragmentActivity {

    public static final String ACTION_IMPORT = Constants.INTENT_PREFIX + "IMPORT";

    public static final String EXTRA_TEXT = "text";

    // protected View mFilterLayout;
    // protected Button mClearFilterButton;
    // protected TextView mFilterInfo;

    protected String mImportFilename = Constants.path.APP_DIR + "/";
    protected String mExportFilename = Constants.path.APP_DIR + "/";

    protected String mImportData;
    protected boolean mDeleteAfterImport = false;

    protected int mKeyType;

    FileDialogFragment mFileDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        handleActions(getIntent());
    }

    // TODO: needed?
    // @Override
    // protected void onNewIntent(Intent intent) {
    // super.onNewIntent(intent);
    // handleActions(intent);
    // }

    protected void handleActions(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        /**
         * Android Standard Actions
         */
        String searchString = null;
        if (Intent.ACTION_SEARCH.equals(action)) {
            searchString = extras.getString(SearchManager.QUERY);
            if (searchString != null && searchString.trim().length() == 0) {
                searchString = null;
            }
        } else if (Intent.ACTION_VIEW.equals(action)) {
            // Android's Action when opening file associated to APG (see AndroidManifest.xml)
            // override action to delegate it to APGs ACTION_IMPORT
            action = ACTION_IMPORT;
        }

        // if (searchString == null) {
        // mFilterLayout.setVisibility(View.GONE);
        // } else {
        // mFilterLayout.setVisibility(View.VISIBLE);
        // mFilterInfo.setText(getString(R.string.filterInfo, searchString));
        // }
        //
        // if (mListAdapter != null) {
        // mListAdapter.cleanup();
        // }
        // mListAdapter = new KeyListAdapter(this, searchString);
        // mList.setAdapter(mListAdapter);

        /**
         * APG's own Actions
         */
        if (ACTION_IMPORT.equals(action)) {
            if ("file".equals(intent.getScheme()) && intent.getDataString() != null) {
                mImportFilename = intent.getData().getPath();
            } else {
                mImportData = intent.getStringExtra(EXTRA_TEXT);
            }
            importKeys();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case Id.request.filename: {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    String path = data.getData().getPath();
                    Log.d(Constants.TAG, "path=" + path);

                    // set filename used in export/import dialogs
                    mFileDialog.setFilename(path);
                } catch (NullPointerException e) {
                    Log.e(Constants.TAG, "Nullpointer while retrieving path!", e);
                }
            }
            return;
        }

        default: {
            break;
        }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // TODO: reimplement!
        // menu.add(3, Id.menu.option.search, 0, R.string.menu_search)
        // .setIcon(R.drawable.ic_menu_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, Id.menu.option.import_keys, 5, R.string.menu_importKeys).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, Id.menu.option.export_keys, 6, R.string.menu_exportKeys).setShowAsAction(
                MenuItem.SHOW_AS_ACTION_NEVER | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

        case Id.menu.option.import_keys: {
            showImportKeysDialog();
            return true;
        }

        case Id.menu.option.export_keys: {
            showExportKeysDialog(-1);
            return true;
        }

        // case Id.menu.option.search:
        // startSearch("", false, null, false);
        // return true;

        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    /**
     * Show to dialog from where to import keys
     */
    public void showImportKeysDialog() {
        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OKAY) {
                    Log.d(Constants.TAG, "FileDialogFragment.MESSAGE_OKAY");
                    Bundle data = message.getData();
                    mImportFilename = data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME);

                    mDeleteAfterImport = data.getBoolean(FileDialogFragment.MESSAGE_DATA_CHECKED);
                    importKeys();
                }
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                mFileDialog = FileDialogFragment.newInstance(messenger,
                        getString(R.string.title_importKeys),
                        getString(R.string.specifyFileToImportFrom), mImportFilename, null,
                        Id.request.filename);

                mFileDialog.show(getSupportFragmentManager(), "fileDialog");
            }
        });
    }

    /**
     * Show dialog where to export keys
     * 
     * @param keyRingRowId
     *            if -1 export all keys
     */
    public void showExportKeysDialog(final long keyRingRowId) {
        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();
                    mExportFilename = data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME);

                    exportKeys(keyRingRowId);
                }
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                String title = null;
                if (keyRingRowId != -1) {
                    // single key export
                    title = getString(R.string.title_exportKey);
                } else {
                    title = getString(R.string.title_exportKeys);
                }

                String message = null;
                if (mKeyType == Id.type.public_key) {
                    message = getString(R.string.specifyFileToExportTo);
                } else {
                    message = getString(R.string.specifyFileToExportSecretKeysTo);
                }

                mFileDialog = FileDialogFragment.newInstance(messenger, title, message,
                        mExportFilename, null, Id.request.filename);

                mFileDialog.show(getSupportFragmentManager(), "fileDialog");
            }
        });
    }

    /**
     * Show dialog to delete key
     * 
     * @param keyRingId
     */
    public void showDeleteKeyDialog(long keyRingId) {
        // Message is received after key is deleted
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == DeleteKeyDialogFragment.MESSAGE_OKAY) {
                    // no further actions needed
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        DeleteKeyDialogFragment deleteKeyDialog = DeleteKeyDialogFragment.newInstance(messenger,
                keyRingId, mKeyType);

        deleteKeyDialog.show(getSupportFragmentManager(), "deleteKeyDialog");
    }

    /**
     * Import keys with mImportData
     */
    public void importKeys() {
        Log.d(Constants.TAG, "importKeys started");

        // Send all information needed to service to import key in other thread
        Intent intent = new Intent(this, ApgIntentService.class);

        intent.putExtra(ApgIntentService.EXTRA_ACTION, ApgIntentService.ACTION_IMPORT_KEY);

        // fill values for this action
        Bundle data = new Bundle();

        data.putInt(ApgIntentService.IMPORT_KEY_TYPE, mKeyType);

        if (mImportData != null) {
            data.putInt(ApgIntentService.TARGET, ApgIntentService.TARGET_BYTES);
            data.putByteArray(ApgIntentService.IMPORT_BYTES, mImportData.getBytes());
        } else {
            data.putInt(ApgIntentService.TARGET, ApgIntentService.TARGET_FILE);
            data.putString(ApgIntentService.IMPORT_FILENAME, mImportFilename);
        }

        intent.putExtra(ApgIntentService.EXTRA_DATA, data);

        // Message is received after importing is done in ApgService
        ApgIntentServiceHandler saveHandler = new ApgIntentServiceHandler(this,
                R.string.progress_importing, ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == ApgIntentServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    int added = returnData.getInt(ApgIntentService.RESULT_IMPORT_ADDED);
                    int updated = returnData.getInt(ApgIntentService.RESULT_IMPORT_UPDATED);
                    int bad = returnData.getInt(ApgIntentService.RESULT_IMPORT_BAD);
                    String toastMessage;
                    if (added > 0 && updated > 0) {
                        toastMessage = getString(R.string.keysAddedAndUpdated, added, updated);
                    } else if (added > 0) {
                        toastMessage = getString(R.string.keysAdded, added);
                    } else if (updated > 0) {
                        toastMessage = getString(R.string.keysUpdated, updated);
                    } else {
                        toastMessage = getString(R.string.noKeysAddedOrUpdated);
                    }
                    Toast.makeText(KeyListActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
                    if (bad > 0) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(KeyListActivity.this);

                        alert.setIcon(android.R.drawable.ic_dialog_alert);
                        alert.setTitle(R.string.warning);
                        alert.setMessage(KeyListActivity.this.getString(
                                R.string.badKeysEncountered, bad));

                        alert.setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        alert.setCancelable(true);
                        alert.create().show();
                    } else if (mDeleteAfterImport) {
                        // everything went well, so now delete, if that was turned on
                        DeleteFileDialogFragment deleteFileDialog = DeleteFileDialogFragment
                                .newInstance(mImportFilename);
                        deleteFileDialog.show(getSupportFragmentManager(), "deleteDialog");
                    }
                }
            };
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(ApgIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);
    }

    /**
     * Export keys
     * 
     * @param keyRingRowId
     *            if -1 export all keys
     */
    public void exportKeys(long keyRingRowId) {
        Log.d(Constants.TAG, "exportKeys started");

        // Send all information needed to service to export key in other thread
        Intent intent = new Intent(this, ApgIntentService.class);

        intent.putExtra(ApgIntentService.EXTRA_ACTION, ApgIntentService.ACTION_EXPORT_KEY);

        // fill values for this action
        Bundle data = new Bundle();

        data.putString(ApgIntentService.EXPORT_FILENAME, mExportFilename);
        data.putInt(ApgIntentService.EXPORT_KEY_TYPE, mKeyType);

        if (keyRingRowId == -1) {
            data.putBoolean(ApgIntentService.EXPORT_ALL, true);
        } else {
            data.putLong(ApgIntentService.EXPORT_KEY_RING_ROW_ID, keyRingRowId);
        }

        intent.putExtra(ApgIntentService.EXTRA_DATA, data);

        // Message is received after exporting is done in ApgService
        ApgIntentServiceHandler exportHandler = new ApgIntentServiceHandler(this,
                R.string.progress_exporting, ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == ApgIntentServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    int exported = returnData.getInt(ApgIntentService.RESULT_EXPORT);
                    String toastMessage;
                    if (exported == 1) {
                        toastMessage = getString(R.string.keyExported);
                    } else if (exported > 0) {
                        toastMessage = getString(R.string.keysExported, exported);
                    } else {
                        toastMessage = getString(R.string.noKeysExported);
                    }
                    Toast.makeText(KeyListActivity.this, toastMessage, Toast.LENGTH_SHORT).show();

                }
            };
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(exportHandler);
        intent.putExtra(ApgIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        exportHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);
    }
}
