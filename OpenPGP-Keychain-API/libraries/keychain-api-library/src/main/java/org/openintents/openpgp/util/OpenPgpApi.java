/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import org.openintents.openpgp.IOpenPgpService;
import org.openintents.openpgp.OpenPgpError;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class OpenPgpApi {

    IOpenPgpService mService;

    public OpenPgpApi(IOpenPgpService service) {
        this.mService = service;
    }

    public Bundle sign(InputStream is, final OutputStream os) {
        try {
            // send the input and output pfds
            ParcelFileDescriptor input = ParcelFileDescriptorUtil.pipeFrom(is,
                    new ParcelFileDescriptorUtil.IThreadListener() {

                        @Override
                        public void onThreadFinished(Thread thread) {
                            Log.d(OpenPgpConstants.TAG, "Copy to service finished");
                        }
                    });
            ParcelFileDescriptor output = ParcelFileDescriptorUtil.pipeTo(os,
                    new ParcelFileDescriptorUtil.IThreadListener() {

                        @Override
                        public void onThreadFinished(Thread thread) {
                            Log.d(OpenPgpConstants.TAG, "Service finished writing!");
                        }
                    });

            // blocks until result is ready
            Bundle result = mService.sign(null, input, output);
            // close() is required to halt the TransferThread
            output.close();

            return result;
        } catch (RemoteException e) {
            Log.e(OpenPgpConstants.TAG, "RemoteException", e);
            Bundle result = new Bundle();
            result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_ERROR);
            result.putParcelable(OpenPgpConstants.RESULT_ERRORS,
                    new OpenPgpError(OpenPgpError.CLIENT_SIDE_ERROR, e.getMessage()));
            return result;
        } catch (IOException e) {
            Log.e(OpenPgpConstants.TAG, "IOException", e);
            Bundle result = new Bundle();
            result.putInt(OpenPgpConstants.RESULT_CODE, OpenPgpConstants.RESULT_CODE_ERROR);
            result.putParcelable(OpenPgpConstants.RESULT_ERRORS,
                    new OpenPgpError(OpenPgpError.CLIENT_SIDE_ERROR, e.getMessage()));
            return result;
        }
    }


}
