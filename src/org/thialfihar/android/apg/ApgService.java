package org.thialfihar.android.apg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class ApgService extends Service {
    static String TAG = "ApgService";

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "bound");
        return mBinder;
    }

    private enum error {
        ARGUMENTS_MISSING,
        APG_FAILURE
    }

    private final IApgService.Stub mBinder = new IApgService.Stub() {

        public boolean encrypt_with_passphrase(Bundle pArgs, Bundle pReturn) {
            ArrayList<String> errors = new ArrayList<String>();
            ArrayList<String> warnings = new ArrayList<String>();

            pReturn.putStringArrayList("ERRORS", errors);
            pReturn.putStringArrayList("WARNINGS", warnings);

            String msg = pArgs.getString("MSG");
            pArgs.remove("MSG");

            String passphrase = pArgs.getString("SYM_KEY");
            pArgs.remove("SYM_KEY");

            if (msg == null) {
                errors.add("Message to encrypt (MSG) missing");
            }

            if (passphrase == null) {
                errors.add("Symmetric key (SYM_KEY) missing");
            }

            if (!pArgs.isEmpty()) {
                Iterator<String> _iter = pArgs.keySet().iterator();
                while (_iter.hasNext()) {
                    warnings.add("Unknown key: " + _iter.next());
                }
            }

            if (errors.size() != 0) {
                pReturn.putInt("ERROR", error.ARGUMENTS_MISSING.ordinal());
                return false;
            }

            Preferences _mPreferences = Preferences.getPreferences(getBaseContext(), true);
            InputStream _inStream = new ByteArrayInputStream(msg.getBytes());
            InputData _in = new InputData(_inStream, 9999);
            OutputStream _out = new ByteArrayOutputStream();
            long _enc_keys[] = {};

            Apg.initialize(getApplicationContext());
            try {
                Apg.encrypt(getApplicationContext(), // context
                        _in, // input stream
                        _out, // output stream
                        true, // armored
                        _enc_keys, // encryption keys
                        0, // signature key
                        null, // signature passphrase
                        null, // progress
                        _mPreferences.getDefaultEncryptionAlgorithm(), // encryption
                        _mPreferences.getDefaultHashAlgorithm(), // hash
                        Id.choice.compression.none, // compression
                        false, // mPreferences.getForceV3Signatures(),
                        passphrase // passPhrase
                        );
            } catch (Exception e) {
                Log.d(TAG, "Exception in encrypt");
                errors.add("Internal failure in APG when encrypting: " + e.getMessage());

                pReturn.putInt("ERROR", error.APG_FAILURE.ordinal());
                return false;
            }

            Log.d(TAG, "Encrypted");
            pReturn.putString("RESULT", _out.toString());
            return true;
        }

        public boolean decrypt_with_passphrase(Bundle pArgs, Bundle pReturn) {
            ArrayList<String> errors = new ArrayList<String>();
            ArrayList<String> warnings = new ArrayList<String>();

            pReturn.putStringArrayList("ERRORS", errors);
            pReturn.putStringArrayList("WARNINGS", warnings);

            String encrypted_msg = pArgs.getString("MSG");
            pArgs.remove("MSG");

            String passphrase = pArgs.getString("SYM_KEY");
            pArgs.remove("SYM_KEY");

            if (encrypted_msg == null) {
                errors.add("Message to decrypt (MSG) missing");
            }

            if (passphrase == null) {
                errors.add("Symmetric key (SYM_KEY) missing");
            }

            if (!pArgs.isEmpty()) {
                Iterator<String> iter = pArgs.keySet().iterator();
                while (iter.hasNext()) {
                    warnings.add("Unknown key: " + iter.next());
                }
            }

            if (errors.size() != 0) {
                pReturn.putStringArrayList("ERROR", errors);
                pReturn.putInt("ERROR", error.ARGUMENTS_MISSING.ordinal());
                return false;
            }

            InputStream inStream = new ByteArrayInputStream(encrypted_msg.getBytes());
            InputData in = new InputData(inStream, 9999); // XXX what size in
            // second parameter?
            OutputStream out = new ByteArrayOutputStream();
            try {
                Apg.decrypt(getApplicationContext(), in, out, passphrase, null, // progress
                        true // symmetric
                        );
            } catch (Exception e) {
                Log.d(TAG, "Exception in decrypt");
                errors.add("Internal failure in APG when decrypting: " + e.getMessage());

                pReturn.putInt("ERROR", error.APG_FAILURE.ordinal());
                pReturn.putStringArrayList("ERROR", errors);
                return false;
            }

            pReturn.putString("RESULT", out.toString());
            return true;
        }
    };
}
