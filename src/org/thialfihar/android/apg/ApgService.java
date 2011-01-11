package org.thialfihar.android.apg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class ApgService extends Service {
    final static String TAG = "ApgService";

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "bound");
        return mBinder;
    }

    private enum error {
        ARGUMENTS_MISSING,
        APG_FAILURE
    }

    /** a map from ApgService parameters to function calls to get the default */
    static final HashMap<String, String> FUNCTIONS_DEFAULTS = new HashMap<String, String>();
    static {
        FUNCTIONS_DEFAULTS.put("ENCRYPTION_ALGO", "getDefaultEncryptionAlgorithm");
        FUNCTIONS_DEFAULTS.put("HASH_ALGO", "getDefaultHashAlgorithm");
        FUNCTIONS_DEFAULTS.put("ARMORED", "getDefaultAsciiArmour");
        FUNCTIONS_DEFAULTS.put("FORCE_V3_SIG", "getForceV3Signatures");
        FUNCTIONS_DEFAULTS.put("COMPRESSION", "getDefaultMessageCompression");
    }

    /** a map the default functions to their return types */
    static final HashMap<String, Class> FUNCTIONS_DEFAULTS_TYPES = new HashMap<String, Class>();
    static {
        try {
            FUNCTIONS_DEFAULTS_TYPES.put("getDefaultEncryptionAlgorithm", Preferences.class.getMethod("getDefaultEncryptionAlgorithm").getReturnType());
            FUNCTIONS_DEFAULTS_TYPES.put("getDefaultHashAlgorithm", Preferences.class.getMethod("getDefaultHashAlgorithm").getReturnType());
            FUNCTIONS_DEFAULTS_TYPES.put("getDefaultAsciiArmour", Preferences.class.getMethod("getDefaultAsciiArmour").getReturnType());
            FUNCTIONS_DEFAULTS_TYPES.put("getForceV3Signatures", Preferences.class.getMethod("getForceV3Signatures").getReturnType());
            FUNCTIONS_DEFAULTS_TYPES.put("getDefaultMessageCompression", Preferences.class.getMethod("getDefaultMessageCompression").getReturnType());
        } catch (Exception e) {
            Log.e(TAG, "Function default exception: " + e.getMessage());
        }
    }

    /** a map the default function names to their method */
    static final HashMap<String, Method> FUNCTIONS_DEFAULTS_METHODS = new HashMap<String, Method>();
    static {
        try {
            FUNCTIONS_DEFAULTS_METHODS.put("getDefaultEncryptionAlgorithm", Preferences.class.getMethod("getDefaultEncryptionAlgorithm"));
            FUNCTIONS_DEFAULTS_METHODS.put("getDefaultHashAlgorithm", Preferences.class.getMethod("getDefaultHashAlgorithm"));
            FUNCTIONS_DEFAULTS_METHODS.put("getDefaultAsciiArmour", Preferences.class.getMethod("getDefaultAsciiArmour"));
            FUNCTIONS_DEFAULTS_METHODS.put("getForceV3Signatures", Preferences.class.getMethod("getForceV3Signatures"));
            FUNCTIONS_DEFAULTS_METHODS.put("getDefaultMessageCompression", Preferences.class.getMethod("getDefaultMessageCompression"));
        } catch (Exception e) {
            Log.e(TAG, "Function method exception: " + e.getMessage());
        }
    }

    /**
     * Add default arguments if missing
     * 
     * @param args
     *            the bundle to add default parameters to if missing
     * 
     */
    private void add_defaults(Bundle args) {
        Preferences _mPreferences = Preferences.getPreferences(getBaseContext(), true);

        Iterator<String> _iter = FUNCTIONS_DEFAULTS.keySet().iterator();
        while (_iter.hasNext()) {
            String _current_key = _iter.next();
            if (!args.containsKey(_current_key)) {
                String _current_function_name = FUNCTIONS_DEFAULTS.get(_current_key);
                try {
                    @SuppressWarnings("unchecked")
                    Class _ret_type = FUNCTIONS_DEFAULTS_TYPES.get(_current_function_name);
                    if (_ret_type == String.class) {
                        args.putString(_current_key, (String) FUNCTIONS_DEFAULTS_METHODS.get(_current_function_name).invoke(_mPreferences));
                    } else if (_ret_type == boolean.class) {
                        args.putBoolean(_current_key, (Boolean) FUNCTIONS_DEFAULTS_METHODS.get(_current_function_name).invoke(_mPreferences));
                    } else if (_ret_type == int.class) {
                        args.putInt(_current_key, (Integer) FUNCTIONS_DEFAULTS_METHODS.get(_current_function_name).invoke(_mPreferences));
                    } else {
                        Log.e(TAG, "Unknown return type " + _ret_type.toString() + " for default option");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception in add_defaults " + e.getMessage());
                }
            }
        }
    }

    private final IApgService.Stub mBinder = new IApgService.Stub() {

        public boolean encrypt_with_passphrase(Bundle pArgs, Bundle pReturn) {

            ArrayList<String> errors = new ArrayList<String>();
            ArrayList<String> warnings = new ArrayList<String>();

            pReturn.putStringArrayList("ERRORS", errors);
            pReturn.putStringArrayList("WARNINGS", warnings);

            Bundle _my_args = new Bundle(pArgs);

            /* add default values if missing */
            add_defaults(_my_args);

            /* required args */
            String msg = _my_args.getString("MSG");
            _my_args.remove("MSG");

            String passphrase = _my_args.getString("SYM_KEY");
            _my_args.remove("SYM_KEY");

            /* optional args */
            Boolean armored = _my_args.getBoolean("ARMORED");
            _my_args.remove("ARMORED");

            int encryption_algorithm = _my_args.getInt("ENCRYPTION_ALGO");
            _my_args.remove("ENCRYPTION_ALGO");

            int hash_algorithm = _my_args.getInt("HASH_ALGO");
            _my_args.remove("HASH_ALGO");

            int compression = _my_args.getInt("COMPRESSION");
            _my_args.remove("COMPRESSION");

            Boolean force_v3_signatures = _my_args.getBoolean("FORCE_V3_SIG");
            _my_args.remove("FORCE_V3_SIG");

            /* check required args */
            if (msg == null) {
                errors.add("Message to encrypt (MSG) missing");
            }

            if (passphrase == null) {
                errors.add("Symmetric key (SYM_KEY) missing");
            }

            /* check for unknown args and add to warning */
            if (!_my_args.isEmpty()) {
                Iterator<String> _iter = _my_args.keySet().iterator();
                while (_iter.hasNext()) {
                    warnings.add("Unknown key: " + _iter.next());
                }
            }

            /* return if errors happened */
            if (errors.size() != 0) {
                pReturn.putInt("ERROR", error.ARGUMENTS_MISSING.ordinal());
                return false;
            }

            InputStream _inStream = new ByteArrayInputStream(msg.getBytes());
            InputData _in = new InputData(_inStream, 0); // XXX Size second
            // param?
            OutputStream _out = new ByteArrayOutputStream();

            Apg.initialize(getApplicationContext());
            try {
                Apg.encrypt(getApplicationContext(), // context
                        _in, // input stream
                        _out, // output stream
                        armored, // armored
                        new long[0], // encryption keys
                        0, // signature key
                        null, // signature passphrase
                        null, // progress
                        encryption_algorithm, // encryption
                        hash_algorithm, // hash
                        compression, // compression
                        force_v3_signatures, // mPreferences.getForceV3Signatures(),
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
            InputData in = new InputData(inStream, 0); // XXX what size in
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
