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

import java.util.ArrayList;

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.provider.ProviderHelper;
import org.thialfihar.android.apg.service.handler.IApgGetKeyringsHandler;
import org.thialfihar.android.apg.util.Log;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * TODO:
 * 
 * - is this service thread safe?
 * 
 */
public class ApgKeyService extends Service {
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
            boolean asAsciiArmoredStringArray, IApgGetKeyringsHandler handler)
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
            boolean asAsciiArmoredStringArray, IApgGetKeyringsHandler handler)
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
    private final IApgKeyService.Stub mBinder = new IApgKeyService.Stub() {

        @Override
        public void getPublicKeyRings(long[] masterKeyIds, boolean asAsciiArmoredStringArray,
                IApgGetKeyringsHandler handler) throws RemoteException {
            getPublicKeyRingsSafe(masterKeyIds, asAsciiArmoredStringArray, handler);
        }

        @Override
        public void getSecretKeyRings(long[] masterKeyIds, boolean asAsciiArmoredStringArray,
                IApgGetKeyringsHandler handler) throws RemoteException {
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
