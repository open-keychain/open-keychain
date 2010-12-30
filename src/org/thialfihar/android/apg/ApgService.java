package org.thialfihar.android.apg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ApgService extends Service {
    static String TAG = "ApgService";

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "bound");
        return mBinder;
    }

    private final IApgService.Stub mBinder = new IApgService.Stub() {

        public String encrypt_with_passphrase(String msg, String passphrase) {
            Preferences mPreferences = Preferences.getPreferences(getBaseContext(), true);
            InputStream inStream = new ByteArrayInputStream(msg.getBytes());
            InputData in = new InputData(inStream, 9999);
            OutputStream out = new ByteArrayOutputStream();
            long enc_keys[] = {};

            Apg.initialize(getApplicationContext());
            try {
                Apg.encrypt(
                        getApplicationContext(),
                        in,
                        out,
                        true, // armored
                        enc_keys, // encryption keys
                        0, // signature key
                        null, // signature passphrase
                        null, // progress
                        mPreferences.getDefaultEncryptionAlgorithm(),
                        mPreferences.getDefaultHashAlgorithm(),
                        Id.choice.compression.none, // compression
                        false, // mPreferences.getForceV3Signatures(),
                        passphrase // passPhrase
                        );
            } catch (Exception e) {
                Log.d(TAG, "Exception in encrypt");
                e.printStackTrace();
                return null;
            }
            Log.d(TAG, "Encrypted");
            return out.toString();
        }

        public String decrypt_with_passphrase(String encrypted_msg,
                String passphrase) {
            InputStream inStream = new ByteArrayInputStream(encrypted_msg
                    .getBytes());
            InputData in = new InputData(inStream, 9999); // XXX what size in
                                                          // second parameter?
            OutputStream out = new ByteArrayOutputStream();
            try {
                Apg.decrypt(getApplicationContext(), in, out, passphrase, null, // progress
                        true // symmetric
                        );
            } catch (Exception e) {
                Log.d(TAG, "Exception in decrypt");
                e.printStackTrace();
                return null;
            }

            return out.toString();
        }
    };
}
