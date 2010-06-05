package org.thialfihar.android.apg;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Vector;

import org.thialfihar.android.apg.utils.Choice;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class GeneralActivity extends BaseActivity {
    private Intent mIntent;
    private ArrayAdapter<Choice> mAdapter;
    private ListView mList;
    private Button mCancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.general);

        mIntent = getIntent();

        boolean isEncrypted = false;
        boolean containsKeys = false;

        InputStream inStream = null;
        {
            byte[] data = mIntent.getByteArrayExtra(Intent.EXTRA_TEXT);
            if (data != null) {
                inStream = new ByteArrayInputStream(data);
            }
        }

        if (inStream == null) {
            Uri data = mIntent.getData();
            if (data != null) {
                try {
                    inStream = getContentResolver().openInputStream(data);
                } catch (FileNotFoundException e) {
                    // didn't work
                    Toast.makeText(this, "failed to open stream", Toast.LENGTH_SHORT);
                }
            }
        }

        if (inStream == null) {
            Toast.makeText(this, "no data found", Toast.LENGTH_SHORT);
            finish();
            return;
        }

        mList = (ListView) findViewById(R.id.options);
        Vector<Choice> choices = new Vector<Choice>();

        if (containsKeys) {
            choices.add(new Choice(Id.choice.action.import_public, getString(R.string.action_import_public)));
            choices.add(new Choice(Id.choice.action.import_secret, getString(R.string.action_import_secret)));
        }

        if (isEncrypted) {
            choices.add(new Choice(Id.choice.action.decrypt, getString(R.string.action_decrypt)));
        }

        if (!containsKeys && !isEncrypted) {
            choices.add(new Choice(Id.choice.action.encrypt, getString(R.string.action_encrypt)));
        }

        mAdapter = new ArrayAdapter<Choice>(this, android.R.layout.simple_list_item_1, choices);
        mList.setAdapter(mAdapter);

        mList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                clicked(mAdapter.getItem(arg2).getId());
            }
        });

        mCancelButton = (Button) findViewById(R.id.btn_cancel);
        mCancelButton.setOnClickListener(new OnClickListener() {

            @Override
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
                if (mIntent.hasExtra(Intent.EXTRA_TEXT)) {
                    intent.putExtra(Intent.EXTRA_TEXT, mIntent.getByteArrayExtra(Intent.EXTRA_TEXT));
                } else if (mIntent.getData() != null) {
                    intent.setData(mIntent.getData());
                    intent.setType(mIntent.getType());
                }

                break;
            }

            case Id.choice.action.decrypt: {
                break;
            }

            case Id.choice.action.import_public: {
                break;
            }

            case Id.choice.action.import_secret: {
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
