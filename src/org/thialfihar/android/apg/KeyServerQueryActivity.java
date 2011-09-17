package org.thialfihar.android.apg;

import java.util.List;
import java.util.Vector;

import org.thialfihar.android.apg.KeyServer.InsufficientQuery;
import org.thialfihar.android.apg.KeyServer.KeyInfo;
import org.thialfihar.android.apg.KeyServer.QueryException;
import org.thialfihar.android.apg.KeyServer.TooManyResponses;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
import android.widget.Toast;

public class KeyServerQueryActivity extends BaseActivity {
    private ListView mList;
    private EditText mQuery;
    private Button mSearch;
    private KeyInfoListAdapter mAdapter;
    private Spinner mKeyServer;

    private int mQueryType;
    private String mQueryString;
    private long mQueryId;
    private volatile List<KeyInfo> mSearchResult;
    private volatile String mKeyData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.key_server_query_layout);

        mQuery = (EditText) findViewById(R.id.query);
        mSearch = (Button) findViewById(R.id.btn_search);
        mList = (ListView) findViewById(R.id.list);
        mAdapter = new KeyInfoListAdapter(this);
        mList.setAdapter(mAdapter);

        mKeyServer = (Spinner) findViewById(R.id.keyServer);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this,
                                         android.R.layout.simple_spinner_item,
                                         mPreferences.getKeyServers());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mKeyServer.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            mKeyServer.setSelection(0);
        } else {
            mSearch.setEnabled(false);
        }

        mList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapter, View view, int position, long keyId) {
                get(keyId);
            }
        });

        mSearch.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String query = mQuery.getText().toString();
                search(query);
            }
        });

        Intent intent = getIntent();
        if (Apg.Intent.LOOK_UP_KEY_ID.equals(intent.getAction()) ||
            Apg.Intent.LOOK_UP_KEY_ID_AND_RETURN.equals(intent.getAction())) {
            long keyId = intent.getLongExtra(Apg.EXTRA_KEY_ID, 0);
            if (keyId != 0) {
                String query = "0x" + Apg.keyToHex(keyId);
                mQuery.setText(query);
                search(query);
            }
        }
    }

    private void search(String query) {
        showDialog(Id.dialog.querying);
        mQueryType = Id.query.search;
        mQueryString = query;
        mAdapter.setKeys(new Vector<KeyInfo>());
        startThread();
    }

    private void get(long keyId) {
        showDialog(Id.dialog.querying);
        mQueryType = Id.query.get;
        mQueryId = keyId;
        startThread();
    }

    @Override
	protected Dialog onCreateDialog(int id) {
        ProgressDialog progress = (ProgressDialog) super.onCreateDialog(id);
        progress.setMessage(this.getString(R.string.progress_queryingServer,
                                           (String)mKeyServer.getSelectedItem()));
        return progress;
    }

    @Override
    public void run() {
        String error = null;
        Bundle data = new Bundle();
        Message msg = new Message();

        try {
            HkpKeyServer server = new HkpKeyServer((String)mKeyServer.getSelectedItem());
            if (mQueryType == Id.query.search) {
                mSearchResult = server.search(mQueryString);
            } else if (mQueryType == Id.query.get) {
                mKeyData = server.get(mQueryId);
            }
        } catch (QueryException e) {
            error = "" + e;
        } catch (InsufficientQuery e) {
            error = "Insufficient query.";
        } catch (TooManyResponses e) {
            error = "Too many responses.";
        }

        data.putInt(Constants.extras.status, Id.message.done);

        if (error != null) {
            data.putString(Apg.EXTRA_ERROR, error);
        }

        msg.setData(data);
        sendMessage(msg);
    }

    @Override
    public void doneCallback(Message msg) {
        super.doneCallback(msg);

        removeDialog(Id.dialog.querying);

        Bundle data = msg.getData();
        String error = data.getString(Apg.EXTRA_ERROR);
        if (error != null) {
            Toast.makeText(this, getString(R.string.errorMessage, error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (mQueryType == Id.query.search) {
            if (mSearchResult != null) {
                Toast.makeText(this, getString(R.string.keysFound, mSearchResult.size()), Toast.LENGTH_SHORT).show();
                mAdapter.setKeys(mSearchResult);
            }
        } else if (mQueryType == Id.query.get) {
            Intent orgIntent = getIntent();
            if (Apg.Intent.LOOK_UP_KEY_ID_AND_RETURN.equals(orgIntent.getAction())) {
                if (mKeyData != null) {
                    Intent intent = new Intent();
                    intent.putExtra(Apg.EXTRA_TEXT, mKeyData);
                    setResult(RESULT_OK, intent);
                } else {
                    setResult(RESULT_CANCELED);
                }
                finish();
            } else {
                if (mKeyData != null) {
                    Intent intent = new Intent(this, PublicKeyListActivity.class);
                    intent.setAction(Apg.Intent.IMPORT);
                    intent.putExtra(Apg.EXTRA_TEXT, mKeyData);
                    startActivity(intent);
                }
            }
        }
    }

    public class KeyInfoListAdapter extends BaseAdapter {
        protected LayoutInflater mInflater;
        protected Activity mActivity;
        protected List<KeyInfo> mKeys;

        public KeyInfoListAdapter(Activity activity) {
            mActivity = activity;
            mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mKeys = new Vector<KeyInfo>();
        }

        public void setKeys(List<KeyInfo> keys) {
            mKeys = keys;
            notifyDataSetChanged();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public int getCount() {
            return mKeys.size();
        }

        public Object getItem(int position) {
            return mKeys.get(position);
        }

        public long getItemId(int position) {
            return mKeys.get(position).keyId;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            KeyInfo keyInfo = mKeys.get(position);

            View view = mInflater.inflate(R.layout.key_server_query_result_item, null);

            TextView mainUserId = (TextView) view.findViewById(R.id.mainUserId);
            mainUserId.setText(R.string.unknownUserId);
            TextView mainUserIdRest = (TextView) view.findViewById(R.id.mainUserIdRest);
            mainUserIdRest.setText("");
            TextView keyId = (TextView) view.findViewById(R.id.keyId);
            keyId.setText(R.string.noKey);
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

            keyId.setText(Apg.getSmallFingerPrint(keyInfo.keyId));

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
                        sep.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 1));
                        sep.setBackgroundResource(android.R.drawable.divider_horizontal_dark);
                        ll.addView(sep);
                    }
                    TextView uidView = (TextView) mInflater.inflate(R.layout.key_server_query_result_user_id, null);
                    uidView.setText(uid);
                    ll.addView(uidView);
                    second = false;
                }
            }

            return view;
        }
    }
}
