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

package org.sufficientlysecure.keychain.crypto_provider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.helper.PgpMain;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;

import com.android.crypto.CryptoError;
import com.android.crypto.ICryptoCallback;
import com.android.crypto.ICryptoService;
import com.android.crypto.CryptoSignatureResult;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

public class CryptoService extends Service {
    Context mContext;

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
        return mBinder;
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
            
            Log.d(Constants.TAG, "Got input:\n"+new String(inputBytes));

            Log.d(Constants.TAG, "secretKeyId " + secretKeyId);

            String passphrase = PassphraseCacheService.getCachedPassphrase(mContext, secretKeyId);

            if (passphrase == null) {
                Log.d(Constants.TAG, "No passphrase! Activity required!");
                // No passphrase cached for this ciphertext! Intent required to cache
                // passphrase!
                Intent intent = new Intent(CryptoActivity.ACTION_CACHE_PASSPHRASE);
                intent.putExtra(CryptoActivity.EXTRA_SECRET_KEY_ID, secretKeyId);
                callback.onActivityRequired(intent);
                return;
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
            callback.onDecryptVerifySuccess(outputBytes, sigResult);
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
        public void encrypt(byte[] inputBytes, String[] encryptionUserIds, ICryptoCallback callback)
                throws RemoteException {
            // TODO Auto-generated method stub

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
        public void decryptAndVerify(byte[] inputBytes, ICryptoCallback callback)
                throws RemoteException {
            decryptAndVerifySafe(inputBytes, callback);
        }

    };

    // /**
    // * As we can not throw an exception through Android RPC, we assign identifiers to the
    // exception
    // * types.
    // *
    // * @param e
    // * @return
    // */
    // private int getExceptionId(Exception e) {
    // if (e instanceof NoSuchProviderException) {
    // return 0;
    // } else if (e instanceof NoSuchAlgorithmException) {
    // return 1;
    // } else if (e instanceof SignatureException) {
    // return 2;
    // } else if (e instanceof IOException) {
    // return 3;
    // } else if (e instanceof PgpGeneralException) {
    // return 4;
    // } else if (e instanceof PGPException) {
    // return 5;
    // } else {
    // return -1;
    // }
    // }

}
