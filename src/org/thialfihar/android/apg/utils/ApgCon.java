package org.thialfihar.android.apg.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.thialfihar.android.apg.IApgService;

/**
 * This class can be used by other projects to simplify connecting to the
 * APG-Service. Kind of wrapper of for AIDL.
 * 
 * It is not used in this project.
 */
public class ApgCon {

    private String TAG = "ApgCon";

    private final Context mContext;

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

    /** Possible fields which are returned to the application */
    public static enum retKey {
        ERROR, // error enum, see below
        ERROR_DESC, // human readable description
        RESULT, // if everything went fine, result
    }

    public static enum error {
        GENERIC, // no special type
        CANNOT_BIND_TO_APG, // connection to apg service not possible
        CALL_MISSING, // function to call not provided
        CALL_NOT_KNOWN, // apg service does not know what to do
    }

    public ApgCon(Context ctx) {
        Log.d(TAG, "EncryptionService created");
        mContext = ctx;
    }

    /** try to connect to the apg service */
    private boolean connect() {
        Log.d(TAG, "trying to bind the apgService to context");

        if (apgService != null) {
            Log.d(TAG, "allready connected");
            return true;
        }

        try {
            mContext.bindService(new Intent(IApgService.class.getName()), apgConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.d(TAG, "could not bind APG service");
            return false;
        }

        int wait_count = 0;
        while (apgService == null && wait_count++ < 15) {
            Log.d(TAG, "sleeping 1 second to wait for apg");
            android.os.SystemClock.sleep(1000);
        }
        ;

        if (wait_count >= 15) {
            Log.d(TAG, "slept waiting for nothing!");
            return false;
        }

        return true;
    }

    private boolean initialize() {
        if (apgService == null) {
            if (!connect()) {
                Log.d(TAG, "connection to apg service failed");
                return false;
            }
        }
        return true;
    }

    public boolean call(String function, Map<retKey, Object> return_map, String... function_params) {

        if (!initialize()) {
            return_map.put(retKey.ERROR, error.CANNOT_BIND_TO_APG);
            return false;
        }

        if (function == null || function.length() == 0) {
            return_map.put(retKey.ERROR, error.CALL_MISSING);
            return false;
        }

        try {
            List<String> params_list = Arrays.asList(function_params);
            return_map.put(retKey.RESULT, IApgService.class.getMethod(function, List.class).invoke(apgService, params_list));
        } catch (NoSuchMethodException e) {
            Log.d(TAG, e.getMessage());
            return_map.put(retKey.ERROR, error.CALL_NOT_KNOWN);
            return_map.put(retKey.ERROR_DESC, e.getMessage());
            return false;
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            return_map.put(retKey.ERROR, error.GENERIC);
            return_map.put(retKey.ERROR_DESC, e.getMessage());
            return false;
        }

        return true;

    }

    private void disconnect() {
        Log.d(TAG, "disconnecting apgService");
        if (apgService != null) {
            mContext.unbindService(apgConnection);
            apgService = null;
        }
    }

}
