/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.remote_api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.openintents.crypto.CryptoError;
import org.openintents.crypto.CryptoSignatureResult;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.helper.PgpMain;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote_api.IServiceActivityCallback;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.util.PausableThreadPoolExecutor;

import org.openintents.crypto.ICryptoCallback;
import org.openintents.crypto.ICryptoService;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

public class CryptoService extends Service {
    Context mContext;

    // just one pool of 4 threads, pause on every user action needed
    final ArrayBlockingQueue<Runnable> mPoolQueue = new ArrayBlockingQueue<Runnable>(20);
    PausableThreadPoolExecutor mThreadPool = new PausableThreadPoolExecutor(2, 4, 10,
            TimeUnit.SECONDS, mPoolQueue);

    public static final String ACTION_SERVICE_ACTIVITY = "org.sufficientlysecure.keychain.crypto_provider.IServiceActivityCallback";

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d(Constants.TAG, "CryptoService, onCreate()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "CryptoService, onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // return different binder for connections from internal service activity
        if (ACTION_SERVICE_ACTIVITY.equals(intent.getAction())) {
            String callingPackageName = intent.getPackage();

            // this binder can only be used from OpenPGP Keychain
            if (callingPackageName.equals(Constants.PACKAGE_NAME)) {
                return mBinderServiceActivity;
            } else {
                Log.e(Constants.TAG, "This binder can only be used from " + Constants.PACKAGE_NAME);
                return null;
            }
        } else {
            return mBinder;
        }
    }

    private synchronized void encryptSafe(byte[] inputBytes, String[] encryptionUserIds,
            ICryptoCallback callback) throws RemoteException {
        try {
            // build InputData and write into OutputStream
            InputStream inputStream = new ByteArrayInputStream(inputBytes);
            long inputLength = inputBytes.length;
            InputData inputData = new InputData(inputStream, inputLength);

            OutputStream outStream = new ByteArrayOutputStream();

            // TODO: hardcoded...
            boolean useAsciiArmor = true;
            int compressionId = 2; // zlib

            // PgpMain.encryptAndSign(this, this, inputData, outStream, useAsciiArmor,
            // compressionId, encryptionKeyIds, encryptionPassphrase, Preferences
            // .getPreferences(this).getDefaultEncryptionAlgorithm(),
            // secretKeyId,
            // Preferences.getPreferences(this).getDefaultHashAlgorithm(), Preferences
            // .getPreferences(this).getForceV3Signatures(),
            // PassphraseCacheService.getCachedPassphrase(this, secretKeyId));

            outStream.close();
        } catch (Exception e) {
            Log.e(Constants.TAG, "KeychainService, Exception!", e);

            try {
                callback.onError(new CryptoError(0, e.getMessage()));
            } catch (Exception t) {
                Log.e(Constants.TAG, "Error returning exception to client", t);
            }
        }
    }

    private synchronized void decryptAndVerifySafe(byte[] inputBytes, ICryptoCallback callback)
            throws RemoteException {
        try {
            // build InputData and write into OutputStream
            InputStream inputStream = new ByteArrayInputStream(inputBytes);
            long inputLength = inputBytes.length;
            InputData inputData = new InputData(inputStream, inputLength);

            OutputStream outputStream = new ByteArrayOutputStream();

            long secretKeyId = PgpMain.getDecryptionKeyId(mContext, inputStream);
            if (secretKeyId == Id.key.none) {
                throw new PgpMain.PgpGeneralException(getString(R.string.error_noSecretKeyFound));
            }

            Log.d(Constants.TAG, "Got input:\n" + new String(inputBytes));

            Log.d(Constants.TAG, "secretKeyId " + secretKeyId);

            String passphrase = PassphraseCacheService.getCachedPassphrase(mContext, secretKeyId);

            if (passphrase == null) {
                Log.d(Constants.TAG, "No passphrase! Activity required!");

                // start passphrase dialog
                Bundle extras = new Bundle();
                extras.putLong(ServiceActivity.EXTRA_SECRET_KEY_ID, secretKeyId);
                pauseQueueAndStartServiceActivity(ServiceActivity.ACTION_CACHE_PASSPHRASE, extras);
            }

            // if (signedOnly) {
            // resultData = PgpMain.verifyText(this, this, inputData, outStream,
            // lookupUnknownKey);
            // } else {
            // resultData = PgpMain.decryptAndVerify(this, this, inputData, outStream,
            // PassphraseCacheService.getCachedPassphrase(this, secretKeyId),
            // assumeSymmetricEncryption);
            // }

            Bundle outputBundle = PgpMain.decryptAndVerify(mContext, null, inputData, outputStream,
                    passphrase, false);

            outputStream.close();

            byte[] outputBytes = ((ByteArrayOutputStream) outputStream).toByteArray();

            // get signature informations from bundle
            boolean signature = outputBundle.getBoolean(KeychainIntentService.RESULT_SIGNATURE);
            long signatureKeyId = outputBundle
                    .getLong(KeychainIntentService.RESULT_SIGNATURE_KEY_ID);
            String signatureUserId = outputBundle
                    .getString(KeychainIntentService.RESULT_SIGNATURE_USER_ID);
            boolean signatureSuccess = outputBundle
                    .getBoolean(KeychainIntentService.RESULT_SIGNATURE_SUCCESS);
            boolean signatureUnknown = outputBundle
                    .getBoolean(KeychainIntentService.RESULT_SIGNATURE_UNKNOWN);

            CryptoSignatureResult sigResult = new CryptoSignatureResult(signatureUserId, signature,
                    signatureSuccess, signatureUnknown);

            // return over handler on client side
            callback.onSuccess(outputBytes, sigResult);
        } catch (Exception e) {
            Log.e(Constants.TAG, "KeychainService, Exception!", e);

            try {
                callback.onError(new CryptoError(0, e.getMessage()));
            } catch (Exception t) {
                Log.e(Constants.TAG, "Error returning exception to client", t);
            }
        }
    }

    private final ICryptoService.Stub mBinder = new ICryptoService.Stub() {

        @Override
        public void encrypt(final byte[] inputBytes, final String[] encryptionUserIds,
                final ICryptoCallback callback) throws RemoteException {

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        encryptSafe(inputBytes, encryptionUserIds, callback);
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "CryptoService", e);
                    }
                }
            };

            checkAndEnqueue(r);
        }

        @Override
        public void encryptAndSign(byte[] inputBytes, String[] encryptionUserIds,
                String signatureUserId, ICryptoCallback callback) throws RemoteException {
            // TODO Auto-generated method stub

        }

        @Override
        public void sign(byte[] inputBytes, String signatureUserId, ICryptoCallback callback)
                throws RemoteException {
            // TODO Auto-generated method stub

        }

        @Override
        public void decryptAndVerify(final byte[] inputBytes, final ICryptoCallback callback)
                throws RemoteException {

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        decryptAndVerifySafe(inputBytes, callback);
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "CryptoService", e);
                    }
                }
            };

            checkAndEnqueue(r);
        }

        @Override
        public void setup(boolean asciiArmor, boolean newKeyring, String newKeyringUserId)
                throws RemoteException {
            // TODO Auto-generated method stub
            
        }

    };

    private final IServiceActivityCallback.Stub mBinderServiceActivity = new IServiceActivityCallback.Stub() {

        @Override
        public void onRegistered(boolean success, String packageName) throws RemoteException {

            if (success) {
                // resume threads
                if (isPackageAllowed(packageName)) {
                    mThreadPool.resume();
                } else {
                    // TODO: should not happen?
                }
            } else {
                // TODO
                mPoolQueue.clear();
            }

        }

        @Override
        public void onCachedPassphrase(boolean success) throws RemoteException {

        }

    };

    private void checkAndEnqueue(Runnable r) {
        if (isCallerAllowed()) {
            mThreadPool.execute(r);

            Log.d(Constants.TAG, "Enqueued runnable…");
        } else {
            String[] callingPackages = getPackageManager()
                    .getPackagesForUid(Binder.getCallingUid());

            Log.e(Constants.TAG, "Not allowed to use service! Starting activity for registration!");
            Bundle extras = new Bundle();
            // TODO: currently simply uses first entry
            extras.putString(ServiceActivity.EXTRA_PACKAGE_NAME, callingPackages[0]);
            pauseQueueAndStartServiceActivity(ServiceActivity.ACTION_REGISTER, extras);

            mThreadPool.execute(r);

            Log.d(Constants.TAG, "Enqueued runnable…");
        }
    }

    /**
     * Checks if process that binds to this service (i.e. the package name corresponding to the
     * process) is in the list of allowed package names.
     * 
     * @return true if process is allowed to use this service
     */
    private boolean isCallerAllowed() {
        String[] callingPackages = getPackageManager().getPackagesForUid(Binder.getCallingUid());

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

    private boolean isPackageAllowed(String packageName) {
        Log.d(Constants.TAG, "packageName: " + packageName);

        ArrayList<String> allowedPkgs = ProviderHelper.getCryptoConsumers(mContext);
        Log.d(Constants.TAG, "allowed: " + allowedPkgs);

        // check if package is allowed to use our service
        if (allowedPkgs.contains(packageName)) {
            Log.d(Constants.TAG, "Package is allowed! packageName: " + packageName);

            return true;
        } else if (Constants.PACKAGE_NAME.equals(packageName)) {
            Log.d(Constants.TAG, "Package is OpenPGP Keychain! -> allowed!");

            return true;
        }

        return false;
    }

    private void pauseQueueAndStartServiceActivity(String action, Bundle extras) {
        mThreadPool.pause();

        Log.d(Constants.TAG, "starting activity...");
        Intent intent = new Intent(getBaseContext(), ServiceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(action);
        if (extras != null) {
            intent.putExtras(extras);
        }
        getApplication().startActivity(intent);
    }

}
