/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.util;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.KeychainApplication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * When sending large data (over 1MB) through Androids Binder IPC you get
 * JavaBinder  E  !!! FAILED BINDER TRANSACTION !!!
 * <p/>
 * To overcome this problem, we cache large Parcelables into a file in our private cache directory
 * instead of sending them through IPC.
 */
public class FileImportCache<E extends Parcelable> {

    private Context mContext;

    private final String mFilename;

    public FileImportCache(Context context, String filename) {
        mContext = context;
        mFilename = filename;
    }

    public void writeCache(ArrayList<E> selectedEntries) throws IOException {
        writeCache(selectedEntries.iterator());
    }

    public void writeCache(Iterator<E> it) throws IOException {

        File cacheDir = mContext.getCacheDir();
        if (cacheDir == null) {
            // https://groups.google.com/forum/#!topic/android-developers/-694j87eXVU
            throw new IOException("cache dir is null!");
        }

        File tempFile = new File(mContext.getCacheDir(), mFilename);

        DataOutputStream oos = new DataOutputStream(new FileOutputStream(tempFile));

        while (it.hasNext()) {
            Parcel p = Parcel.obtain(); // creating empty parcel object
            p.writeParcelable(it.next(), 0); // saving bundle as parcel
            byte[] buf = p.marshall();
            oos.writeInt(buf.length);
            oos.write(buf);
            p.recycle();
        }

        oos.close();

    }

    public List<E> readCacheIntoList() throws IOException {
        ArrayList<E> result = new ArrayList<E>();
        Iterator<E> it = readCache();
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }

    public Iterator<E> readCache() throws IOException {

        File cacheDir = mContext.getCacheDir();
        if (cacheDir == null) {
            // https://groups.google.com/forum/#!topic/android-developers/-694j87eXVU
            throw new IOException("cache dir is null!");
        }

        final File tempFile = new File(cacheDir, mFilename);
        final DataInputStream ois = new DataInputStream(new FileInputStream(tempFile));

        return new Iterator<E>() {

            E mRing = null;
            boolean closed = false;
            byte[] buf = new byte[512];

            private void readNext() {
                if (mRing != null || closed) {
                    return;
                }

                try {

                    int length = ois.readInt();
                    while (buf.length < length) {
                        buf = new byte[buf.length * 2];
                    }
                    ois.readFully(buf, 0, length);

                    Parcel parcel = Parcel.obtain(); // creating empty parcel object
                    parcel.unmarshall(buf, 0, length);
                    parcel.setDataPosition(0);
                    mRing = parcel.readParcelable(KeychainApplication.class.getClassLoader());
                    parcel.recycle();
                } catch (EOFException e) {
                    // aight
                    close();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "Encountered IOException during cache read!", e);
                }

            }

            @Override
            public boolean hasNext() {
                readNext();
                return mRing != null;
            }

            @Override
            public E next() {
                readNext();
                try {
                    return mRing;
                } finally {
                    mRing = null;
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void finalize() throws Throwable {
                close();
                super.finalize();
            }

            private void close() {
                if (!closed) {
                    try {
                        ois.close();
                        tempFile.delete();
                    } catch (IOException e) {
                        // nvm
                    }
                }
                closed = true;
            }


        };
    }
}
