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
import org.bouncycastle2.openpgp.PGPSecretKey;
import org.bouncycastle2.openpgp.PGPSecretKeyRing;
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
import android.widget.ExpandableListView.OnChildClickListener;

public class SecretKeyListActivity extends BaseActivity implements OnChildClickListener {
    ExpandableListView mList;

    protected int mSelectedItem = -1;
    protected int mTask = 0;

    private String mImportFilename = Constants.path.app_dir + "/secring.gpg";
    private String mExportFilename = Constants.path.app_dir + "/secexport.asc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.key_list);

        mList = (ExpandableListView) findViewById(R.id.list);
        mList.setAdapter(new SecretKeyListAdapter(this));
        registerForContextMenu(mList);
        mList.setOnChildClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Id.menu.option.import_keys, 0, "Import Keys")
                .setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, Id.menu.option.export_keys, 1, "Export Keys")
                .setIcon(android.R.drawable.ic_menu_save);
        menu.add(1, Id.menu.option.create, 2, "Create Key")
                .setIcon(android.R.drawable.ic_menu_add);
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

            case Id.menu.option.create: {
                createKey();
                return true;
            }

            default: {
                break;
            }
        }
        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        ExpandableListView.ExpandableListContextMenuInfo info =
                (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition);

        if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            PGPSecretKeyRing keyRing = Apg.getSecretKeyRings().get(groupPosition);
            String userId = Apg.getMainUserIdSafe(this, Apg.getMasterKey(keyRing));
            menu.setHeaderTitle(userId);
            menu.add(0, Id.menu.edit, 0, "Edit Key");
            menu.add(0, Id.menu.export, 1, "Export Key");
            menu.add(0, Id.menu.delete, 2, "Delete Key");
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
            case Id.menu.edit: {
                mSelectedItem = groupPosition;
                showDialog(Id.dialog.pass_phrase);
                return true;
            }

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
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                int childPosition, long id) {
        mSelectedItem = groupPosition;
        showDialog(Id.dialog.pass_phrase);
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        boolean singleKeyExport = false;

        switch (id) {
            case Id.dialog.delete_key: {
                PGPSecretKeyRing keyRing = Apg.getSecretKeyRings().get(mSelectedItem);

                String userId = Apg.getMainUserIdSafe(this, Apg.getMasterKey(keyRing));

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Warning  ");
                builder.setMessage("Do you really want to delete the key '" + userId + "'?\n" +
                                   "You can't undo this!");
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int id) {
                                                  deleteKey(mSelectedItem);
                                                  mSelectedItem = -1;
                                                  removeDialog(Id.dialog.delete_key);
                                              }
                                          });
                builder.setNegativeButton(android.R.string.ok,
                                          new DialogInterface.OnClickListener() {
                                              public void onClick(DialogInterface dialog, int id) {
                                                  mSelectedItem = -1;
                                                  removeDialog(Id.dialog.delete_key);
                                              }
                                          });
                return builder.create();
            }

            case Id.dialog.import_keys: {
                return FileDialog.build(this, "Import Keys",
                                        "Please specify which file to import from.",
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
                                        getString(R.string.filemanager_title_open),
                                        getString(R.string.filemanager_btn_open),
                                        Id.request.filename);
            }

            case Id.dialog.export_key: {
                singleKeyExport = true;
                // break intentionally omitted, to use the Id.dialog.export_keys dialog
            }

            case Id.dialog.export_keys: {
                String title = "Export Key";

                if (!singleKeyExport) {
                    // plural "Keys"
                    title += "s";
                }
                final int thisDialogId = (singleKeyExport ? Id.dialog.delete_key : Id.dialog.export_keys);

                return FileDialog.build(this, title,
                                        "Please specify which file to export to.\n" +
                                        "WARNING! You are about to export SECRET keys.\n" +
                                        "WARNING! File will be overwritten if it exists.",
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
                                        getString(R.string.filemanager_title_save),
                                        getString(R.string.filemanager_btn_save),
                                        Id.request.filename);
            }

            case Id.dialog.pass_phrase: {
                PGPSecretKeyRing keyRing = Apg.getSecretKeyRings().get(mSelectedItem);
                long keyId = keyRing.getSecretKey().getKeyID();
                return AskForSecretKeyPassPhrase.createDialog(this, keyId, this);
            }
        }
        return super.onCreateDialog(id);
    }

    @Override
    public void passPhraseCallback(String passPhrase) {
        super.passPhraseCallback(passPhrase);
        editKey();
    }

    private void createKey() {
        Intent intent = new Intent(this, EditKeyActivity.class);
        startActivityForResult(intent, Id.message.create_key);
    }

    private void editKey() {
        PGPSecretKeyRing keyRing = Apg.getSecretKeyRings().get(mSelectedItem);
        long keyId = keyRing.getSecretKey().getKeyID();
        Intent intent = new Intent(this, EditKeyActivity.class);
        intent.putExtra("keyId", keyId);
        startActivityForResult(intent, Id.message.edit_key);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Id.message.create_key: // intentionally no break
            case Id.message.edit_key: {
                if (resultCode == RESULT_OK) {
                    refreshList();
                }
                break;
            }

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
                data = Apg.importKeyRings(this, Id.type.secret_key, filename, this);
            } else {
                Vector<Object> keys = new Vector<Object>();
                if (mSelectedItem == -1) {
                    for (PGPSecretKeyRing key : Apg.getSecretKeyRings()) {
                        keys.add(key);
                    }
                } else {
                    keys.add(Apg.getSecretKeyRings().get(mSelectedItem));
                }
                data = Apg.exportKeyRings(this, keys, filename, this);
            }
        } catch (FileNotFoundException e) {
            error = "file '" + filename + "' not found";
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
        PGPSecretKeyRing keyRing = Apg.getSecretKeyRings().get(index);
        Apg.deleteKey(this, keyRing);
        refreshList();
    }

    private void refreshList() {
        ((SecretKeyListAdapter) mList.getExpandableListAdapter()).notifyDataSetChanged();
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
                        Toast.makeText(SecretKeyListActivity.this,
                                       "Error: " + data.getString("error"),
                                       Toast.LENGTH_SHORT).show();
                    } else {
                        int added = data.getInt("added");
                        int updated = data.getInt("updated");
                        String message;
                        if (added > 0 && updated > 0) {
                            message = "Succssfully added " + added + " keys and updated " +
                                      updated + " keys.";
                        } else if (added > 0) {
                            message = "Succssfully added " + added + " keys.";
                        } else if (updated > 0) {
                            message = "Succssfully updated " + updated + " keys.";
                        } else {
                            message = "No keys added or updated.";
                        }
                        Toast.makeText(SecretKeyListActivity.this, message,
                                       Toast.LENGTH_SHORT).show();
                    }
                    refreshList();
                    break;
                }

                case Id.message.export_done: {
                    removeDialog(Id.dialog.exporting);

                    String error = data.getString("error");
                    if (error != null) {
                        Toast.makeText(SecretKeyListActivity.this,
                                       "Error: " + data.getString("error"),
                                       Toast.LENGTH_SHORT).show();
                    } else {
                        int exported = data.getInt("exported");
                        String message;
                        if (exported == 1) {
                            message = "Succssfully exported 1 key.";
                        } else if (exported > 0) {
                            message = "Succssfully exported " + exported + " keys.";
                        } else{
                            message = "No keys exported.";
                        }
                        Toast.makeText(SecretKeyListActivity.this, message,
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

    private static class SecretKeyListAdapter extends BaseExpandableListAdapter {
        private LayoutInflater mInflater;

        private class KeyChild {
            static final int KEY = 0;
            static final int USER_ID = 1;

            public int type;
            public PGPSecretKey key;
            public String userId;

            public KeyChild(PGPSecretKey key) {
                type = KEY;
                this.key = key;
            }

            public KeyChild(String userId) {
                type = USER_ID;
                this.userId = userId;
            }
        }

        public SecretKeyListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        protected Vector<KeyChild> getChildrenOfKeyRing(PGPSecretKeyRing keyRing) {
            Vector<KeyChild> children = new Vector<KeyChild>();
            PGPSecretKey masterKey = null;
            for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
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
            return Apg.getSecretKeyRings().size();
        }

        public Object getChild(int groupPosition, int childPosition) {
            PGPSecretKeyRing keyRing = Apg.getSecretKeyRings().get(groupPosition);
            Vector<KeyChild> children = getChildrenOfKeyRing(keyRing);
            KeyChild child = children.get(childPosition);
            return child;
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) {
            return getChildrenOfKeyRing(Apg.getSecretKeyRings().get(groupPosition)).size();
        }

        public Object getGroup(int position) {
            return position;
        }

        public long getGroupId(int position) {
            return position;
        }

        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            PGPSecretKeyRing keyRing = Apg.getSecretKeyRings().get(groupPosition);
            for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(keyRing.getSecretKeys())) {
                View view;
                if (!key.isMasterKey()) {
                    continue;
                }
                view = mInflater.inflate(R.layout.key_list_group_item, null);
                view.setBackgroundResource(android.R.drawable.list_selector_background);

                TextView mainUserId = (TextView) view.findViewById(R.id.main_user_id);
                mainUserId.setText("");
                TextView mainUserIdRest = (TextView) view.findViewById(R.id.main_user_id_rest);
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
                    mainUserId.setText(R.string.unknown_user_id);
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
            PGPSecretKeyRing keyRing = Apg.getSecretKeyRings().get(groupPosition);
            Vector<KeyChild> children = getChildrenOfKeyRing(keyRing);

            KeyChild child = children.get(childPosition);
            View view = null;
            switch (child.type) {
                case KeyChild.KEY: {
                    PGPSecretKey key = child.key;
                    if (key.isMasterKey()) {
                        view = mInflater.inflate(R.layout.key_list_child_item_master_key, null);
                    } else {
                        view = mInflater.inflate(R.layout.key_list_child_item_sub_key, null);
                    }

                    TextView keyId = (TextView) view.findViewById(R.id.key_id);
                    String keyIdStr = Long.toHexString(key.getKeyID() & 0xffffffffL);
                    while (keyIdStr.length() < 8) {
                        keyIdStr = "0" + keyIdStr;
                    }
                    keyId.setText(keyIdStr);
                    TextView keyDetails = (TextView) view.findViewById(R.id.key_details);
                    String algorithmStr = Apg.getAlgorithmInfo(key);
                    keyDetails.setText("(" + algorithmStr + ")");

                    ImageView encryptIcon = (ImageView) view.findViewById(R.id.ic_encrypt_key);
                    if (!Apg.isEncryptionKey(key)) {
                        encryptIcon.setVisibility(View.GONE);
                    }

                    ImageView signIcon = (ImageView) view.findViewById(R.id.ic_sign_key);
                    if (!Apg.isSigningKey(key)) {
                        signIcon.setVisibility(View.GONE);
                    }
                    break;
                }

                case KeyChild.USER_ID: {
                    view = mInflater.inflate(R.layout.key_list_child_item_user_id, null);
                    TextView userId = (TextView) view.findViewById(R.id.user_id);
                    userId.setText(child.userId);
                    break;
                }
            }
            return view;
        }
    }
}
