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

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Iterator;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.PGPUtil;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.helper.PGPMain;
import org.thialfihar.android.apg.helper.Preferences;
import org.thialfihar.android.apg.service.ApgService;
import org.thialfihar.android.apg.service.ApgServiceHandler;
import org.thialfihar.android.apg.ui.dialog.PassphraseDialogFragment;
import org.thialfihar.android.apg.util.HkpKeyServer;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

import org.thialfihar.android.apg.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * gpg --sign-key
 * 
 * signs the specified public key with the specified secret master key
 */
public class SignKeyActivity extends SherlockFragmentActivity {

    public static final String EXTRA_KEY_ID = "keyId";

    // TODO: remove when using new intentservice:
    public static final String EXTRA_ERROR = "error";

    private long pubKeyId = 0;
    private long masterKeyId = 0;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case android.R.id.home:
            startActivity(new Intent(this, PublicKeyListActivity.class));
            return true;

        default:
            break;

        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check we havent already signed it
        setContentView(R.layout.sign_key_layout);

        final Spinner keyServer = (Spinner) findViewById(R.id.keyServer);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, Preferences.getPreferences(this)
                        .getKeyServers());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keyServer.setAdapter(adapter);

        final CheckBox sendKey = (CheckBox) findViewById(R.id.sendKey);
        if (!sendKey.isChecked()) {
            keyServer.setEnabled(false);
        } else {
            keyServer.setEnabled(true);
        }

        sendKey.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    keyServer.setEnabled(false);
                } else {
                    keyServer.setEnabled(true);
                }
            }
        });

        Button sign = (Button) findViewById(R.id.sign);
        sign.setEnabled(false); // disabled until the user selects a key to sign with
        sign.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (pubKeyId != 0) {
                    initiateSigning();
                }
            }
        });

        pubKeyId = getIntent().getLongExtra(EXTRA_KEY_ID, 0);
        if (pubKeyId == 0) {
            finish(); // nothing to do if we dont know what key to sign
        } else {
            // kick off the SecretKey selection activity so the user chooses which key to sign with
            // first
            Intent intent = new Intent(this, SelectSecretKeyListActivity.class);
            startActivityForResult(intent, Id.request.secret_keys);
        }
    }

    private void showPassphraseDialog(final long secretKeyId) {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    startSigning();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(
                    messenger, secretKeyId);

            passphraseDialog.show(getSupportFragmentManager(), "passphraseDialog");
        } catch (PGPMain.GeneralException e) {
            Log.d(Constants.TAG, "No passphrase for this secret key, encrypt directly!");
            // send message to handler to start encryption directly
            returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
        }
    }

    /**
     * handles the UI bits of the signing process on the UI thread
     */
    private void initiateSigning() {
        PGPPublicKeyRing pubring = PGPMain.getPublicKeyRing(pubKeyId);
        if (pubring != null) {
            // if we have already signed this key, dont bother doing it again
            boolean alreadySigned = false;

            @SuppressWarnings("unchecked")
            Iterator<PGPSignature> itr = pubring.getPublicKey(pubKeyId).getSignatures();
            while (itr.hasNext()) {
                PGPSignature sig = itr.next();
                if (sig.getKeyID() == masterKeyId) {
                    alreadySigned = true;
                    break;
                }
            }

            if (!alreadySigned) {
                /*
                 * get the user's passphrase for this key (if required)
                 */
                String passphrase = PGPMain.getCachedPassPhrase(masterKeyId);
                if (passphrase == null) {
                    showPassphraseDialog(masterKeyId);
                    return; // bail out; need to wait until the user has entered the passphrase
                            // before trying again
                } else {
                    startSigning();
                }
            } else {
                final Bundle status = new Bundle();
                // Message msg = new Message();

                // status.putString(EXTRA_ERROR, "Key has already been signed");

                // status.putInt(Constants.extras.STATUS, Id.message.done);

                // msg.setData(status);
                // sendMessage(msg);

                setResult(Id.return_value.error);
                finish();
            }
        }
    }

    // @Override
    // public long getSecretKeyId() {
    // return masterKeyId;
    // }
    //
    // @Override
    // public void passPhraseCallback(long keyId, String passPhrase) {
    // super.passPhraseCallback(keyId, passPhrase);
    // startSigning();
    // }

    /**
     * kicks off the actual signing process on a background thread
     */
    private void startSigning() {
        // Send all information needed to service to sign key in other thread
        Intent intent = new Intent(this, ApgService.class);

        intent.putExtra(ApgService.EXTRA_ACTION, ApgService.ACTION_SIGN_KEY);

        // fill values for this action
        Bundle data = new Bundle();

        int keyRingId = getIntent().getIntExtra(EXTRA_KEY_ID, -1);
        data.putInt(ApgService.UPLOAD_KEY_KEYRING_ID, keyRingId);

        Spinner keyServer = (Spinner) findViewById(R.id.keyServer);
        String server = (String) keyServer.getSelectedItem();
        data.putString(ApgService.UPLOAD_KEY_SERVER, server);

        intent.putExtra(ApgService.EXTRA_DATA, data);

        // Message is received after signing is done in ApgService
        ApgServiceHandler saveHandler = new ApgServiceHandler(this, R.string.progress_signing,
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == ApgServiceHandler.MESSAGE_OKAY) {

                    Toast.makeText(SignKeyActivity.this, R.string.keySignSuccess,
                            Toast.LENGTH_SHORT).show();

                    // check if we need to send the key to the server or not
                    CheckBox sendKey = (CheckBox) findViewById(R.id.sendKey);
                    if (sendKey.isChecked()) {
                        /*
                         * upload the newly signed key to the key server
                         */
                        uploadKey();
                    } else {
                        finish();
                    }
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

    private void uploadKey() {
        // Send all information needed to service to upload key in other thread
        Intent intent = new Intent(this, ApgService.class);

        intent.putExtra(ApgService.EXTRA_ACTION, ApgService.ACTION_UPLOAD_KEY);

        // fill values for this action
        Bundle data = new Bundle();

        data.putLong(ApgService.UPLOAD_KEY_KEYRING_ID, pubKeyId);

        Spinner keyServer = (Spinner) findViewById(R.id.keyServer);
        String server = (String) keyServer.getSelectedItem();
        data.putString(ApgService.UPLOAD_KEY_SERVER, server);

        intent.putExtra(ApgService.EXTRA_DATA, data);

        // Message is received after uploading is done in ApgService
        ApgServiceHandler saveHandler = new ApgServiceHandler(this, R.string.progress_exporting,
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == ApgServiceHandler.MESSAGE_OKAY) {

                    Toast.makeText(SignKeyActivity.this, R.string.keySendSuccess,
                            Toast.LENGTH_SHORT).show();

                    finish();
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

    // private void startSigning() {
    // showDialog(Id.dialog.signing);
    // startThread();
    // }

    // @Override
    // public void run() {
    // final Bundle status = new Bundle();
    // Message msg = new Message();
    //
    // try {
    // String passphrase = PGPMain.getCachedPassPhrase(masterKeyId);
    // if (passphrase == null || passphrase.length() <= 0) {
    // status.putString(EXTRA_ERROR, "Unable to obtain passphrase");
    // } else {
    // PGPPublicKeyRing pubring = PGPMain.getPublicKeyRing(pubKeyId);
    //
    // /*
    // * sign the incoming key
    // */
    // PGPSecretKey secretKey = PGPMain.getSecretKey(masterKeyId);
    // PGPPrivateKey signingKey = secretKey.extractPrivateKey(passphrase.toCharArray(),
    // BouncyCastleProvider.PROVIDER_NAME);
    // PGPSignatureGenerator sGen = new PGPSignatureGenerator(secretKey.getPublicKey()
    // .getAlgorithm(), PGPUtil.SHA256, BouncyCastleProvider.PROVIDER_NAME);
    // sGen.initSign(PGPSignature.DIRECT_KEY, signingKey);
    //
    // PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
    //
    // PGPSignatureSubpacketVector packetVector = spGen.generate();
    // sGen.setHashedSubpackets(packetVector);
    //
    // PGPPublicKey signedKey = PGPPublicKey.addCertification(
    // pubring.getPublicKey(pubKeyId), sGen.generate());
    // pubring = PGPPublicKeyRing.insertPublicKey(pubring, signedKey);
    //
    // // check if we need to send the key to the server or not
    // CheckBox sendKey = (CheckBox) findViewById(R.id.sendKey);
    // if (sendKey.isChecked()) {
    // Spinner keyServer = (Spinner) findViewById(R.id.keyServer);
    // HkpKeyServer server = new HkpKeyServer((String) keyServer.getSelectedItem());
    //
    // /*
    // * upload the newly signed key to the key server
    // */
    //
    // PGPMain.uploadKeyRingToServer(server, pubring);
    // }
    //
    // // store the signed key in our local cache
    // int retval = PGPMain.storeKeyRingInCache(pubring);
    // if (retval != Id.return_value.ok && retval != Id.return_value.updated) {
    // status.putString(EXTRA_ERROR, "Failed to store signed key in local cache");
    // }
    // }
    // } catch (PGPException e) {
    // Log.e(Constants.TAG, "Failed to sign key", e);
    // status.putString(EXTRA_ERROR, "Failed to sign key");
    // status.putInt(Constants.extras.STATUS, Id.message.done);
    // return;
    // } catch (NoSuchAlgorithmException e) {
    // Log.e(Constants.TAG, "Failed to sign key", e);
    // status.putString(EXTRA_ERROR, "Failed to sign key");
    // status.putInt(Constants.extras.STATUS, Id.message.done);
    // return;
    // } catch (NoSuchProviderException e) {
    // Log.e(Constants.TAG, "Failed to sign key", e);
    // status.putString(EXTRA_ERROR, "Failed to sign key");
    // status.putInt(Constants.extras.STATUS, Id.message.done);
    // return;
    // } catch (SignatureException e) {
    // Log.e(Constants.TAG, "Failed to sign key", e);
    // status.putString(EXTRA_ERROR, "Failed to sign key");
    // status.putInt(Constants.extras.STATUS, Id.message.done);
    // return;
    // }
    //
    // status.putInt(Constants.extras.STATUS, Id.message.done);
    //
    // msg.setData(status);
    // sendMessage(msg);
    //
    // if (status.containsKey(EXTRA_ERROR)) {
    // setResult(Id.return_value.error);
    // } else {
    // setResult(Id.return_value.ok);
    // }
    //
    // finish();
    // }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case Id.request.secret_keys: {
            if (resultCode == RESULT_OK) {
                masterKeyId = data.getLongExtra(EXTRA_KEY_ID, 0);

                // re-enable the sign button so the user can initiate the sign process
                Button sign = (Button) findViewById(R.id.sign);
                sign.setEnabled(true);
            }

            break;
        }

        default: {
            super.onActivityResult(requestCode, resultCode, data);
        }
        }
    }
    //
    // @Override
    // public void doneCallback(Message msg) {
    // super.doneCallback(msg);
    //
    // removeDialog(Id.dialog.signing);
    //
    // Bundle data = msg.getData();
    // String error = data.getString(EXTRA_ERROR);
    // if (error != null) {
    // Toast.makeText(this, getString(R.string.errorMessage, error), Toast.LENGTH_SHORT)
    // .show();
    // return;
    // }
    //
    // Toast.makeText(this, R.string.keySignSuccess, Toast.LENGTH_SHORT).show();
    // finish();
    // }
}
