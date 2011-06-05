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

import org.thialfihar.android.apg.provider.KeyRings;
import org.thialfihar.android.apg.provider.Keys;
import org.thialfihar.android.apg.provider.UserIds;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class ApgService extends Service {
    private final static String TAG = "ApgService";
    private static final boolean LOCAL_LOGV = true;
    private static final boolean LOCAL_LOGD = true;

    @Override
    public IBinder onBind(Intent intent) {
        if( LOCAL_LOGD ) Log.d(TAG, "bound");
        return mBinder;
    }

    /** error status */
    private enum error {
        ARGUMENTS_MISSING,
        APG_FAILURE,
        NO_MATCHING_SECRET_KEY,
        PRIVATE_KEY_PASSPHRASE_WRONG,
        PRIVATE_KEY_PASSPHRASE_MISSING;

        public int shifted_ordinal() {
            return ordinal() + 100;
        }
    }

    /** all arguments that can be passed by calling application */
    private enum arg {
        MESSAGE, // message to encrypt or to decrypt
        SYMMETRIC_PASSPHRASE, // key for symmetric en/decryption
        PUBLIC_KEYS, // public keys for encryption
        ENCRYPTION_ALGORYTHM, // encryption algorithm
        HASH_ALGORYTHM, // hash algorithm
        ARMORED_OUTPUT, // whether to armor output
        FORCE_V3_SIGNATURE, // whether to force v3 signature
        COMPRESSION, // what compression to use for encrypted output
        SIGNATURE_KEY, // key for signing
        PRIVATE_KEY_PASSPHRASE, // passphrase for encrypted private key
        KEY_TYPE, // type of key (private or public)
    }

    /** all things that might be returned */
    private enum ret {
        ERRORS, // string array list with errors
        WARNINGS, // string array list with warnings
        ERROR, // numeric error
        RESULT, // en-/decrypted
        FINGERPRINTS, // fingerprints of keys
        USER_IDS, // user ids
    }

    /** required arguments for each AIDL function */
    private static final HashMap<String, HashSet<arg>> FUNCTIONS_REQUIRED_ARGS = new HashMap<String, HashSet<arg>>();
    static {
        HashSet<arg> args = new HashSet<arg>();
        args.add(arg.SYMMETRIC_PASSPHRASE);
        args.add(arg.MESSAGE);
        FUNCTIONS_REQUIRED_ARGS.put("encrypt_with_passphrase", args);

        args = new HashSet<arg>();
        args.add(arg.PUBLIC_KEYS);
        args.add(arg.MESSAGE);
        FUNCTIONS_REQUIRED_ARGS.put("encrypt_with_public_key", args);

        args = new HashSet<arg>();
        args.add(arg.MESSAGE);
        FUNCTIONS_REQUIRED_ARGS.put("decrypt", args);

        args = new HashSet<arg>();
        args.add(arg.KEY_TYPE);
        FUNCTIONS_REQUIRED_ARGS.put("get_keys", args);

    }

    /** optional arguments for each AIDL function */
    private static final HashMap<String, HashSet<arg>> FUNCTIONS_OPTIONAL_ARGS = new HashMap<String, HashSet<arg>>();
    static {
        HashSet<arg> args = new HashSet<arg>();
        args.add(arg.ENCRYPTION_ALGORYTHM);
        args.add(arg.HASH_ALGORYTHM);
        args.add(arg.ARMORED_OUTPUT);
        args.add(arg.FORCE_V3_SIGNATURE);
        args.add(arg.COMPRESSION);
        args.add(arg.PRIVATE_KEY_PASSPHRASE);
        args.add(arg.SIGNATURE_KEY);
        FUNCTIONS_OPTIONAL_ARGS.put("encrypt_with_passphrase", args);
        FUNCTIONS_OPTIONAL_ARGS.put("encrypt_with_public_key", args);

        args = new HashSet<arg>();
        args.add(arg.SYMMETRIC_PASSPHRASE);
        args.add(arg.PUBLIC_KEYS);
        args.add(arg.PRIVATE_KEY_PASSPHRASE);
        FUNCTIONS_OPTIONAL_ARGS.put("decrypt", args);
    }

    /** a map from ApgService parameters to function calls to get the default */
    private static final HashMap<arg, String> FUNCTIONS_DEFAULTS = new HashMap<arg, String>();
    static {
        FUNCTIONS_DEFAULTS.put(arg.ENCRYPTION_ALGORYTHM, "getDefaultEncryptionAlgorithm");
        FUNCTIONS_DEFAULTS.put(arg.HASH_ALGORYTHM, "getDefaultHashAlgorithm");
        FUNCTIONS_DEFAULTS.put(arg.ARMORED_OUTPUT, "getDefaultAsciiArmour");
        FUNCTIONS_DEFAULTS.put(arg.FORCE_V3_SIGNATURE, "getForceV3Signatures");
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
     * maps a fingerprint or user id of a key to as master key in database
     * 
     * @param search_key
     *            fingerprint or user id to search for
     * @return master key if found, or 0
     */
    private static long get_master_key(String search_key, Bundle pReturn) {
        if (search_key == null || search_key.length() != 8) {
            return 0;
        }
        ArrayList<String> tmp = new ArrayList<String>();
        tmp.add(search_key);
        long[] _keys = get_master_key(tmp, pReturn);
        if (_keys.length > 0)
            return _keys[0];
        else
            return 0;
    }

    private static Cursor get_key_entries(HashMap<String, Object> params) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(KeyRings.TABLE_NAME + " INNER JOIN " + Keys.TABLE_NAME + " ON " + "(" + KeyRings.TABLE_NAME + "." + KeyRings._ID + " = " + Keys.TABLE_NAME
                + "." + Keys.KEY_RING_ID + " AND " + Keys.TABLE_NAME + "." + Keys.IS_MASTER_KEY + " = '1'" + ") " + " INNER JOIN " + UserIds.TABLE_NAME
                + " ON " + "(" + Keys.TABLE_NAME + "." + Keys._ID + " = " + UserIds.TABLE_NAME + "." + UserIds.KEY_ID + " AND " + UserIds.TABLE_NAME + "."
                + UserIds.RANK + " = '0') ");

        String orderBy = params.containsKey("order_by") ? (String) params.get("order_by") : UserIds.TABLE_NAME + "." + UserIds.USER_ID + " ASC";

        String type_val[] = null;
        String type_where = null;
        if (params.containsKey("key_type")) {
            type_where = KeyRings.TABLE_NAME + "." + KeyRings.TYPE + " = ?";
            type_val = new String[] {
                "" + params.get("key_type")
            };
        }
        return qb.query(Apg.getDatabase().db(), (String[]) params.get("columns"), type_where, type_val, null, null, orderBy);
    }

    /**
     * maps fingerprints or user ids of keys to master keys in database
     * 
     * @param search_keys
     *            a list of keys (fingerprints or user ids) to look for in
     *            database
     * @return an array of master keys
     */
    private static long[] get_master_key(ArrayList<String> search_keys, Bundle pReturn) {

        HashMap<String, Object> qParams = new HashMap<String, Object>();
        qParams.put("columns", new String[] {
                KeyRings.TABLE_NAME + "." + KeyRings.MASTER_KEY_ID, // 0
                UserIds.TABLE_NAME + "." + UserIds.USER_ID, // 1
        });
        qParams.put("key_type", Id.database.type_public);

        Cursor mCursor = get_key_entries(qParams);

        if( LOCAL_LOGV ) Log.v(TAG, "going through installed user keys");
        ArrayList<Long> _master_keys = new ArrayList<Long>();
        while (mCursor.moveToNext()) {
            long _cur_mkey = mCursor.getLong(0);
            String _cur_user = mCursor.getString(1);

            String _cur_fprint = Apg.getSmallFingerPrint(_cur_mkey);
            if( LOCAL_LOGV ) Log.v(TAG, "current user: " + _cur_user + " (" + _cur_fprint + ")");
            if (search_keys.contains(_cur_fprint) || search_keys.contains(_cur_user)) {
                if( LOCAL_LOGV ) Log.v(TAG, "master key found for: " + _cur_fprint);
                _master_keys.add(_cur_mkey);
                search_keys.remove(_cur_fprint);
            } else {
                if( LOCAL_LOGV ) Log.v(TAG, "Installed key " + _cur_fprint + " is not in the list of public keys to encrypt with");
            }
        }
        mCursor.close();

        long[] _master_longs = new long[_master_keys.size()];
        int i = 0;
        for (Long _key : _master_keys) {
            _master_longs[i++] = _key;
        }

        if (i == 0) {
            Log.w(TAG, "Found not one public key");
            pReturn.getStringArrayList(ret.WARNINGS.name()).add("Searched for public key(s) but found not one");
        }

        for (String _key : search_keys) {
            Log.w(TAG, "Searched for key " + _key + " but cannot find it in APG");
            pReturn.getStringArrayList(ret.WARNINGS.name()).add("Searched for key " + _key + " but cannot find it in APG");
        }

        return _master_longs;
    }

    /**
     * Add default arguments if missing
     * 
     * @param args
     *            the bundle to add default parameters to if missing
     */
    private void add_default_arguments(String call, Bundle args) {
        // check whether there are optional elements defined for that call
        if (FUNCTIONS_OPTIONAL_ARGS.containsKey(call)) {
            Preferences _mPreferences = Preferences.getPreferences(getBaseContext(), true);

            Iterator<arg> _iter = FUNCTIONS_DEFAULTS.keySet().iterator();
            while (_iter.hasNext()) {
                arg _current_arg = _iter.next();
                String _current_key = _current_arg.name();
                if (!args.containsKey(_current_key) && FUNCTIONS_OPTIONAL_ARGS.get(call).contains(_current_arg)) {
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
        if (FUNCTIONS_REQUIRED_ARGS.containsKey(function)) {
            Iterator<arg> _iter = FUNCTIONS_REQUIRED_ARGS.get(function).iterator();
            while (_iter.hasNext()) {
                String _cur_arg = _iter.next().name();
                if (!pArgs.containsKey(_cur_arg)) {
                    pReturn.getStringArrayList(ret.ERRORS.name()).add("Argument missing: " + _cur_arg);
                }
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

        HashSet<arg> all_args = new HashSet<arg>();
        if (FUNCTIONS_REQUIRED_ARGS.containsKey(function)) {
            all_args.addAll(FUNCTIONS_REQUIRED_ARGS.get(function));
        }
        if (FUNCTIONS_OPTIONAL_ARGS.containsKey(function)) {
            all_args.addAll(FUNCTIONS_OPTIONAL_ARGS.get(function));
        }

        ArrayList<String> _unknown_args = new ArrayList<String>();
        Iterator<String> _iter = pArgs.keySet().iterator();
        while (_iter.hasNext()) {
            String _cur_key = _iter.next();
            try {
                arg _cur_arg = arg.valueOf(_cur_key);
                if (!all_args.contains(_cur_arg)) {
                    pReturn.getStringArrayList(ret.WARNINGS.name()).add("Unknown argument: " + _cur_key);
                    _unknown_args.add(_cur_key);
                }
            } catch (Exception e) {
                pReturn.getStringArrayList(ret.WARNINGS.name()).add("Unknown argument: " + _cur_key);
                _unknown_args.add(_cur_key);
            }
        }

        // remove unknown arguments so our bundle has just what we need
        for (String _arg : _unknown_args) {
            pArgs.remove(_arg);
        }
    }

    private boolean prepare_args(String call, Bundle pArgs, Bundle pReturn) {
        Apg.initialize(getBaseContext());

        /* add default return values for all functions */
        add_default_returns(pReturn);

        /* add default arguments if missing */
        add_default_arguments(call, pArgs);
        if( LOCAL_LOGV ) Log.v(TAG, "add_default_arguments");

        /* check for required arguments */
        check_required_args(call, pArgs, pReturn);
        if( LOCAL_LOGV ) Log.v(TAG, "check_required_args");

        /* check for unknown arguments and add to warning if found */
        check_unknown_args(call, pArgs, pReturn);
        if( LOCAL_LOGV ) Log.v(TAG, "check_unknown_args");

        /* return if errors happened */
        if (pReturn.getStringArrayList(ret.ERRORS.name()).size() != 0) {
            if( LOCAL_LOGV ) Log.v(TAG, "Errors after preparing, not executing "+call);
            pReturn.putInt(ret.ERROR.name(), error.ARGUMENTS_MISSING.shifted_ordinal());
            return false;
        }
        if( LOCAL_LOGV ) Log.v(TAG, "error return");

        return true;
    }

    private boolean encrypt(Bundle pArgs, Bundle pReturn) {

        long _pub_master_keys[] = {};
        if (pArgs.containsKey(arg.PUBLIC_KEYS.name())) {
            ArrayList<String> _list = pArgs.getStringArrayList(arg.PUBLIC_KEYS.name());
            ArrayList<String> _pub_keys = new ArrayList<String>();
            if( LOCAL_LOGV ) Log.v(TAG, "Long size: " + _list.size());
            Iterator<String> _iter = _list.iterator();
            while (_iter.hasNext()) {
                _pub_keys.add(_iter.next());
            }
            _pub_master_keys = get_master_key(_pub_keys, pReturn);
        }

        InputStream _inStream = new ByteArrayInputStream(pArgs.getString(arg.MESSAGE.name()).getBytes());
        InputData _in = new InputData(_inStream, 0); // XXX Size second param?

        OutputStream _out = new ByteArrayOutputStream();
        if( LOCAL_LOGV ) Log.v(TAG, "About to encrypt");
        try {
            Apg.encrypt(getBaseContext(), // context
                    _in, // input stream
                    _out, // output stream
                    pArgs.getBoolean(arg.ARMORED_OUTPUT.name()), // ARMORED_OUTPUT
                    _pub_master_keys, // encryption keys
                    get_master_key(pArgs.getString(arg.SIGNATURE_KEY.name()), pReturn), // signature key
                    pArgs.getString(arg.PRIVATE_KEY_PASSPHRASE.name()), // signature passphrase
                    null, // progress
                    pArgs.getInt(arg.ENCRYPTION_ALGORYTHM.name()), // encryption
                    pArgs.getInt(arg.HASH_ALGORYTHM.name()), // hash
                    pArgs.getInt(arg.COMPRESSION.name()), // compression
                    pArgs.getBoolean(arg.FORCE_V3_SIGNATURE.name()), // mPreferences.getForceV3Signatures(),
                    pArgs.getString(arg.SYMMETRIC_PASSPHRASE.name()) // passPhrase
                    );
        } catch (Exception e) {
            Log.e(TAG, "Exception in encrypt");
            String _msg = e.getMessage();
            if (_msg.equals(getBaseContext().getString(R.string.error_noSignaturePassPhrase))) {
                pReturn.getStringArrayList(ret.ERRORS.name()).add("Cannot encrypt (" + arg.PRIVATE_KEY_PASSPHRASE.name() + " missing): " + _msg);
                pReturn.putInt(ret.ERROR.name(), error.PRIVATE_KEY_PASSPHRASE_MISSING.shifted_ordinal());
            } else if (_msg.equals(getBaseContext().getString(R.string.error_couldNotExtractPrivateKey))) {
                pReturn.getStringArrayList(ret.ERRORS.name()).add("Cannot encrypt (" + arg.PRIVATE_KEY_PASSPHRASE.name() + " probably wrong): " + _msg);
                pReturn.putInt(ret.ERROR.name(), error.PRIVATE_KEY_PASSPHRASE_WRONG.shifted_ordinal());
            } else {
                pReturn.getStringArrayList(ret.ERRORS.name()).add("Internal failure (" + e.getClass() + ") in APG when encrypting: " + e.getMessage());
                pReturn.putInt(ret.ERROR.name(), error.APG_FAILURE.shifted_ordinal());
            }
            return false;
        }
        if( LOCAL_LOGV ) Log.v(TAG, "Encrypted");
        pReturn.putString(ret.RESULT.name(), _out.toString());
        return true;
    }

    private final IApgService.Stub mBinder = new IApgService.Stub() {

        public boolean get_keys(Bundle pArgs, Bundle pReturn) {

            prepare_args("get_keys", pArgs, pReturn);

            HashMap<String, Object> qParams = new HashMap<String, Object>();
            qParams.put("columns", new String[] {
                    KeyRings.TABLE_NAME + "." + KeyRings.MASTER_KEY_ID, // 0
                    UserIds.TABLE_NAME + "." + UserIds.USER_ID, // 1
            });

            qParams.put("key_type", pArgs.getInt(arg.KEY_TYPE.name()));

            Cursor mCursor = get_key_entries(qParams);
            ArrayList<String> fprints = new ArrayList<String>();
            ArrayList<String> ids = new ArrayList<String>();
            while (mCursor.moveToNext()) {
                if( LOCAL_LOGV ) Log.v(TAG, "adding key "+Apg.getSmallFingerPrint(mCursor.getLong(0)));
                fprints.add(Apg.getSmallFingerPrint(mCursor.getLong(0)));
                ids.add(mCursor.getString(1));
            }
            mCursor.close();

            pReturn.putStringArrayList(ret.FINGERPRINTS.name(), fprints);
            pReturn.putStringArrayList(ret.USER_IDS.name(), ids);
            return true;
        }

        public boolean encrypt_with_public_key(Bundle pArgs, Bundle pReturn) {
            if (!prepare_args("encrypt_with_public_key", pArgs, pReturn)) {
                return false;
            }

            return encrypt(pArgs, pReturn);
        }

        public boolean encrypt_with_passphrase(Bundle pArgs, Bundle pReturn) {
            if (!prepare_args("encrypt_with_passphrase", pArgs, pReturn)) {
                return false;
            }

            return encrypt(pArgs, pReturn);

        }

        public boolean decrypt(Bundle pArgs, Bundle pReturn) {
            if (!prepare_args("decrypt", pArgs, pReturn)) {
                return false;
            }

            String _passphrase = pArgs.getString(arg.SYMMETRIC_PASSPHRASE.name()) != null ? pArgs.getString(arg.SYMMETRIC_PASSPHRASE.name()) : pArgs
                    .getString(arg.PRIVATE_KEY_PASSPHRASE.name());

            InputStream inStream = new ByteArrayInputStream(pArgs.getString(arg.MESSAGE.name()).getBytes());
            InputData in = new InputData(inStream, 0); // XXX what size in second parameter?
            OutputStream out = new ByteArrayOutputStream();
            if( LOCAL_LOGV ) Log.v(TAG, "About to decrypt");
            try {
                Apg.decrypt(getBaseContext(), in, out, _passphrase, null, // progress
                        pArgs.getString(arg.SYMMETRIC_PASSPHRASE.name()) != null // symmetric
                        );
            } catch (Exception e) {
                Log.e(TAG, "Exception in decrypt");
                String _msg = e.getMessage();
                if (_msg.equals(getBaseContext().getString(R.string.error_noSecretKeyFound))) {
                    pReturn.getStringArrayList(ret.ERRORS.name()).add("Cannot decrypt: " + _msg);
                    pReturn.putInt(ret.ERROR.name(), error.NO_MATCHING_SECRET_KEY.shifted_ordinal());
                } else if (_msg.equals(getBaseContext().getString(R.string.error_wrongPassPhrase))) {
                    pReturn.getStringArrayList(ret.ERRORS.name()).add("Cannot decrypt (" + arg.PRIVATE_KEY_PASSPHRASE.name() + " wrong/missing): " + _msg);
                    pReturn.putInt(ret.ERROR.name(), error.PRIVATE_KEY_PASSPHRASE_WRONG.shifted_ordinal());
                } else {
                    pReturn.getStringArrayList(ret.ERRORS.name()).add("Internal failure (" + e.getClass() + ") in APG when decrypting: " + _msg);
                    pReturn.putInt(ret.ERROR.name(), error.APG_FAILURE.shifted_ordinal());
                }
                return false;
            }
            if( LOCAL_LOGV ) Log.v(TAG, "Decrypted");

            pReturn.putString(ret.RESULT.name(), out.toString());
            return true;
        }

    };
}
