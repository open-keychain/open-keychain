/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.remote;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import org.openintents.openpgp.IOpenPgpService2;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OpenPgpService2 extends OpenPgpService {

    private Map<Long, ParcelFileDescriptor> mOutputPipeMap = new HashMap<Long, ParcelFileDescriptor>();

    private long createKey(int id) {
        int callingPid = Binder.getCallingPid();
        return ((long) callingPid << 32) | ((long) id & 0xFFFFFFFL);
    }

    private final IOpenPgpService2.Stub mBinder = new IOpenPgpService2.Stub() {

        @Override
        public ParcelFileDescriptor createOutputPipe(int outputPipeId) {
            try {
                ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
                mOutputPipeMap.put(createKey(outputPipeId), pipe[1]);
                return pipe[0];
            } catch (IOException e) {
                Log.e(Constants.TAG, "IOException in OpenPgpService2", e);
                return null;
            }

        }

        @Override
        public Intent execute(Intent data, ParcelFileDescriptor input, int outputPipeId) {
            long key = createKey(outputPipeId);
            ParcelFileDescriptor output = mOutputPipeMap.get(key);
            mOutputPipeMap.remove(key);
            return executeInternal(data, input, output);
        }

    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
