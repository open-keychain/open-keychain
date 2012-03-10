package org.apg.ui;

import org.apg.Apg;
import org.apg.Constants;
import org.apg.HkpKeyServer;
import org.apg.Id;
import org.apg.Constants.extras;
import org.apg.Id.message;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.apg.R;

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
public class SendKeyActivity extends BaseActivity {

    private Button export;
    private Spinner keyServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.key_server_export_layout);

        export = (Button) findViewById(R.id.btn_export_to_server);
        keyServer = (Spinner) findViewById(R.id.keyServer);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mPreferences.getKeyServers());
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

        int keyRingId = getIntent().getIntExtra(Apg.EXTRA_KEY_ID, -1);

        PGPKeyRing keyring = Apg.getKeyRing(keyRingId);
        if (keyring != null && keyring instanceof PGPPublicKeyRing) {
            boolean uploaded = Apg.uploadKeyRingToServer(server, (PGPPublicKeyRing) keyring);
            if (!uploaded) {
                error = "Unable to export key to selected server";
            }
        }

        data.putInt(Constants.extras.STATUS, Id.message.export_done);

        if (error != null) {
            data.putString(Apg.EXTRA_ERROR, error);
        }

        msg.setData(data);
        sendMessage(msg);
    }

    @Override
    public void doneCallback(Message msg) {
        super.doneCallback(msg);

        Bundle data = msg.getData();
        String error = data.getString(Apg.EXTRA_ERROR);
        if (error != null) {
            Toast.makeText(this, getString(R.string.errorMessage, error), Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.keySendSuccess, Toast.LENGTH_SHORT).show();
        finish();
    }
}
