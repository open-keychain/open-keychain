/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.openintents.openpgp.util;


import android.content.Context;

import org.sufficientlysecure.keychain.remote.OpenPgpService;


public class OpenPgpServiceConnection {

    // callback interface
    public interface OnBound {
        void onBound(OpenPgpService service);

        void onError(Exception e);
    }

    private Context mApplicationContext;

    private OpenPgpService mService;

    private OnBound mOnBoundListener;

    /**
     * Create new connection
     *  @param context
     *
     */
    public OpenPgpServiceConnection(Context context) {
        this.mApplicationContext = context.getApplicationContext();
    }

    /**
     * Create new connection with callback
     *
     * @param context
     * @param onBoundListener     callback, executed when connection to service has been established
     */
    public OpenPgpServiceConnection(Context context, OnBound onBoundListener) {
        this(context);
        this.mOnBoundListener = onBoundListener;
    }

    public OpenPgpService getService() {
        return mService;
    }

    public boolean isBound() {
        return (mService != null);
    }

    /**
     * If not already bound, bind to service!
     *
     * @return
     */
    public void bindToService() {
        // if not already bound...
        if (mService == null) {
            mService = new OpenPgpService(mApplicationContext);
            try {
                if (mOnBoundListener != null) {
                    mOnBoundListener.onBound(mService);
                }
            } catch (Exception e) {
                mOnBoundListener.onError(e);
            }
        } else {
            // already bound, but also inform client about it with callback
            if (mOnBoundListener != null) {
                mOnBoundListener.onBound(mService);
            }
        }
    }

    public void unbindFromService() {
        mService = null;
    }

}
