package org.thialfihar.android.apg.utils;

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
 * This class can be used by other projects to simplify connecting to the
 * APG-Service. Kind of wrapper of for AIDL.
 * It is not used in this project.
 */
public class ApgCon {

    public class call_async extends AsyncTask<String, Void, Void> {

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
                    callback_object.getClass().getMethod(callback_method).invoke(callback_object);
                    Log.d(TAG, "Callback executed");
                } catch (NoSuchMethodException e) {
                    Log.w(TAG, "Exception in callback: Method '" + callback_method + "' not found");
                    warning_list.add("LOCAL: Could not execute callback, method '" + callback_method + "' not found");
                } catch (Exception e) {
                    Log.w(TAG, "Exception on callback: " + e.getMessage());
                    warning_list.add("LOCAL: Could not execute callback");
                }
            }
        }

    }

    private final static String TAG = "ApgCon";
    private final static int api_version = 1; // aidl api-version it expects

    private final Context mContext;
    private boolean async_running = false;
    private Object callback_object;
    private String callback_method;

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

    public static enum error {
        GENERIC, // no special type
        CANNOT_BIND_TO_APG, // connection to apg service not possible
        CALL_MISSING, // function to call not provided
        CALL_NOT_KNOWN, // apg service does not know what to do
    }

    public ApgCon(Context ctx) {
        Log.v(TAG, "EncryptionService created");
        mContext = ctx;

        try {
            Log.v(TAG, "Searching for the right APG version");
            ServiceInfo apg_services[] = ctx.getPackageManager().getPackageInfo("org.thialfihar.android.apg",
                    PackageManager.GET_SERVICES | PackageManager.GET_META_DATA).services;
            if (apg_services == null) {
                Log.e(TAG, "Could not fetch services");
            } else {
                boolean apg_service_found = false;
                for (ServiceInfo inf : apg_services) {
                    Log.v(TAG, "Found service of APG: " + inf.name);
                    if (inf.name.equals("org.thialfihar.android.apg.ApgService")) {
                        apg_service_found = true;
                        if (inf.metaData == null) {
                            Log.w(TAG, "Could not determine ApgService API");
                            Log.w(TAG, "This probably won't work!");
                        } else if (inf.metaData.getInt("api_version") != api_version) {
                            Log.w(TAG, "Found ApgService API version" + inf.metaData.getInt("api_version") + " but exspected " + api_version);
                            Log.w(TAG, "This probably won't work!");
                        } else {
                            Log.v(TAG, "Found api_version " + api_version + ", everything should work");
                        }
                    }
                }

                if (!apg_service_found) {
                    Log.e(TAG, "Could not find APG with AIDL interface, this probably won't work");
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find APG, is it installed?");
        }
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

    public boolean call(String function) {
        return this.call(function, args, result);
    }

    public void call_async(String function) {
        async_running = true;
        new call_async().execute(function);
    }

    public boolean call(String function, Bundle pArgs) {
        return this.call(function, pArgs, result);
    }

    public boolean call(String function, Bundle pArgs, Bundle pReturn) {

        error_list.clear();
        warning_list.clear();

        if (!initialize()) {
            error_list.add("LOCAL: Cannot bind to ApgService");
            pReturn.putInt("LOCAL_ERROR", error.CANNOT_BIND_TO_APG.ordinal());
            return false;
        }

        if (function == null || function.length() == 0) {
            error_list.add("LOCAL: Function to call missing");
            pReturn.putInt("LOCAL_ERROR", error.CALL_MISSING.ordinal());
            return false;
        }

        try {
            Boolean ret = (Boolean) IApgService.class.getMethod(function, Bundle.class, Bundle.class).invoke(apgService, pArgs, pReturn);
            error_list.addAll(pReturn.getStringArrayList("ERRORS"));
            warning_list.addAll(pReturn.getStringArrayList("WARNINGS"));
            return ret;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, e.getMessage());
            error_list.add("LOCAL: " + e.getMessage());
            pReturn.putInt("LOCAL_ERROR", error.CALL_NOT_KNOWN.ordinal());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "" + e.getMessage());
            error_list.add("LOCAL: " + e.getMessage());
            pReturn.putInt("LOCAL_ERROR", error.GENERIC.ordinal());
            return false;
        }

    }

    public void set_arg(String key, String val) {
        args.putString(key, val);
    }

    public void set_arg(String key, String vals[]) {
        ArrayList<String> list = new ArrayList<String>();
        for (String val : vals) {
            list.add(val);
        }
        set_arg(key, list);
    }

    public void set_arg(String key, ArrayList<String> vals) {
        args.putStringArrayList(key, vals);
    }

    public void set_arg(String key, boolean val) {
        args.putBoolean(key, val);
    }

    public void set_arg(String key, int val) {
        args.putInt(key, val);
    }

    public void set_arg(String key, int vals[]) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int val : vals) {
            list.add(val);
        }
        args.putIntegerArrayList(key, list);
    }

    public void clear_args() {
        args.clear();
    }

    public Object get_arg(String key) {
        return args.get(key);
    }

    public String get_next_error() {
        if (error_list.size() != 0)
            return error_list.remove(0);
        else
            return null;
    }

    public boolean has_next_error() {
        return error_list.size() != 0;
    }

    public String get_next_warning() {
        if (warning_list.size() != 0)
            return warning_list.remove(0);
        else
            return null;
    }

    public boolean has_next_warning() {
        return warning_list.size() != 0;
    }

    public String get_result() {
        return result.getString("RESULT");
    }

    public void clear_errors() {
        error_list.clear();
    }

    public void clear_warnings() {
        warning_list.clear();
    }

    public void clear_result() {
        result.clear();
    }

    /**
     * Set a callback object and method
     * 
     * <p>After an async execution is finished, obj.meth() will be called. You can
     * use this in order to get notified, when encrypting/decrypting of long
     * data finishes and do not have to poll {@link #is_running()} in your
     * thread. Note, that if the call of the method fails for whatever reason,
     * you won't get notified in any way - so you still should check
     * {@link #is_running()} from time to time.</p>
     * 
     * <p>It produces a warning fetchable with {@link #get_next_warning()} when the callback fails.</p>
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
     */
    public void set_callback(Object obj, String meth) {
        set_callback_object(obj);
        set_callback_method(meth);
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

    public void clear_callback_object() {
        callback_object = null;
    }

    public void clear_callback_method() {
        callback_method = null;
    }

    public boolean is_running() {
        return async_running;
    }

    public void reset() {
        clear_errors();
        clear_warnings();
        clear_args();
        clear_result();
        clear_callback_object();
        clear_callback_method();
    }

}
