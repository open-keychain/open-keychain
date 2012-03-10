package org.apg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apg.provider.KeyRings;
import org.apg.provider.Keys;
import org.apg.provider.UserIds;
import org.apg.IApgService;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class ApgService extends Service {
    private final static String TAG = "ApgService";
    public static final boolean LOCAL_LOGV = true;
    public static final boolean LOCAL_LOGD = true;

    @Override
    public IBinder onBind(Intent intent) {
        if (LOCAL_LOGD)
            Log.d(TAG, "bound");
        return mBinder;
    }

    /** error status */
    private static enum error {
        ARGUMENTS_MISSING, APG_FAILURE, NO_MATCHING_SECRET_KEY, PRIVATE_KEY_PASSPHRASE_WRONG, PRIVATE_KEY_PASSPHRASE_MISSING;

        public int shiftedOrdinal() {
            return ordinal() + 100;
        }
    }

    private static enum call {
        encrypt_with_passphrase, encrypt_with_public_key, decrypt, get_keys
    }

    /** all arguments that can be passed by calling application */
    public static enum arg {
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
        BLOB, // blob passed
    }

    /** all things that might be returned */
    private static enum ret {
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
        FUNCTIONS_REQUIRED_ARGS.put(call.encrypt_with_passphrase.name(), args);

        args = new HashSet<arg>();
        args.add(arg.PUBLIC_KEYS);
        FUNCTIONS_REQUIRED_ARGS.put(call.encrypt_with_public_key.name(), args);

        args = new HashSet<arg>();
        FUNCTIONS_REQUIRED_ARGS.put(call.decrypt.name(), args);

        args = new HashSet<arg>();
        args.add(arg.KEY_TYPE);
        FUNCTIONS_REQUIRED_ARGS.put(call.get_keys.name(), args);
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
        args.add(arg.BLOB);
        args.add(arg.MESSAGE);
        FUNCTIONS_OPTIONAL_ARGS.put(call.encrypt_with_passphrase.name(), args);
        FUNCTIONS_OPTIONAL_ARGS.put(call.encrypt_with_public_key.name(), args);

        args = new HashSet<arg>();
        args.add(arg.SYMMETRIC_PASSPHRASE);
        args.add(arg.PUBLIC_KEYS);
        args.add(arg.PRIVATE_KEY_PASSPHRASE);
        args.add(arg.MESSAGE);
        args.add(arg.BLOB);
        FUNCTIONS_OPTIONAL_ARGS.put(call.decrypt.name(), args);
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

    /** a map of the default function names to their method */
    private static final HashMap<String, Method> FUNCTIONS_DEFAULTS_METHODS = new HashMap<String, Method>();
    static {
        try {
            FUNCTIONS_DEFAULTS_METHODS.put("getDefaultEncryptionAlgorithm",
                    Preferences.class.getMethod("getDefaultEncryptionAlgorithm"));
            FUNCTIONS_DEFAULTS_METHODS.put("getDefaultHashAlgorithm",
                    Preferences.class.getMethod("getDefaultHashAlgorithm"));
            FUNCTIONS_DEFAULTS_METHODS.put("getDefaultAsciiArmour",
                    Preferences.class.getMethod("getDefaultAsciiArmour"));
            FUNCTIONS_DEFAULTS_METHODS.put("getForceV3Signatures",
                    Preferences.class.getMethod("getForceV3Signatures"));
            FUNCTIONS_DEFAULTS_METHODS.put("getDefaultMessageCompression",
                    Preferences.class.getMethod("getDefaultMessageCompression"));
        } catch (Exception e) {
            Log.e(TAG, "Function method exception: " + e.getMessage());
        }
    }

    private static void writeToOutputStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[8];
        int len = 0;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }

    private static Cursor getKeyEntries(HashMap<String, Object> pParams) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(KeyRings.TABLE_NAME + " INNER JOIN " + Keys.TABLE_NAME + " ON " + "("
                + KeyRings.TABLE_NAME + "." + KeyRings._ID + " = " + Keys.TABLE_NAME + "."
                + Keys.KEY_RING_ID + " AND " + Keys.TABLE_NAME + "." + Keys.IS_MASTER_KEY
                + " = '1'" + ") " + " INNER JOIN " + UserIds.TABLE_NAME + " ON " + "("
                + Keys.TABLE_NAME + "." + Keys._ID + " = " + UserIds.TABLE_NAME + "."
                + UserIds.KEY_ID + " AND " + UserIds.TABLE_NAME + "." + UserIds.RANK + " = '0') ");

        String orderBy = pParams.containsKey("order_by") ? (String) pParams.get("order_by")
                : UserIds.TABLE_NAME + "." + UserIds.USER_ID + " ASC";

        String typeVal[] = null;
        String typeWhere = null;
        if (pParams.containsKey("key_type")) {
            typeWhere = KeyRings.TABLE_NAME + "." + KeyRings.TYPE + " = ?";
            typeVal = new String[] { "" + pParams.get("key_type") };
        }
        return qb.query(Apg.getDatabase().db(), (String[]) pParams.get("columns"), typeWhere,
                typeVal, null, null, orderBy);
    }

    /**
     * maps a fingerprint or user id of a key to a master key in database
     * 
     * @param search_key
     *            fingerprint or user id to search for
     * @return master key if found, or 0
     */
    private static long getMasterKey(String pSearchKey, Bundle pReturn) {
        if (pSearchKey == null || pSearchKey.length() != 8) {
            return 0;
        }
        ArrayList<String> keyList = new ArrayList<String>();
        keyList.add(pSearchKey);
        long[] keys = getMasterKey(keyList, pReturn);
        if (keys.length > 0) {
            return keys[0];
        } else {
            return 0;
        }
    }

    /**
     * maps fingerprints or user ids of keys to master keys in database
     * 
     * @param search_keys
     *            a list of keys (fingerprints or user ids) to look for in database
     * @return an array of master keys
     */
    private static long[] getMasterKey(ArrayList<String> pSearchKeys, Bundle pReturn) {

        HashMap<String, Object> qParams = new HashMap<String, Object>();
        qParams.put("columns", new String[] { KeyRings.TABLE_NAME + "." + KeyRings.MASTER_KEY_ID, // 0
                UserIds.TABLE_NAME + "." + UserIds.USER_ID, // 1
        });
        qParams.put("key_type", Id.database.type_public);

        Cursor mCursor = getKeyEntries(qParams);

        if (LOCAL_LOGV)
            Log.v(TAG, "going through installed user keys");
        ArrayList<Long> masterKeys = new ArrayList<Long>();
        while (mCursor.moveToNext()) {
            long curMkey = mCursor.getLong(0);
            String curUser = mCursor.getString(1);

            String curFprint = Apg.getSmallFingerPrint(curMkey);
            if (LOCAL_LOGV)
                Log.v(TAG, "current user: " + curUser + " (" + curFprint + ")");
            if (pSearchKeys.contains(curFprint) || pSearchKeys.contains(curUser)) {
                if (LOCAL_LOGV)
                    Log.v(TAG, "master key found for: " + curFprint);
                masterKeys.add(curMkey);
                pSearchKeys.remove(curFprint);
            } else {
                if (LOCAL_LOGV)
                    Log.v(TAG, "Installed key " + curFprint
                            + " is not in the list of public keys to encrypt with");
            }
        }
        mCursor.close();

        long[] masterKeyLongs = new long[masterKeys.size()];
        int i = 0;
        for (Long key : masterKeys) {
            masterKeyLongs[i++] = key;
        }

        if (i == 0) {
            Log.w(TAG, "Found not one public key");
            pReturn.getStringArrayList(ret.WARNINGS.name()).add(
                    "Searched for public key(s) but found not one");
        }

        for (String key : pSearchKeys) {
            Log.w(TAG, "Searched for key " + key + " but cannot find it in APG");
            pReturn.getStringArrayList(ret.WARNINGS.name()).add(
                    "Searched for key " + key + " but cannot find it in APG");
        }

        return masterKeyLongs;
    }

    /**
     * Add default arguments if missing
     * 
     * @param args
     *            the bundle to add default parameters to if missing
     */
    private void addDefaultArguments(String pCall, Bundle pArgs) {
        // check whether there are optional elements defined for that call
        if (FUNCTIONS_OPTIONAL_ARGS.containsKey(pCall)) {
            Preferences preferences = Preferences.getPreferences(getBaseContext(), true);

            Iterator<arg> iter = FUNCTIONS_DEFAULTS.keySet().iterator();
            while (iter.hasNext()) {
                arg currentArg = iter.next();
                String currentKey = currentArg.name();
                if (!pArgs.containsKey(currentKey)
                        && FUNCTIONS_OPTIONAL_ARGS.get(pCall).contains(currentArg)) {
                    String currentFunctionName = FUNCTIONS_DEFAULTS.get(currentArg);
                    try {
                        Class<?> returnType = FUNCTIONS_DEFAULTS_METHODS.get(currentFunctionName)
                                .getReturnType();
                        if (returnType == String.class) {
                            pArgs.putString(currentKey,
                                    (String) FUNCTIONS_DEFAULTS_METHODS.get(currentFunctionName)
                                            .invoke(preferences));
                        } else if (returnType == boolean.class) {
                            pArgs.putBoolean(currentKey,
                                    (Boolean) FUNCTIONS_DEFAULTS_METHODS.get(currentFunctionName)
                                            .invoke(preferences));
                        } else if (returnType == int.class) {
                            pArgs.putInt(currentKey,
                                    (Integer) FUNCTIONS_DEFAULTS_METHODS.get(currentFunctionName)
                                            .invoke(preferences));
                        } else {
                            Log.e(TAG, "Unknown return type " + returnType.toString()
                                    + " for default option");
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
    private void addDefaultReturns(Bundle pReturn) {
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
    private void checkForRequiredArgs(String pFunction, Bundle pArgs, Bundle pReturn) {
        if (FUNCTIONS_REQUIRED_ARGS.containsKey(pFunction)) {
            Iterator<arg> iter = FUNCTIONS_REQUIRED_ARGS.get(pFunction).iterator();
            while (iter.hasNext()) {
                String curArg = iter.next().name();
                if (!pArgs.containsKey(curArg)) {
                    pReturn.getStringArrayList(ret.ERRORS.name())
                            .add("Argument missing: " + curArg);
                }
            }
        }

        if (pFunction.equals(call.encrypt_with_passphrase.name())
                || pFunction.equals(call.encrypt_with_public_key.name())
                || pFunction.equals(call.decrypt.name())) {
            // check that either MESSAGE or BLOB are there
            if (!pArgs.containsKey(arg.MESSAGE.name()) && !pArgs.containsKey(arg.BLOB.name())) {
                pReturn.getStringArrayList(ret.ERRORS.name()).add(
                        "Arguments missing: Neither MESSAGE nor BLOG found");
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
    private void checkForUnknownArgs(String pFunction, Bundle pArgs, Bundle pReturn) {

        HashSet<arg> allArgs = new HashSet<arg>();
        if (FUNCTIONS_REQUIRED_ARGS.containsKey(pFunction)) {
            allArgs.addAll(FUNCTIONS_REQUIRED_ARGS.get(pFunction));
        }
        if (FUNCTIONS_OPTIONAL_ARGS.containsKey(pFunction)) {
            allArgs.addAll(FUNCTIONS_OPTIONAL_ARGS.get(pFunction));
        }

        ArrayList<String> unknownArgs = new ArrayList<String>();
        Iterator<String> iter = pArgs.keySet().iterator();
        while (iter.hasNext()) {
            String curKey = iter.next();
            try {
                arg curArg = arg.valueOf(curKey);
                if (!allArgs.contains(curArg)) {
                    pReturn.getStringArrayList(ret.WARNINGS.name()).add(
                            "Unknown argument: " + curKey);
                    unknownArgs.add(curKey);
                }
            } catch (Exception e) {
                pReturn.getStringArrayList(ret.WARNINGS.name()).add("Unknown argument: " + curKey);
                unknownArgs.add(curKey);
            }
        }

        // remove unknown arguments so our bundle has just what we need
        for (String arg : unknownArgs) {
            pArgs.remove(arg);
        }
    }

    private boolean prepareArgs(String pCall, Bundle pArgs, Bundle pReturn) {
        Apg.initialize(getBaseContext());

        /* add default return values for all functions */
        addDefaultReturns(pReturn);

        /* add default arguments if missing */
        addDefaultArguments(pCall, pArgs);
        if (LOCAL_LOGV)
            Log.v(TAG, "add_default_arguments");

        /* check for required arguments */
        checkForRequiredArgs(pCall, pArgs, pReturn);
        if (LOCAL_LOGV)
            Log.v(TAG, "check_required_args");

        /* check for unknown arguments and add to warning if found */
        checkForUnknownArgs(pCall, pArgs, pReturn);
        if (LOCAL_LOGV)
            Log.v(TAG, "check_unknown_args");

        /* return if errors happened */
        if (pReturn.getStringArrayList(ret.ERRORS.name()).size() != 0) {
            if (LOCAL_LOGV)
                Log.v(TAG, "Errors after preparing, not executing " + pCall);
            pReturn.putInt(ret.ERROR.name(), error.ARGUMENTS_MISSING.shiftedOrdinal());
            return false;
        }
        if (LOCAL_LOGV)
            Log.v(TAG, "error return");

        return true;
    }

    private boolean encrypt(Bundle pArgs, Bundle pReturn) {
        boolean isBlob = pArgs.containsKey(arg.BLOB.name());

        long pubMasterKeys[] = {};
        if (pArgs.containsKey(arg.PUBLIC_KEYS.name())) {
            ArrayList<String> list = pArgs.getStringArrayList(arg.PUBLIC_KEYS.name());
            ArrayList<String> pubKeys = new ArrayList<String>();
            if (LOCAL_LOGV)
                Log.v(TAG, "Long size: " + list.size());
            Iterator<String> iter = list.iterator();
            while (iter.hasNext()) {
                pubKeys.add(iter.next());
            }
            pubMasterKeys = getMasterKey(pubKeys, pReturn);
        }

        InputStream inStream = null;
        if (isBlob) {
            ContentResolver cr = getContentResolver();
            try {
                inStream = cr.openInputStream(Uri.parse(pArgs.getString(arg.BLOB.name())));
            } catch (Exception e) {
                Log.e(TAG, "... exception on opening blob", e);
            }
        } else {
            inStream = new ByteArrayInputStream(pArgs.getString(arg.MESSAGE.name()).getBytes());
        }
        InputData in = new InputData(inStream, 0); // XXX Size second param?

        OutputStream out = new ByteArrayOutputStream();
        if (LOCAL_LOGV)
            Log.v(TAG, "About to encrypt");
        try {
            Apg.encrypt(getBaseContext(), // context
                    in, // input stream
                    out, // output stream
                    pArgs.getBoolean(arg.ARMORED_OUTPUT.name()), // ARMORED_OUTPUT
                    pubMasterKeys, // encryption keys
                    getMasterKey(pArgs.getString(arg.SIGNATURE_KEY.name()), pReturn), // signature
                                                                                      // key
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
            String msg = e.getMessage();
            if (msg.equals(getBaseContext().getString(R.string.error_noSignaturePassPhrase))) {
                pReturn.getStringArrayList(ret.ERRORS.name()).add(
                        "Cannot encrypt (" + arg.PRIVATE_KEY_PASSPHRASE.name() + " missing): "
                                + msg);
                pReturn.putInt(ret.ERROR.name(),
                        error.PRIVATE_KEY_PASSPHRASE_MISSING.shiftedOrdinal());
            } else if (msg.equals(getBaseContext().getString(
                    R.string.error_couldNotExtractPrivateKey))) {
                pReturn.getStringArrayList(ret.ERRORS.name()).add(
                        "Cannot encrypt (" + arg.PRIVATE_KEY_PASSPHRASE.name()
                                + " probably wrong): " + msg);
                pReturn.putInt(ret.ERROR.name(),
                        error.PRIVATE_KEY_PASSPHRASE_WRONG.shiftedOrdinal());
            } else {
                pReturn.getStringArrayList(ret.ERRORS.name()).add(
                        "Internal failure (" + e.getClass() + ") in APG when encrypting: "
                                + e.getMessage());
                pReturn.putInt(ret.ERROR.name(), error.APG_FAILURE.shiftedOrdinal());
            }
            return false;
        }
        if (LOCAL_LOGV)
            Log.v(TAG, "Encrypted");
        if (isBlob) {
            ContentResolver cr = getContentResolver();
            try {
                OutputStream outStream = cr.openOutputStream(Uri.parse(pArgs.getString(arg.BLOB
                        .name())));
                writeToOutputStream(new ByteArrayInputStream(out.toString().getBytes()), outStream);
                outStream.close();
            } catch (Exception e) {
                Log.e(TAG, "... exception on writing blob", e);
            }
        } else {
            pReturn.putString(ret.RESULT.name(), out.toString());
        }
        return true;
    }

    private final IApgService.Stub mBinder = new IApgService.Stub() {

        public boolean getKeys(Bundle pArgs, Bundle pReturn) {

            prepareArgs("get_keys", pArgs, pReturn);

            HashMap<String, Object> qParams = new HashMap<String, Object>();
            qParams.put("columns", new String[] {
                    KeyRings.TABLE_NAME + "." + KeyRings.MASTER_KEY_ID, // 0
                    UserIds.TABLE_NAME + "." + UserIds.USER_ID, // 1
            });

            qParams.put("key_type", pArgs.getInt(arg.KEY_TYPE.name()));

            Cursor cursor = getKeyEntries(qParams);
            ArrayList<String> fPrints = new ArrayList<String>();
            ArrayList<String> ids = new ArrayList<String>();
            while (cursor.moveToNext()) {
                if (LOCAL_LOGV)
                    Log.v(TAG, "adding key " + Apg.getSmallFingerPrint(cursor.getLong(0)));
                fPrints.add(Apg.getSmallFingerPrint(cursor.getLong(0)));
                ids.add(cursor.getString(1));
            }
            cursor.close();

            pReturn.putStringArrayList(ret.FINGERPRINTS.name(), fPrints);
            pReturn.putStringArrayList(ret.USER_IDS.name(), ids);
            return true;
        }

        public boolean encryptWithPublicKey(Bundle pArgs, Bundle pReturn) {
            if (!prepareArgs("encrypt_with_public_key", pArgs, pReturn)) {
                return false;
            }

            return encrypt(pArgs, pReturn);
        }

        public boolean encryptWithPassphrase(Bundle pArgs, Bundle pReturn) {
            if (!prepareArgs("encrypt_with_passphrase", pArgs, pReturn)) {
                return false;
            }

            return encrypt(pArgs, pReturn);

        }

        public boolean decrypt(Bundle pArgs, Bundle pReturn) {
            if (!prepareArgs("decrypt", pArgs, pReturn)) {
                return false;
            }

            boolean isBlob = pArgs.containsKey(arg.BLOB.name());

            String passphrase = pArgs.getString(arg.SYMMETRIC_PASSPHRASE.name()) != null ? pArgs
                    .getString(arg.SYMMETRIC_PASSPHRASE.name()) : pArgs
                    .getString(arg.PRIVATE_KEY_PASSPHRASE.name());

            InputStream inStream = null;
            if (isBlob) {
                ContentResolver cr = getContentResolver();
                try {
                    inStream = cr.openInputStream(Uri.parse(pArgs.getString(arg.BLOB.name())));
                } catch (Exception e) {
                    Log.e(TAG, "... exception on opening blob", e);
                }
            } else {
                inStream = new ByteArrayInputStream(pArgs.getString(arg.MESSAGE.name()).getBytes());
            }

            InputData in = new InputData(inStream, 0); // XXX what size in second parameter?
            OutputStream out = new ByteArrayOutputStream();
            if (LOCAL_LOGV)
                Log.v(TAG, "About to decrypt");
            try {
                Apg.decrypt(getBaseContext(), in, out, passphrase, null, // progress
                        pArgs.getString(arg.SYMMETRIC_PASSPHRASE.name()) != null // symmetric
                );
            } catch (Exception e) {
                Log.e(TAG, "Exception in decrypt");
                String msg = e.getMessage();
                if (msg.equals(getBaseContext().getString(R.string.error_noSecretKeyFound))) {
                    pReturn.getStringArrayList(ret.ERRORS.name()).add("Cannot decrypt: " + msg);
                    pReturn.putInt(ret.ERROR.name(), error.NO_MATCHING_SECRET_KEY.shiftedOrdinal());
                } else if (msg.equals(getBaseContext().getString(R.string.error_wrongPassPhrase))) {
                    pReturn.getStringArrayList(ret.ERRORS.name()).add(
                            "Cannot decrypt (" + arg.PRIVATE_KEY_PASSPHRASE.name()
                                    + " wrong/missing): " + msg);
                    pReturn.putInt(ret.ERROR.name(),
                            error.PRIVATE_KEY_PASSPHRASE_WRONG.shiftedOrdinal());
                } else {
                    pReturn.getStringArrayList(ret.ERRORS.name()).add(
                            "Internal failure (" + e.getClass() + ") in APG when decrypting: "
                                    + msg);
                    pReturn.putInt(ret.ERROR.name(), error.APG_FAILURE.shiftedOrdinal());
                }
                return false;
            }
            if (LOCAL_LOGV)
                Log.v(TAG, "... decrypted");

            if (isBlob) {
                ContentResolver cr = getContentResolver();
                try {
                    OutputStream outStream = cr.openOutputStream(Uri.parse(pArgs.getString(arg.BLOB
                            .name())));
                    writeToOutputStream(new ByteArrayInputStream(out.toString().getBytes()),
                            outStream);
                    outStream.close();
                } catch (Exception e) {
                    Log.e(TAG, "... exception on writing blob", e);
                }
            } else {
                pReturn.putString(ret.RESULT.name(), out.toString());
            }
            return true;
        }

    };
}
