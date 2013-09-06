/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;

import org.spongycastle.openpgp.PGPException;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.helper.PgpMain;
import org.sufficientlysecure.keychain.helper.PgpMain.PgpGeneralException;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.IKeychainApiService;
import org.sufficientlysecure.keychain.service.handler.IKeychainDecryptHandler;
import org.sufficientlysecure.keychain.service.handler.IKeychainEncryptHandler;
import org.sufficientlysecure.keychain.service.handler.IKeychainGetDecryptionKeyIdHandler;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

public class KeychainApiService extends Service {
    Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d(Constants.TAG, "KeychainApiService, onCreate()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "KeychainApiService, onDestroy()");
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

    private synchronized void encryptAndSignSafe(byte[] inputBytes, String inputUri,
            boolean useAsciiArmor, int compression, long[] encryptionKeyIds,
            String encryptionPassphrase, int symmetricEncryptionAlgorithm, long signatureKeyId,
            int signatureHashAlgorithm, boolean signatureForceV3, String signaturePassphrase,
            IKeychainEncryptHandler handler) throws RemoteException {

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

            PgpMain.encryptAndSign(mContext, null, input, output, useAsciiArmor, compression,
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
            Log.e(Constants.TAG, "KeychainService, Exception!", e);

            try {
                handler.onException(getExceptionId(e), e.getMessage());
            } catch (Exception t) {
                Log.e(Constants.TAG, "Error returning exception to client", t);
            }
        }
    }

    private synchronized void decryptAndVerifySafe(byte[] inputBytes, String inputUri,
            String passphrase, boolean assumeSymmetric, IKeychainDecryptHandler handler)
            throws RemoteException {

        try {
            // build InputData and write into OutputStream
            InputStream inputStream = new ByteArrayInputStream(inputBytes);
            long inputLength = inputBytes.length;
            InputData inputData = new InputData(inputStream, inputLength);

            OutputStream outputStream = new ByteArrayOutputStream();

            Bundle outputBundle = PgpMain.decryptAndVerify(mContext, null, inputData, outputStream,
                    passphrase, assumeSymmetric);

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

            // return over handler on client side
            handler.onSuccess(outputBytes, null, signature, signatureKeyId, signatureUserId,
                    signatureSuccess, signatureUnknown);
        } catch (Exception e) {
            Log.e(Constants.TAG, "KeychainService, Exception!", e);

            try {
                handler.onException(getExceptionId(e), e.getMessage());
            } catch (Exception t) {
                Log.e(Constants.TAG, "Error returning exception to client", t);
            }
        }
    }

    private synchronized void getDecryptionKeySafe(byte[] inputBytes, String inputUri,
            IKeychainGetDecryptionKeyIdHandler handler) {

        // TODO: implement inputUri

        try {
            InputStream inputStream = new ByteArrayInputStream(inputBytes);

            long secretKeyId = Id.key.none;
            boolean symmetric;

            try {
                secretKeyId = PgpMain.getDecryptionKeyId(KeychainApiService.this, inputStream);
                if (secretKeyId == Id.key.none) {
                    throw new PgpGeneralException(getString(R.string.error_noSecretKeyFound));
                }
                symmetric = false;
            } catch (PgpMain.NoAsymmetricEncryptionException e) {
                secretKeyId = Id.key.symmetric;
                if (!PgpMain.hasSymmetricEncryption(KeychainApiService.this, inputStream)) {
                    throw new PgpGeneralException(getString(R.string.error_noKnownEncryptionFound));
                }
                symmetric = true;
            }

            handler.onSuccess(secretKeyId, symmetric);

        } catch (Exception e) {
            Log.e(Constants.TAG, "KeychainService, Exception!", e);

            try {
                handler.onException(getExceptionId(e), e.getMessage());
            } catch (Exception t) {
                Log.e(Constants.TAG, "Error returning exception to client", t);
            }
        }
    }

    /**
     * This is the implementation of the interface IKeychainService. All methods are oneway, meaning
     * asynchronous and return to the client using IKeychainHandler.
     * 
     * The real PGP code is located in PGPMain.
     */
    private final IKeychainApiService.Stub mBinder = new IKeychainApiService.Stub() {

        @Override
        public void encryptAsymmetric(byte[] inputBytes, String inputUri, boolean useAsciiArmor,
                int compression, long[] encryptionKeyIds, int symmetricEncryptionAlgorithm,
                IKeychainEncryptHandler handler) throws RemoteException {

            encryptAndSignSafe(inputBytes, inputUri, useAsciiArmor, compression, encryptionKeyIds,
                    null, symmetricEncryptionAlgorithm, Id.key.none, 0, false, null, handler);
        }

        @Override
        public void encryptSymmetric(byte[] inputBytes, String inputUri, boolean useAsciiArmor,
                int compression, String encryptionPassphrase, int symmetricEncryptionAlgorithm,
                IKeychainEncryptHandler handler) throws RemoteException {

            encryptAndSignSafe(inputBytes, inputUri, useAsciiArmor, compression, null,
                    encryptionPassphrase, symmetricEncryptionAlgorithm, Id.key.none, 0, false,
                    null, handler);
        }

        @Override
        public void encryptAndSignAsymmetric(byte[] inputBytes, String inputUri,
                boolean useAsciiArmor, int compression, long[] encryptionKeyIds,
                int symmetricEncryptionAlgorithm, long signatureKeyId, int signatureHashAlgorithm,
                boolean signatureForceV3, String signaturePassphrase,
                IKeychainEncryptHandler handler) throws RemoteException {

            encryptAndSignSafe(inputBytes, inputUri, useAsciiArmor, compression, encryptionKeyIds,
                    null, symmetricEncryptionAlgorithm, signatureKeyId, signatureHashAlgorithm,
                    signatureForceV3, signaturePassphrase, handler);
        }

        @Override
        public void encryptAndSignSymmetric(byte[] inputBytes, String inputUri,
                boolean useAsciiArmor, int compression, String encryptionPassphrase,
                int symmetricEncryptionAlgorithm, long signatureKeyId, int signatureHashAlgorithm,
                boolean signatureForceV3, String signaturePassphrase,
                IKeychainEncryptHandler handler) throws RemoteException {

            encryptAndSignSafe(inputBytes, inputUri, useAsciiArmor, compression, null,
                    encryptionPassphrase, symmetricEncryptionAlgorithm, signatureKeyId,
                    signatureHashAlgorithm, signatureForceV3, signaturePassphrase, handler);
        }

        @Override
        public void decryptAndVerifyAsymmetric(byte[] inputBytes, String inputUri,
                String keyPassphrase, IKeychainDecryptHandler handler) throws RemoteException {

            decryptAndVerifySafe(inputBytes, inputUri, keyPassphrase, false, handler);
        }

        @Override
        public void decryptAndVerifySymmetric(byte[] inputBytes, String inputUri,
                String encryptionPassphrase, IKeychainDecryptHandler handler)
                throws RemoteException {

            decryptAndVerifySafe(inputBytes, inputUri, encryptionPassphrase, true, handler);
        }

        @Override
        public void getDecryptionKeyId(byte[] inputBytes, String inputUri,
                IKeychainGetDecryptionKeyIdHandler handler) throws RemoteException {

            getDecryptionKeySafe(inputBytes, inputUri, handler);
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
        } else if (e instanceof PgpGeneralException) {
            return 4;
        } else if (e instanceof PGPException) {
            return 5;
        } else {
            return -1;
        }
    }

}
