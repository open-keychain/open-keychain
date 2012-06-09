/*
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

package org.thialfihar.android.apg;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.Apg.GeneralException;

import android.content.Context;
import android.os.Environment;

public class DataDestination implements Serializable {

    private static final long serialVersionUID = -6478075911319320498L;
    
    private String mStreamFilename;
    private String mFilename;
    private int mMode = Id.mode.undefined;

    public DataDestination() {

    }

    public void setMode(int mode) {
        mMode = mode;
    }

    public void setFilename(String filename) {
        mFilename = filename;
    }

    public String getStreamFilename() {
        return mStreamFilename;
    }

    public OutputStream getOutputStream(Context context) throws Apg.GeneralException,
            FileNotFoundException, IOException {
        OutputStream out = null;
        mStreamFilename = null;

        switch (mMode) {
        case Id.mode.stream: {
            try {
                while (true) {
                    mStreamFilename = Apg.generateRandomString(32);
                    if (mStreamFilename == null) {
                        throw new Apg.GeneralException("couldn't generate random file name");
                    }
                    context.openFileInput(mStreamFilename).close();
                }
            } catch (FileNotFoundException e) {
                // found a name that isn't used yet
            }
            out = context.openFileOutput(mStreamFilename, Context.MODE_PRIVATE);
            break;
        }

        case Id.mode.byte_array: {
            out = new ByteArrayOutputStream();
            break;
        }

        case Id.mode.file: {
            if (mFilename.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    throw new GeneralException(
                            context.getString(R.string.error_externalStorageNotReady));
                }
            }
            out = new FileOutputStream(mFilename);
            break;
        }

        default: {
            break;
        }
        }

        return out;
    }
}
