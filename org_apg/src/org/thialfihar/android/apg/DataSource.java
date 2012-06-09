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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.Apg.GeneralException;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

public class DataSource implements Serializable {

    private static final long serialVersionUID = 2377217399907415255L;

    private Uri mContentUri = null;
    private String mText = null;
    private byte[] mData = null;

    public DataSource() {

    }

    public void setUri(Uri uri) {
        mContentUri = uri;
        mText = null;
        mData = null;
    }

    public void setUri(String uri) {
        if (uri.startsWith("/")) {
            setUri(Uri.parse("file://" + uri));
        } else {
            setUri(Uri.parse(uri));
        }
    }

    public Uri getUri() {
        return mContentUri;
    }

    public void setText(String text) {
        mText = text;
        mData = null;
        mContentUri = null;
    }

    public void setData(byte[] data) {
        mData = data;
        mText = null;
        mContentUri = null;
    }

    public boolean isText() {
        return mText != null;
    }

    public boolean isBinary() {
        return mData != null || mContentUri != null;
    }

    public byte[] getBytes() {
        byte[] bytes = null;
        if (mData != null) {
            bytes = mData;
        } else {
            bytes = mText.getBytes();
        }

        return bytes;
    }

    public InputData getInputData(Context context, boolean withSize) throws GeneralException,
            FileNotFoundException, IOException {
        InputStream in = null;
        long size = 0;

        if (mContentUri != null) {
            if (mContentUri.getScheme().equals("file")) {
                // get the rest after "file://"
                String path = Uri.decode(mContentUri.toString().substring(7));
                if (path.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        throw new GeneralException(
                                context.getString(R.string.error_externalStorageNotReady));
                    }
                }
                in = new FileInputStream(path);
                File file = new File(path);
                if (withSize) {
                    size = file.length();
                }
            } else {
                in = context.getContentResolver().openInputStream(mContentUri);
                if (withSize) {
                    InputStream tmp = context.getContentResolver().openInputStream(mContentUri);
                    size = Apg.getLengthOfStream(tmp);
                    tmp.close();
                }
            }
        } else if (mText != null || mData != null) {
            byte[] bytes = null;
            if (mData != null) {
                bytes = mData;
            } else {
                bytes = mText.getBytes();
            }
            in = new ByteArrayInputStream(bytes);
            if (withSize) {
                size = bytes.length;
            }
        }

        return new InputData(in, size);
    }

}
