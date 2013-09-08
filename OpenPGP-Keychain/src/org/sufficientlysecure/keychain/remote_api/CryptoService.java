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
import org.openintents.crypto.ICryptoCallback;
import org.openintents.crypto.ICryptoService;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.PgpMain;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.PausableThreadPoolExecutor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
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

            // this binder can only be used from OpenPGP Keychain
            if (isCallerAllowed(true)) {
                return mBinderServiceActivity;
            } else {
                Log.e(Constants.TAG, "This binder can only be used from " + Constants.PACKAGE_NAME);
                return null;
            }
        } else {
            return mBinder;
        }
    }

    private String getCachedPassphrase(long keyId) {
        String passphrase = PassphraseCacheService.getCachedPassphrase(mContext, keyId);

        if (passphrase == null) {
            Log.d(Constants.TAG, "No passphrase! Activity required!");

            // start passphrase dialog
            Bundle extras = new Bundle();
            extras.putLong(CryptoServiceActivity.EXTRA_SECRET_KEY_ID, keyId);
            pauseQueueAndStartServiceActivity(CryptoServiceActivity.ACTION_CACHE_PASSPHRASE, extras);
        }

        return passphrase;
    }

    /**
     * Search database for key ids based on emails.
     * 
     * @param encryptionUserIds
     * @return
     */
    private ArrayList<Long> getKeyIdsFromEmails(String[] encryptionUserIds) {
        // find key ids to given emails in database
        boolean manySameUserIds = false;
        boolean missingUserIds = false;
        ArrayList<Long> keyIds = new ArrayList<Long>();
        for (String email : encryptionUserIds) {
            Uri uri = KeychainContract.KeyRings.buildPublicKeyRingsByEmailsUri(email);
            Cursor cur = getContentResolver().query(uri, null, null, null, null);
            if (cur.moveToFirst()) {
                long id = cur.getLong(cur.getColumnIndex(KeychainContract.KeyRings.MASTER_KEY_ID));
                keyIds.add(id);
            } else {
                missingUserIds = true;
                Log.d(Constants.TAG, "user id missing");
            }
            if (cur.moveToNext()) {
                manySameUserIds = true;
                Log.d(Constants.TAG, "more than one user id with the same email");
            }
        }

        // TODO: show selection activity on missingUserIds or manySameUserIds

        return keyIds;
    }

    private synchronized void encryptAndSignSafe(byte[] inputBytes, String[] encryptionUserIds,
            ICryptoCallback callback, AppSettings appSettings, boolean sign) throws RemoteException {

        try {
            String passphrase = null;
            if (sign) {
                passphrase = getCachedPassphrase(appSettings.getKeyId());
            }

            // build InputData and write into OutputStream
            InputStream inputStream = new ByteArrayInputStream(inputBytes);
            long inputLength = inputBytes.length;
            InputData inputData = new InputData(inputStream, inputLength);

            OutputStream outputStream = new ByteArrayOutputStream();

            ArrayList<Long> keyIds = getKeyIdsFromEmails(encryptionUserIds);

            // also encrypt to our self (so that we can decrypt it later!)
            keyIds.add(appSettings.getKeyId());

            if (sign) {
                PgpMain.encryptAndSign(mContext, null, inputData, outputStream,
                        appSettings.isAsciiArmor(), appSettings.getCompression(), keyIds, null,
                        appSettings.getEncryptionAlgorithm(), appSettings.getKeyId(),
                        appSettings.getHashAlgorithm(), true, passphrase);
            } else {
                PgpMain.encryptAndSign(mContext, null, inputData, outputStream,
                        appSettings.isAsciiArmor(), appSettings.getCompression(), keyIds, null,
                        appSettings.getEncryptionAlgorithm(), Id.key.none,
                        appSettings.getHashAlgorithm(), true, null);
            }

            outputStream.close();

            byte[] outputBytes = ((ByteArrayOutputStream) outputStream).toByteArray();

            // return over handler on client side
            callback.onSuccess(outputBytes, null);
        } catch (Exception e) {
            Log.e(Constants.TAG, "KeychainService, Exception!", e);

            try {
                callback.onError(new CryptoError(0, e.getMessage()));
            } catch (Exception t) {
                Log.e(Constants.TAG, "Error returning exception to client", t);
            }
        }

    }

    private void signSafe(byte[] inputBytes, ICryptoCallback callback, AppSettings appSettings)
            throws RemoteException {
        // TODO!
    }

    private synchronized void decryptAndVerifySafe(byte[] inputBytes, ICryptoCallback callback,
            AppSettings appSettings) throws RemoteException {
        try {
            // build InputData and write into OutputStream
            InputStream inputStream = new ByteArrayInputStream(inputBytes);
            long inputLength = inputBytes.length;
            InputData inputData = new InputData(inputStream, inputLength);

            Log.d(Constants.TAG, "in: " + new String(inputBytes));

            OutputStream outputStream = new ByteArrayOutputStream();

            // TODO: This allows to decrypt messages with ALL secret keys, not only the one for the
            // app, Fix this?
            long secretKeyId = PgpMain.getDecryptionKeyId(mContext, inputStream);
            if (secretKeyId == Id.key.none) {
                throw new PgpMain.PgpGeneralException(getString(R.string.error_noSecretKeyFound));
            }

            Log.d(Constants.TAG, "Got input:\n" + new String(inputBytes));

            Log.d(Constants.TAG, "secretKeyId " + secretKeyId);

            String passphrase = getCachedPassphrase(secretKeyId);

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

            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        encryptAndSignSafe(inputBytes, encryptionUserIds, callback, settings, false);
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "CryptoService", e);
                    }
                }
            };

            checkAndEnqueue(r);
        }

        @Override
        public void encryptAndSign(final byte[] inputBytes, final String[] encryptionUserIds,
                final ICryptoCallback callback) throws RemoteException {

            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        encryptAndSignSafe(inputBytes, encryptionUserIds, callback, settings, true);
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "CryptoService", e);
                    }
                }
            };

            checkAndEnqueue(r);
        }

        @Override
        public void sign(final byte[] inputBytes, final ICryptoCallback callback)
                throws RemoteException {
            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        signSafe(inputBytes, callback, settings);
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "CryptoService", e);
                    }
                }
            };

            checkAndEnqueue(r);

        }

        @Override
        public void decryptAndVerify(final byte[] inputBytes, final ICryptoCallback callback)
                throws RemoteException {

            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {
                        decryptAndVerifySafe(inputBytes, callback, settings);
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
                if (isPackageAllowed(packageName, false)) {
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

        @Override
        public void onSelectedPublicKeys(long[] keyIds) throws RemoteException {
            // TODO Auto-generated method stub
            
        }

    };

    private void checkAndEnqueue(Runnable r) {
        if (isCallerAllowed(false)) {
            mThreadPool.execute(r);

            Log.d(Constants.TAG, "Enqueued runnable…");
        } else {
            String[] callingPackages = getPackageManager()
                    .getPackagesForUid(Binder.getCallingUid());

            Log.e(Constants.TAG, "Not allowed to use service! Starting activity for registration!");
            Bundle extras = new Bundle();
            // TODO: currently simply uses first entry
            extras.putString(CryptoServiceActivity.EXTRA_PACKAGE_NAME, callingPackages[0]);
            pauseQueueAndStartServiceActivity(CryptoServiceActivity.ACTION_REGISTER, extras);

            mThreadPool.execute(r);

            Log.d(Constants.TAG, "Enqueued runnable…");
        }
    }

    /**
     * Checks if process that binds to this service (i.e. the package name corresponding to the
     * process) is in the list of allowed package names.
     * 
     * @param allowOnlySelf
     *            allow only Keychain app itself
     * @return true if process is allowed to use this service
     */
    private boolean isCallerAllowed(boolean allowOnlySelf) {
        String[] callingPackages = getPackageManager().getPackagesForUid(Binder.getCallingUid());

        // is calling package allowed to use this service?
        for (int i = 0; i < callingPackages.length; i++) {
            String currentPkg = callingPackages[i];

            if (isPackageAllowed(currentPkg, allowOnlySelf)) {
                return true;
            }
        }

        Log.d(Constants.TAG, "Caller is NOT allowed!");
        return false;
    }

    private AppSettings getAppSettings() {
        String[] callingPackages = getPackageManager().getPackagesForUid(Binder.getCallingUid());

        // is calling package allowed to use this service?
        for (int i = 0; i < callingPackages.length; i++) {
            String currentPkg = callingPackages[i];

            Uri uri = KeychainContract.ApiApps.buildByPackageNameUri(currentPkg);

            AppSettings settings = ProviderHelper.getApiAppSettings(this, uri);

            return settings;
        }

        return null;
    }

    /**
     * Checks if packageName is a registered app for the API.
     * 
     * @param packageName
     * @param allowOnlySelf
     *            allow only Keychain app itself
     * @return
     */
    private boolean isPackageAllowed(String packageName, boolean allowOnlySelf) {
        Log.d(Constants.TAG, "packageName: " + packageName);

        ArrayList<String> allowedPkgs = ProviderHelper.getRegisteredApiApps(mContext);
        Log.d(Constants.TAG, "allowed: " + allowedPkgs);

        // check if package is allowed to use our service
        if (allowedPkgs.contains(packageName) && (!allowOnlySelf)) {
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
        Intent intent = new Intent(getBaseContext(), CryptoServiceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(action);
        if (extras != null) {
            intent.putExtras(extras);
        }
        getApplication().startActivity(intent);
    }

}
