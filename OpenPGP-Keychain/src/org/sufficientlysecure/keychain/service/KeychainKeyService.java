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

import java.util.ArrayList;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.service.IKeychainKeyService;
import org.sufficientlysecure.keychain.service.handler.IKeychainGetKeyringsHandler;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class KeychainKeyService extends Service {
    Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d(Constants.TAG, "ApgKeyService, onCreate()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "ApgKeyService, onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Synchronized implementation of getPublicKeyRings
     */
    private synchronized void getPublicKeyRingsSafe(long[] masterKeyIds,
            boolean asAsciiArmoredStringArray, IKeychainGetKeyringsHandler handler)
            throws RemoteException {
        if (asAsciiArmoredStringArray) {
            ArrayList<String> output = ProviderHelper.getPublicKeyRingsAsArmoredString(mContext,
                    masterKeyIds);

            handler.onSuccess(null, output);
        } else {
            byte[] outputBytes = ProviderHelper
                    .getPublicKeyRingsAsByteArray(mContext, masterKeyIds);
            handler.onSuccess(outputBytes, null);
        }
    }

    /**
     * Synchronized implementation of getSecretKeyRings
     */
    private synchronized void getSecretKeyRingsSafe(long[] masterKeyIds,
            boolean asAsciiArmoredStringArray, IKeychainGetKeyringsHandler handler)
            throws RemoteException {
        if (asAsciiArmoredStringArray) {
            ArrayList<String> output = ProviderHelper.getSecretKeyRingsAsArmoredString(mContext,
                    masterKeyIds);

            handler.onSuccess(null, output);
        } else {
            byte[] outputBytes = ProviderHelper
                    .getSecretKeyRingsAsByteArray(mContext, masterKeyIds);
            handler.onSuccess(outputBytes, null);
        }

    }

    /**
     * This is the implementation of the interface IApgKeyService. All methods are oneway, meaning
     * asynchronous and return to the client using handlers.
     * 
     * The real PGP code is located in PGPMain.
     */
    private final IKeychainKeyService.Stub mBinder = new IKeychainKeyService.Stub() {

        @Override
        public void getPublicKeyRings(long[] masterKeyIds, boolean asAsciiArmoredStringArray,
                IKeychainGetKeyringsHandler handler) throws RemoteException {
            getPublicKeyRingsSafe(masterKeyIds, asAsciiArmoredStringArray, handler);
        }

        @Override
        public void getSecretKeyRings(long[] masterKeyIds, boolean asAsciiArmoredStringArray,
                IKeychainGetKeyringsHandler handler) throws RemoteException {
            getSecretKeyRingsSafe(masterKeyIds, asAsciiArmoredStringArray, handler);
        }

    };

    /**
     * As we can not throw an exception through Android RPC, we assign identifiers to the exception
     * types.
     * 
     * @param e
     * @return
     */
    // private int getExceptionId(Exception e) {
    // if (e instanceof NoSuchProviderException) {
    // return 0;
    // } else if (e instanceof NoSuchAlgorithmException) {
    // return 1;
    // } else if (e instanceof SignatureException) {
    // return 2;
    // } else if (e instanceof IOException) {
    // return 3;
    // } else if (e instanceof ApgGeneralException) {
    // return 4;
    // } else if (e instanceof PGPException) {
    // return 5;
    // } else {
    // return -1;
    // }
    // }

}
