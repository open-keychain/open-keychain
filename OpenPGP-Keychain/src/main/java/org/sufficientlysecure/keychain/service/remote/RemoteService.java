/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.service.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpConstants;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.PausableThreadPoolExecutor;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

/**
 * Abstract service class for remote APIs that handle app registration and user input.
 */
public abstract class RemoteService extends Service {
    Context mContext;

    private final ArrayBlockingQueue<Runnable> mPoolQueue = new ArrayBlockingQueue<Runnable>(100);
    // TODO: Are these parameters okay?
    private PausableThreadPoolExecutor mThreadPool = new PausableThreadPoolExecutor(2, 4, 10,
            TimeUnit.SECONDS, mPoolQueue);

    private final Object userInputLock = new Object();

    private static final int PRIVATE_REQUEST_CODE_REGISTER = 651;
    private static final int PRIVATE_REQUEST_CODE_ERROR = 652;


    /**
     * Extends Handler.Callback with OKAY (1), CANCEL (0) variables
     */
    private class BaseCallback implements Handler.Callback {
        public static final int OKAY = 1;
        public static final int CANCEL = 0;

        @Override
        public boolean handleMessage(Message msg) {
            return false;
        }

    }

    public Context getContext() {
        return mContext;
    }

    protected Bundle isAllowed(Bundle params) {
        try {
            if (isCallerAllowed(false)) {
//                mThreadPool.execute(r);

                return null;
//                Log.d(Constants.TAG, "Enqueued runnable…");
            } else {
                String[] callingPackages = getPackageManager().getPackagesForUid(
                        Binder.getCallingUid());
                // TODO: currently simply uses first entry
                String packageName = callingPackages[0];

                byte[] packageSignature;
                try {
                    packageSignature = getPackageSignature(packageName);
                } catch (NameNotFoundException e) {
                    Log.e(Constants.TAG, "Should not happen, returning!", e);
                    // return error
                    Bundle result = new Bundle();
                    result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_ERROR);
                    result.putParcelable(OpenPgpConstants.RESULT_ERRORS,
                            new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
                    return result;
                }
                Log.e(Constants.TAG, "Not allowed to use service! return PendingIntent for registration!");

                Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
                intent.setAction(RemoteServiceActivity.ACTION_REGISTER);
                intent.putExtra(RemoteServiceActivity.EXTRA_PACKAGE_NAME, packageName);
                intent.putExtra(RemoteServiceActivity.EXTRA_PACKAGE_SIGNATURE, packageSignature);
                intent.putExtra(OpenPgpConstants.PI_RESULT_PARAMS, params);

                PendingIntent pi = PendingIntent.getActivity(getBaseContext(), PRIVATE_REQUEST_CODE_REGISTER, intent, 0);

                // return PendingIntent to be executed by client
                Bundle result = new Bundle();
                result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_USER_INTERACTION_REQUIRED);
                result.putParcelable(OpenPgpConstants.RESULT_INTENT, pi);

//                pauseAndStartUserInteraction(RemoteServiceActivity.ACTION_REGISTER, callback,
//                        extras);
                return result;
//                if (callback.isAllowed()) {
//                    mThreadPool.execute(r);
//                    Log.d(Constants.TAG, "Enqueued runnable…");
//                } else {
//                    Log.d(Constants.TAG, "User disallowed app!");
//                }
            }
        } catch (WrongPackageSignatureException e) {
            Log.e(Constants.TAG, "wrong signature!", e);

            Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
            intent.setAction(RemoteServiceActivity.ACTION_ERROR_MESSAGE);
            intent.putExtra(RemoteServiceActivity.EXTRA_ERROR_MESSAGE, getString(R.string.api_error_wrong_signature));
            intent.putExtra(OpenPgpConstants.PI_RESULT_PARAMS, params);

            PendingIntent pi = PendingIntent.getActivity(getBaseContext(), PRIVATE_REQUEST_CODE_ERROR, intent, 0);

            // return PendingIntent to be executed by client
            Bundle result = new Bundle();
            result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_USER_INTERACTION_REQUIRED);
            result.putParcelable(OpenPgpConstants.RESULT_INTENT, pi);

            return result;

//            Bundle extras = new Bundle();
//            extras.putString(RemoteServiceActivity.EXTRA_ERROR_MESSAGE,
//                    getString(R.string.api_error_wrong_signature));
//            pauseAndStartUserInteraction(RemoteServiceActivity.ACTION_ERROR_MESSAGE, null, extras);
        }
    }

    private byte[] getPackageSignature(String packageName) throws NameNotFoundException {
        PackageInfo pkgInfo = getPackageManager().getPackageInfo(packageName,
                PackageManager.GET_SIGNATURES);
        Signature[] signatures = pkgInfo.signatures;
        // TODO: Only first signature?!
        byte[] packageSignature = signatures[0].toByteArray();

        return packageSignature;
    }

    /**
     * Locks current thread and pauses execution of runnables and starts activity for user input
     */
    protected void pauseAndStartUserInteraction(String action, BaseCallback callback, Bundle extras) {
        synchronized (userInputLock) {
            mThreadPool.pause();

            Log.d(Constants.TAG, "starting activity...");
            Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(action);

            Messenger messenger = new Messenger(new Handler(getMainLooper(), callback));

            extras.putParcelable(RemoteServiceActivity.EXTRA_MESSENGER, messenger);
            intent.putExtras(extras);

            startActivity(intent);

            // lock current thread for user input
            try {
                userInputLock.wait();
            } catch (InterruptedException e) {
                Log.e(Constants.TAG, "CryptoService", e);
            }
        }
    }

    /**
     * Retrieves AppSettings from database for the application calling this remote service
     *
     * @return
     */
    protected AppSettings getAppSettings() {
        String[] callingPackages = getPackageManager().getPackagesForUid(Binder.getCallingUid());

        // get app settings for this package
        for (int i = 0; i < callingPackages.length; i++) {
            String currentPkg = callingPackages[i];

            Uri uri = KeychainContract.ApiApps.buildByPackageNameUri(currentPkg);

            AppSettings settings = ProviderHelper.getApiAppSettings(this, uri);

            if (settings != null)
                return settings;
        }

        return null;
    }

    class RegisterActivityCallback extends BaseCallback {
        public static final String PACKAGE_NAME = "package_name";

        private boolean allowed = false;
        private String packageName;

        public boolean isAllowed() {
            return allowed;
        }

        public String getPackageName() {
            return packageName;
        }

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.arg1 == OKAY) {
                allowed = true;
                packageName = msg.getData().getString(PACKAGE_NAME);

                // resume threads
                try {
                    if (isPackageAllowed(packageName)) {
                        synchronized (userInputLock) {
                            userInputLock.notifyAll();
                        }
                        mThreadPool.resume();
                    } else {
                        // Should not happen!
                        Log.e(Constants.TAG, "Should not happen! Emergency shutdown!");
                        mThreadPool.shutdownNow();
                    }
                } catch (WrongPackageSignatureException e) {
                    Log.e(Constants.TAG, e.getMessage());

                    Bundle extras = new Bundle();
                    extras.putString(RemoteServiceActivity.EXTRA_ERROR_MESSAGE,
                            getString(R.string.api_error_wrong_signature));
                    pauseAndStartUserInteraction(RemoteServiceActivity.ACTION_ERROR_MESSAGE, null,
                            extras);
                }
            } else {
                allowed = false;

                synchronized (userInputLock) {
                    userInputLock.notifyAll();
                }
                mThreadPool.resume();
            }
            return true;
        }

    }

    /**
     * Checks if process that binds to this service (i.e. the package name corresponding to the
     * process) is in the list of allowed package names.
     *
     * @param allowOnlySelf allow only Keychain app itself
     * @return true if process is allowed to use this service
     * @throws WrongPackageSignatureException
     */
    private boolean isCallerAllowed(boolean allowOnlySelf) throws WrongPackageSignatureException {
        return isUidAllowed(Binder.getCallingUid(), allowOnlySelf);
    }

    private boolean isUidAllowed(int uid, boolean allowOnlySelf)
            throws WrongPackageSignatureException {
        if (android.os.Process.myUid() == uid) {
            return true;
        }
        if (allowOnlySelf) { // barrier
            return false;
        }

        String[] callingPackages = getPackageManager().getPackagesForUid(uid);

        // is calling package allowed to use this service?
        for (int i = 0; i < callingPackages.length; i++) {
            String currentPkg = callingPackages[i];

            if (isPackageAllowed(currentPkg)) {
                return true;
            }
        }

        Log.d(Constants.TAG, "Caller is NOT allowed!");
        return false;
    }

    /**
     * Checks if packageName is a registered app for the API. Does not return true for own package!
     *
     * @param packageName
     * @return
     * @throws WrongPackageSignatureException
     */
    private boolean isPackageAllowed(String packageName) throws WrongPackageSignatureException {
        Log.d(Constants.TAG, "packageName: " + packageName);

        ArrayList<String> allowedPkgs = ProviderHelper.getRegisteredApiApps(this);
        Log.d(Constants.TAG, "allowed: " + allowedPkgs);

        // check if package is allowed to use our service
        if (allowedPkgs.contains(packageName)) {
            Log.d(Constants.TAG, "Package is allowed! packageName: " + packageName);

            // check package signature
            byte[] currentSig;
            try {
                currentSig = getPackageSignature(packageName);
            } catch (NameNotFoundException e) {
                throw new WrongPackageSignatureException(e.getMessage());
            }

            byte[] storedSig = ProviderHelper.getApiAppSignature(this, packageName);
            if (Arrays.equals(currentSig, storedSig)) {
                Log.d(Constants.TAG,
                        "Package signature is correct! (equals signature from database)");
                return true;
            } else {
                throw new WrongPackageSignatureException(
                        "PACKAGE NOT ALLOWED! Signature wrong! (Signature not equals signature from database)");
            }
        }

        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

}
