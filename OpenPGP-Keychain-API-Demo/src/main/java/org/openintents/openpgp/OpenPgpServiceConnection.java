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

package org.openintents.openpgp;

import org.openintents.openpgp.IOpenPgpService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class OpenPgpServiceConnection {
    private Context mApplicationContext;

    private IOpenPgpService mService;
    private boolean mBound;
    private String mCryptoProviderPackageName;

    public OpenPgpServiceConnection(Context context, String cryptoProviderPackageName) {
        this.mApplicationContext = context.getApplicationContext();
        this.mCryptoProviderPackageName = cryptoProviderPackageName;
    }

    public IOpenPgpService getService() {
        return mService;
    }

    public boolean isBound() {
        return mBound;
    }

    private ServiceConnection mCryptoServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IOpenPgpService.Stub.asInterface(service);
            Log.d(OpenPgpConstants.TAG, "connected to service");
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            Log.d(OpenPgpConstants.TAG, "disconnected from service");
            mBound = false;
        }
    };

    /**
     * If not already bound, bind!
     * 
     * @return
     */
    public boolean bindToService() {
        if (mService == null && !mBound) { // if not already connected
            try {
                Log.d(OpenPgpConstants.TAG, "not bound yet");

                Intent serviceIntent = new Intent();
                serviceIntent.setAction(IOpenPgpService.class.getName());
                serviceIntent.setPackage(mCryptoProviderPackageName);
                mApplicationContext.bindService(serviceIntent, mCryptoServiceConnection,
                        Context.BIND_AUTO_CREATE);

                return true;
            } catch (Exception e) {
                Log.d(OpenPgpConstants.TAG, "Exception on binding", e);
                return false;
            }
        } else {
            Log.d(OpenPgpConstants.TAG, "already bound");
            return true;
        }
    }

    public void unbindFromService() {
        mApplicationContext.unbindService(mCryptoServiceConnection);
    }

}
