/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import java.util.ArrayList;
import java.util.List;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.util.KeyServer.KeyInfo;
import org.sufficientlysecure.keychain.util.Log;

public class KeyServerQueryActivity extends SherlockFragmentActivity {

    // possible intent actions for this activity
    public static final String ACTION_LOOK_UP_KEY_ID = Constants.INTENT_PREFIX + "LOOK_UP_KEY_ID";

    public static final String ACTION_LOOK_UP_KEY_ID_AND_RETURN = Constants.INTENT_PREFIX
            + "LOOK_UP_KEY_ID_AND_RETURN";

    public static final String EXTRA_KEY_ID = "key_id";
    public static final String EXTRA_FINGERPRINT = "fingerprint";

    public static final String RESULT_EXTRA_TEXT = "text";

    private ListView mList;
    private EditText mQuery;
    private Button mSearch;
    private Spinner mKeyServer;

    private KeyInfoListAdapter mAdapter;

    private int mQueryType;
    private String mQueryString;
    private long mQueryId;

    private volatile List<KeyInfo> mSearchResult;

    private volatile String mKeyData;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, KeyListPublicActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

        default:
            break;

        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.key_server_query);

        mQuery = (EditText) findViewById(R.id.query);
        mSearch = (Button) findViewById(R.id.btn_search);
        mList = (ListView) findViewById(R.id.list);
        mAdapter = new KeyInfoListAdapter(this);
        mList.setAdapter(mAdapter);

        mKeyServer = (Spinner) findViewById(R.id.sign_key_keyserver);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, Preferences.getPreferences(this)
                        .getKeyServers());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mKeyServer.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            mKeyServer.setSelection(0);
        } else {
            mSearch.setEnabled(false);
        }

        mList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long keyId) {
                get(keyId);
            }
        });

        mSearch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = mQuery.getText().toString();
                search(query);
            }
        });
        mQuery.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = mQuery.getText().toString();
                    search(query);
                    return false; // FIXME This is a hack to hide a keyboard
                                  // after search http://tinyurl.com/pwdc3q9

                }
                return false;
            }
        });

        Intent intent = getIntent();
        String action = intent.getAction();
        if (ACTION_LOOK_UP_KEY_ID.equals(action) || ACTION_LOOK_UP_KEY_ID_AND_RETURN.equals(action)) {
            long keyId = intent.getLongExtra(EXTRA_KEY_ID, 0);
            if (keyId != 0) {
                String query = "0x" + PgpKeyHelper.convertKeyToHex(keyId);
                mQuery.setText(query);
                search(query);
            }
            String fingerprint = intent.getStringExtra(EXTRA_FINGERPRINT);
            if (fingerprint != null) {
                fingerprint = "0x" + fingerprint;
                mQuery.setText(fingerprint);
                search(fingerprint);
            }
        }
    }

    private void search(String query) {
        mQueryType = Id.keyserver.search;
        mQueryString = query;
        mAdapter.setKeys(new ArrayList<KeyInfo>());

        start();
    }

    private void get(long keyId) {
        mQueryType = Id.keyserver.get;
        mQueryId = keyId;

        start();
    }

    private void start() {
        Log.d(Constants.TAG, "start search with service");

        // Send all information needed to service to query keys in other thread
        Intent intent = new Intent(this, KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_QUERY_KEYRING);

        // fill values for this action
        Bundle data = new Bundle();

        String server = (String) mKeyServer.getSelectedItem();
        data.putString(KeychainIntentService.QUERY_KEY_SERVER, server);

        data.putInt(KeychainIntentService.QUERY_KEY_TYPE, mQueryType);

        if (mQueryType == Id.keyserver.search) {
            data.putString(KeychainIntentService.QUERY_KEY_STRING, mQueryString);
        } else if (mQueryType == Id.keyserver.get) {
            data.putLong(KeychainIntentService.QUERY_KEY_ID, mQueryId);
        }

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after querying is done in ApgService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(this,
                R.string.progress_querying, ProgressDialog.STYLE_SPINNER) {
            @Override
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    if (mQueryType == Id.keyserver.search) {
                        mSearchResult = returnData
                                .getParcelableArrayList(KeychainIntentService.RESULT_QUERY_KEY_SEARCH_RESULT);
                    } else if (mQueryType == Id.keyserver.get) {
                        mKeyData = returnData
                                .getString(KeychainIntentService.RESULT_QUERY_KEY_DATA);
                    }

                    // TODO: IMPROVE CODE!!! some global variables can be
                    // avoided!!!
                    if (mQueryType == Id.keyserver.search) {
                        if (mSearchResult != null) {
                            Toast.makeText(KeyServerQueryActivity.this,
                                    getString(R.string.keys_found, mSearchResult.size()),
                                    Toast.LENGTH_SHORT).show();
                            mAdapter.setKeys(mSearchResult);
                        }
                    } else if (mQueryType == Id.keyserver.get) {
                        Intent orgIntent = getIntent();
                        if (ACTION_LOOK_UP_KEY_ID_AND_RETURN.equals(orgIntent.getAction())) {
                            if (mKeyData != null) {
                                Intent intent = new Intent();
                                intent.putExtra(RESULT_EXTRA_TEXT, mKeyData);
                                setResult(RESULT_OK, intent);
                            } else {
                                setResult(RESULT_CANCELED);
                            }
                            finish();
                        } else {
                            if (mKeyData != null) {
                                Intent intent = new Intent(KeyServerQueryActivity.this,
                                        ImportKeysActivity.class);
                                intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY);
                                intent.putExtra(ImportKeysActivity.EXTRA_KEY_BYTES,
                                        mKeyData.getBytes());
                                startActivity(intent);
                            }
                        }
                    }

                }
            };
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);
    }

    public class KeyInfoListAdapter extends BaseAdapter {
        protected LayoutInflater mInflater;

        protected Activity mActivity;

        protected List<KeyInfo> mKeys;

        public KeyInfoListAdapter(Activity activity) {
            mActivity = activity;
            mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mKeys = new ArrayList<KeyInfo>();
        }

        public void setKeys(List<KeyInfo> keys) {
            mKeys = keys;
            notifyDataSetChanged();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public int getCount() {
            return mKeys.size();
        }

        @Override
        public Object getItem(int position) {
            return mKeys.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mKeys.get(position).keyId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            KeyInfo keyInfo = mKeys.get(position);

            View view = mInflater.inflate(R.layout.key_server_query_result_item, null);

            TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
            mainUserId.setText(R.string.unknown_user_id);
            TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
            mainUserIdRest.setText("");
            TextView keyId = (TextView) view.findViewById(R.id.keyId);
            keyId.setText(R.string.no_key);
            TextView algorithm = (TextView) view.findViewById(R.id.algorithm);
            algorithm.setText("");
            TextView status = (TextView) view.findViewById(R.id.status);
            status.setText("");

            String userId = keyInfo.userIds.get(0);
            if (userId != null) {
                String chunks[] = userId.split(" <", 2);
                userId = chunks[0];
                if (chunks.length > 1) {
                    mainUserIdRest.setText("<" + chunks[1]);
                }
                mainUserId.setText(userId);
            }

            keyId.setText(PgpKeyHelper.convertKeyIdToHex(keyInfo.keyId));

            if (mainUserIdRest.getText().length() == 0) {
                mainUserIdRest.setVisibility(View.GONE);
            }

            algorithm.setText("" + keyInfo.size + "/" + keyInfo.algorithm);

            if (keyInfo.revoked != null) {
                status.setText("revoked");
            } else {
                status.setVisibility(View.GONE);
            }

            LinearLayout ll = (LinearLayout) view.findViewById(R.id.list);
            if (keyInfo.userIds.size() == 1) {
                ll.setVisibility(View.GONE);
            } else {
                boolean first = true;
                boolean second = true;
                for (String uid : keyInfo.userIds) {
                    if (first) {
                        first = false;
                        continue;
                    }
                    if (!second) {
                        View sep = new View(mActivity);
                        sep.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 1));
                        sep.setBackgroundResource(android.R.drawable.divider_horizontal_dark);
                        ll.addView(sep);
                    }
                    TextView uidView = (TextView) mInflater.inflate(
                            R.layout.key_server_query_result_user_id, null);
                    uidView.setText(uid);
                    ll.addView(uidView);
                    second = false;
                }
            }

            return view;
        }
    }
}
