/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.thialfihar.android.apg.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;

import org.spongycastle.openpgp.PGPException;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.helper.PGPMain;
import org.thialfihar.android.apg.helper.PGPMain.ApgGeneralException;
import org.thialfihar.android.apg.service.handler.IApgDecryptHandler;
import org.thialfihar.android.apg.service.handler.IApgEncryptHandler;
import org.thialfihar.android.apg.service.handler.IApgGetDecryptionKeyIdHandler;
import org.thialfihar.android.apg.util.InputData;
import org.thialfihar.android.apg.util.Log;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * TODO:
 * 
 * - is this service thread safe? Probably not!
 * 
 */
public class ApgApiService extends Service {
    Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d(Constants.TAG, "ApgApiService, onCreate()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "ApgApiService, onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // private static void writeToOutputStream(InputStream is, OutputStream os) throws IOException {
    // byte[] buffer = new byte[8];
    // int len = 0;
    // while ((len = is.read(buffer)) != -1) {
    // os.write(buffer, 0, len);
    // }
    // }

    private void encryptAndSignImplementation(byte[] inputBytes, String inputUri,
            boolean useAsciiArmor, int compression, long[] encryptionKeyIds,
            String encryptionPassphrase, int symmetricEncryptionAlgorithm, long signatureKeyId,
            int signatureHashAlgorithm, boolean signatureForceV3, String signaturePassphrase,
            IApgEncryptHandler handler) throws RemoteException {

        try {
            // TODO use inputUri

            // InputStream inStream = null;
            // if (isBlob) {
            // ContentResolver cr = getContentResolver();
            // try {
            // inStream = cr.openInputStream(Uri.parse(pArgs.getString(arg.BLOB.name())));
            // } catch (Exception e) {
            // Log.e(TAG, "... exception on opening blob", e);
            // }
            // } else {
            // inStream = new ByteArrayInputStream(pArgs.getString(arg.MESSAGE.name()).getBytes());
            // }
            // InputData in = new InputData(inStream, 0); // XXX Size second param?

            // build InputData and write into OutputStream
            InputStream inputStream = new ByteArrayInputStream(inputBytes);
            long inputLength = inputBytes.length;
            InputData input = new InputData(inputStream, inputLength);

            OutputStream output = new ByteArrayOutputStream();

            PGPMain.encryptAndSign(mContext, null, input, output, useAsciiArmor, compression,
                    encryptionKeyIds, encryptionPassphrase, symmetricEncryptionAlgorithm,
                    signatureKeyId, signatureHashAlgorithm, signatureForceV3, signaturePassphrase);

            output.close();

            // if (isBlob) {
            // ContentResolver cr = getContentResolver();
            // try {
            // OutputStream outStream = cr.openOutputStream(Uri.parse(pArgs.getString(arg.BLOB
            // .name())));
            // writeToOutputStream(new ByteArrayInputStream(out.toString().getBytes()), outStream);
            // outStream.close();
            // } catch (Exception e) {
            // Log.e(TAG, "... exception on writing blob", e);
            // }
            // } else {
            // pReturn.putString(ret.RESULT.name(), out.toString());
            // }

            byte[] outputBytes = ((ByteArrayOutputStream) output).toByteArray();

            // return over handler on client side
            handler.onSuccess(outputBytes, null);
        } catch (Exception e) {
            Log.e(Constants.TAG, "ApgService, Exception!", e);

            try {
                handler.onException(getExceptionId(e), e.getMessage());
            } catch (Exception t) {
                Log.e(Constants.TAG, "Error returning exception to client", t);
            }
        }
    }

    private void decryptAndVerifyImplementation(byte[] inputBytes, String inputUri,
            String passphrase, boolean assumeSymmetric, IApgDecryptHandler handler)
            throws RemoteException {

        try {
            // build InputData and write into OutputStream
            InputStream inputStream = new ByteArrayInputStream(inputBytes);
            long inputLength = inputBytes.length;
            InputData inputData = new InputData(inputStream, inputLength);

            OutputStream outputStream = new ByteArrayOutputStream();

            Bundle outputBundle = PGPMain.decryptAndVerify(mContext, null, inputData, outputStream,
                    passphrase, assumeSymmetric);

            outputStream.close();

            byte[] outputBytes = ((ByteArrayOutputStream) outputStream).toByteArray();

            // get signature informations from bundle
            boolean signature = outputBundle.getBoolean(ApgIntentService.RESULT_SIGNATURE);
            long signatureKeyId = outputBundle.getLong(ApgIntentService.RESULT_SIGNATURE_KEY_ID);
            String signatureUserId = outputBundle
                    .getString(ApgIntentService.RESULT_SIGNATURE_USER_ID);
            boolean signatureSuccess = outputBundle
                    .getBoolean(ApgIntentService.RESULT_SIGNATURE_SUCCESS);
            boolean signatureUnknown = outputBundle
                    .getBoolean(ApgIntentService.RESULT_SIGNATURE_UNKNOWN);

            // return over handler on client side
            handler.onSuccess(outputBytes, null, signature, signatureKeyId, signatureUserId,
                    signatureSuccess, signatureUnknown);
        } catch (Exception e) {
            Log.e(Constants.TAG, "ApgService, Exception!", e);

            try {
                handler.onException(getExceptionId(e), e.getMessage());
            } catch (Exception t) {
                Log.e(Constants.TAG, "Error returning exception to client", t);
            }
        }
    }

    private void getDecryptionKeyImplementation(byte[] inputBytes, String inputUri,
            IApgGetDecryptionKeyIdHandler handler) {

        // TODO: implement inputUri

        try {
            InputStream inputStream = new ByteArrayInputStream(inputBytes);

            long secretKeyId = Id.key.none;
            boolean symmetric;

            try {
                secretKeyId = PGPMain.getDecryptionKeyId(ApgApiService.this, inputStream);
                if (secretKeyId == Id.key.none) {
                    throw new ApgGeneralException(getString(R.string.error_noSecretKeyFound));
                }
                symmetric = false;
            } catch (PGPMain.NoAsymmetricEncryptionException e) {
                secretKeyId = Id.key.symmetric;
                if (!PGPMain.hasSymmetricEncryption(ApgApiService.this, inputStream)) {
                    throw new ApgGeneralException(getString(R.string.error_noKnownEncryptionFound));
                }
                symmetric = true;
            }

            handler.onSuccess(secretKeyId, symmetric);

        } catch (Exception e) {
            Log.e(Constants.TAG, "ApgService, Exception!", e);

            try {
                handler.onException(getExceptionId(e), e.getMessage());
            } catch (Exception t) {
                Log.e(Constants.TAG, "Error returning exception to client", t);
            }
        }
    }

    /**
     * This is the implementation of the interface IApgService. All methods are oneway, meaning
     * asynchronous and return to the client using IApgHandler.
     * 
     * The real PGP code is located in PGPMain.
     */
    private final IApgApiService.Stub mBinder = new IApgApiService.Stub() {

        @Override
        public void encryptAsymmetric(byte[] inputBytes, String inputUri, boolean useAsciiArmor,
                int compression, long[] encryptionKeyIds, int symmetricEncryptionAlgorithm,
                IApgEncryptHandler handler) throws RemoteException {

            encryptAndSignImplementation(inputBytes, inputUri, useAsciiArmor, compression,
                    encryptionKeyIds, null, symmetricEncryptionAlgorithm, Id.key.none, 0, false,
                    null, handler);
        }

        @Override
        public void encryptSymmetric(byte[] inputBytes, String inputUri, boolean useAsciiArmor,
                int compression, String encryptionPassphrase, int symmetricEncryptionAlgorithm,
                IApgEncryptHandler handler) throws RemoteException {

            encryptAndSignImplementation(inputBytes, inputUri, useAsciiArmor, compression, null,
                    encryptionPassphrase, symmetricEncryptionAlgorithm, Id.key.none, 0, false,
                    null, handler);
        }

        @Override
        public void encryptAndSignAsymmetric(byte[] inputBytes, String inputUri,
                boolean useAsciiArmor, int compression, long[] encryptionKeyIds,
                int symmetricEncryptionAlgorithm, long signatureKeyId, int signatureHashAlgorithm,
                boolean signatureForceV3, String signaturePassphrase, IApgEncryptHandler handler)
                throws RemoteException {

            encryptAndSignImplementation(inputBytes, inputUri, useAsciiArmor, compression,
                    encryptionKeyIds, null, symmetricEncryptionAlgorithm, signatureKeyId,
                    signatureHashAlgorithm, signatureForceV3, signaturePassphrase, handler);
        }

        @Override
        public void encryptAndSignSymmetric(byte[] inputBytes, String inputUri,
                boolean useAsciiArmor, int compression, String encryptionPassphrase,
                int symmetricEncryptionAlgorithm, long signatureKeyId, int signatureHashAlgorithm,
                boolean signatureForceV3, String signaturePassphrase, IApgEncryptHandler handler)
                throws RemoteException {

            encryptAndSignImplementation(inputBytes, inputUri, useAsciiArmor, compression, null,
                    encryptionPassphrase, symmetricEncryptionAlgorithm, signatureKeyId,
                    signatureHashAlgorithm, signatureForceV3, signaturePassphrase, handler);
        }

        @Override
        public void decryptAndVerifyAsymmetric(byte[] inputBytes, String inputUri,
                String keyPassphrase, IApgDecryptHandler handler) throws RemoteException {

            decryptAndVerifyImplementation(inputBytes, inputUri, keyPassphrase, false, handler);
        }

        @Override
        public void decryptAndVerifySymmetric(byte[] inputBytes, String inputUri,
                String encryptionPassphrase, IApgDecryptHandler handler) throws RemoteException {

            decryptAndVerifyImplementation(inputBytes, inputUri, encryptionPassphrase, true,
                    handler);
        }

        @Override
        public void getDecryptionKeyId(byte[] inputBytes, String inputUri,
                IApgGetDecryptionKeyIdHandler handler) throws RemoteException {

            getDecryptionKeyImplementation(inputBytes, inputUri, handler);
        }

    };

    /**
     * As we can not throw an exception through Android RPC, we assign identifiers to the exception
     * types.
     * 
     * @param e
     * @return
     */
    private int getExceptionId(Exception e) {
        if (e instanceof NoSuchProviderException) {
            return 0;
        } else if (e instanceof NoSuchAlgorithmException) {
            return 1;
        } else if (e instanceof SignatureException) {
            return 2;
        } else if (e instanceof IOException) {
            return 3;
        } else if (e instanceof ApgGeneralException) {
            return 4;
        } else if (e instanceof PGPException) {
            return 5;
        } else {
            return -1;
        }
    }

}
