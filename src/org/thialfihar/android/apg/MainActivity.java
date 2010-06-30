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

import org.thialfihar.android.apg.provider.Accounts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends BaseActivity {
    private ListView mAccounts = null;
    private AccountListAdapter mListAdapter = null;
    private Cursor mAccountCursor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button encryptMessageButton = (Button) findViewById(R.id.btn_encryptMessage);
        Button decryptMessageButton = (Button) findViewById(R.id.btn_decryptMessage);
        Button encryptFileButton = (Button) findViewById(R.id.btn_encryptFile);
        Button decryptFileButton = (Button) findViewById(R.id.btn_decryptFile);
        mAccounts = (ListView) findViewById(R.id.accounts);

        encryptMessageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EncryptActivity.class);
                intent.setAction(Apg.Intent.ENCRYPT);
                startActivity(intent);
            }
        });

        decryptMessageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DecryptActivity.class);
                intent.setAction(Apg.Intent.DECRYPT);
                startActivity(intent);
            }
        });

        encryptFileButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EncryptActivity.class);
                intent.setAction(Apg.Intent.ENCRYPT_FILE);
                startActivity(intent);
            }
        });

        decryptFileButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DecryptActivity.class);
                intent.setAction(Apg.Intent.DECRYPT_FILE);
                startActivity(intent);
            }
        });

        mAccountCursor =
                Apg.getDatabase().db().query(Accounts.TABLE_NAME,
                                             new String[] {
                                                 Accounts._ID,
                                                 Accounts.NAME,
                                             }, null, null, null, null, Accounts.NAME + " ASC");
        startManagingCursor(mAccountCursor);

        mListAdapter = new AccountListAdapter(this, mAccountCursor);
        mAccounts.setAdapter(mListAdapter);
        mAccounts.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int index, long id) {
                String accountName = (String) mAccounts.getItemAtPosition(index);
                startActivity(new Intent(MainActivity.this, MailListActivity.class)
                                        .putExtra(Apg.EXTRA_ACCOUNT, accountName));
            }
        });
        registerForContextMenu(mAccounts);

        if (!mPreferences.hasSeenChangeLog(Apg.getVersion(this))) {
            showDialog(Id.dialog.change_log);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case Id.dialog.new_account: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle(R.string.title_addAccount);
                alert.setMessage(R.string.specifyGoogleMailAccount);

                final EditText input = new EditText(this);
                alert.setView(input);

                alert.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                MainActivity.this.removeDialog(Id.dialog.new_account);
                                String accountName = "" + input.getText();

                                Cursor testCursor =
                                        managedQuery(Uri.parse("content://gmail-ls/conversations/" +
                                                               accountName),
                                                     null, null, null, null);
                                if (testCursor == null) {
                                    Toast.makeText(MainActivity.this,
                                                   getString(R.string.errorMessage,
                                                             getString(R.string.error_accountNotFound,
                                                                       accountName)),
                                                   Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                ContentValues values = new ContentValues();
                                values.put(Accounts.NAME, accountName);
                                try {
                                    Apg.getDatabase().db().insert(Accounts.TABLE_NAME,
                                                                  Accounts.NAME, values);
                                    mAccountCursor.requery();
                                    mListAdapter.notifyDataSetChanged();
                                } catch (SQLException e) {
                                    Toast.makeText(MainActivity.this,
                                                   getString(R.string.errorMessage,
                                                             getString(R.string.error_addingAccountFailed,
                                                                       accountName)),
                                                   Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                alert.setNegativeButton(android.R.string.cancel,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                MainActivity.this.removeDialog(Id.dialog.new_account);
                                            }
                                        });

                return alert.create();
            }

            case Id.dialog.change_log: {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle("Changes " + Apg.getFullVersion(this));
                LayoutInflater inflater =
                    (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.info, null);
                TextView message = (TextView) layout.findViewById(R.id.message);

                message.setText("Read the warnings!\n\n" +
                                "Changes:\n" +
                                "* k9mail integration, k9mail beta build is available on the k9mail website\n" +
                                "* German and Italian translation (thanks, cwoehrl and Fabrizio)\n" +
                                "* new preferences GUI\n" +
                                "* much smaller package\n" +
                                "* signature bugfix\n" +
                                "\n" +
                                "WARNING: be careful editing your existing keys, as they " +
                                "WILL be stripped of certificates right now.\n" +
                                "\n" +
                                "WARNING: key creation/editing doesn't support all " +
                                "GPG features yet. In particular: " +
                                "key cross-certification is NOT supported, so signing " +
                                "with those keys will get a warning when the signature is " +
                                "checked.\n" +
                                "\n" +
                                "I hope APG continues to be useful to you, please send " +
                                "bug reports, feature wishes, feedback.");
                alert.setView(layout);

                alert.setCancelable(false);
                alert.setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                MainActivity.this.removeDialog(Id.dialog.change_log);
                                                mPreferences.setHasSeenChangeLog(
                                                        Apg.getVersion(MainActivity.this), true);
                                            }
                                        });

                return alert.create();
            }

            default: {
                return super.onCreateDialog(id);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, Id.menu.option.create, 0, R.string.menu_addAccount)
                .setIcon(android.R.drawable.ic_menu_add);
        menu.add(1, Id.menu.option.manage_public_keys, 1, R.string.menu_managePublicKeys)
                .setIcon(android.R.drawable.ic_menu_manage);
        menu.add(1, Id.menu.option.manage_secret_keys, 2, R.string.menu_manageSecretKeys)
                .setIcon(android.R.drawable.ic_menu_manage);
        menu.add(2, Id.menu.option.preferences, 3, R.string.menu_preferences)
                .setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(2, Id.menu.option.about, 4, R.string.menu_about)
                .setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Id.menu.option.create: {
                showDialog(Id.dialog.new_account);
                return true;
            }

            case Id.menu.option.manage_public_keys: {
                startActivity(new Intent(this, PublicKeyListActivity.class));
                return true;
            }

            case Id.menu.option.manage_secret_keys: {
                startActivity(new Intent(this, SecretKeyListActivity.class));
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

        TextView nameTextView = (TextView) v.findViewById(R.id.accountName);
        if (nameTextView != null) {
            menu.setHeaderTitle(nameTextView.getText());
            menu.add(0, Id.menu.delete, 0, R.string.menu_deleteAccount);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();

        switch (menuItem.getItemId()) {
            case Id.menu.delete: {
                Apg.getDatabase().db().delete(Accounts.TABLE_NAME,
                                              Accounts._ID + " = ?",
                                              new String[] { "" + info.id });
                mAccountCursor.requery();
                mListAdapter.notifyDataSetChanged();
                return true;
            }

            default: {
                return super.onContextItemSelected(menuItem);
            }
        }
    }


    private static class AccountListAdapter extends CursorAdapter {
        private LayoutInflater minflater;

        public AccountListAdapter(Context context, Cursor cursor) {
            super(context, cursor);
            minflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public Object getItem(int position) {
            Cursor c = getCursor();
            c.moveToPosition(position);
            return c.getString(c.getColumnIndex(Accounts.NAME));
        }

        @Override
        public int getCount() {
            return super.getCount();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return minflater.inflate(R.layout.account_item, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView nameTextView = (TextView) view.findViewById(R.id.accountName);
            int nameIndex = cursor.getColumnIndex(Accounts.NAME);
            final String account = cursor.getString(nameIndex);
            nameTextView.setText(account);
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }
    }
}