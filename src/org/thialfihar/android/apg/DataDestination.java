package org.thialfihar.android.apg;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.thialfihar.android.apg.Apg.GeneralException;

import android.content.Context;
import android.os.Environment;

public class DataDestination {
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

    protected OutputStream getOutputStream(Context context)
            throws Apg.GeneralException, FileNotFoundException, IOException {
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
                        throw new GeneralException(context.getString(R.string.error_externalStorageNotReady));
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
