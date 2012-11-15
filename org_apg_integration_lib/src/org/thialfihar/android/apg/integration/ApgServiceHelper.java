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

package org.thialfihar.android.apg.integration;

import java.io.InputStream;
import java.io.OutputStream;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class ApgServiceHelper {

    private final static String BLOB_URI = "content://org.thialfihar.android.apg.provider.apgserviceblobprovider";

    private Context context;

    public ApgServiceHelper(Context context) {
        this.context = context;
    }
    
    /**
     * Set up binary data to en/decrypt
     * 
     * @param is
     *            InputStream to get the data from
     */
    public void setBlob(InputStream is) {
        Log.d(Constants.TAG, "setBlob() called");
        // 1. get the new contentUri
        ContentResolver cr = context.getContentResolver();
        Uri contentUri = cr.insert(Uri.parse(BLOB_URI), new ContentValues());

        // 2. insert binary data
        OutputStream os = null;
        try {
            os = cr.openOutputStream(contentUri, "w");
        } catch (Exception e) {
            Log.e(Constants.TAG, "... exception on setBlob", e);
        }

        byte[] buffer = new byte[8];
        int len = 0;
        try {
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            Log.d(Constants.TAG, "... write finished, now closing");
            os.close();
        } catch (Exception e) {
            Log.e(Constants.TAG, "... error on writing buffer", e);
        }

//        mArgs.putString("BLOB", contentUri.toString());
    }

    /**
     * Get the binary result
     * 
     * <p>
     * This gets your binary result. It only works if you called {@link #setBlob(InputStream)}
     * before.
     * 
     * If you did not call encrypt nor decrypt, this will be the same data as you inputed.
     * </p>
     * 
     * @return InputStream of the binary data which was en/decrypted
     * 
     * @see #setBlob(InputStream)
     * @see #getResult()
     */
    public InputStream getBlobResult() {
        // if (mArgs.containsKey("BLOB")) {
        ContentResolver cr = context.getContentResolver();
        InputStream in = null;
        try {
//            in = cr.openInputStream(Uri.parse(mArgs.getString("BLOB")));
        } catch (Exception e) {
            Log.e(Constants.TAG, "Could not return blob in result", e);
        }
        return in;
        // } else {
        // return null;
        // }
    }
}
