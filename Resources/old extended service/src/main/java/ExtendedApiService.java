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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;

import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openssl.PEMWriter;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.pgp.PgpToX509;
import org.sufficientlysecure.keychain.util.Log;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class ExtendedApiService extends RemoteService {

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void selfSignedX509CertSafe(String subjAltNameURI, IExtendedApiCallback callback,
            AppSettings appSettings) {

        // TODO: for pgp keyrings with password
        CallbackHandler pgpPwdCallbackHandler = new PgpToX509.PredefinedPasswordCallbackHandler("");

        try {
            long keyId = appSettings.getKeyId();
            PGPSecretKey pgpSecretKey = PgpKeyHelper.getSigningKey(this, keyId);

            PasswordCallback pgpSecKeyPasswordCallBack = new PasswordCallback("pgp passphrase?",
                    false);
            pgpPwdCallbackHandler.handle(new Callback[] { pgpSecKeyPasswordCallBack });
            PGPPrivateKey pgpPrivKey = pgpSecretKey.extractPrivateKey(
                    pgpSecKeyPasswordCallBack.getPassword(), Constants.BOUNCY_CASTLE_PROVIDER_NAME);
            pgpSecKeyPasswordCallBack.clearPassword();

            X509Certificate selfSignedCert = PgpToX509.createSelfSignedCert(pgpSecretKey,
                    pgpPrivKey, subjAltNameURI);

            // Write x509cert and privKey into files
            // FileOutputStream fosCert = context.openFileOutput(CERT_FILENAME,
            // Context.MODE_PRIVATE);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            PEMWriter pemWriterCert = new PEMWriter(new PrintWriter(outStream));
            pemWriterCert.writeObject(selfSignedCert);
            pemWriterCert.close();

            byte[] outputBytes = outStream.toByteArray();

            callback.onSuccess(outputBytes);
        } catch (Exception e) {
            Log.e(Constants.TAG, "ExtendedApiService", e);
            try {
                callback.onError(e.getMessage());
            } catch (RemoteException e1) {
                Log.e(Constants.TAG, "ExtendedApiService", e);
            }
        }

        // TODO: no private key at the moment! Don't give it to others
        // PrivateKey privKey = pgpPrivKey.getKey();
        // FileOutputStream fosKey = context.openFileOutput(PRIV_KEY_FILENAME,
        // Context.MODE_PRIVATE);
        // PEMWriter pemWriterKey = new PEMWriter(new PrintWriter(fosKey));
        // pemWriterKey.writeObject(privKey);
        // pemWriterKey.close();
    }

    private final IExtendedApiService.Stub mBinder = new IExtendedApiService.Stub() {

        @Override
        public void encrypt(byte[] inputBytes, String passphrase, IExtendedApiCallback callback)
                throws RemoteException {
            // TODO : implement

        }

        @Override
        public void selfSignedX509Cert(final String subjAltNameURI,
                final IExtendedApiCallback callback) throws RemoteException {
            final AppSettings settings = getAppSettings();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    selfSignedX509CertSafe(subjAltNameURI, callback, settings);
                }
            };

            checkAndEnqueue(r);
        }

    };

}
