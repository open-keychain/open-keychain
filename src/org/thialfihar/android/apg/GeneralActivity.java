package org.thialfihar.android.apg;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import org.thialfihar.android.apg.utils.Choice;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class GeneralActivity extends BaseActivity {
    private Intent mIntent;
    private ArrayAdapter<Choice> mAdapter;
    private ListView mList;
    private Button mCancelButton;
    private String mDataString;
    private Uri mDataUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.general);

        mIntent = getIntent();

        InputStream inStream = null;
        {
            String data = mIntent.getStringExtra(Intent.EXTRA_TEXT);
            if (data != null) {
                mDataString = data;
                inStream = new ByteArrayInputStream(data.getBytes());
            }
        }

        if (inStream == null) {
            Uri data = mIntent.getData();
            if (data != null) {
                mDataUri = data;
                try {
                    inStream = getContentResolver().openInputStream(data);
                } catch (FileNotFoundException e) {
                    // didn't work
                    Toast.makeText(this, "failed to open stream", Toast.LENGTH_SHORT).show();
                }
            }
        }

        if (inStream == null) {
            Toast.makeText(this, "no data found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        int contentType = Id.content.unknown;
        try {
            contentType = Apg.getStreamContent(this, inStream);
            inStream.close();
        } catch (IOException e) {
            // just means that there's no PGP data in there
        }

        mList = (ListView) findViewById(R.id.options);
        Vector<Choice> choices = new Vector<Choice>();

        if (contentType == Id.content.keys) {
            choices.add(new Choice(Id.choice.action.import_public,
                                   getString(R.string.action_importPublic)));
            choices.add(new Choice(Id.choice.action.import_secret,
                                   getString(R.string.action_importSecret)));
        }

        if (contentType == Id.content.encrypted_data) {
            choices.add(new Choice(Id.choice.action.decrypt, getString(R.string.action_decrypt)));
        }

        if (contentType == Id.content.unknown) {
            choices.add(new Choice(Id.choice.action.encrypt, getString(R.string.action_encrypt)));
        }

        mAdapter = new ArrayAdapter<Choice>(this, android.R.layout.simple_list_item_1, choices);
        mList.setAdapter(mAdapter);

        mList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                clicked(mAdapter.getItem(arg2).getId());
            }
        });

        mCancelButton = (Button) findViewById(R.id.btn_cancel);
        mCancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                GeneralActivity.this.finish();
            }
        });

        if (choices.size() == 1) {
            clicked(choices.get(0).getId());
        }
    }

    private void clicked(int id) {
        Intent intent = new Intent();
        switch (id) {
            case Id.choice.action.encrypt: {
                intent.setClass(this, EncryptActivity.class);
                if (mDataString != null) {
                    intent.setAction(Apg.Intent.ENCRYPT);
                    intent.putExtra(Apg.EXTRA_TEXT, mDataString);
                } else if (mDataUri != null) {
                    intent.setAction(Apg.Intent.ENCRYPT_FILE);
                    intent.setDataAndType(mDataUri, mIntent.getType());
                }

                break;
            }

            case Id.choice.action.decrypt: {
                intent.setClass(this, DecryptActivity.class);
                if (mDataString != null) {
                    intent.setAction(Apg.Intent.DECRYPT);
                    intent.putExtra(Apg.EXTRA_TEXT, mDataString);
                } else if (mDataUri != null) {
                    intent.setAction(Apg.Intent.DECRYPT_FILE);
                    intent.setDataAndType(mDataUri, mIntent.getType());
                }

                break;
            }

            case Id.choice.action.import_public: {
                intent.setClass(this, PublicKeyListActivity.class);
                intent.setAction(Apg.Intent.IMPORT);
                if (mDataString != null) {
                    intent.putExtra(Apg.EXTRA_TEXT, mDataString);
                } else if (mDataUri != null) {
                    intent.setDataAndType(mDataUri, mIntent.getType());
                }
                break;
            }

            case Id.choice.action.import_secret: {
                intent.setClass(this, SecretKeyListActivity.class);
                intent.setAction(Apg.Intent.IMPORT);
                if (mDataString != null) {
                    intent.putExtra(Apg.EXTRA_TEXT, mDataString);
                } else if (mDataUri != null) {
                    intent.setDataAndType(mDataUri, mIntent.getType());
                }
                break;
            }

            default: {
                // shouldn't happen
                return;
            }
        }

        startActivity(intent);
        finish();
    }
}
