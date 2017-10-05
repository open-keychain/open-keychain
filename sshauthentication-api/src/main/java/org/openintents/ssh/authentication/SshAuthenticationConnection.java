/*
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
 * Copyright (C) 2017 Jonas Dippel
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

package org.openintents.ssh.authentication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class SshAuthenticationConnection {

    private final Context mContext;
    private final String mTargetPackage;

    private ISshAuthenticationService mSSHAgent;

    /**
     * Callback for signaling connection events
     */
    public interface OnBound {
        /**
         * Called when the connection is bound to the service
         *
         * @param sshAgent the bound remote service stub
         */
        void onBound(ISshAuthenticationService sshAgent);

        /**
         * Called when the connection is disconnected due to some error
         */
        void onError();
    }

    private OnBound mOnBoundListener;

    /**
     * Construct an SshAuthenticationConnection instance with the desired attributes
     *
     * @param context       the application context
     * @param targetPackage the package of the service to bind to
     */
    public SshAuthenticationConnection(Context context, String targetPackage) {
        mContext = context;
        mTargetPackage = targetPackage;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSSHAgent = ISshAuthenticationService.Stub.asInterface(service);
            mOnBoundListener.onBound(mSSHAgent);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSSHAgent = null;
            mOnBoundListener.onError();
        }
    };

    /**
     * Connect to the Service via a Connection
     *
     * @return false if service was not found or an error occured, else true
     */
    public boolean connect(final OnBound onBoundListener) {
        mOnBoundListener = onBoundListener;
		if (mSSHAgent == null) {
            Intent intent = new Intent(SshAuthenticationApi.SERVICE_INTENT);
            intent.setPackage(mTargetPackage);
            return mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            mOnBoundListener.onBound(mSSHAgent);
            return true;
        }
    }

    /**
     * Check whether the Connection is bound to a service
     *
     * @return whether the Connection is bound to a service
     */
    public boolean isConnected() {
        return mSSHAgent != null;
    }

    public void disconnect() {
        mSSHAgent = null;
        mContext.unbindService(mServiceConnection);
    }

    public ISshAuthenticationService getService() {
        return mSSHAgent;
    }
}
