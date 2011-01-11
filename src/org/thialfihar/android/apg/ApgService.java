package org.thialfihar.android.apg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

    /** error status */
    private enum error {
        ARGUMENTS_MISSING,
        APG_FAILURE
    }

    /** all arguments that can be passed by calling application */
    private enum arg {
        MSG, // message to encrypt or to decrypt
        SYM_KEY, // key for symmetric en/decryption
        PUBLIC_KEYS, // public keys for encryption
        ENCRYPTION_ALGO, // encryption algorithm
        HASH_ALGO, // hash algorithm
        ARMORED, // whether to armor output
        FORCE_V3_SIG, // whether to force v3 signature
        COMPRESSION
        // what compression to use for encrypted output
    }

    /** all things that might be returned */
    private enum ret {
        ERRORS,
        WARNINGS,
        ERROR,
        RESULT
    }

    /** required arguments for each AIDL function */
    private static final HashMap<String, Set<arg>> FUNCTIONS_REQUIRED_ARGS = new HashMap<String, Set<arg>>();
    static {
        HashSet<arg> args = new HashSet<arg>();
        args.add(arg.SYM_KEY);
        args.add(arg.MSG);
        FUNCTIONS_REQUIRED_ARGS.put("encrypt_with_passphrase", args);
        FUNCTIONS_REQUIRED_ARGS.put("decrypt_with_passphrase", args);

        args = new HashSet<arg>();
        args.add(arg.PUBLIC_KEYS);
        args.add(arg.MSG);
        FUNCTIONS_REQUIRED_ARGS.put("encrypt_with_public_key", args);
    }

    /** optional arguments for each AIDL function */
    private static final HashMap<String, Set<arg>> FUNCTIONS_OPTIONAL_ARGS = new HashMap<String, Set<arg>>();
    static {
        HashSet<arg> args = new HashSet<arg>();
        args.add(arg.ENCRYPTION_ALGO);
        args.add(arg.HASH_ALGO);
        args.add(arg.ARMORED);
        args.add(arg.FORCE_V3_SIG);
        args.add(arg.COMPRESSION);
        FUNCTIONS_OPTIONAL_ARGS.put("encrypt_with_passphrase", args);
        FUNCTIONS_OPTIONAL_ARGS.put("encrypt_with_public_key", args);
        FUNCTIONS_OPTIONAL_ARGS.put("decrypt_with_passphrase", args);
    }

    /** a map from ApgService parameters to function calls to get the default */
    private static final HashMap<arg, String> FUNCTIONS_DEFAULTS = new HashMap<arg, String>();
    static {
        FUNCTIONS_DEFAULTS.put(arg.ENCRYPTION_ALGO, "getDefaultEncryptionAlgorithm");
        FUNCTIONS_DEFAULTS.put(arg.HASH_ALGO, "getDefaultHashAlgorithm");
        FUNCTIONS_DEFAULTS.put(arg.ARMORED, "getDefaultAsciiArmour");
        FUNCTIONS_DEFAULTS.put(arg.FORCE_V3_SIG, "getForceV3Signatures");
        FUNCTIONS_DEFAULTS.put(arg.COMPRESSION, "getDefaultMessageCompression");
    }

    /** a map the default functions to their return types */
    private static final HashMap<String, Class<?>> FUNCTIONS_DEFAULTS_TYPES = new HashMap<String, Class<?>>();
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
    private static final HashMap<String, Method> FUNCTIONS_DEFAULTS_METHODS = new HashMap<String, Method>();
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
    private void add_default_arguments(Bundle args) {
        Preferences _mPreferences = Preferences.getPreferences(getBaseContext(), true);

        Iterator<arg> _iter = FUNCTIONS_DEFAULTS.keySet().iterator();
        while (_iter.hasNext()) {
            arg _current_arg = _iter.next();
            String _current_key = _current_arg.name();
            if (!args.containsKey(_current_key)) {
                String _current_function_name = FUNCTIONS_DEFAULTS.get(_current_arg);
                try {
                    Class<?> _ret_type = FUNCTIONS_DEFAULTS_TYPES.get(_current_function_name);
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
                    Log.e(TAG, "Exception in add_default_arguments " + e.getMessage());
                }
            }
        }
    }

    /**
     * updates a Bundle with default return values
     * 
     * @param pReturn
     *            the Bundle to update
     */
    private void add_default_returns(Bundle pReturn) {
        ArrayList<String> errors = new ArrayList<String>();
        ArrayList<String> warnings = new ArrayList<String>();

        pReturn.putStringArrayList(ret.ERRORS.name(), errors);
        pReturn.putStringArrayList(ret.WARNINGS.name(), warnings);
    }

    /**
     * checks for required arguments and adds them to the error if missing
     * 
     * @param function
     *            the functions required arguments to check for
     * @param pArgs
     *            the Bundle of arguments to check
     * @param pReturn
     *            the bundle to write errors to
     */
    private void check_required_args(String function, Bundle pArgs, Bundle pReturn) {
        Iterator<arg> _iter = FUNCTIONS_REQUIRED_ARGS.get(function).iterator();
        while (_iter.hasNext()) {
            String _cur_arg = _iter.next().name();
            if (!pArgs.containsKey(_cur_arg)) {
                pReturn.getStringArrayList(ret.ERRORS.name()).add("Argument missing: " + _cur_arg);
            }
        }
    }

    /**
     * checks for unknown arguments and add them to warning if found
     * 
     * @param function
     *            the functions name to check against
     * @param pArgs
     *            the Bundle of arguments to check
     * @param pReturn
     *            the bundle to write warnings to
     */
    private void check_unknown_args(String function, Bundle pArgs, Bundle pReturn) {
        HashSet<arg> all_args = new HashSet<arg>(FUNCTIONS_REQUIRED_ARGS.get(function));
        all_args.addAll(FUNCTIONS_OPTIONAL_ARGS.get(function));

        Iterator<String> _iter = pArgs.keySet().iterator();
        while (_iter.hasNext()) {
            String _cur_key = _iter.next();
            try {
                arg _cur_arg = arg.valueOf(_cur_key);
                if( !all_args.contains(_cur_arg)) {
                    pReturn.getStringArrayList(ret.WARNINGS.name()).add("Unknown argument: " + _cur_key);
                }
            } catch (Exception e) {
                pReturn.getStringArrayList(ret.WARNINGS.name()).add("Unknown argument: " + _cur_key);
            }

        }
    }

    private final IApgService.Stub mBinder = new IApgService.Stub() {

        public boolean encrypt_with_passphrase(Bundle pArgs, Bundle pReturn) {
            /* add default return values for all functions */
            add_default_returns(pReturn);

            /* add default arguments if missing */
            add_default_arguments(pArgs);
            Log.d(TAG, "add_default_arguments");

            /* check for required arguments */
            check_required_args("encrypt_with_passphrase", pArgs, pReturn);
            Log.d(TAG, "check_required_args");

            /* check for unknown arguments and add to warning if found */
            check_unknown_args("encrypt_with_passphrase", pArgs, pReturn);
            Log.d(TAG, "check_unknown_args");

            /* return if errors happened */
            if (pReturn.getStringArrayList(ret.ERRORS.name()).size() != 0) {
                pReturn.putInt(ret.ERROR.name(), error.ARGUMENTS_MISSING.ordinal());
                return false;
            }
            Log.d(TAG, "error return");

            InputStream _inStream = new ByteArrayInputStream(pArgs.getString(arg.MSG.name()).getBytes());
            InputData _in = new InputData(_inStream, 0); // XXX Size second
            // param?
            OutputStream _out = new ByteArrayOutputStream();

            Apg.initialize(getApplicationContext());
            try {
                Apg.encrypt(getApplicationContext(), // context
                        _in, // input stream
                        _out, // output stream
                        pArgs.getBoolean(arg.ARMORED.name()), // armored
                        new long[0], // encryption keys
                        0, // signature key
                        null, // signature passphrase
                        null, // progress
                        pArgs.getInt(arg.ENCRYPTION_ALGO.name()), // encryption
                        pArgs.getInt(arg.HASH_ALGO.name()), // hash
                        pArgs.getInt(arg.COMPRESSION.name()), // compression
                        pArgs.getBoolean(arg.FORCE_V3_SIG.name()), // mPreferences.getForceV3Signatures(),
                        pArgs.getString(arg.SYM_KEY.name()) // passPhrase
                        );
            } catch (Exception e) {
                Log.d(TAG, "Exception in encrypt");
                pReturn.getStringArrayList(ret.ERRORS.name()).add("Internal failure in APG when encrypting: " + e.getMessage());

                pReturn.putInt(ret.ERROR.name(), error.APG_FAILURE.ordinal());
                return false;
            }
            Log.d(TAG, "Encrypted");
            pReturn.putString(ret.RESULT.name(), _out.toString());
            return true;
        }

        public boolean decrypt_with_passphrase(Bundle pArgs, Bundle pReturn) {
            /* add default return values for all functions */
            add_default_returns(pReturn);

            /* add default arguments if missing */
            add_default_arguments(pArgs);
            Log.d(TAG, "add_default_arguments");


            /* check required args */
            check_required_args("decrypt_with_passphrase", pArgs, pReturn);
            Log.d(TAG, "check_required_args");


            /* check for unknown args and add to warning */
            check_unknown_args("decrypt_with_passphrase", pArgs, pReturn);
            Log.d(TAG, "check_unknown_args");

            
            /* return if errors happened */
            if (pReturn.getStringArrayList(ret.ERRORS.name()).size() != 0) {
                pReturn.putInt(ret.ERROR.name(), error.ARGUMENTS_MISSING.ordinal());
                return false;
            }

            InputStream inStream = new ByteArrayInputStream(pArgs.getString(arg.MSG.name()).getBytes());
            InputData in = new InputData(inStream, 0); // XXX what size in
            // second parameter?
            OutputStream out = new ByteArrayOutputStream();
            try {
                Apg.decrypt(getApplicationContext(), in, out, pArgs.getString(arg.SYM_KEY.name()), null, // progress
                        true // symmetric
                        );
            } catch (Exception e) {
                Log.d(TAG, "Exception in decrypt");
                pReturn.getStringArrayList(ret.ERRORS.name()).add("Internal failure in APG when decrypting: " + e.getMessage());

                pReturn.putInt(ret.ERROR.name(), error.APG_FAILURE.ordinal());
                return false;
            }

            pReturn.putString(ret.RESULT.name(), out.toString());
            return true;
        }
    };
}
