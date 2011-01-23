package org.thialfihar.android.apg.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import android.content.Context;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.thialfihar.android.apg.IApgService;

/**
 * A APG-AIDL-Wrapper
 * 
 * <p>
 * This class can be used by other projects to simplify connecting to the
 * APG-AIDL-Service. Kind of wrapper of for AIDL.
 * </p>
 * 
 * <p>
 * It is not used in this project.
 * </p>
 * 
 * @author Markus Doits <markus.doits@googlemail.com>
 * @version 0.9.1
 * 
 */
public class ApgCon {

    /**
     * Put stacktraces into the log?
     */
    private final static boolean stacktraces = true;

    private class call_async extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... arg) {
            Log.d(TAG, "Async execution starting");
            call(arg[0]);
            return null;
        }

        protected void onPostExecute(Void result) {
            Log.d(TAG, "Async execution finished");
            async_running = false;
            if (callback_object != null && callback_method != null) {
                try {
                    Log.d(TAG, "About to execute callback");
                    if (callback_return_self) {
                        callback_object.getClass().getMethod(callback_method, ApgCon.class).invoke(callback_object, get_self());
                    } else {
                        callback_object.getClass().getMethod(callback_method).invoke(callback_object);
                    }
                    Log.d(TAG, "Callback executed");
                } catch (NoSuchMethodException e) {
                    if (stacktraces)
                        e.printStackTrace();
                    Log.w(TAG, "Exception in callback: Method '" + callback_method + "' not found");
                    warning_list.add("(LOCAL) Could not execute callback, method '" + callback_method + "()' not found");
                } catch (InvocationTargetException e) {
                    if (stacktraces)
                        e.printStackTrace();
                    Throwable orig = e.getTargetException();
                    Log.w(TAG, "Exception of type '" + orig.getClass() + "' in callback's method '" + callback_method + "()':" + orig.getMessage());
                    warning_list.add("(LOCAL) Exception of type '" + orig.getClass() + "' in callback's method '" + callback_method + "()':"
                            + orig.getMessage());
                } catch (Exception e) {
                    if (stacktraces)
                        e.printStackTrace();
                    Log.w(TAG, "Exception on callback: (" + e.getClass() + ") " + e.getMessage());
                    warning_list.add("(LOCAL) Could not execute callback (" + e.getClass() + "): " + e.getMessage());
                }
            }
        }

    }

    private final static String TAG = "ApgCon";
    private final static int api_version = 1; // aidl api-version it expects

    private final Context mContext;
    private final error connection_status;
    private boolean async_running = false;
    private Object callback_object;
    private String callback_method;
    public static final boolean default_callback_return_self = false;
    private boolean callback_return_self = default_callback_return_self;

    private final Bundle result = new Bundle();
    private final Bundle args = new Bundle();
    private final ArrayList<String> error_list = new ArrayList<String>();
    private final ArrayList<String> warning_list = new ArrayList<String>();

    /** Remote service for decrypting and encrypting data */
    private IApgService apgService = null;

    /** Set apgService accordingly to connection status */
    private ServiceConnection apgConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "IApgService bound to apgService");
            apgService = IApgService.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "IApgService disconnected");
            apgService = null;
        }
    };

    /**
     * Different types of local errors
     * 
     * @author markus
     * 
     */
    public static enum error {
        NO_ERROR,
        /**
         * generic error
         */
        GENERIC,
        /**
         * connection to apg service not possible
         */
        CANNOT_BIND_TO_APG,
        /**
         * function to call not provided
         */
        CALL_MISSING,
        /**
         * apg service does not know what to do
         */
        CALL_NOT_KNOWN,
        /**
         * could not find APG being installed
         */
        APG_NOT_FOUND,
        /**
         * found APG but without AIDL interface
         */
        APG_AIDL_MISSING,
        APG_API_MISSMATCH
    }

    private static enum ret {
        ERROR, // returned from AIDL
        RESULT, // returned from AIDL
        WARNINGS, // mixed AIDL and LOCAL
        ERRORS, // mixed AIDL and LOCAL
    }

    /**
     * Constructor
     * 
     * <p>
     * Creates a new ApgCon object and searches for the right APG version on
     * initialization. If not found, errors are printed to the error log.
     * </p>
     * 
     * @param ctx
     *            the running context
     */
    public ApgCon(Context ctx) {
        Log.v(TAG, "EncryptionService created");
        mContext = ctx;

        error tmp_connection_status = null;
        try {
            Log.v(TAG, "Searching for the right APG version");
            ServiceInfo apg_services[] = ctx.getPackageManager().getPackageInfo("org.thialfihar.android.apg",
                    PackageManager.GET_SERVICES | PackageManager.GET_META_DATA).services;
            if (apg_services == null) {
                Log.e(TAG, "Could not fetch services");
                tmp_connection_status = error.GENERIC;
            } else {
                boolean apg_service_found = false;
                for (ServiceInfo inf : apg_services) {
                    Log.v(TAG, "Found service of APG: " + inf.name);
                    if (inf.name.equals("org.thialfihar.android.apg.ApgService")) {
                        apg_service_found = true;
                        if (inf.metaData == null) {
                            Log.w(TAG, "Could not determine ApgService API");
                            Log.w(TAG, "This probably won't work!");
                            warning_list.add("(LOCAL) Could not determine ApgService API");
                            tmp_connection_status = error.APG_API_MISSMATCH;
                        } else if (inf.metaData.getInt("api_version") != api_version) {
                            Log.w(TAG, "Found ApgService API version" + inf.metaData.getInt("api_version") + " but exspected " + api_version);
                            Log.w(TAG, "This probably won't work!");
                            warning_list.add("(LOCAL) Found ApgService API version" + inf.metaData.getInt("api_version") + " but exspected " + api_version);
                            tmp_connection_status = error.APG_API_MISSMATCH;
                        } else {
                            Log.v(TAG, "Found api_version " + api_version + ", everything should work");
                            tmp_connection_status = error.NO_ERROR;
                        }
                    }
                }

                if (!apg_service_found) {
                    Log.e(TAG, "Could not find APG with AIDL interface, this probably won't work");
                    error_list.add("(LOCAL) Could not find APG with AIDL interface, this probably won't work");
                    result.putInt(ret.ERROR.name(), error.APG_AIDL_MISSING.ordinal());
                    tmp_connection_status = error.APG_NOT_FOUND;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            if (stacktraces)
                e.printStackTrace();
            Log.e(TAG, "Could not find APG, is it installed?");
            error_list.add("(LOCAL) Could not find APG, is it installed?");
            result.putInt(ret.ERROR.name(), error.APG_NOT_FOUND.ordinal());
            tmp_connection_status = error.APG_NOT_FOUND;
        }
        
        connection_status = tmp_connection_status;
    }

    /** try to connect to the apg service */
    private boolean connect() {
        Log.v(TAG, "trying to bind the apgService to context");

        if (apgService != null) {
            Log.v(TAG, "allready connected");
            return true;
        }

        try {
            mContext.bindService(new Intent(IApgService.class.getName()), apgConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            if (stacktraces)
                e.printStackTrace();
            Log.v(TAG, "could not bind APG service");
            return false;
        }

        int wait_count = 0;
        while (apgService == null && wait_count++ < 15) {
            Log.v(TAG, "sleeping 1 second to wait for apg");
            android.os.SystemClock.sleep(1000);
        }

        if (wait_count >= 15) {
            Log.v(TAG, "slept waiting for nothing!");
            return false;
        }

        return true;
    }

    /**
     * Disconnects ApgCon from Apg
     * 
     * <p>
     * This should be called whenever all work with APG is done (e.g. everything
     * you wanted to encrypt is encrypted), since connections with AIDL should
     * not be upheld indefinitely.
     * <p>
     * 
     * <p>
     * Also, if you destroy you end using your ApgCon-instance, this must be
     * called or else the connection to APG is leaked
     * </p>
     */
    public void disconnect() {
        Log.v(TAG, "disconnecting apgService");
        if (apgService != null) {
            mContext.unbindService(apgConnection);
            apgService = null;
        }
    }

    private boolean initialize() {
        if (apgService == null) {
            if (!connect()) {
                Log.v(TAG, "connection to apg service failed");
                return false;
            }
        }
        return true;
    }

    /**
     * Calls a function from APG's AIDL-interface
     * 
     * <p>
     * After you have set up everything with {@link #set_arg(String, String)}
     * (and variants), you can call a function from the AIDL-interface. This
     * will
     * <ul>
     * <li>start connection to the remote interface (if not already connected)</li>
     * <li>call the passed function with all set up parameters synchronously</li>
     * <li>set up everything to retrieve the result and/or warnings/errors</li>
     * </ul>
     * </p>
     * 
     * <p>
     * Note your thread will be blocked during execution - if you want to call
     * the function asynchronously, see {@link #call_async(String)}.
     * </p>
     * 
     * @param function
     *            a remote function to call
     * @return true, if call successful (= no errors), else false
     * 
     * @see #call_async(String)
     * @see #set_arg(String, String)
     */
    public boolean call(String function) {
        return this.call(function, args, result);
    }

    /**
     * Calls a function from remote interface asynchronously
     * 
     * <p>
     * This does exactly the same as {@link #call(String)}, but asynchronously.
     * While connection to APG and work are done in background, your thread can
     * go on executing.
     * <p>
     * 
     * <p>
     * To see whether the task is finished, you have to possibilities:
     * <ul>
     * <li>In your thread, poll {@link #is_running()}</li>
     * <li>Supply a callback with {@link #set_callback(Object, String)}</li>
     * </ul>
     * </p>
     * 
     * @param function
     *            a remote function to call
     * 
     * @see #call(String)
     * @see #is_running()
     * @see #set_callback(Object, String)
     */
    public void call_async(String function) {
        async_running = true;
        new call_async().execute(function);
    }

    private boolean call(String function, Bundle pArgs, Bundle pReturn) {

        if (!initialize()) {
            error_list.add("(LOCAL) Cannot bind to ApgService");
            result.putInt(ret.ERROR.name(), error.CANNOT_BIND_TO_APG.ordinal());
            return false;
        }

        if (function == null || function.length() == 0) {
            error_list.add("(LOCAL) Function to call missing");
            result.putInt(ret.ERROR.name(), error.CALL_MISSING.ordinal());
            return false;
        }

        try {
            Boolean success = (Boolean) IApgService.class.getMethod(function, Bundle.class, Bundle.class).invoke(apgService, pArgs, pReturn);
            error_list.addAll(pReturn.getStringArrayList(ret.ERRORS.name()));
            warning_list.addAll(pReturn.getStringArrayList(ret.WARNINGS.name()));
            return success;
        } catch (NoSuchMethodException e) {
            if (stacktraces)
                e.printStackTrace();
            Log.e(TAG, "Remote call not known (" + function + "): " + e.getMessage());
            error_list.add("(LOCAL) Remote call not known (" + function + "): " + e.getMessage());
            result.putInt(ret.ERROR.name(), error.CALL_NOT_KNOWN.ordinal());
            return false;
        } catch (InvocationTargetException e) {
            if (stacktraces)
                e.printStackTrace();
            Throwable orig = e.getTargetException();
            Log.w(TAG, "Exception of type '" + orig.getClass() + "' on AIDL call '" + function + "': " + orig.getMessage());
            error_list.add("(LOCAL) Exception of type '" + orig.getClass() + "' on AIDL call '" + function + "': " + orig.getMessage());
            return false;
        } catch (Exception e) {
            if (stacktraces)
                e.printStackTrace();
            Log.e(TAG, "Generic error (" + e.getClass() + "): " + e.getMessage());
            error_list.add("(LOCAL) Generic error (" + e.getClass() + "): " + e.getMessage());
            result.putInt(ret.ERROR.name(), error.GENERIC.ordinal());
            return false;
        }

    }

    /**
     * Set a string argument for APG
     * 
     * <p>
     * This defines a string argument for APG's AIDL-interface.
     * </p>
     * 
     * <p>
     * To know what key-value-pairs are possible (or required), take a look into
     * the IApgService.aidl
     * </p>
     * 
     * <p>
     * Note, that the parameters are not deleted after a call, so you have to
     * reset ({@link #clear_args()}) them manually if you want to.
     * </p>
     * 
     * 
     * @param key
     *            the key
     * @param val
     *            the value
     * 
     * @see #clear_args()
     */
    public void set_arg(String key, String val) {
        args.putString(key, val);
    }

    /**
     * Set a string-array argument for APG
     * 
     * <p>
     * If the AIDL-parameter is an {@literal ArrayList<String>}, you have to use
     * this function.
     * </p>
     * 
     * <code>
     * <pre>
     * set_arg("a key", new String[]{ "entry 1", "entry 2" });
     * </pre>
     * </code>
     * 
     * @param key
     *            the key
     * @param vals
     *            the value
     * 
     * @see #set_arg(String, String)
     */
    public void set_arg(String key, String vals[]) {
        ArrayList<String> list = new ArrayList<String>();
        for (String val : vals) {
            list.add(val);
        }
        args.putStringArrayList(key, list);
    }

    /**
     * Set up a boolean argument for APG
     * 
     * @param key
     *            the key
     * @param vals
     *            the value
     * 
     * @see #set_arg(String, String)
     */
    public void set_arg(String key, boolean val) {
        args.putBoolean(key, val);
    }

    /**
     * Set up a int argument for APG
     * 
     * @param key
     *            the key
     * @param vals
     *            the value
     * 
     * @see #set_arg(String, String)
     */
    public void set_arg(String key, int val) {
        args.putInt(key, val);
    }

    /**
     * Set up a int-array argument for APG
     * <p>
     * If the AIDL-parameter is an {@literal ArrayList<Integer>}, you have to
     * use this function.
     * </p>
     * 
     * @param key
     *            the key
     * @param vals
     *            the value
     * 
     * @see #set_arg(String, String)
     */
    public void set_arg(String key, int vals[]) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int val : vals) {
            list.add(val);
        }
        args.putIntegerArrayList(key, list);
    }

    /**
     * Clears all arguments
     * 
     * <p>
     * Anything the has been set up with the various
     * {@link #set_arg(String, String)} functions, is cleared.
     * </p>
     * <p>
     * Note, that any warning, error, callback, result etc. is not cleared with
     * this.
     * </p>
     * 
     * @see #reset()
     */
    public void clear_args() {
        args.clear();
    }

    /**
     * Return the object associated with the key
     * 
     * @param key
     *            the object's key you want to return
     * @return an object at position key, or null if not set
     */
    public Object get_arg(String key) {
        return args.get(key);
    }

    /**
     * Iterates through the errors
     * 
     * <p>
     * With this method, you can iterate through all errors. The errors are only
     * returned once and deleted immediately afterwards, so you can only return
     * each error once.
     * </p>
     * 
     * @return a human readable description of a error that happened, or null if
     *         no more errors
     * 
     * @see #has_next_error()
     * @see #clear_errors()
     */
    public String get_next_error() {
        if (error_list.size() != 0)
            return error_list.remove(0);
        else
            return null;
    }

    /**
     * Check, if there are any new errors
     * 
     * @return true, if there are unreturned errors, false otherwise
     * 
     * @see #get_next_error()
     */
    public boolean has_next_error() {
        return error_list.size() != 0;
    }

    /**
     * Get the numeric representation of the last error
     * 
     * <p>
     * Values <100 mean the error happened locally, values >=100 mean the error
     * happened at the remote side (APG). See the IApgService.aidl (or get the
     * human readable description with {@link #get_next_error()}) for what
     * errors >=100 mean.
     * </p>
     * 
     * @return the id of the error that happened
     */
    public int get_error() {
        if (result.containsKey(ret.ERROR.name()))
            return result.getInt(ret.ERROR.name());
        else
            return -1;
    }

    /**
     * Iterates through the warnings
     * 
     * <p>
     * With this method, you can iterate through all warnings. The warnings are
     * only returned once and deleted immediately afterwards, so you can only
     * return each warning once.
     * </p>
     * 
     * @return a human readable description of a warning that happened, or null
     *         if no more warnings
     * 
     * @see #has_next_warning()
     * @see #clear_warnings()
     */
    public String get_next_warning() {
        if (warning_list.size() != 0)
            return warning_list.remove(0);
        else
            return null;
    }

    /**
     * Check, if there are any new warnings
     * 
     * @return true, if there are unreturned warnings, false otherwise
     * 
     * @see #get_next_warning()
     */
    public boolean has_next_warning() {
        return warning_list.size() != 0;
    }

    /**
     * Get the result
     * 
     * <p>
     * This gets your result. After doing an encryption or decryption with APG,
     * you get the output with this function.
     * </p>
     * <p>
     * Note, that when your last remote call is unsuccessful, the result will
     * still have the same value like the last successful call (or null, if no
     * call was successful). To ensure you do not work with old call's results,
     * either be sure to {@link #reset()} (or at least {@link #clear_result()})
     * your instance before each new call or always check that
     * {@link #has_next_error()} is false.
     * </p>
     * 
     * @return the result of the last {@link #call(String)} or
     *         {@link #call_asinc(String)}.
     * 
     * @see #reset()
     * @see #clear_result()
     * @see #get_result_bundle()
     */
    public String get_result() {
        return result.getString(ret.RESULT.name());
    }

    /**
     * Get the result bundle
     * 
     * <p>
     * Unlike {@link #get_result()}, which only returns any en-/decrypted
     * message, this function returns the complete information that was returned
     * by Apg. This also includes the "RESULT", but additionally the warnings,
     * errors and any other information.
     * </p>
     * <p>
     * For warnings and errors it is suggested to use the functions that are
     * provided here, namely {@link #get_error()}, {@link #get_next_error()},
     * {@link #get_next_Warning()} etc.), but if any call returns something non
     * standard, you have access to the complete result bundle to extract the
     * information.
     * </p>
     * 
     * @return the complete result-bundle of the last call to apg
     */
    public Bundle get_result_bundle() {
        return result;
    }
    
    public error get_connection_status() {
        return connection_status;
    }

    /**
     * Clears all unfetched errors
     * 
     * @see #get_next_error()
     * @see #has_next_error()
     */
    public void clear_errors() {
        error_list.clear();
        result.remove(ret.ERROR.name());
    }

    /**
     * Clears all unfetched warnings
     * 
     * @see #get_next_warning()
     * @see #has_next_warning()
     */
    public void clear_warnings() {
        warning_list.clear();
    }

    /**
     * Clears the last result
     * 
     * @see #get_result()
     */
    public void clear_result() {
        result.remove(ret.RESULT.name());
    }

    /**
     * Set a callback object and method
     * 
     * <p>
     * After an async execution is finished, obj.meth() will be called. You can
     * use this in order to get notified, when encrypting/decrypting of long
     * data finishes and do not have to poll {@link #is_running()} in your
     * thread. Note, that if the call of the method fails for whatever reason,
     * you won't get notified in any way - so you still should check
     * {@link #is_running()} from time to time.
     * </p>
     * 
     * <p>
     * It produces a warning fetchable with {@link #get_next_warning()} when the
     * callback fails.
     * </p>
     * 
     * <pre>
     * <code>
     * .... your class ...
     * public void callback() {
     *   // do something after encryption finished
     * }
     * 
     * public void encrypt() {
     *   ApgCon mEnc = new ApgCon(context);
     *   // set parameters
     *   mEnc.set_arg(key, value);
     *   ...
     *   
     *   // set callback object and method 
     *   mEnc.set_callback( this, "callback" );
     *   
     *   // start asynchronous call
     *   mEnc.call_async( call );
     *   
     *   // when the call_async finishes, the method "callback()" will be called automatically
     * }
     * </code>
     * </pre>
     * 
     * @param obj
     *            The object, which has the public method meth
     * @param meth
     *            Method to call on the object obj
     * 
     * @see #set_callback(Object, String, boolean)
     */
    public void set_callback(Object obj, String meth) {
        set_callback(obj, meth, default_callback_return_self);
    }

    /**
     * Set a callback and whether to return self as a additional parameter
     * 
     * <p>
     * This does the same as {@link #set_callback(Object, String)} with one
     * Additionally parameter return_self.
     * </p>
     * <p>
     * The additional parameter controls, whether to return itself as a
     * parameter to the callback method meth (in order to go on working after
     * async execution has finished). This means, your callback method must have
     * one parameter of the type ApgCon.
     * </p>
     * 
     * @param obj
     *            The object, which has the public method meth
     * @param meth
     *            Method to call on the object obj
     * @param return_self
     *            Whether to return itself as an parameter to meth
     */
    public void set_callback(Object obj, String meth, boolean return_self) {
        set_callback_object(obj);
        set_callback_method(meth);
        set_callback_return_self(return_self);
    }

    /**
     * Set a callback object
     * 
     * @param obj
     *            a object to call back after async execution
     * @see #set_callback(Object, String)
     */
    public void set_callback_object(Object obj) {
        callback_object = obj;
    }

    /**
     * Set a callback method
     * 
     * @param meth
     *            a method to call on a callback object after async execution
     * @see #set_callback(Object, String)
     */
    public void set_callback_method(String meth) {
        callback_method = meth;
    }

    /**
     * Set whether to return self on callback
     * 
     * @param arg
     *            set results as param for callback method
     * @see #set_callback(Object, String)
     */
    public void set_callback_return_self(boolean arg) {
        callback_return_self = arg;
    }

    /**
     * Clears any callback object
     * 
     * @see #set_callback(Object, String)
     */
    public void clear_callback_object() {
        callback_object = null;
    }

    /**
     * Clears any callback method
     * 
     * @see #set_callback(Object, String)
     */
    public void clear_callback_method() {
        callback_method = null;
    }

    /**
     * Sets to default value of whether to return self on callback
     * 
     * @see #set_callback(Object, String, boolean)
     * @see #default_callback_return_self
     */
    public void clear_callback_return_self() {
        callback_return_self = default_callback_return_self;
    }

    /**
     * Clears anything related to callback
     * 
     * @see #set_callback(Object, String)
     */
    public void clear_callback() {
        clear_callback_object();
        clear_callback_method();
        clear_callback_return_self();
    }

    /**
     * Checks, whether an async execution is running
     * 
     * <p>
     * If you started something with {@link #call_async(String)}, this will
     * return true if the task is still running
     * </p>
     * 
     * @return true, if an async task is still running, false otherwise
     * 
     * @see #call_async(String)
     * 
     */
    public boolean is_running() {
        return async_running;
    }

    /**
     * Completely resets your instance
     * 
     * <p>
     * This currently resets everything in this instance. Errors, warnings,
     * results, callbacks, ... are removed. Any connection to the remote
     * interface is upheld, though.
     * </p>
     * 
     * <p>
     * Note, that when an async execution ({@link #call_async(String)}) is
     * running, it's result, warnings etc. will still be evaluated (which might
     * be not what you want). Also mind, that any callback you set is also
     * reseted, so on finishing the async execution any defined callback will
     * NOT BE TRIGGERED.
     * </p>
     */
    public void reset() {
        clear_errors();
        clear_warnings();
        clear_args();
        clear_callback();
        result.clear();
    }

    public ApgCon get_self() {
        return this;
    }

}
