/*
 * Copyright (C) 2011 Markus Doits <markus.doits@googlemail.com>
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
import org.thialfihar.android.apg.utils.ApgConInterface.OnCallFinishListener;

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
 * @version 1.0rc1
 * 
 */
public class ApgCon {
    private static final boolean LOCAL_LOGV = true;
    private static final boolean LOCAL_LOGD = true;

    private final static String TAG = "ApgCon";
    private final static int API_VERSION = 1; // aidl api-version it expects
    
    public int secondsToWaitForConnection = 15;

    private class CallAsync extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... arg) {
            if( LOCAL_LOGD ) Log.d(TAG, "Async execution starting");
            call(arg[0]);
            return null;
        }

        protected void onPostExecute(Void res) {
            if( LOCAL_LOGD ) Log.d(TAG, "Async execution finished");
            mAsyncRunning = false;

        }

    }


    private final Context mContext;
    private final error mConnectionStatus;
    private boolean mAsyncRunning = false;
    private OnCallFinishListener mOnCallFinishListener;

    private final Bundle mResult = new Bundle();
    private final Bundle mArgs = new Bundle();
    private final ArrayList<String> mErrorList = new ArrayList<String>();
    private final ArrayList<String> mWarningList = new ArrayList<String>();

    /** Remote service for decrypting and encrypting data */
    private IApgService mApgService = null;

    /** Set apgService accordingly to connection status */
    private ServiceConnection mApgConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if( LOCAL_LOGD ) Log.d(TAG, "IApgService bound to apgService");
            mApgService = IApgService.Stub.asInterface(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            if( LOCAL_LOGD ) Log.d(TAG, "IApgService disconnected");
            mApgService = null;
        }
    };
    
    /**
     * Different types of local errors
     */
    public static enum error {
        /**
         * no error
         */
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
        /**
         * found APG but with wrong API
         */
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
        if( LOCAL_LOGV ) Log.v(TAG, "EncryptionService created");
        mContext = ctx;

        error tmpError = null;
        try {
            if( LOCAL_LOGV ) Log.v(TAG, "Searching for the right APG version");
            ServiceInfo apgServices[] = ctx.getPackageManager().getPackageInfo("org.thialfihar.android.apg",
                    PackageManager.GET_SERVICES | PackageManager.GET_META_DATA).services;
            if (apgServices == null) {
                Log.e(TAG, "Could not fetch services");
                tmpError = error.GENERIC;
            } else {
                boolean apgServiceFound = false;
                for (ServiceInfo inf : apgServices) {
                    if( LOCAL_LOGV ) Log.v(TAG, "Found service of APG: " + inf.name);
                    if (inf.name.equals("org.thialfihar.android.apg.ApgService")) {
                        apgServiceFound = true;
                        if (inf.metaData == null) {
                            Log.w(TAG, "Could not determine ApgService API");
                            Log.w(TAG, "This probably won't work!");
                            mWarningList.add("(LOCAL) Could not determine ApgService API");
                            tmpError = error.APG_API_MISSMATCH;
                        } else if (inf.metaData.getInt("api_version") != API_VERSION) {
                            Log.w(TAG, "Found ApgService API version" + inf.metaData.getInt("api_version") + " but exspected " + API_VERSION);
                            Log.w(TAG, "This probably won't work!");
                            mWarningList.add("(LOCAL) Found ApgService API version" + inf.metaData.getInt("api_version") + " but exspected " + API_VERSION);
                            tmpError = error.APG_API_MISSMATCH;
                        } else {
                            if( LOCAL_LOGV ) Log.v(TAG, "Found api_version " + API_VERSION + ", everything should work");
                            tmpError = error.NO_ERROR;
                        }
                    }
                }

                if (!apgServiceFound) {
                    Log.e(TAG, "Could not find APG with AIDL interface, this probably won't work");
                    mErrorList.add("(LOCAL) Could not find APG with AIDL interface, this probably won't work");
                    mResult.putInt(ret.ERROR.name(), error.APG_AIDL_MISSING.ordinal());
                    tmpError = error.APG_NOT_FOUND;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not find APG, is it installed?", e);
            mErrorList.add("(LOCAL) Could not find APG, is it installed?");
            mResult.putInt(ret.ERROR.name(), error.APG_NOT_FOUND.ordinal());
            tmpError = error.APG_NOT_FOUND;
        }
        
        mConnectionStatus = tmpError;

    }

    /** try to connect to the apg service */
    private boolean connect() {
        if( LOCAL_LOGV ) Log.v(TAG, "trying to bind the apgService to context");

        if (mApgService != null) {
            if( LOCAL_LOGV ) Log.v(TAG, "allready connected");
            return true;
        }

        try {
            mContext.bindService(new Intent(IApgService.class.getName()), mApgConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(TAG, "could not bind APG service", e);
            return false;
        }

        int waitCount = 0;
        while (mApgService == null && waitCount++ < secondsToWaitForConnection) {
            if( LOCAL_LOGV ) Log.v(TAG, "sleeping 1 second to wait for apg");
            android.os.SystemClock.sleep(1000);
        }

        if (waitCount >= secondsToWaitForConnection) {
            if( LOCAL_LOGV ) Log.v(TAG, "slept waiting for nothing!");
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
        if( LOCAL_LOGV ) Log.v(TAG, "disconnecting apgService");
        if (mApgService != null) {
            mContext.unbindService(mApgConnection);
            mApgService = null;
        }
    }

    private boolean initialize() {
        if (mApgService == null) {
            if (!connect()) {
                if( LOCAL_LOGV ) Log.v(TAG, "connection to apg service failed");
                return false;
            }
        }
        return true;
    }

    /**
     * Calls a function from APG's AIDL-interface
     * 
     * <p>
     * After you have set up everything with {@link #setArg(String, String)}
     * (and variants), you can call a function from the AIDL-interface. This
     * will
     * <ul>
     * <li>start connection to the remote interface (if not already connected)</li>
     * <li>call the passed function with all set up parameters synchronously</li>
     * <li>set up everything to retrieve the mResult and/or warnings/errors</li>
     * <li>call the callback if provided
     * </ul>
     * </p>
     * 
     * <p>
     * Note your thread will be blocked during execution - if you want to call
     * the function asynchronously, see {@link #callAsync(String)}.
     * </p>
     * 
     * @param function
     *            a remote function to call
     * @return true, if call successful (= no errors), else false
     * 
     * @see #callAsync(String)
     * @see #setArg(String, String)
     * @see #setOnCallFinishListener(OnCallFinishListener)
     */
    public boolean call(String function) {
        boolean success = this.call(function, mArgs, mResult);
        if (mOnCallFinishListener != null) {
            try {
                if( LOCAL_LOGD ) Log.d(TAG, "About to execute callback");
                mOnCallFinishListener.onCallFinish(mResult);
                if( LOCAL_LOGD ) Log.d(TAG, "Callback executed");
            } catch (Exception e) {
                Log.w(TAG, "Exception on callback: (" + e.getClass() + ") " + e.getMessage(), e);
                mWarningList.add("(LOCAL) Could not execute callback (" + e.getClass() + "): " + e.getMessage());
            }
        }
        return success;
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
     * <li>In your thread, poll {@link #isRunning()}</li>
     * <li>Supply a callback with {@link #setOnCallFinishListener(OnCallFinishListener)}</li>
     * </ul>
     * </p>
     * 
     * @param function
     *            a remote function to call
     * 
     * @see #call(String)
     * @see #isRunning()
     * @see #setOnCallFinishListener(OnCallFinishListener)
     */
    public void callAsync(String function) {
        mAsyncRunning = true;
        new CallAsync().execute(function);
    }

    private boolean call(String function, Bundle pArgs, Bundle pReturn) {

        if (!initialize()) {
            mErrorList.add("(LOCAL) Cannot bind to ApgService");
            mResult.putInt(ret.ERROR.name(), error.CANNOT_BIND_TO_APG.ordinal());
            return false;
        }

        if (function == null || function.length() == 0) {
            mErrorList.add("(LOCAL) Function to call missing");
            mResult.putInt(ret.ERROR.name(), error.CALL_MISSING.ordinal());
            return false;
        }

        try {
            Boolean success = (Boolean) IApgService.class.getMethod(function, Bundle.class, Bundle.class).invoke(mApgService, pArgs, pReturn);
            mErrorList.addAll(pReturn.getStringArrayList(ret.ERRORS.name()));
            mWarningList.addAll(pReturn.getStringArrayList(ret.WARNINGS.name()));
            return success;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Remote call not known (" + function + "): " + e.getMessage(), e);
            mErrorList.add("(LOCAL) Remote call not known (" + function + "): " + e.getMessage());
            mResult.putInt(ret.ERROR.name(), error.CALL_NOT_KNOWN.ordinal());
            return false;
        } catch (InvocationTargetException e) {
            Throwable orig = e.getTargetException();
            Log.w(TAG, "Exception of type '" + orig.getClass() + "' on AIDL call '" + function + "': " + orig.getMessage(), orig);
            mErrorList.add("(LOCAL) Exception of type '" + orig.getClass() + "' on AIDL call '" + function + "': " + orig.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Generic error (" + e.getClass() + "): " + e.getMessage(), e);
            mErrorList.add("(LOCAL) Generic error (" + e.getClass() + "): " + e.getMessage());
            mResult.putInt(ret.ERROR.name(), error.GENERIC.ordinal());
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
     * reset ({@link #clearArgs()}) them manually if you want to.
     * </p>
     * 
     * 
     * @param key
     *            the key
     * @param val
     *            the value
     * 
     * @see #clearArgs()
     */
    public void setArg(String key, String val) {
        mArgs.putString(key, val);
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
     * setArg("a key", new String[]{ "entry 1", "entry 2" });
     * </pre>
     * </code>
     * 
     * @param key
     *            the key
     * @param vals
     *            the value
     * 
     * @see #setArg(String, String)
     */
    public void setArg(String key, String vals[]) {
        ArrayList<String> list = new ArrayList<String>();
        for (String val : vals) {
            list.add(val);
        }
        mArgs.putStringArrayList(key, list);
    }

    /**
     * Set up a boolean argument for APG
     * 
     * @param key
     *            the key
     * @param vals
     *            the value
     * 
     * @see #setArg(String, String)
     */
    public void setArg(String key, boolean val) {
        mArgs.putBoolean(key, val);
    }

    /**
     * Set up a int argument for APG
     * 
     * @param key
     *            the key
     * @param vals
     *            the value
     * 
     * @see #setArg(String, String)
     */
    public void setArg(String key, int val) {
        mArgs.putInt(key, val);
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
     * @see #setArg(String, String)
     */
    public void setArg(String key, int vals[]) {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int val : vals) {
            list.add(val);
        }
        mArgs.putIntegerArrayList(key, list);
    }

    /**
     * Clears all arguments
     * 
     * <p>
     * Anything the has been set up with the various
     * {@link #setArg(String, String)} functions, is cleared.
     * </p>
     * <p>
     * Note, that any warning, error, callback, mResult, etc. is not cleared with
     * this.
     * </p>
     * 
     * @see #reset()
     */
    public void clearArgs() {
        mArgs.clear();
    }

    /**
     * Return the object associated with the key
     * 
     * @param key
     *            the object's key you want to return
     * @return an object at position key, or null if not set
     */
    public Object getArg(String key) {
        return mArgs.get(key);
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
     * @see #hasNextError()
     * @see #clearErrors()
     */
    public String getNextError() {
        if (mErrorList.size() != 0)
            return mErrorList.remove(0);
        else
            return null;
    }

    /**
     * Check, if there are any new errors
     * 
     * @return true, if there are unreturned errors, false otherwise
     * 
     * @see #getNextError()
     */
    public boolean hasNextError() {
        return mErrorList.size() != 0;
    }

    /**
     * Get the numeric representation of the last error
     * 
     * <p>
     * Values <100 mean the error happened locally, values >=100 mean the error
     * happened at the remote side (APG). See the IApgService.aidl (or get the
     * human readable description with {@link #getNextError()}) for what
     * errors >=100 mean.
     * </p>
     * 
     * @return the id of the error that happened
     */
    public int getError() {
        if (mResult.containsKey(ret.ERROR.name()))
            return mResult.getInt(ret.ERROR.name());
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
     * @see #hasNextWarning()
     * @see #clearWarnings()
     */
    public String getNextWarning() {
        if (mWarningList.size() != 0)
            return mWarningList.remove(0);
        else
            return null;
    }

    /**
     * Check, if there are any new warnings
     * 
     * @return true, if there are unreturned warnings, false otherwise
     * 
     * @see #getNextWarning()
     */
    public boolean hasNextWarning() {
        return mWarningList.size() != 0;
    }

    /**
     * Get the result
     * 
     * <p>
     * This gets your mResult. After doing an encryption or decryption with APG,
     * you get the output with this function.
     * </p>
     * <p>
     * Note, that when your last remote call is unsuccessful, the mResult will
     * still have the same value like the last successful call (or null, if no
     * call was successful). To ensure you do not work with old call's mResults,
     * either be sure to {@link #reset()} (or at least {@link #clearResult()})
     * your instance before each new call or always check that
     * {@link #hasNextError()} is false.
     * </p>
     * 
     * @return the mResult of the last {@link #call(String)} or
     *         {@link #call_asinc(String)}.
     * 
     * @see #reset()
     * @see #clearResult()
     * @see #getResultBundle()
     */
    public String getResult() {
        return mResult.getString(ret.RESULT.name());
    }

    /**
     * Get the mResult bundle
     * 
     * <p>
     * Unlike {@link #getResult()}, which only returns any en-/decrypted
     * message, this function returns the complete information that was returned
     * by Apg. This also includes the "RESULT", but additionally the warnings,
     * errors and any other information.
     * </p>
     * <p>
     * For warnings and errors it is suggested to use the functions that are
     * provided here, namely {@link #getError()}, {@link #getNextError()},
     * {@link #get_next_Warning()} etc.), but if any call returns something non
     * standard, you have access to the complete mResult bundle to extract the
     * information.
     * </p>
     * 
     * @return the complete mResult-bundle of the last call to apg
     */
    public Bundle getResultBundle() {
        return mResult;
    }

    public error getConnectionStatus() {
        return mConnectionStatus;
    }

    /**
     * Clears all unfetched errors
     * 
     * @see #getNextError()
     * @see #hasNextError()
     */
    public void clearErrors() {
        mErrorList.clear();
        mResult.remove(ret.ERROR.name());
    }

    /**
     * Clears all unfetched warnings
     * 
     * @see #getNextWarning()
     * @see #hasNextWarning()
     */
    public void clearWarnings() {
        mWarningList.clear();
    }

    /**
     * Clears the last mResult
     * 
     * @see #getResult()
     */
    public void clearResult() {
        mResult.remove(ret.RESULT.name());
    }

    /**
     * Set a callback listener when call to AIDL finishes
     * 
     * @param obj
     *            a object to call back after async execution
     * @see ApgConInterface
     */
    public void setOnCallFinishListener(OnCallFinishListener lis) {
        mOnCallFinishListener = lis;
    }

    /**
     * Clears any callback object
     * 
     * @see #setOnCallFinishListener(OnCallFinishListener)
     */
    public void clearOnCallFinishListener() {
        mOnCallFinishListener = null;
    }

    /**
     * Checks, whether an async execution is running
     * 
     * <p>
     * If you started something with {@link #callAsync(String)}, this will
     * return true if the task is still running
     * </p>
     * 
     * @return true, if an async task is still running, false otherwise
     * 
     * @see #callAsync(String)
     * 
     */
    public boolean isRunning() {
        return mAsyncRunning;
    }

    /**
     * Completely resets your instance
     * 
     * <p>
     * This currently resets everything in this instance. Errors, warnings,
     * mResults, callbacks, ... are removed. Any connection to the remote
     * interface is upheld, though.
     * </p>
     * 
     * <p>
     * Note, that when an async execution ({@link #callAsync(String)}) is
     * running, it's mResult, warnings etc. will still be evaluated (which might
     * be not what you want). Also mind, that any callback you set is also
     * reseted, so on finishing the execution any before defined callback will
     * NOT BE TRIGGERED.
     * </p>
     */
    public void reset() {
        clearErrors();
        clearWarnings();
        clearArgs();
        clearOnCallFinishListener();
        mResult.clear();
    }

}
