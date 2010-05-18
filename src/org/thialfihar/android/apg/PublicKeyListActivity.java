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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import org.bouncycastle2.openpgp.PGPException;
import org.bouncycastle2.openpgp.PGPPublicKey;
import org.bouncycastle2.openpgp.PGPPublicKeyRing;
import org.thialfihar.android.apg.utils.IterableIterator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class PublicKeyListActivity extends BaseActivity {
    ExpandableListView mList;

    protected int mSelectedItem = -1;
    protected int mTask = 0;

    private String mImportFilename = Constants.path.app_dir + "/pubring.gpg";
    private String mExportFilename = Constants.path.app_dir + "/pubexport.asc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.key_list);

        mList = (ExpandableListView) findViewById(R.id.list);
        mList.setAdapter(new PublicKeyListAdapter(this));
        registerForContextMenu(mList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Id.menu.option.import_keys, 0, R.string.menu_importKeys)
                .setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, Id.menu.option.export_keys, 1, R.string.menu_exportKeys)
                .setIcon(android.R.drawable.ic_menu_save);
        menu.add(1, Id.menu.option.preferences, 2, R.string.menu_preferences)
                .setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(1, Id.menu.option.about, 3, R.string.menu_about)
                .setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Id.menu.option.import_keys: {
                showDialog(Id.dialog.import_keys);
                return true;
            }

            case Id.menu.option.export_keys: {
                showDialog(Id.dialog.export_keys);
                return true;
            }

            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ExpandableListView.ExpandableListContextMenuInfo info =
                (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);

        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            PGPPublicKeyRing keyRing = Apg.getPublicKeyRings().get(groupPosition);
            String userId = Apg.getMainUserIdSafe(this, Apg.getMasterKey(keyRing));
            menu.setHeaderTitle(userId);
            menu.add(0, Id.menu.export, 0, R.string.menu_exportKey);
            menu.add(0, Id.menu.delete, 1, R.string.menu_deleteKey);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuItem.getMenuInfo();
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);

        if (type != ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            return super.onContextItemSelected(menuItem);
        }

        switch (menuItem.getItemId()) {
            case Id.menu.export: {
                mSelectedItem = groupPosition;
                showDialog(Id.dialog.export_key);
                return true;
            }

            case Id.menu.delete: {
                mSelectedItem = groupPosition;
                showDialog(Id.dialog.delete_key);
                return true;
            }

            default: {
                return super.onContextItemSelected(menuItem);
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        boolean singleKeyExport = false;

        switch (id) {
            case Id.dialog.delete_key: {
                PGPPublicKeyRing keyRing = Apg.getPublicKeyRings().get(mSelectedItem);
                String userId = Apg.getMainUserIdSafe(this, Apg.getMasterKey(keyRing));

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.warning);
                builder.setMessage(getString(R.string.keyDeletionConfirmation, userId));
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setPositiveButton(R.string.btn_delete,
                                          new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int id) {
                                                  deleteKey(mSelectedItem);
                                                  mSelectedItem = -1;
                                                  removeDialog(Id.dialog.delete_key);
                                              }
                                          });
                builder.setNegativeButton(android.R.string.cancel,
                                          new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int id) {
                                                  mSelectedItem = -1;
                                                  removeDialog(Id.dialog.delete_key);
                                              }
                                          });
                return builder.create();
            }

            case Id.dialog.import_keys: {
                return FileDialog.build(this, getString(R.string.title_importKeys),
                                        getString(R.string.specifyFileToImportFrom),
                                        mImportFilename,
                                        new FileDialog.OnClickListener() {

                                            @Override
                                            public void onOkClick(String filename) {
                                                removeDialog(Id.dialog.import_keys);
                                                mImportFilename = filename;
                                                importKeys();
                                            }

                                            @Override
                                            public void onCancelClick() {
                                                removeDialog(Id.dialog.import_keys);
                                            }
                                        },
                                        getString(R.string.filemanager_titleOpen),
                                        getString(R.string.filemanager_btnOpen),
                                        Id.request.filename);
            }

            case Id.dialog.export_key: {
                singleKeyExport = true;
                // break intentionally omitted, to use the Id.dialog.export_keys dialog
            }

            case Id.dialog.export_keys: {
                String title = (singleKeyExport ?
                                    getString(R.string.title_exportKey) :
                                    getString(R.string.title_exportKeys));

                final int thisDialogId = (singleKeyExport ? Id.dialog.export_key : Id.dialog.export_keys);

                return FileDialog.build(this, title,
                                        getString(R.string.specifyFileToExportTo),
                                        mExportFilename,
                                        new FileDialog.OnClickListener() {

                                            @Override
                                            public void onOkClick(String filename) {
                                                removeDialog(thisDialogId);
                                                mExportFilename = filename;
                                                exportKeys();
                                            }

                                            @Override
                                            public void onCancelClick() {
                                                removeDialog(thisDialogId);
                                            }
                                        },
                                        getString(R.string.filemanager_titleSave),
                                        getString(R.string.filemanager_btnSave),
                                        Id.request.filename);
            }

            default: {
                return super.onCreateDialog(id);
            }
        }
    }

    public void importKeys() {
        showDialog(Id.dialog.importing);
        mTask = Id.task.import_keys;
        startThread();
    }

    public void exportKeys() {
        showDialog(Id.dialog.exporting);
        mTask = Id.task.export_keys;
        startThread();
    }

    @Override
    public void run() {
        String error = null;
        Bundle data = new Bundle();
        Message msg = new Message();

        String filename = null;
        if (mTask == Id.task.import_keys) {
            filename = mImportFilename;
        } else {
            filename = mExportFilename;
        }

        try {
            if (mTask == Id.task.import_keys) {
                data = Apg.importKeyRings(this, Id.type.public_key, filename, this);
            } else {
                Vector<Object> keys = new Vector<Object>();
                if (mSelectedItem == -1) {
                    for (PGPPublicKeyRing key : Apg.getPublicKeyRings()) {
                        keys.add(key);
                    }
                } else {
                    keys.add(Apg.getPublicKeyRings().get(mSelectedItem));
                }
                data = Apg.exportKeyRings(this, keys, filename, this);
            }
        } catch (FileNotFoundException e) {
            error = getString(R.string.error_fileNotFound);
        } catch (IOException e) {
            error = e.getMessage();
        } catch (PGPException e) {
            error = e.getMessage();
        } catch (Apg.GeneralException e) {
            error = e.getMessage();
        }

        if (mTask == Id.task.import_keys) {
            data.putInt("type", Id.message.import_done);
        } else {
            data.putInt("type", Id.message.export_done);
        }

        if (error != null) {
            data.putString("error", error);
        }

        msg.setData(data);
        sendMessage(msg);
    }

    private void deleteKey(int index) {
        PGPPublicKeyRing keyRing = Apg.getPublicKeyRings().get(index);
        Apg.deleteKey(this, keyRing);
        refreshList();
    }

    private void refreshList() {
        ((PublicKeyListAdapter) mList.getExpandableListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void doneCallback(Message msg) {
        super.doneCallback(msg);

        Bundle data = msg.getData();
        if (data != null) {
            int type = data.getInt("type");
            switch (type) {
                case Id.message.import_done: {
                    removeDialog(Id.dialog.importing);

                    String error = data.getString("error");
                    if (error != null) {
                        Toast.makeText(PublicKeyListActivity.this,
                                       getString(R.string.errorMessage, data.getString("error")),
                                       Toast.LENGTH_SHORT).show();
                    } else {
                        int added = data.getInt("added");
                        int updated = data.getInt("updated");
                        String message;
                        if (added > 0 && updated > 0) {
                            message = getString(R.string.keysAddedAndUpdated, added, updated);
                        } else if (added > 0) {
                            message = getString(R.string.keysAdded, added);
                        } else if (updated > 0) {
                            message = getString(R.string.keysUpdated, updated);
                        } else {
                            message = getString(R.string.noKeysAddedOrUpdated);
                        }
                        Toast.makeText(PublicKeyListActivity.this, message,
                                       Toast.LENGTH_SHORT).show();
                    }
                    refreshList();
                    break;
                }

                case Id.message.export_done: {
                    removeDialog(Id.dialog.exporting);

                    String error = data.getString("error");
                    if (error != null) {
                        Toast.makeText(PublicKeyListActivity.this,
                                       getString(R.string.errorMessage, data.getString("error")),
                                       Toast.LENGTH_SHORT).show();
                    } else {
                        int exported = data.getInt("exported");
                        String message;
                        if (exported == 1) {
                            message = getString(R.string.keyExported);
                        } else if (exported > 0) {
                            message = getString(R.string.keysExported);
                        } else{
                            message = getString(R.string.noKeysExported);
                        }
                        Toast.makeText(PublicKeyListActivity.this, message,
                                       Toast.LENGTH_SHORT).show();
                    }
                    break;
                }

                default: {
                    break;
                }
            }
        }
    }

    private static class PublicKeyListAdapter extends BaseExpandableListAdapter {
        private LayoutInflater mInflater;

        private class KeyChild {
            public static final int KEY = 0;
            public static final int USER_ID = 1;

            public int type;
            public PGPPublicKey key;
            public String userId;

            public KeyChild(PGPPublicKey key) {
                type = KEY;
                this.key = key;
            }

            public KeyChild(String userId) {
                type = USER_ID;
                this.userId = userId;
            }
        }

        public PublicKeyListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        protected Vector<KeyChild> getChildrenOfKeyRing(PGPPublicKeyRing keyRing) {
            Vector<KeyChild> children = new Vector<KeyChild>();
            PGPPublicKey masterKey = null;
            for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
                children.add(new KeyChild(key));
                if (key.isMasterKey()) {
                    masterKey = key;
                }
            }

            if (masterKey != null) {
                boolean isFirst = true;
                for (String userId : new IterableIterator<String>(masterKey.getUserIDs())) {
                    if (isFirst) {
                        // ignore first, it's in the group already
                        isFirst = false;
                        continue;
                    }
                    children.add(new KeyChild(userId));
                }
            }

            return children;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public int getGroupCount() {
            return Apg.getPublicKeyRings().size();
        }

        public Object getChild(int groupPosition, int childPosition) {
            PGPPublicKeyRing keyRing = Apg.getPublicKeyRings().get(groupPosition);
            Vector<KeyChild> children = getChildrenOfKeyRing(keyRing);

            KeyChild child = children.get(childPosition);
            return child;
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) {
            return getChildrenOfKeyRing(Apg.getPublicKeyRings().get(groupPosition)).size();
        }

        public Object getGroup(int position) {
            return position;
        }

        public long getGroupId(int position) {
            return position;
        }

        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                                 ViewGroup parent) {
            PGPPublicKeyRing keyRing = Apg.getPublicKeyRings().get(groupPosition);
            for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
                View view;
                if (!key.isMasterKey()) {
                    continue;
                }
                view = mInflater.inflate(R.layout.key_list_group_item, null);
                view.setBackgroundResource(android.R.drawable.list_selector_background);

                TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
                mainUserId.setText("");
                TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
                mainUserIdRest.setText("");

                String userId = Apg.getMainUserId(key);
                if (userId != null) {
                    String chunks[] = userId.split(" <", 2);
                    userId = chunks[0];
                    if (chunks.length > 1) {
                        mainUserIdRest.setText("<" + chunks[1]);
                    }
                    mainUserId.setText(userId);
                }

                if (mainUserId.getText().length() == 0) {
                    mainUserId.setText(R.string.unknownUserId);
                }

                if (mainUserIdRest.getText().length() == 0) {
                    mainUserIdRest.setVisibility(View.GONE);
                }
                return view;
            }
            return null;
        }

        public View getChildView(int groupPosition, int childPosition,
                                 boolean isLastChild, View convertView,
                                 ViewGroup parent) {
            PGPPublicKeyRing keyRing = Apg.getPublicKeyRings().get(groupPosition);
            Vector<KeyChild> children = getChildrenOfKeyRing(keyRing);

            KeyChild child = children.get(childPosition);
            View view = null;
            switch (child.type) {
                case KeyChild.KEY: {
                    PGPPublicKey key = child.key;
                    if (key.isMasterKey()) {
                        view = mInflater.inflate(R.layout.key_list_child_item_master_key, null);
                    } else {
                        view = mInflater.inflate(R.layout.key_list_child_item_sub_key, null);
                    }

                    TextView keyId = (TextView) view.findViewById(R.id.keyId);
                    String keyIdStr = Long.toHexString(key.getKeyID() & 0xffffffffL);
                    while (keyIdStr.length() < 8) {
                        keyIdStr = "0" + keyIdStr;
                    }
                    keyId.setText(keyIdStr);
                    TextView keyDetails = (TextView) view.findViewById(R.id.keyDetails);
                    String algorithmStr = Apg.getAlgorithmInfo(key);
                    keyDetails.setText("(" + algorithmStr + ")");

                    ImageView encryptIcon = (ImageView) view.findViewById(R.id.ic_encryptKey);
                    if (!Apg.isEncryptionKey(key)) {
                        encryptIcon.setVisibility(View.GONE);
                    }

                    ImageView signIcon = (ImageView) view.findViewById(R.id.ic_signKey);
                    if (!Apg.isSigningKey(key)) {
                        signIcon.setVisibility(View.GONE);
                    }
                    break;
                }

                case KeyChild.USER_ID: {
                    view = mInflater.inflate(R.layout.key_list_child_item_user_id, null);
                    TextView userId = (TextView) view.findViewById(R.id.userId);
                    userId.setText(child.userId);
                    break;
                }
            }
            return view;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Id.request.filename: {
                if (resultCode == RESULT_OK && data != null) {
                    String filename = data.getDataString();
                    if (filename != null) {
                        // Get rid of URI prefix:
                        if (filename.startsWith("file://")) {
                            filename = filename.substring(7);
                        }
                        // replace %20 and so on
                        filename = Uri.decode(filename);

                        FileDialog.setFilename(filename);
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
}
