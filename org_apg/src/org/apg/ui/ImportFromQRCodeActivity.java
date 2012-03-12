/*
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

package org.apg.ui;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apg.Apg;
import org.apg.Constants;
import org.apg.HkpKeyServer;
import org.apg.Id;
import org.apg.KeyServer.QueryException;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.apg.R;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ImportFromQRCodeActivity extends BaseActivity {
    private static final String TAG = "ImportFromQRCodeActivity";

    private final Bundle status = new Bundle();
    private final Message msg = new Message();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new IntentIntegrator(this).initiateScan();
    }

    private void importAndSign(final long keyId, final String expectedFingerprint) {
        if (expectedFingerprint != null && expectedFingerprint.length() > 0) {
            
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        // TODO: display some sort of spinner here while the user waits
                        
                        HkpKeyServer server = new HkpKeyServer(mPreferences.getKeyServers()[0]); // TODO: there should be only 1
                        String encodedKey = server.get(keyId);

                        PGPKeyRing keyring = Apg.decodeKeyRing(new ByteArrayInputStream(encodedKey.getBytes()));
                        if (keyring != null && keyring instanceof PGPPublicKeyRing) {
                            PGPPublicKeyRing publicKeyRing = (PGPPublicKeyRing) keyring;

                            // make sure the fingerprints match before we cache this thing
                            String actualFingerprint = Apg.convertToHex(publicKeyRing.getPublicKey().getFingerprint());
                            if (expectedFingerprint.equals(actualFingerprint)) {
                                // store the signed key in our local cache
                                int retval = Apg.storeKeyRingInCache(publicKeyRing);
                                if (retval != Id.return_value.ok && retval != Id.return_value.updated) {
                                    status.putString(Apg.EXTRA_ERROR, "Failed to store signed key in local cache");
                                } else {
                                    Intent intent = new Intent(ImportFromQRCodeActivity.this, SignKeyActivity.class);
                                    intent.putExtra(Apg.EXTRA_KEY_ID, keyId);
                                    startActivityForResult(intent, Id.request.sign_key);
                                }
                            } else {
                                status.putString(Apg.EXTRA_ERROR, "Scanned fingerprint does NOT match the fingerprint of the received key.  You shouldnt trust this key.");
                            }
                        }
                    } catch (QueryException e) {
                        Log.e(TAG, "Failed to query KeyServer", e);
                        status.putString(Apg.EXTRA_ERROR, "Failed to query KeyServer");
                        status.putInt(Constants.extras.STATUS, Id.message.done);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to query KeyServer", e);
                        status.putString(Apg.EXTRA_ERROR, "Failed to query KeyServer");
                        status.putInt(Constants.extras.STATUS, Id.message.done);
                    }
                }
            };

            t.setName("KeyExchange Download Thread");
            t.setDaemon(true);
            t.start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case IntentIntegrator.REQUEST_CODE: {
                boolean debug = true; // TODO: remove this!!!
                IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                if (debug || (scanResult != null && scanResult.getFormatName() != null)) {
                    String[] bits = debug ? new String[] { "5993515643896327656", "0816 F68A 6816 68FB 01BF  2CA5 532D 3EB9 1E2F EDE8" } : scanResult.getContents().split(",");
                    if (bits.length != 2) {
                        return; // dont know how to handle this.  Not a valid code
                    }

                    long keyId = Long.parseLong(bits[0]);
                    String expectedFingerprint = bits[1];
                    
                    importAndSign(keyId, expectedFingerprint);
                    
                    break;
                }
            }
            
            case Id.request.sign_key: {
                // signals the end of processing.  Signature was either applied, or it wasnt
                status.putInt(Constants.extras.STATUS, Id.message.done);
                
                msg.setData(status);
                sendMessage(msg);
                
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
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

        Toast.makeText(this, R.string.keySignSuccess, Toast.LENGTH_SHORT).show(); // TODO
        finish();
    }
}
