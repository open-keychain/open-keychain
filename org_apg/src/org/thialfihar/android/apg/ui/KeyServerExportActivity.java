/*
 * Copyright (C) 2011 Senecaso
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

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.helper.PGPMain;
import org.thialfihar.android.apg.util.HkpKeyServer;

import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * gpg --send-key activity
 * 
 * Sends the selected public key to a key server
 */
public class KeyServerExportActivity extends BaseActivity {

    // Not used in sourcode, but listed in AndroidManifest!
    public static final String ACTION_EXPORT_KEY_TO_SERVER = Constants.INTENT_PREFIX
            + "EXPORT_KEY_TO_SERVER";

    public static final String EXTRA_KEY_ID = "keyId";

    // TODO: remove when using new intentservice:
    public static final String EXTRA_ERROR = "error";

    private Button export;
    private Spinner keyServer;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, PublicKeyListActivity.class);
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

        setContentView(R.layout.key_server_export_layout);

        export = (Button) findViewById(R.id.btn_export_to_server);
        keyServer = (Spinner) findViewById(R.id.keyServer);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, mPreferences.getKeyServers());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keyServer.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            keyServer.setSelection(0);
        } else {
            export.setEnabled(false);
        }

        export.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startThread();
            }
        });
    }

    @Override
    public void run() {
        String error = null;
        Bundle data = new Bundle();
        Message msg = new Message();

        HkpKeyServer server = new HkpKeyServer((String) keyServer.getSelectedItem());

        int keyRingId = getIntent().getIntExtra(EXTRA_KEY_ID, -1);

        PGPKeyRing keyring = PGPMain.getKeyRing(keyRingId);
        if (keyring != null && keyring instanceof PGPPublicKeyRing) {
            boolean uploaded = PGPMain.uploadKeyRingToServer(server, (PGPPublicKeyRing) keyring);
            if (!uploaded) {
                error = "Unable to export key to selected server";
            }
        }

        data.putInt(Constants.extras.STATUS, Id.message.export_done);

        if (error != null) {
            data.putString(EXTRA_ERROR, error);
        }

        msg.setData(data);
        sendMessage(msg);
    }

    @Override
    public void doneCallback(Message msg) {
        super.doneCallback(msg);

        Bundle data = msg.getData();
        String error = data.getString(EXTRA_ERROR);
        if (error != null) {
            Toast.makeText(this, getString(R.string.errorMessage, error), Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        Toast.makeText(this, R.string.keySendSuccess, Toast.LENGTH_SHORT).show();
        finish();
    }
}
