package org.thialfihar.android.apg.ui;

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.service.ApgService;
import org.thialfihar.android.apg.service.ApgServiceHandler;
import org.thialfihar.android.apg.ui.KeyListActivityOld.KeyListAdapter;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class KeyListActivity extends SherlockFragmentActivity {

    public static final String ACTION_IMPORT = Constants.INTENT_PREFIX + "IMPORT";

    public static final String EXTRA_TEXT = "text";

    protected ExpandableListView mList;
    protected KeyListAdapter mListAdapter;
    protected View mFilterLayout;
    protected Button mClearFilterButton;
    protected TextView mFilterInfo;

    protected int mSelectedItem = -1;
    // protected int mTask = 0;

    protected String mImportFilename = Constants.path.APP_DIR + "/";
    protected String mExportFilename = Constants.path.APP_DIR + "/";

    protected String mImportData;
    protected boolean mDeleteAfterImport = false;

    protected int mKeyType = Id.type.public_key;

    FileDialogFragment mFileDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    protected void handleIntent(Intent intent) {
        String searchString = null;
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchString = intent.getStringExtra(SearchManager.QUERY);
            if (searchString != null && searchString.trim().length() == 0) {
                searchString = null;
            }
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

        // Get intent, action
        // Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            // Android's Action when opening file associated to APG (see AndroidManifest.xml)

            handleActionImport(intent);
        } else if (ACTION_IMPORT.equals(action)) {
            // APG's own Actions

            handleActionImport(intent);
        }
    }

    /**
     * Handles import action
     * 
     * @param intent
     */
    private void handleActionImport(Intent intent) {
        if ("file".equals(intent.getScheme()) && intent.getDataString() != null) {
            mImportFilename = intent.getData().getPath();
        } else {
            mImportData = intent.getStringExtra(EXTRA_TEXT);
        }
        importKeys();
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
            showExportKeysDialog(false);
            return true;
        }

//        case Id.menu.option.search:
//            startSearch("", false, null, false);
//            return true;

        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }

    private void showImportKeysDialog() {
        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();
                    mImportFilename = data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME);

                    mDeleteAfterImport = data.getBoolean(FileDialogFragment.MESSAGE_DATA_CHECKED);
                    importKeys();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        mFileDialog = FileDialogFragment.newInstance(messenger,
                getString(R.string.title_importKeys), getString(R.string.specifyFileToImportFrom),
                mImportFilename, null, Id.request.filename);

        mFileDialog.show(getSupportFragmentManager(), "fileDialog");
    }

    private void showExportKeysDialog(boolean singleKeyExport) {
        String title = (singleKeyExport ? getString(R.string.title_exportKey)
                : getString(R.string.title_exportKeys));
        String message = getString(mKeyType == Id.type.public_key ? R.string.specifyFileToExportTo
                : R.string.specifyFileToExportSecretKeysTo);

        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();
                    mExportFilename = data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME);

                    exportKeys();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        mFileDialog = FileDialogFragment.newInstance(messenger, title, message, mExportFilename,
                null, Id.request.filename);

        mFileDialog.show(getSupportFragmentManager(), "fileDialog");
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem menuItem) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuItem.getMenuInfo();
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);

        if (type != ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            return super.onContextItemSelected(menuItem);
        }

        switch (menuItem.getItemId()) {
        case Id.menu.export: {
            mSelectedItem = groupPosition;
            showExportKeysDialog(true);
            return true;
        }

        case Id.menu.delete: {
            mSelectedItem = groupPosition;
            showDeleteKeyDialog();
            return true;
        }

        default: {
            return super.onContextItemSelected(menuItem);
        }
        }
    }

    private void showDeleteKeyDialog() {
        final int keyRingId = mListAdapter.getKeyRingId(mSelectedItem);
        mSelectedItem = -1;

        // Message is received after key is deleted
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == DeleteKeyDialogFragment.MESSAGE_OKAY) {
//                    refreshList();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        DeleteKeyDialogFragment deleteKeyDialog = DeleteKeyDialogFragment.newInstance(messenger,
                keyRingId, mKeyType);

        deleteKeyDialog.show(getSupportFragmentManager(), "deleteKeyDialog");
    }

    public void importKeys() {
        Log.d(Constants.TAG, "importKeys started");

        // Send all information needed to service to import key in other thread
        Intent intent = new Intent(this, ApgService.class);

        intent.putExtra(ApgService.EXTRA_ACTION, ApgService.ACTION_IMPORT_KEY);

        // fill values for this action
        Bundle data = new Bundle();

        data.putInt(ApgService.IMPORT_KEY_TYPE, mKeyType);

        if (mImportData != null) {
            data.putInt(ApgService.TARGET, ApgService.TARGET_BYTES);
            data.putByteArray(ApgService.IMPORT_BYTES, mImportData.getBytes());
        } else {
            data.putInt(ApgService.TARGET, ApgService.TARGET_FILE);
            data.putString(ApgService.IMPORT_FILENAME, mImportFilename);
        }

        intent.putExtra(ApgService.EXTRA_DATA, data);

        // Message is received after importing is done in ApgService
        ApgServiceHandler saveHandler = new ApgServiceHandler(this, R.string.progress_importing,
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == ApgServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    int added = returnData.getInt(ApgService.RESULT_IMPORT_ADDED);
                    int updated = returnData.getInt(ApgService.RESULT_IMPORT_UPDATED);
                    int bad = returnData.getInt(ApgService.RESULT_IMPORT_BAD);
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
                    Toast.makeText(KeyListActivity.this, toastMessage, Toast.LENGTH_SHORT)
                            .show();
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
//                    refreshList();

                }
            };
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(ApgService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);
    }

    public void exportKeys() {
        Log.d(Constants.TAG, "exportKeys started");

        // Send all information needed to service to export key in other thread
        Intent intent = new Intent(this, ApgService.class);

        intent.putExtra(ApgService.EXTRA_ACTION, ApgService.ACTION_EXPORT_KEY);

        // fill values for this action
        Bundle data = new Bundle();

        data.putString(ApgService.EXPORT_FILENAME, mExportFilename);
        data.putInt(ApgService.EXPORT_KEY_TYPE, mKeyType);

        if (mSelectedItem == -1) {
            data.putBoolean(ApgService.EXPORT_ALL, true);
        } else {
            int keyRingId = mListAdapter.getKeyRingId(mSelectedItem);
            data.putInt(ApgService.EXPORT_KEY_RING_ID, keyRingId);
            mSelectedItem = -1;
        }

        intent.putExtra(ApgService.EXTRA_DATA, data);

        // Message is received after exporting is done in ApgService
        ApgServiceHandler exportHandler = new ApgServiceHandler(this, R.string.progress_exporting,
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == ApgServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    int exported = returnData.getInt(ApgService.RESULT_EXPORT);
                    String toastMessage;
                    if (exported == 1) {
                        toastMessage = getString(R.string.keyExported);
                    } else if (exported > 0) {
                        toastMessage = getString(R.string.keysExported, exported);
                    } else {
                        toastMessage = getString(R.string.noKeysExported);
                    }
                    Toast.makeText(KeyListActivity.this, toastMessage, Toast.LENGTH_SHORT)
                            .show();

                }
            };
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(exportHandler);
        intent.putExtra(ApgService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        exportHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);
    }
}
